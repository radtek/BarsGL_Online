package ru.rbt.barsgl.ejb.controller.acc;

import ru.rbt.barsgl.ejb.controller.acc.act.AccountBatchSendToValidate;
import ru.rbt.barsgl.ejb.controller.acc.act.AccountBatchValidate;
import ru.rbt.barsgl.ejb.controller.sm.StateMachine;
import ru.rbt.barsgl.ejb.controller.sm.StateMachineBuilder;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.shared.enums.AccountBatchPackageState;

import static ru.rbt.barsgl.ejb.controller.acc.AccountBatchPackageEvent.*;
import static ru.rbt.barsgl.shared.enums.AccountBatchPackageState.*;

/**
 * Created by Ivan Sevastyanov on 22.10.2018.
 */
public class AccountBatchStateController {

    @SuppressWarnings("unchecked")
    private static StateMachine<AccountBatchPackageState, AccountBatchPackageEvent, AccountBatchPackage> sm
            = new StateMachineBuilder<AccountBatchPackageState, AccountBatchPackageEvent, AccountBatchPackage>()
            .makeTransition(IS_LOAD, ON_VALID, SEND_TO_VALIDATE,  AccountBatchSendToValidate.class)
            .makeTransition(ON_VALID, IS_VALID, VALIDATE,  AccountBatchValidate.class)
            .makeTransition(ON_VALID, ERROR, VALIDATE_ERROR,  null)
            .makeTransition(IS_VALID, PROCESSED, PROCESS, null)
            .makeTransition(IS_VALID, PROC_ERR, PROCESS_ERROR,null)
            .build();

    public void sendToValidation(AccountBatchPackage batchPackage) throws Exception {
        sm.acceptEvent(batchPackage, SEND_TO_VALIDATE);
    }

    public void startValidation(AccountBatchPackage batchPackage) throws Exception {
        sm.acceptEvent(batchPackage, VALIDATE);
    }

}
