package ru.rbt.barsgl.ejb.controller.acc.act;

import ru.rbt.barsgl.ejb.controller.acc.AccountBatchPackageEvent;
import ru.rbt.barsgl.ejb.controller.sm.Transition;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;

/**
 * Created by Ivan Sevastyanov on 22.10.2018.
 */
public class AccountBatchSendToValidate extends AbstractAccountBatchAction{

    @Override
    public AccountBatchPackageEvent proceed(AccountBatchPackage stateObject, Transition transition) {
        return AccountBatchPackageEvent.VALIDATE;
    }
}
