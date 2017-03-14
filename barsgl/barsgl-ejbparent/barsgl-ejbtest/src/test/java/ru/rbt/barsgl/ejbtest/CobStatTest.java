package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.cob.CobStatService;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.barsgl.ejb.repository.cob.CobStatRepository;
import ru.rbt.barsgl.ejbcore.repository.PropertiesRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.cob.CobStepItem;
import ru.rbt.barsgl.shared.cob.CobWrapper;
import ru.rbt.barsgl.shared.enums.CobStep;
import ru.rbt.barsgl.shared.enums.CobStepStatus;

import java.math.BigDecimal;

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
        Assert.assertEquals(6, wrapper.getStepList().size());
        CobStepItem total = wrapper.getTotal();
        Assert.assertNotNull(total);
        checkStepState(total, Step_NotStart);
        Assert.assertNotEquals(BigDecimal.ZERO, total.getEstimation());
        System.out.println(total);
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
        checkGetInfo(wrapper, null, phaseFirst, Step_Running, Step_Running);

        Thread.sleep(2000L);
        remoteAccess.invoke(CobStatRepository.class, "updateStepMessage", wrapper.getIdCob(), phaseFirst, "Выполнен этап 2");
        checkGetInfo(wrapper, null, phaseFirst, Step_Running, Step_Running);

        remoteAccess.invoke(CobStatRepository.class, "setStepSuccess", wrapper.getIdCob(), phaseFirst, getSystemDateTime(), "Шаг завершен успешно");
        checkGetInfo(wrapper, null, phaseFirst, Step_Success, Step_Running);

        baseEntityRepository.executeNativeUpdate("update GL_COB_STAT set ESTIMATED = ? where ID_COB = ? and PHASE_NO = ?", 0, wrapper.getIdCob(), phaseLast);
        remoteAccess.invoke(CobStatRepository.class, "setStepStart", wrapper.getIdCob(), phaseLast, getSystemDateTime());
        checkGetInfo(wrapper, null, phaseLast, Step_Running, Step_Running);

        Thread.sleep(2000L);
        checkGetInfo(wrapper, null, phaseLast, Step_Running, Step_Running);

        remoteAccess.invoke(CobStatRepository.class, "setStepError", wrapper.getIdCob(), phaseLast, getSystemDateTime(), "Шаг завершен с ошибкой",
            "Ошибка при выполнении шага " + CobStep.values()[0].name());
        CobWrapper wrapper1 = checkGetInfo(wrapper, null, phaseLast, Step_Error, Step_Error);
        Assert.assertNotNull(wrapper1.getErrorMessage());
        System.out.println("ErrorMessage: " + wrapper1.getErrorMessage());
    }

    private CobWrapper checkGetInfo(CobWrapper wr0, Long idCob, int phaseNo, CobStepStatus stepStatus, CobStepStatus totalStatus) {
        RpcRes_Base<CobWrapper> res = remoteAccess.invoke(CobStatService.class, "getCobInfo", idCob);
        Assert.assertFalse(res.isError());
        CobWrapper wrapper1 = res.getResult();
        Assert.assertEquals(wr0.getIdCob(), wrapper1.getIdCob());
        CobStepItem phase = wrapper1.getStepList().get(phaseNo - 1);
        CobStepItem total = wrapper1.getTotal();

        System.out.println("ID COB: " + wrapper1.getIdCob());
        System.out.println(phase);
        checkStepState(phase, stepStatus);

        System.out.println(total);
        checkStepState(total, totalStatus);
        return wrapper1;
    }

    private void checkStepState(CobStepItem step, CobStepStatus status) {
        Assert.assertEquals(status, step.getStatus());
//        if (BigDecimal.ZERO == step.getEstimation());   // TODO
        switch(status) {
            case Step_NotStart:
                Assert.assertTrue(step.getDuration().compareTo(BigDecimal.ZERO) == 0);
                Assert.assertTrue(step.getPercent().compareTo(BigDecimal.ZERO) == 0);
                break;
            case Step_Running:
                Assert.assertTrue(step.getDuration().compareTo(BigDecimal.ZERO) >= 0);
                Assert.assertTrue(step.getPercent().compareTo(BigDecimal.ZERO) >= 0);
                break;
            case Step_Success:
                Assert.assertTrue(step.getDuration().compareTo(step.getEstimation()) <= 0);
                Assert.assertTrue(step.getPercent().compareTo(new BigDecimal(100)) == 0);
                break;
            case Step_Error:
                Assert.assertTrue(step.getDuration().compareTo(step.getEstimation()) <= 0);
                Assert.assertTrue(step.getPercent().compareTo(new BigDecimal(100)) == 0);
                break;
        }

    }

    private void setKoefIncrease(BigDecimal koef) {
        baseEntityRepository.executeNativeUpdate("delete from GL_PRPRP where ID_PRP = ?", PropertyName.COB_STAT_INC.getName());
        baseEntityRepository.executeNativeUpdate("insert into GL_PRPRP(ID_PRP, ID_PRN, REQUIRED, PRPTP, DESCRP, DECIMAL_VALUE, STRING_VALUE, NUMBER_VALUE)" +
                " VALUES (?, 'root', 'N', 'DECIMAL_TYPE', 'Коэффициент COB', ?, null, null)", PropertyName.COB_STAT_INC.getName(), koef);
        remoteAccess.invoke(PropertiesRepository.class, "flushCache");
    }
}
