package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.LoadManagementAction;
import ru.rbt.barsgl.shared.enums.LoadManagementStatus;
import ru.rbt.barsgl.shared.loader.LoadStepWrapper;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.util.Date;

/**
 * Created by SotnikovAV on 28.10.2016.
 */
@Ignore("Интерфейс управления загрузчиком переделывается на другой платформе")
public class LoadManagementIT extends AbstractRemoteIT {

    @Test
    public void testCreateLoadManagement() throws Exception {
        final String stepCode = "P1";
        final String expertName = "TestExpert";

        DataRecord wdRec = baseEntityRepository.selectFirst("select workday from workday");
        Date dat = wdRec.getDate("workday");

        Assert.assertNotNull(dat);

        baseEntityRepository.executeNativeUpdate("delete from load_management where dat=? and code=? and status=0", dat, stepCode);

        DataRecord lmRec = baseEntityRepository.selectFirst("select max(ordid) as ordid from load_management");
        Long ordid = lmRec.getLong("ordid");

        if(null == ordid) {
            ordid = 1L;
        }

        LoadStepWrapper wrapper = new LoadStepWrapper();
        wrapper.setDat(dat);
        wrapper.setCode(stepCode);
        wrapper.setAction(LoadManagementAction.Restart.ordinal());
        wrapper.setExpert(expertName);
        wrapper.setExpertModified(new Date());

        RpcRes_Base<LoadStepWrapper> result = loadManagementController.create(wrapper);

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isError());

        wrapper = result.getResult();

        Assert.assertNotNull(wrapper);
        Assert.assertTrue(ordid == wrapper.getOrdid());

        RpcRes_Base<LoadStepWrapper> errorResult = loadManagementController.create(wrapper);

        Assert.assertNotNull(errorResult);
        Assert.assertTrue(errorResult.isError());
    }

    @Test
    public void testUpdateLoadManagement() throws Exception {
        final String stepCode = "P1";
        final String expertName = "TestExpert";
        final String managerName = "TestManager";
        final String operatorName = "TestOperator";

        DataRecord wdRec = baseEntityRepository.selectFirst("select workday from workday");
        Date dat = wdRec.getDate("workday");

        Assert.assertNotNull(dat);

        baseEntityRepository.executeNativeUpdate("delete from load_management where dat=? and code=? and status=0", dat, stepCode);

        DataRecord lmRec = baseEntityRepository.selectFirst("select max(ordid) as ordid from load_management");
        Long ordid = lmRec.getLong("ordid");

        if(null == ordid) {
            ordid = 1L;
        }

        LoadStepWrapper wrapper = new LoadStepWrapper();
        wrapper.setDat(dat);
        wrapper.setCode(stepCode);
        wrapper.setAction(LoadManagementAction.Restart.ordinal());

        RpcRes_Base<LoadStepWrapper> result = loadManagementController.create(wrapper);

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isError());

        wrapper = result.getResult();

        Assert.assertNotNull(wrapper);
        Assert.assertTrue(ordid == wrapper.getOrdid());

        wrapper.setAction(LoadManagementAction.SetOK.ordinal());

        RpcRes_Base<LoadStepWrapper> updateResult_1 = loadManagementController.update(wrapper);

        Assert.assertNotNull(updateResult_1);
        Assert.assertFalse(updateResult_1.getMessage(), updateResult_1.isError());

        wrapper.setExpert(expertName);
        wrapper.setExpertModified(new Date());
        wrapper.setStatus(LoadManagementStatus.Assigned.ordinal());

        RpcRes_Base<LoadStepWrapper> updateResult_2 = loadManagementController.update(wrapper);

        Assert.assertNotNull(updateResult_2);
        Assert.assertFalse(updateResult_2.isError());

        wrapper.setManager(managerName);
        wrapper.setManagerModified(new Date());
        wrapper.setStatus(LoadManagementStatus.Approved.ordinal());

        RpcRes_Base<LoadStepWrapper> updateResult_3 = loadManagementController.update(wrapper);

        Assert.assertNotNull(updateResult_3);
        Assert.assertFalse(updateResult_3.isError());

        wrapper.setOperator(operatorName);
        wrapper.setOperatorModified(new Date());
        wrapper.setStatus(LoadManagementStatus.Executed.ordinal());

        RpcRes_Base<LoadStepWrapper> updateResult_4 = loadManagementController.update(wrapper);

        Assert.assertNotNull(updateResult_4);
        Assert.assertFalse(updateResult_4.isError());
    }

}
