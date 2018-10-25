package ru.rbt.barsgl.ejb.controller.acc.act;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.acc.AccountBatchPackageEvent;
import ru.rbt.barsgl.ejb.controller.sm.Transition;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchRequest;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.validation.DataValidator;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.barsgl.shared.enums.AccountBatchState;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountBatch;
import static ru.rbt.barsgl.ejb.controller.acc.AccountBatchPackageEvent.VALIDATE_ERROR;
import static ru.rbt.barsgl.ejb.controller.acc.AccountBatchPackageEvent.VALIDATE_SUCESS;
import static ru.rbt.barsgl.ejb.integr.ValidationAwareHandler.validationErrorsToString;
import static ru.rbt.ejbcore.util.StringUtils.leftPad;
import static ru.rbt.ejbcore.validation.ErrorCode.ACC_BATCH_OPEN;
import static ru.rbt.shared.ExceptionUtils.getErrorMessage;

/**
 * Created by Ivan Sevastyanov on 23.10.2018.
 */
public class AccountBatchValidate extends AbstractAccountBatchAction {

    @EJB
    private CoreRepository repository;

    @EJB
    private OperdayController operdayController;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private AuditController auditController;

    @EJB
    private AsyncProcessor asyncProcessor;

    @Override
    public AccountBatchPackageEvent proceed(AccountBatchPackage stateObject, Transition transition) {
        if (validatePackage(stateObject) && validateRequests(stateObject)) {
            return VALIDATE_SUCESS;
        } else {
            return VALIDATE_ERROR;
        }
    }

