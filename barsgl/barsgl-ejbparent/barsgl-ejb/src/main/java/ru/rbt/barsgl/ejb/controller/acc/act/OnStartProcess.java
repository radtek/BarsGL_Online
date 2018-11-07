package ru.rbt.barsgl.ejb.controller.acc.act;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.sm.StateTrigger;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.DefaultApplicationException;

import javax.ejb.EJB;

/**
 * Created by Ivan Sevastyanov on 07.11.2018.
 */
public class OnStartProcess implements StateTrigger<AccountBatchPackage> {

    @EJB
    private CoreRepository repository;

    @EJB
    private OperdayController operdayController;

    @Override
    public void execute(AccountBatchPackage batchPackage) {
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
