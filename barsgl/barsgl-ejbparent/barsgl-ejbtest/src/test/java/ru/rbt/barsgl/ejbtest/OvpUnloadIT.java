package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.controller.operday.task.ovp.OvpUnloadTask;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.util.List;
import java.util.Objects;

/**
 * Created by Ivan Sevastyanov on 29.11.2017.
 */
public class OvpUnloadIT extends AbstractRemoteIT {

    @Test public void test() throws Exception {
        baseEntityRepository.executeNativeUpdate("delete from GL_OCPTDS");

        SingleActionJob ovpjob = SingleActionJobBuilder.create().withClass(OvpUnloadTask.class).withName(OvpUnloadTask.class.getSimpleName()).build();
        jobService.executeJob(ovpjob);
        List<DataRecord> headers = baseEntityRepository.select("select * from GL_OCPTDS");
        Assert.assertEquals(2, headers.size());
        Assert.assertTrue(headers.stream().allMatch(p -> p.getString("parvalue").equals(DwhUnloadStatus.SUCCEDED.getFlag())));

        jobService.executeJob(ovpjob);
        List<DataRecord> headers2 = baseEntityRepository.select("select * from GL_OCPTDS");
        Assert.assertEquals(2, headers2.size());
        Assert.assertTrue(headers.stream().anyMatch(p -> Objects.equals(p.getLong("id_key"), headers2.get(0).getLong("id_key"))));
        Assert.assertTrue(headers.stream().anyMatch(p -> Objects.equals(p.getLong("id_key"), headers2.get(1).getLong("id_key"))));

    }

}
