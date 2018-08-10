package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.ovp.OvpUnloadFinalTask;
import ru.rbt.barsgl.ejb.controller.operday.task.ovp.OvpUnloadTask;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.CLOSED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.controller.operday.task.ovp.OvpUnloadParam.FINAL_POSTING;
import static ru.rbt.barsgl.ejb.controller.operday.task.ovp.OvpUnloadParam.FINAL_REST;

/**
 * Created by Ivan Sevastyanov on 29.11.2017.
 */
public class OvpUnloadIT extends AbstractRemoteIT {

    /**
     * выгрузка по ОВП
     * @throws Exception
     */
    @Test public void test() throws Exception {
        updateOperday(ONLINE, OPEN);
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

    /**
     * финальная выгрузка по ОПВ
     * @throws Exception
     */
    @Test public void testFinal() throws Exception {
        final String jobName = OvpUnloadFinalTask.class.getSimpleName();
        updateOperday(COB, CLOSED);

        baseEntityRepository.executeNativeUpdate("delete from GL_OCPTDS");
        baseEntityRepository.executeNativeUpdate("update gl_sched_h set SCHRSLT = 2 where SCHED_NAME = ?", jobName);

        SingleActionJob ovpFinalJob = SingleActionJobBuilder.create().withClass(OvpUnloadFinalTask.class).withName(jobName).build();
        jobService.executeJob(ovpFinalJob);
        List<DataRecord> headers = baseEntityRepository.select("select * from GL_OCPTDS");
        Assert.assertEquals(2, headers.size());
        Assert.assertTrue(headers.stream().allMatch(p -> p.getDate("operday").equals(getOperday().getCurrentDate())));
        Assert.assertTrue(headers.stream().allMatch(p -> p.getString("parvalue").equals(DwhUnloadStatus.SUCCEDED.getFlag())));

        Assert.assertTrue(headers.stream().anyMatch(p -> Objects.equals(p.getString("PARDESC"), FINAL_POSTING.getParDesc())));
        Assert.assertTrue(headers.stream().anyMatch(p -> Objects.equals(p.getString("PARDESC"), FINAL_REST.getParDesc())));

        // выгрузка не произведена так как TDS не забрал данные
        jobService.executeJob(ovpFinalJob);
        List<DataRecord> headers2 = baseEntityRepository.select("select * from GL_OCPTDS");
        Assert.assertEquals(2, headers2.size());
        List<DataRecord> finalHeaders = headers2;
        Assert.assertTrue(headers.stream().anyMatch(p -> Objects.equals(p.getLong("id_key"), finalHeaders.get(0).getLong("id_key"))));
        List<DataRecord> finalHeaders1 = headers2;
        Assert.assertTrue(headers.stream().anyMatch(p -> Objects.equals(p.getLong("id_key"), finalHeaders1.get(1).getLong("id_key"))));

        baseEntityRepository.executeNativeUpdate("delete from GL_OCPTDS");

        // следующий ОД
        Date newOperday = DateUtils.addDay(getOperday().getCurrentDate(), 1);
        Date newLwOperday = DateUtils.addDay(getOperday().getLastWorkingDay(), 1);
        setOperday(newOperday, newLwOperday, Operday.OperdayPhase.COB, Operday.LastWorkdayStatus.CLOSED);

        jobService.executeJob(ovpFinalJob);
        headers2 = baseEntityRepository.select("select * from GL_OCPTDS");
        Assert.assertEquals(2, headers2.size());
        List<DataRecord> finalHeaders2 = headers2;
        Assert.assertFalse(headers.stream().anyMatch(p -> Objects.equals(p.getLong("id_key"), finalHeaders2.get(0).getLong("id_key"))));
        List<DataRecord> finalHeaders3 = headers2;
        Assert.assertFalse(headers.stream().anyMatch(p -> Objects.equals(p.getLong("id_key"), finalHeaders3.get(1).getLong("id_key"))));

    }

}
