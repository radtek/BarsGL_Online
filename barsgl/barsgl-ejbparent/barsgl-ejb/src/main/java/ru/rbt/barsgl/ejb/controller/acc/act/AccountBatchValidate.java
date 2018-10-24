package ru.rbt.barsgl.ejb.controller.acc.act;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.acc.AccountBatchPackageEvent;
import ru.rbt.barsgl.ejb.controller.sm.Transition;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchRequest;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.barsgl.shared.enums.AccountBatchState;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ValidationError;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountBatch;
import static ru.rbt.barsgl.ejb.controller.acc.AccountBatchPackageEvent.VALIDATE_ERROR;
import static ru.rbt.barsgl.ejb.controller.acc.AccountBatchPackageEvent.VALIDATE_SUCESS;
import static ru.rbt.barsgl.ejb.integr.ValidationAwareHandler.validationErrorsToString;
import static ru.rbt.ejbcore.validation.ErrorCode.ACC_BATCH_OPEN;

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

    @Override
    public AccountBatchPackageEvent proceed(AccountBatchPackage stateObject, Transition transition) {
        List<AccountBatchRequest> requests = repository.select(AccountBatchRequest.class
                , "from AccountBatchRequest r where r.batchPackage = ?1", stateObject);
        for (AccountBatchRequest request : requests) {
            request.setState(AccountBatchState.VALID);
            repository.update(request);
        }
        ValidationContext context = new ValidationContext();
        validatePackage(context, stateObject);

        if (context.getErrors().isEmpty()) {
            return VALIDATE_SUCESS;
        } else {
            auditController.error(AccountBatch, "Не прошла проверка корректности пакета: "
                    + validationErrorsToString(context.getErrors()), null, context.getErrors().get(0));
            return VALIDATE_ERROR;
        }
    }

    private void validatePackage(ValidationContext context, AccountBatchPackage accountBatchPackage) {
        Date currdate = operdayController.getOperday().getCurrentDate();
        context.addValidator(() -> {
            if (!accountBatchPackage.getOperday().equals(currdate)) {
                throw new ValidationError(ACC_BATCH_OPEN
                        , format("Операционная дата пакета '%s' не соответствует текущему операционному дню '%s'"
                            , dateUtils.onlyDateString(accountBatchPackage.getOperday()), dateUtils.onlyDateString(currdate)));
            }
        });
        context.validateAll();
    }

}
