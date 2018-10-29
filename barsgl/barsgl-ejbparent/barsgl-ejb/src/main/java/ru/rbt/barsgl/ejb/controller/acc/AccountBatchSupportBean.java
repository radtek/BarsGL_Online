package ru.rbt.barsgl.ejb.controller.acc;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchRequest;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.barsgl.shared.enums.AccountBatchState;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ValidationError;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountBatch;
import static ru.rbt.barsgl.ejb.integr.ValidationAwareHandler.validationErrorsToString;
import static ru.rbt.ejbcore.validation.ErrorCode.ACC_BATCH_OPEN;
import static ru.rbt.shared.ExceptionUtils.getErrorMessage;

/**
 * Created by Ivan Sevastyanov on 26.10.2018.
 */
@Stateless
@LocalBean
public class AccountBatchSupportBean {

    @EJB
    private AuditController auditController;

    @EJB
    private OperdayController operdayController;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private CoreRepository repository;

    @EJB
    private AsyncProcessor asyncProcessor;

    @EJB
    private AccountValidationSupportBean accountValidationSupportBean;

    public boolean validatePackage(AccountBatchPackage batchPackage) {
        ValidationContext context = new ValidationContext();
        Date currdate = operdayController.getOperday().getCurrentDate();
        context.addValidator(() -> {
            if (!batchPackage.getOperday().equals(currdate)) {
                throw new ValidationError(ACC_BATCH_OPEN
                        , format("Операционная дата пакета '%s' не соответствует текущему операционному дню '%s'"
                        , dateUtils.onlyDateString(batchPackage.getOperday()), dateUtils.onlyDateString(currdate)));
            }
        });
        context.validateAll();
        if (context.getErrors().isEmpty()) {
            return true;
        } else {
            auditController.error(AccountBatch, "Не прошла проверка корректности пакета: "
                    + validationErrorsToString(context.getErrors()), null, context.getErrors().get(0));
            return false;
        }
    }

    public boolean validateRequests(AccountBatchPackage batchPackage) {
        ExecutorService executor = asyncProcessor.getBlockingQueueThreadPoolExecutor(10,10,1000);
        try {
            repository.executeTransactionally(connection -> {
                try (PreparedStatement selectstm = connection.prepareCall("select ID_REQ from GL_ACBATREQ r where ID_PKG = ?")){
                    selectstm.setLong(1, batchPackage.getId());
                    try (ResultSet resultSet = selectstm.executeQuery()){
                        while (resultSet.next()) {
                            long requestId = resultSet.getLong(1);
                            executor.submit(() -> {
                                try {
                                    AccountBatchRequest request = (AccountBatchRequest) repository.findById(AccountBatchRequest.class, requestId);
                                    repository.executeInNewTransaction(persistence -> {
                                        List<ValidationError> errors = accountValidationSupportBean.validateUpdateOneRequest(batchPackage, request);
                                        if (errors.isEmpty()) {
                                            updateRequestState(request, AccountBatchState.VALID, "");
                                        } else {
                                            updateRequestState(request, AccountBatchState.ERRCHK, validationErrorsToString(errors));
                                        }
                                        return null;
                                    });
                                } catch (Throwable e) {
                                    auditController.error(AccountBatch, format("Ошибка валидации запроса '%s' на открытие счета", requestId), null, e);
                                    updateRequestState(requestId, AccountBatchState.ERRCHK, getErrorMessage(e, SQLException.class, DefaultApplicationException.class, ValidationError.class));
                                }
                            });
                        }
                    }
                }
                return null;
            });
            asyncProcessor.awaitTermination(executor, 1, TimeUnit.HOURS, TimeUnit.HOURS.toMillis(1));
            return checkPackageRequestsState(batchPackage);
        } catch (Exception e) {
            auditController.error(AccountBatch, format("Ошибка выполнения валидации запросов на открытие счета. Пакет '%s'", batchPackage), null, e);
            return false;
        }
    }



    @SuppressWarnings("All")
    private void updateRequestState(AccountBatchRequest request, AccountBatchState state, String errorMessage) {
        updateRequestState(request.getId(), state, errorMessage);
    }

    @SuppressWarnings("All")
    private void updateRequestState(long requestId, AccountBatchState state, String errorMessage) {
        try {
            repository.executeInNewTransaction(persistence -> {
                repository.executeUpdate("update AccountBatchRequest r set state = ?1, errorMessage = ?2 where r.id = ?3", state, errorMessage, requestId);
                return null;
            });
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private void updatePackageState(long batchPackageId, long cntErrors) {
        try {
            repository.executeInNewTransaction(persistence -> {
                repository.executeUpdate("update AccountBatchPackage p set p.cntErrors = ?1 where p.id = ?2", cntErrors, batchPackageId);
                return null;
            });
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private boolean checkPackageRequestsState(AccountBatchPackage batchPackage) {
        try {
            DataRecord pkgStat = repository.selectFirst(
                    "select nvl(sum(case when state = 'VALID' then 1 else 0 end),0) valid\n" +
                            "       , nvl(sum(case when state = 'ERRCHK' then 1 else 0 end),0) erchk\n" +
                            "       , nvl(sum(case when state not in ('ERRCHK','VALID') then 1 else 0 end),0) other\n" +
                            "  from GL_ACBATREQ where ID_PKG = ?", batchPackage.getId());
            long erchk = pkgStat.getLong("ERCHK"); long other = pkgStat.getLong("OTHER"); long valid = pkgStat.getLong("VALID");

            if (erchk > 0 || other > 0) {
                auditController.error(AccountBatch, format("Не прошла полная валидация запросов пакета '%s': всего запросов '%s', ошибок '%s', в других статусах '%s'"
                        , batchPackage, erchk + other + valid, erchk, other), null, "");
                updatePackageState(batchPackage.getId(), erchk);
                return false;
            }
            return true;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }


}

