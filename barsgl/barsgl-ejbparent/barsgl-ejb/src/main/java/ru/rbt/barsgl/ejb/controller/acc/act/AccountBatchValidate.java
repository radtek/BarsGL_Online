package ru.rbt.barsgl.ejb.controller.acc.act;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.controller.acc.AccountBatchPackageEvent;
import ru.rbt.barsgl.ejb.controller.acc.AccountBatchSupportBean;
import ru.rbt.barsgl.ejb.controller.sm.Transition;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejbcore.CoreRepository;

import javax.ejb.EJB;

import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountBatch;
import static ru.rbt.barsgl.ejb.controller.acc.AccountBatchPackageEvent.VALIDATE_ERROR;
import static ru.rbt.barsgl.ejb.controller.acc.AccountBatchPackageEvent.VALIDATE_SUCESS;

/**
 * Created by Ivan Sevastyanov on 23.10.2018.
 */
public class AccountBatchValidate extends AbstractAccountBatchAction {

    @EJB
    private CoreRepository repository;

    @EJB
    private AuditController auditController;

    @EJB
    private AccountBatchSupportBean accountBatchSupportBean;

    @Override
    public AccountBatchPackageEvent proceed(AccountBatchPackage stateObject, Transition transition) {
        try {
            return (AccountBatchPackageEvent) repository.executeInNewTransaction(persistence -> {
                if (accountBatchSupportBean.validatePackage(stateObject) && accountBatchSupportBean.validateRequests(stateObject)) {
                    return VALIDATE_SUCESS;
                } else {
                    return VALIDATE_ERROR;
                }
            });
        } catch (Exception e) {
            auditController.error(AccountBatch, "Ошибка валидации пакета: " + e.getMessage(), stateObject, e);
            return VALIDATE_ERROR;
        }
    }

}
