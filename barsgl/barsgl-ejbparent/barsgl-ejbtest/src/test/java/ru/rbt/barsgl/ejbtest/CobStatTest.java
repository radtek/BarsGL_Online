package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.cob.CobStatService;
import ru.rbt.barsgl.ejb.controller.operday.task.ExecutePreCOBTaskFake;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.barsgl.ejb.repository.cob.CobStatRepository;
import ru.rbt.barsgl.ejbcore.repository.PropertiesRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
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
        baseEntityRepository.executeNativeUpdate("update GL_COB_STAT set ESTIMATED = ? where ID_COB = ? and PHASE_NO = ?", 3, wrapper.getIdCob(), 1);
        remoteAccess.invoke(CobStatRepository.class, "setStepStart", wrapper.getIdCob(), phaseFirst, getSystemDateTime());

        Thread.sleep(2000L);
        remoteAccess.invoke(CobStatRepository.class, "updateStepMessage", wrapper.getIdCob(), phaseFirst, "Выполнен этап 1");
        checkGetInfo(wrapper, null, phaseFirst, Running, Running);

        Thread.sleep(2000L);
        remoteAccess.invoke(CobStatRepository.class, "updateStepMessage", wrapper.getIdCob(), phaseFirst, "Выполнен этап 2");
        checkGetInfo(wrapper, null, phaseFirst, Running, Running);

        remoteAccess.invoke(CobStatRepository.class, "setStepSuccess", wrapper.getIdCob(), phaseFirst, getSystemDateTime(), "Шаг завершен успешно");
        checkGetInfo(wrapper, null, phaseFirst, Success, Running);

        baseEntityRepository.executeNativeUpdate("update GL_COB_STAT set ESTIMATED = ? where ID_COB = ? and PHASE_NO = ?", 0, wrapper.getIdCob(), phaseLast);
        remoteAccess.invoke(CobStatRepository.class, "setStepStart", wrapper.getIdCob(), phaseLast, getSystemDateTime());
        checkGetInfo(wrapper, null, phaseLast, Running, Running);

        Thread.sleep(2000L);
        checkGetInfo(wrapper, null, phaseLast, Running, Running);

        remoteAccess.invoke(CobStatRepository.class, "setStepError", wrapper.getIdCob(), phaseLast, getSystemDateTime(), "Шаг завершен с ошибкой",
            "Ошибка при выполнении шага " + CobStep.values()[0].name());
        CobWrapper wrapper1 = checkGetInfo(wrapper, null, phaseLast, Error, Error);
        Assert.assertNotNull(wrapper1.getErrorMessage());
        System.out.println("ErrorMessage: " + wrapper1.getErrorMessage());
    }

    @Test
    public void testCobRunningTaskController() {
        boolean res = remoteAccess.invoke(ExecutePreCOBTaskFake.class, "execWork");
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
