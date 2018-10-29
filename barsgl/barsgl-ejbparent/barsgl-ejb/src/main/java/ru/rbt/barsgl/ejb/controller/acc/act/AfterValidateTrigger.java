package ru.rbt.barsgl.ejb.controller.acc.act;

import ru.rbt.barsgl.ejb.controller.acc.AccountBatchSupportBean;
import ru.rbt.barsgl.ejb.controller.sm.StateTrigger;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;

import javax.ejb.EJB;
import java.sql.SQLException;

/**
 * Created by Ivan Sevastyanov on 29.10.2018.
 */
public class AfterValidateTrigger implements StateTrigger<AccountBatchPackage> {

    @EJB
    private AccountBatchSupportBean batchSupport;

    @Override
    public void onStateEnter(AccountBatchPackage batchPackage) {
        try {
            DataRecord stat = batchSupport.getPackageValidateStatistics(batchPackage);
            batchSupport.updatePackageState(batchPackage.getId(), stat.getLong("ERCHK"));
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

}
