package ru.rbt.barsgl.ejbtest;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.cob.CobStatRecalculator;
import ru.rbt.barsgl.ejb.controller.cob.CobStatService;
import ru.rbt.barsgl.ejb.controller.operday.task.EtlStructureMonitorTask;
import ru.rbt.barsgl.ejb.controller.operday.task.ExecutePreCOBTaskFake;
import ru.rbt.barsgl.ejb.controller.operday.task.ExecutePreCOBTaskNew;
import ru.rbt.barsgl.ejb.entity.cob.CobStatId;
import ru.rbt.barsgl.ejb.entity.cob.CobStepStatistics;
import ru.rbt.barsgl.ejb.job.BackgroundJobsController;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.barsgl.ejbcore.repository.PropertiesRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.cob.CobStepItem;
import ru.rbt.barsgl.shared.cob.CobWrapper;
import ru.rbt.barsgl.shared.enums.CobStep;
import ru.rbt.barsgl.shared.enums.CobStepStatus;

import java.math.BigDecimal;

import static ru.rbt.barsgl.shared.enums.CobStepStatus.Error;
import static ru.rbt.barsgl.shared.enums.CobStepStatus.*;

/**
 * Created by ER18837 on 13.03.17.
 * Тестирование статистики по PreCob
 */
public class CobStatTest extends AbstractTimerJobTest  {
    private final int phaseFirst = 1;
    private final int phaseLast = CobStep.values().length;

    @BeforeClass
    public static void beforeClass() {
        initCorrectOperday();
    }

    @AfterClass
    public static void afterClass() {
        initCorrectOperday();
    }

    @Test
    public void testCreateStatistics () {
        RpcRes_Base<CobWrapper> res = remoteAccess.invoke(CobStatService.class, "calculateCob");
        Assert.assertFalse(res.isError());
        System.out.println(res.getMessage());

        CobWrapper wrapper = res.getResult();
        Assert.assertTrue(wrapper.getIdCob() > 0);
        Assert.assertEquals(CobStep.values().length, wrapper.getStepList().size());
        CobStepItem total = wrapper.getTotal();
        Assert.assertNotNull(total);
        checkStepState(total, NotStart);
        Assert.assertNotEquals(BigDecimal.ZERO, total.getEstimation());
        printStepInfo(total);
        System.out.println(wrapper.getErrorMessage());
    }

    @Test
    public void testGetInfo () throws InterruptedException {
        RpcRes_Base<CobWrapper> res = remoteAccess.invoke(CobStatService.class, "calculateCob");
        Assert.assertFalse(res.isError());
        CobWrapper wrapper = res.getResult();
        Assert.assertTrue(wrapper.getIdCob() > 0);

        setKoefIncrease(new BigDecimal(1.5));
        baseEntityRepository.executeNativeUpdate("update GL_COB_STAT set ESTIMATED = ? where ID_COB = ? and PHASE_NO = ?", 3, wrapper.getIdCob(), phaseFirst);
        CobStepStatistics stepFirst = (CobStepStatistics) baseEntityRepository.findById(CobStepStatistics.class, new CobStatId(wrapper.getIdCob(), phaseFirst));
        Assert.assertNotNull(stepFirst);
        remoteAccess.invoke(CobStatRecalculator.class, "setStepStart", wrapper.getIdCob(), stepFirst);

        Thread.sleep(2000L);
        remoteAccess.invoke(CobStatRecalculator.class, "setStepMessage", wrapper.getIdCob(), stepFirst, "Выполнен этап 1");
        checkGetInfo(wrapper, null, phaseFirst, Running, Running);

        Thread.sleep(2000L);
        remoteAccess.invoke(CobStatRecalculator.class, "setStepMessage", wrapper.getIdCob(), stepFirst, "Выполнен этап 2");
        checkGetInfo(wrapper, null, phaseFirst, Running, Running);

        remoteAccess.invoke(CobStatRecalculator.class, "setStepSuccess", wrapper.getIdCob(), stepFirst, "Шаг завершен успешно");
        checkGetInfo(wrapper, null, phaseFirst, Success, Running);

        baseEntityRepository.executeNativeUpdate("update GL_COB_STAT set ESTIMATED = ? where ID_COB = ? and PHASE_NO = ?", 0, wrapper.getIdCob(), phaseLast);
        CobStepStatistics stepLast = (CobStepStatistics) baseEntityRepository.findById(CobStepStatistics.class, new CobStatId(wrapper.getIdCob(), phaseLast));
        Assert.assertNotNull(stepLast);
        remoteAccess.invoke(CobStatRecalculator.class, "setStepStart", wrapper.getIdCob(), stepLast);
        checkGetInfo(wrapper, null, phaseLast, Running, Running);

        Thread.sleep(2000L);
        checkGetInfo(wrapper, null, phaseLast, Running, Running);

        remoteAccess.invoke(CobStatRecalculator.class, "setStepError", wrapper.getIdCob(), stepLast, "Шаг завершен с ошибкой",
            "Ошибка при выполнении шага " + CobStep.values()[0].name(), CobStepStatus.Error);
        CobWrapper wrapper1 = checkGetInfo(wrapper, null, phaseLast, Error, Error);
        Assert.assertNotNull(wrapper1.getErrorMessage());
        System.out.println("ErrorMessage: " + wrapper1.getErrorMessage());
    }

