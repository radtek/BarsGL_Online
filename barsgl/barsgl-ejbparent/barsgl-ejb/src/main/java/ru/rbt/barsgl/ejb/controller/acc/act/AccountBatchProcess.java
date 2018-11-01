package ru.rbt.barsgl.ejb.controller.acc.act;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.acc.*;
import ru.rbt.barsgl.ejb.controller.sm.Transition;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchRequest;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.shared.enums.AccountBatchState;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.ExceptionUtils;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.HOURS;
import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountBatch;
import static ru.rbt.ejbcore.mapping.YesNo.N;
import static ru.rbt.ejbcore.mapping.YesNo.Y;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.ejbcore.util.StringUtils.substr;

/**
 * Created by Ivan Sevastyanov on 29.10.2018.
 */
public class AccountBatchProcess extends AbstractAccountBatchAction {

    @EJB
    private CoreRepository repository;

    @EJB
    private AsyncProcessor asyncProcessor;

    @EJB
    private GLAccountController accountController;

    @EJB
    private OperdayController operdayController;

    @EJB
    private AuditController auditController;

    @EJB
    private AccountBatchSupportBean batchSupportBean;

    @Inject
    private SimpleAccountBatchCreator simpleAccountBatchCreator;

    @Inject
    private PLAccountBatchCreator plAccountBatchCreator;

    @Override
    public AccountBatchPackageEvent proceed(AccountBatchPackage batchPackage, Transition transition) {
        try {
            setPackageStartProcess(batchPackage);
            repository.executeTransactionally(connection -> {
                try (PreparedStatement queryStatement = connection.prepareStatement("select * from GL_ACBATREQ r where R.ID_PKG = ? and r.state = ? and trim(bsaacid) is null ")){
                    queryStatement.setLong(1, batchPackage.getId());
                    queryStatement.setString(2, AccountBatchState.VALID.name());
                    try (ResultSet resultSet = queryStatement.executeQuery()){
                        ExecutorService service = asyncProcessor.getBlockingQueueThreadPoolExecutor(10, 10, 1000);
                        while (resultSet.next()) {
                            long requestId = resultSet.getLong("ID_REQ");
                            service.submit(() -> {
                                try {
                                    AccountBatchRequest request = (AccountBatchRequest) repository.findById(AccountBatchRequest.class, requestId);
                                    if (checkOneRequest(request)){
                                        processOneRequest(request);
                                    }
                                } catch (Throwable e) {
                                    auditController.error(AccountBatch, format("Необработаная ошибка при обработке запроса на открытие счета '%s'", requestId), null, e);
                                }
                            });
                        }
                        asyncProcessor.awaitTermination(service, 1, HOURS, System.currentTimeMillis() + HOURS.toMillis(1));
                    }
                }
                return null;
            });
        } catch (Exception e) {
            auditController.error(AccountBatch, format("Ошибка выполнения обработки пакета '%s'", batchPackage), null, e);
            return AccountBatchPackageEvent.PROCESS_ERROR;
        }
        if (checkProcessState(batchPackage)) {
            return AccountBatchPackageEvent.PROCESS_SUCCESS;
        } else {
            return AccountBatchPackageEvent.PROCESS_ERROR;
        }
    }

    private boolean checkOneRequest(AccountBatchRequest request) {
        if (request.getState() != AccountBatchState.VALID && !isEmpty(request.getBsaAcid())) {
            auditController.warning(AccountBatch, format("Запрос '%s' в недопустимом статусе '%s' будет пропущен", request.getId(), request.getState()));
            return false;
        } else {
            return true;
        }
    }

    private void processOneRequest(AccountBatchRequest request) {
        final AccountBatchCreator accountCreator = isEmpty(request.getCalcPlcodeParm()) ? simpleAccountBatchCreator : plAccountBatchCreator;
        final String[] errorMessage = {null};
        GLAccount account = accountCreator.find(request).orElseGet(() -> {
            try {
                return accountCreator.createAccount(request);
            } catch (Throwable e) {
                auditController.error(AccountBatch, format("Ошибка при создании счета по запросу '%s': ", request) + e.getMessage(), request, e);
                errorMessage[0] = ExceptionUtils.getErrorMessage(e, ValidationError.class, SQLException.class);
                return null;
            }
        });
        if (null != account) {
            updateRequestState(request, account, AccountBatchState.COMPLETED, null);
        } else {
            updateRequestState(request, null, AccountBatchState.ERRPROC, errorMessage[0]);
        }
    }

    private void updateRequestState(AccountBatchRequest request, GLAccount account, AccountBatchState state, String error) {
        try {
            Long requestId = request.getId();
            Long accountId = null != account ? account.getId() : null;
            YesNo newAcc = null != account ? YesNo.getValue(accountController.isNewAccount(account.getBsaAcid())) : N;
            Date openDate = newAcc == Y ? account.getDateOpen() : null;
            String bsaacid = null != account ? account.getBsaAcid() : null;
            repository.executeInNewTransaction(persistence -> {
                repository.executeUpdate("update AccountBatchRequest r set r.state = ?1, r.account.id = ?2, r.bsaAcid = ?3, r.newAccount =?4, r.openDate = ?5, dtOpen = ?6, r.errorMessage = ?7 where r.id = ?8"
                    , state, accountId, bsaacid, newAcc, openDate, operdayController.getSystemDateTime(), substr(error, 4000), requestId);
                return null;
            });
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private boolean checkProcessState(AccountBatchPackage batchPackage) {
        try {
            DataRecord stat = batchSupportBean.getPackageProcessedStatistics(batchPackage);
            if (Objects.equals(stat.getInteger("suc"), stat.getInteger("tot"))) {
                return true;
            } else {
                auditController.error(AccountBatch, format("Не прошла проверка корректной обработки пакета: всего запросов %s, успешно %s, ошибок %s, в других статусах %s"
                    , stat.getInteger("tot"), stat.getInteger("suc"), stat.getInteger("err"), stat.getInteger("oth")), null, "");
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void setPackageStartProcess(AccountBatchPackage batchPackage) {
        try {
            repository.executeInNewTransaction(persistence -> {
                repository.executeUpdate("update AccountBatchPackage p set p.openStartDate = ?1 where p = ?2", operdayController.getSystemDateTime(), batchPackage);
                return null;
            });
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }
}
