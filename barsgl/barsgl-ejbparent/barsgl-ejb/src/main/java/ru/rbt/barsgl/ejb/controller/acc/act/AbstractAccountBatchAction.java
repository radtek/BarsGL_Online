package ru.rbt.barsgl.ejb.controller.acc.act;

import ru.rbt.barsgl.ejb.controller.acc.AccountBatchPackageEvent;
import ru.rbt.barsgl.ejb.controller.sm.StateAction;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;

/**
 * Created by Ivan Sevastyanov on 23.10.2018.
 */
public abstract class AbstractAccountBatchAction implements StateAction<AccountBatchPackage, AccountBatchPackageEvent> {
}
