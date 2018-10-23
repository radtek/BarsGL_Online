package ru.rbt.barsgl.ejb.controller.acc;

import ru.rbt.barsgl.ejb.controller.acc.act.AccountBatchValidateStart;
import ru.rbt.barsgl.ejb.controller.acc.sm.StateMachine;
import ru.rbt.barsgl.ejb.controller.acc.sm.StateMachineBuilder;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.shared.enums.AccountBatchPackageState;

/**
 * Created by Ivan Sevastyanov on 22.10.2018.
 */
public class AccountBatchStateController {

    private static StateMachine<AccountBatchPackageEvent, AccountBatchPackageState, AccountBatchPackage> sm
            = new StateMachineBuilder<>().makeTransition(AccountBatchPackageState.IS_LOAD, AccountBatchPackageState.ON_VALID
            , AccountBatchPackageEvent.VALIDATE,  AccountBatchValidateStart.class).build();

    public void startValidation(AccountBatchPackage batchPackage) throws Exception {
        sm.acceptEvent(batchPackage, AccountBatchPackageEvent.VALIDATE);
    }

}
