package ru.rbt.barsgl.ejbtest;

import org.junit.*;
import ru.rbt.barsgl.ejb.controller.cob.CobStatRecalculator;
import ru.rbt.barsgl.ejb.controller.cob.CobStatService;
import ru.rbt.barsgl.ejb.controller.operday.task.ExecutePreCOBTaskFake;
import ru.rbt.barsgl.ejb.controller.operday.task.ExecutePreCOBTaskNew;
import ru.rbt.barsgl.ejb.entity.cob.CobStatId;
import ru.rbt.barsgl.ejb.entity.cob.CobStepStatistics;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.barsgl.ejb.repository.cob.CobStatRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.cob.CobStepItem;
import ru.rbt.barsgl.shared.cob.CobWrapper;
import ru.rbt.barsgl.shared.enums.CobPhase;
import ru.rbt.barsgl.shared.enums.CobStepStatus;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;
import ru.rbt.ejb.repository.properties.PropertiesRepository;

import java.math.BigDecimal;
import java.util.List;

import static ru.rbt.barsgl.shared.enums.CobStepStatus.Error;
import static ru.rbt.barsgl.shared.enums.CobStepStatus.*;

/**
 * Created by ER18837 on 13.03.17.
 * Тестирование статистики по PreCob
 */
public class CobStatTest extends AbstractTimerJobTest  {
    private final CobPhase phaseFirst = CobPhase.values()[0];
    private final CobPhase phaseLast = CobPhase.values()[CobPhase.values().length-1];

    @BeforeClass
    public static void beforeClass() {
        initCorrectOperday();
    }

    @AfterClass
    public static void afterClass() {
        initCorrectOperday();
    }

    @Before
    public void before() {
        long maxCob = remoteAccess.invoke(CobStatRepository.class, "getMaxRunCobId", getOperday().getCurrentDate());
        if (maxCob > 0) {
            baseEntityRepository.executeNativeUpdate("update GL_COB_STAT set STATUS = ? where ID_COB = ? and STATUS in (?, ?)", Halt.name(), maxCob, Running.name(), Error.name());
        }

    }

    @Test
    public void testCreateStatistics () {
        RpcRes_Base<CobWrapper> res = remoteAccess.invoke(CobStatService.class, "calculateCob");
        Assert.assertFalse(res.isError());
        System.out.println(res.getMessage());

        CobWrapper wrapper = res.getResult();
        Assert.assertTrue(wrapper.getIdCob() > 0);
        Assert.assertEquals(CobPhase.values().length, wrapper.getStepList().size());
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
        baseEntityRepository.executeNativeUpdate("update GL_COB_STAT set ESTIMATED = ? where ID_COB = ? and PHASE_NO = ?", 3, wrapper.getIdCob(), phaseFirst.getPhaseNo());
        CobStepStatistics stepFirst = (CobStepStatistics) baseEntityRepository.findById(CobStepStatistics.class, new CobStatId(wrapper.getIdCob(), phaseFirst.getPhaseNo()));
        Assert.assertNotNull(stepFirst);
        remoteAccess.invoke(CobStatRecalculator.class, "setStepStart", wrapper.getIdCob(), stepFirst,  phaseFirst);

        Thread.sleep(2000L);
        remoteAccess.invoke(CobStatRecalculator.class, "addStepInfo", wrapper.getIdCob(), phaseFirst, "Выполнен этап 1");
        checkGetInfo(wrapper, phaseFirst, Running, Running);

        Thread.sleep(2000L);
        remoteAccess.invoke(CobStatRecalculator.class, "addStepInfo", wrapper.getIdCob(), phaseFirst, "Выполнен этап 2");
        checkGetInfo(wrapper, phaseFirst, Running, Running);

        remoteAccess.invoke(CobStatRecalculator.class, "setStepSuccess", wrapper.getIdCob(), stepFirst, phaseFirst, "Шаг завершен успешно");
        checkGetInfo(wrapper, phaseFirst, Success, Running);

        baseEntityRepository.executeNativeUpdate("update GL_COB_STAT set ESTIMATED = ? where ID_COB = ? and PHASE_NO = ?", 0, wrapper.getIdCob(), phaseLast.getPhaseNo());
        CobStepStatistics stepLast = (CobStepStatistics) baseEntityRepository.findById(CobStepStatistics.class, new CobStatId(wrapper.getIdCob(), phaseLast.getPhaseNo()));
        Assert.assertNotNull(stepLast);
        remoteAccess.invoke(CobStatRecalculator.class, "setStepStart", wrapper.getIdCob(), stepLast, phaseLast);
        checkGetInfo(wrapper, phaseLast, Running, Running);

        Thread.sleep(2000L);
        checkGetInfo(wrapper, phaseLast, Running, Running);

        String longMsg = " 123456789";
        for (int i = 0; i < 8; i++)
            longMsg += longMsg;     // 10 * 2 ^ 8
        remoteAccess.invoke(CobStatRecalculator.class, "addStepInfo", wrapper.getIdCob(), phaseFirst, longMsg);
        remoteAccess.invoke(CobStatRecalculator.class, "setStepError", wrapper.getIdCob(), stepLast, phaseLast,
                "Шаг завершен с ошибкой " + longMsg,
                "Ошибка при выполнении шага " + CobPhase.values()[0].name(), CobStepStatus.Error);
        CobWrapper wrapper1 = checkGetInfo(wrapper, phaseLast, Error, Error);
        Assert.assertNotNull(wrapper1.getErrorMessage());
        System.out.println("ErrorMessage: " + wrapper1.getErrorMessage());
    }

