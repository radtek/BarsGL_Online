package ru.rbt.barsgl.ejb.controller.acc.act;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.acc.AccountBatchSupportBean;
import ru.rbt.barsgl.ejb.controller.sm.StateTrigger;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;

import javax.ejb.EJB;

/**
 * Created by Ivan Sevastyanov on 31.10.2018.
 */
public class AfterProcessTrigger implements StateTrigger<AccountBatchPackage> {

    @EJB
    private CoreRepository repository;

    @EJB
    private AccountBatchSupportBean supportBean;

    @EJB
    private OperdayController operdayController;

    @Override
    public void onStateEnter(AccountBatchPackage batchPackage) {
        try {
            repository.executeInNewTransaction(persistence -> {
                DataRecord stat = supportBean.getPackageProcessedStatistics(batchPackage);
                repository.executeUpdate("update AccountBatchPackage p set p.openEndDate = ?1, p.cntFound = ?2, cntErrors = ?3 where p = ?4"
                        , operdayController.getSystemDateTime(), stat.getLong("fnd"), stat.getLong("err"), batchPackage);
                return null;
            });
        } catch (Throwable e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }
}