    private boolean validatePackage(AccountBatchPackage batchPackage) {
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

    private boolean validateRequests(AccountBatchPackage batchPackage) {
        ExecutorService executor = asyncProcessor.getBlockingQueueThreadPoolExecutor(10,10,1000);
        try {
            repository.executeTransactionally(connection -> {
                try (PreparedStatement selectstm = connection.prepareCall("select ID_REQ from GL_ACBATREQ r where ID_PKG = ?")){
                    selectstm.setLong(1, batchPackage.getId());
                    try (ResultSet resultSet = selectstm.executeQuery()){
                        while (resultSet.next()) {
                            long requestId = resultSet.getLong(1);
                            AccountBatchRequest request = (AccountBatchRequest) repository.findById(AccountBatchRequest.class, requestId);
                            executor.submit(() -> {
                                try {
                                    repository.executeInNewTransaction(persistence -> {
                                        validateUpdateOneRequest(request);
                                        return null;
                                    });
                                } catch (Throwable e) {
                                    auditController.error(AccountBatch, format("Ошибка валидации запроса '%s' на открытие счета", requestId), null, e);
                                    updateRequestState(request, AccountBatchState.ERRCHK, getErrorMessage(e, SQLException.class, DefaultApplicationException.class, ValidationError.class));
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

    private void validateUpdateOneRequest(AccountBatchRequest request) {
        ValidationContext context = new ValidationContext();
        context.addValidator(() -> createValidator(() -> {
            DataRecord record = repository.selectFirst("select * from IMBCBBRP where A8BRCD = ?", request.getInBranch());
            Assert.isTrue(null != record
                , () -> new ValidationError(ACC_BATCH_OPEN, format("Код отделения '%s' не найден в таблице IMBCBBRP", request.getInBranch())));
            request.setCalcCbcc(record.getString("A8CMCD"));
        }));
        context.addValidator(() -> createValidator(() -> {
            Assert.isTrue(null != repository.selectFirst("select * from CURRENCY where glccy = ?", request.getInCcy())
                    , () -> new ValidationError(ACC_BATCH_OPEN, format("Код валюты '%s' не найден в таблице CURRENCY", request.getInCcy())));
        }));
        context.addValidator(() -> createValidator(() -> {
            DataRecord record = repository.selectFirst("select BXCTYP from SDCUSTPD where bbcust = ?", request.getInCustno());
            Assert.isTrue(null != record
                    , () -> new ValidationError(ACC_BATCH_OPEN, format("Код клиента '%s' не найден в таблице SDCUSTPD", request.getInCustno())));
            request.setCalcCtype(record.getString("BXCTYP"));
        }));
        context.addValidator(() -> createValidator(() -> {
            DataRecord record = repository.selectFirst("select FL_CTRL from GL_ACTNAME  where ACCTYPE = ?", request.getInAcctype());
            Assert.isTrue(null != record
                    , () -> new ValidationError(ACC_BATCH_OPEN, format("Код AccType '%s' не найден в таблице GL_ACTNAME", request.getInAcctype().trim())));
            Assert.isTrue(!"Y".equals(record.getString("FL_CTRL"))
                    , () -> new ValidationError(ACC_BATCH_OPEN, format("Код AccType '%s' соответствует счетам, остаток по которым контролируются во внешней системе. Пакетное открытие таких счетов запрещено"
                            , request.getInAcctype())));
        }));
        context.addValidator(() -> createValidator(() -> {
            if (!StringUtils.isEmpty(request.getInCtype())
                    && !"00".equals(leftPad(request.getInCtype(),2,"0"))) {
                Assert.isTrue(null != repository.selectFirst("select 1 from CBCTP where CTYPE = ?", request.getInCtype().trim())
                        , () -> new ValidationError(ACC_BATCH_OPEN, format("Тип собственности '%s' не найден в таблице CBCTP", request.getInCtype())));
            }
        }));
        context.addValidator(() -> createValidator(() -> {
            if (!StringUtils.isEmpty(request.getInTerm())
                    && !"00".equals(leftPad(request.getInTerm(),2,"0"))) {
                Assert.isTrue(null != repository.selectFirst("select 1 from GL_DICTERM where TERM = ?", request.getInTerm().trim())
                        , () -> new ValidationError(ACC_BATCH_OPEN, format("Код срока '%s' не найден в таблице GL_DICTERM’", request.getInTerm())));
            }
        }));
        context.addValidator(() -> createValidator(() -> {
            if (!StringUtils.isEmpty(request.getInAcc2())) {
                Assert.isTrue(null != repository.selectFirst("select 1 from BSS2 where AAC2 = ?", request.getInAcc2())
                        , () -> new ValidationError(ACC_BATCH_OPEN, format("Балансовый счет 2 порядка '%s' не найден в таблице BSS2", request.getInAcc2())));
            }
        }));
        context.addValidator(() -> createValidator(() -> {
            if (!StringUtils.isEmpty(request.getInDealsrc())) {
                Assert.isTrue(null != repository.selectFirst("select 1 from GL_SRCPST where ID_SRC = ?", request.getInDealsrc())
                        , () -> new ValidationError(ACC_BATCH_OPEN, format("Код источника сделки '%s' не найден в таблице GL_SRCPST", request.getInDealsrc())));
            }
        }));
        context.addValidator(createValidator(() -> {
            if (StringUtils.isEmpty(request.getInDealsrc())
                    && !StringUtils.isEmpty(request.getInDealid())) {
                throw new ValidationError(ACC_BATCH_OPEN, "Поле DEALID_IN не может быть заполнено, если источник сделки не заполнен");
            }
        }));
        context.addValidator(createValidator(() -> {
            if (StringUtils.isEmpty(request.getInDealsrc())
                    && !StringUtils.isEmpty(request.getInSubdealid())) {
                throw new ValidationError(ACC_BATCH_OPEN, "Поле SUBDEALID_IN не может быть заполнено, если источник сделки не заполнен");
            }
        }));
        context.addValidator(createValidator(() -> {
            if (null != request.getOpenDate()
                    && request.getOpenDate().after(operdayController.getOperday().getCurrentDate())) {
                throw new ValidationError(ACC_BATCH_OPEN, format("Дата открытия '%s' больше даты текущего операционного дня  '%s'"
                        , dateUtils.onlyDateString(request.getOpenDate()), dateUtils.onlyDateString(operdayController.getOperday().getCurrentDate())));
            }
        }));
        context.validateAll();
        if (!context.getErrors().isEmpty()) {
            updateRequestState(request, AccountBatchState.ERRCHK, validationErrorsToString(context.getErrors()));
        } else {
            updateRequestState(request, AccountBatchState.VALID, null);
        }
        repository.update(request);
    }

    abstract class ExceptionAwareDataValidator implements DataValidator {

        public abstract void validateException() throws SQLException;

        @Override
        public final void validate() throws ValidationError {
            try {
                validateException();
            } catch (SQLException e) {
                throw new DefaultApplicationException(e.getMessage(), e);
            }
        }
    }

    interface ExceptionAwareRunnable {
        void validate() throws SQLException;
    }

    private DataValidator createValidator(ExceptionAwareRunnable validator) {
        return new ExceptionAwareDataValidator() {
            @Override
            public void validateException() throws SQLException {
                validator.validate();
            }
        };
    }

    @SuppressWarnings("All")
    private void updateRequestState(AccountBatchRequest request, AccountBatchState state, String errorMessage) {
        try {
            repository.executeInNewTransaction(persistence -> {
                request.setState(state);
                request.setErrorMessage(errorMessage);
                repository.update(request);
                return null;
            });
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private void updatePackageState(AccountBatchPackage batchPackage, long cntErrors) {
        try {
            repository.executeInNewTransaction(persistence -> {
                batchPackage.setCntErrors(cntErrors);
                repository.update(batchPackage);
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
                updatePackageState(batchPackage, erchk);
                return false;
            }
            return true;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

}
