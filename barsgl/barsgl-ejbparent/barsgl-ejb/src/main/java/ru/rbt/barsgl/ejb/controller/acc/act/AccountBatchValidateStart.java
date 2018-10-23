package ru.rbt.barsgl.ejb.controller.acc.act;

import ru.rbt.barsgl.ejb.controller.acc.AccountBatchPackageEvent;
import ru.rbt.barsgl.ejb.controller.acc.sm.StateAction;
import ru.rbt.barsgl.ejb.controller.acc.sm.Transition;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;

import javax.enterprise.context.RequestScoped;

/**
 * Created by Ivan Sevastyanov on 22.10.2018.
 */
@RequestScoped
public class AccountBatchValidateStart implements StateAction<AccountBatchPackage, AccountBatchPackageEvent> {

    @Override
    public AccountBatchPackageEvent proceed(AccountBatchPackage stateObject, Transition transition) {
        return null;
    }
}