    @Test
    public void testCobRunningTaskController() {
        boolean res = remoteAccess.invoke(ExecutePreCOBTaskFake.class, "execWork");
//        Assert.assertTrue(res);
    }

    @Test
    public void testCobTaskNew() {
        TimerJob job = remoteAccess.invoke(BackgroundJobsController.class, "getJob", EtlStructureMonitorTask.class);
        if (job == null) {
            throw new RuntimeException(Utils.Fmt("Не найдено задание '{0}'.", "errorMessage"));
        }
        if (job.getState() == TimerJob.JobState.STARTED)
            throw new RuntimeException(Utils.Fmt("Задание '{0}' уже запущено.", "errorMessage"));

        remoteAccess.invoke(BackgroundJobsController.class, "executeJob", job);

        boolean res = remoteAccess.invoke(ExecutePreCOBTaskNew.class, "execWork", null, null);
        Assert.assertTrue(res);
    }

    private CobWrapper checkGetInfo(CobWrapper wr0, Long idCob, int phaseNo, CobStepStatus stepStatus, CobStepStatus totalStatus) {
        RpcRes_Base<CobWrapper> res = remoteAccess.invoke(CobStatService.class, "getCobInfo", idCob);
        Assert.assertFalse(res.isError());
        CobWrapper wrapper1 = res.getResult();
        Assert.assertEquals(wr0.getIdCob(), wrapper1.getIdCob());
        CobStepItem phase = wrapper1.getStepList().get(phaseNo - 1);
        CobStepItem total = wrapper1.getTotal();

        System.out.println("ID COB: " + wrapper1.getIdCob());
        printStepInfo(phase);
        checkStepState(phase, stepStatus);

        printStepInfo(total);
        checkStepState(total, totalStatus);
        return wrapper1;
    }

    private void checkStepState(CobStepItem step, CobStepStatus status) {
        Assert.assertEquals(status, step.getStatus());
//        if (BigDecimal.ZERO == step.getEstimation());   // TODO
        switch(status) {
            case NotStart:
                Assert.assertTrue(step.getDuration().compareTo(BigDecimal.ZERO) == 0);
                Assert.assertTrue(step.getPercent().compareTo(BigDecimal.ZERO) == 0);
                break;
            case Running:
                Assert.assertTrue(step.getDuration().compareTo(BigDecimal.ZERO) >= 0);
                Assert.assertTrue(step.getPercent().compareTo(BigDecimal.ZERO) >= 0);
                break;
            case Success:
                Assert.assertTrue(step.getDuration().compareTo(step.getEstimation()) <= 0);
                Assert.assertTrue(step.getPercent().compareTo(new BigDecimal(100)) == 0);
                break;
            case Error:
                Assert.assertTrue(step.getDuration().compareTo(step.getEstimation()) <= 0);
                Assert.assertTrue(step.getPercent().compareTo(new BigDecimal(100)) <= 0);
                break;
        }

    }

    private void setKoefIncrease(BigDecimal koef) {
        baseEntityRepository.executeNativeUpdate("delete from GL_PRPRP where ID_PRP = ?", PropertyName.COB_STAT_INC.getName());
        baseEntityRepository.executeNativeUpdate("insert into GL_PRPRP(ID_PRP, ID_PRN, REQUIRED, PRPTP, DESCRP, DECIMAL_VALUE, STRING_VALUE, NUMBER_VALUE)" +
                " VALUES (?, 'root', 'N', 'DECIMAL_TYPE', 'Коэффициент COB', ?, null, null)", PropertyName.COB_STAT_INC.getName(), koef);
        remoteAccess.invoke(PropertiesRepository.class, "flushCache");
    }

    private void printStepInfo(CobStepItem item) {
            System.out.printf("Phase: '%s'; status: '%s'; " +    //estimation = %s, duration = %s, percent = %s;" +
                " intEstimation = %s, intDuration = %s, intPercent = %s; %s\n",
                    item.getPhaseNo().toString(), item.getStatus().getLabel(),
                    item.getIntEstimation().toString(), item.getIntDuration().toString(), item.getIntPercent().toString(),
                    null != item.getMessage() ? ("message: " + item.getMessage()) : "");
    }
}