    @Test
    public void testCobRunningTaskController() {
        remoteAccess.invoke(ExecutePreCOBTaskFake.class, "execWork");

        Long idCob = null;
        RpcRes_Base<CobWrapper> res = remoteAccess.invoke(CobStatService.class, "getCobInfo", idCob);
        CobWrapper wrapper1 = res.getResult();
        System.out.println("ID COB: " + wrapper1.getIdCob());

        List<CobStepItem> itemList = wrapper1.getStepList();
        int i = 0;
        for (CobStepStatus stepStatus: ExecutePreCOBTaskFake.results) {
            CobStepItem item = itemList.get(i++);
            printStepInfo(item);
            checkStepState(item, stepStatus);
        }

        CobStepItem totalItem = wrapper1.getTotal();
        printStepInfo(totalItem);
        checkStepState(totalItem, itemList.get(--i).getStatus());
    }

    @Test
    public void testCobTaskNew() throws InterruptedException {
/*
        String monitorName = EtlStructureMonitorTask.class.getSimpleName();
        TimerJob job = remoteAccess.invoke(BackgroundJobsController.class, "getJob", monitorName);
        if (job == null) {
            throw new RuntimeException(Utils.Fmt("Не найдено задание '{0}'.", monitorName));
        }
        if (job.getState() != TimerJob.JobState.STARTED) {
            remoteAccess.invoke(BackgroundJobsController.class, "startupJob", job);
        }
*/

        baseEntityRepository.executeNativeUpdate("update gl_od set prc = ?", ProcessingStatus.STOPPED.name());

        boolean ex = remoteAccess.invoke(ExecutePreCOBTaskNew.class, "execWork", null, null);
        Assert.assertTrue(ex);

        Long idCob = null;
        RpcRes_Base<CobWrapper> res = remoteAccess.invoke(CobStatService.class, "getCobInfo", idCob);
        CobWrapper wrapper1 = res.getResult();
        System.out.println("ID COB: " + wrapper1.getIdCob());

        List<CobStepItem> itemList = wrapper1.getStepList();
        int i = 0;
        for (CobStepItem item : itemList) {
            printStepInfo(item);
            Assert.assertTrue(item.getStatus() == Success || item.getStatus() == Skipped);
            checkStepState(item, item.getStatus());
        }

        CobStepItem totalItem = wrapper1.getTotal();
        printStepInfo(totalItem);
        checkStepState(totalItem, Success);
    }

    private CobWrapper checkGetInfo(CobWrapper wr0, CobPhase phase, CobStepStatus stepStatus, CobStepStatus totalStatus) {
        Long idCob = null;
        RpcRes_Base<CobWrapper> res = remoteAccess.invoke(CobStatService.class, "getCobInfo", idCob);
        Assert.assertFalse(res.isError());
        CobWrapper wrapper1 = res.getResult();
        Assert.assertEquals(wr0.getIdCob(), wrapper1.getIdCob());
        CobStepItem stepItem = wrapper1.getStepList().get(phase.getPhaseNo() - 1);
        CobStepItem totalItem = wrapper1.getTotal();

        System.out.println("ID COB: " + wrapper1.getIdCob());
        printStepInfo(stepItem);
        checkStepState(stepItem, stepStatus);

        printStepInfo(totalItem);
        checkStepState(totalItem, totalStatus);
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
