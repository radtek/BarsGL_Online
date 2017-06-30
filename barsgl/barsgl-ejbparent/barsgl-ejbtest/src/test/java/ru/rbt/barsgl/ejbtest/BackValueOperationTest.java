package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.ClosedPeriodView;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLBackValueOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperationExt;
import ru.rbt.barsgl.ejb.integr.bg.EtlPostingController;
import ru.rbt.barsgl.ejb.integr.oper.IncomingPostingProcessor;
import ru.rbt.barsgl.ejb.repository.dict.BVSouceCachedRepository;
import ru.rbt.barsgl.ejb.repository.dict.ClosedPeriodCashedRepository;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.enums.BackValuePostStatus;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Logger;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperClass.AUTOMATIC;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperClass.BACK_VALUE;
import static ru.rbt.barsgl.shared.enums.DealSource.*;

/**
 * Created by er18837 on 26.06.2017.
 */
public class BackValueOperationTest extends AbstractTimerJobTest {

    public static final Logger log = Logger.getLogger(BackValueOperationTest.class.getName());

    // TODO проверить / заполнить таблицы GL_BVPARM, GL_CRPRD;

    @Test
    public void testFindOperationExt() throws SQLException {
        DataRecord data = baseEntityRepository.selectFirst("select max(GLOID) from GL_OPEREXT");
        Long gloExt = data.getLong(0);

        baseEntityRepository.executeNativeUpdate("update GL_OPER set OPER_CLASS = ? where GLOID = ?",
                GLOperation.OperClass.BACK_VALUE.name(), gloExt);

        GLOperationExt operExt = (GLOperationExt) baseEntityRepository.findById(GLOperationExt.class, gloExt);
        Assert.assertNotNull(operExt);
//        Long gloExt = 42260L;
        GLOperation oper = (GLOperation) baseEntityRepository.findById(GLOperation.class, gloExt);
        Assert.assertNotNull(oper);
        GLBackValueOperation operBV = (GLBackValueOperation) baseEntityRepository.findById(GLBackValueOperation.class, gloExt);
        Assert.assertNotNull(operBV);
        Assert.assertNotNull(operBV.getOperExt().getManualStatus());
    }

    @Test
    public void testCashedRepository() throws SQLException {
        String src = ARMPRO.name();
        Date curdate = getOperday().getCurrentDate();
        DataRecord data = baseEntityRepository.selectFirst("select BV_SHIFT from GL_BVPARM where ID_SRC = ? and DTB <= ?" +
                " and (DTE is null or DTE >= ?)", src, curdate, curdate);

        Integer shift1 = remoteAccess.invoke(BVSouceCachedRepository.class, "getDepth", src);
        Integer shift0 = data.getInteger(0);
        Assert.assertEquals(shift0, shift1);

        DataRecord data1 = baseEntityRepository.selectFirst("select PRD_LDATE, PRD_CUTDATE from V_GL_CRPRD");

        ClosedPeriodView period = remoteAccess.invoke(ClosedPeriodCashedRepository.class, "getPeriod");

        Assert.assertEquals(data1.getDate(0), period.getLastDate());
        Assert.assertEquals(data1.getDate(1), period.getCutDate());
    }

    @Test
    public void testCalculateOperationClass() throws SQLException {
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810%1");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810%3");
        BigDecimal amt = new BigDecimal("45.67");
        Date vdateARM = getOperday().getCurrentDate();

        // ARMPRO сегодня
        EtlPosting pst = createEtlPosting(vdateARM, ARMPRO.getLabel(), bsaDt, RUB, amt, bsaCt, RUB, amt);
        GLOperation.OperClass operClass = remoteAccess.invoke(IncomingPostingProcessor.class, "calculateOperationClass", pst);
        Assert.assertEquals(AUTOMATIC, operClass);

        // ARMPRO на глубину BACK_VALUE
        Integer shiftArm = remoteAccess.invoke(BVSouceCachedRepository.class, "getDepth", ARMPRO.getLabel());
        Assert.assertNotNull(shiftArm);
        vdateARM = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", vdateARM, shiftArm, true);
        pst = createEtlPosting(vdateARM, ARMPRO.getLabel(), bsaDt, RUB, amt, bsaCt, RUB, amt);
        operClass = remoteAccess.invoke(IncomingPostingProcessor.class, "calculateOperationClass", pst);
        Assert.assertEquals(AUTOMATIC, operClass);

        // ARMPRO на глубину > BACK_VALUE
        vdateARM = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", vdateARM, 1, true);
        pst = createEtlPosting(vdateARM, ARMPRO.getLabel(), bsaDt, RUB, amt, bsaCt, RUB, amt);
        operClass = remoteAccess.invoke(IncomingPostingProcessor.class, "calculateOperationClass", pst);
        Assert.assertEquals(BACK_VALUE, operClass);
//        Assert.assertEquals(OverDepth.getValue(), pst.getReason());

        // PH в прошлый день
        Integer shiftPH = remoteAccess.invoke(BVSouceCachedRepository.class, "getDepth", PaymentHub.getLabel());
        Assert.assertNull(shiftPH);
        Date vdatePH = getOperday().getLastWorkingDay();
        pst = createEtlPosting(vdatePH, PaymentHub.getLabel(), bsaDt, RUB, amt, bsaCt, RUB, amt);
        operClass = remoteAccess.invoke(IncomingPostingProcessor.class, "calculateOperationClass", pst);
        Assert.assertEquals(AUTOMATIC, operClass);

        // PH гдубже
        vdatePH = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", vdatePH, 1, false);
        pst = createEtlPosting(vdatePH, PaymentHub.getLabel(), bsaDt, RUB, amt, bsaCt, RUB, amt);
        operClass = remoteAccess.invoke(IncomingPostingProcessor.class, "calculateOperationClass", pst);
        Assert.assertEquals(BACK_VALUE, operClass);
//        Assert.assertEquals(OverDepth.getValue(), pst.getReason());

        // SECMOD в закрытый период
        Integer shiftSEC = remoteAccess.invoke(BVSouceCachedRepository.class, "getDepth", SECMOD.getLabel());
        Assert.assertNotNull(shiftSEC);
        ClosedPeriodView period = remoteAccess.invoke(ClosedPeriodCashedRepository.class, "getPeriod");
        Assert.assertNotNull(period);
        Date operday = period.getCutDate();
        Date lwdate = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", operday, 1, false);
        setOperday(operday, lwdate, ONLINE, Operday.LastWorkdayStatus.OPEN);
        Date cutDate = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", operday, shiftSEC, false);
        Date vdateSEC = period.getLastDate();
        Assert.assertTrue(cutDate.before(vdateSEC));

        pst = createEtlPosting(vdateSEC, SECMOD.getLabel(), bsaDt, RUB, amt, bsaCt, RUB, amt);
        operClass = remoteAccess.invoke(IncomingPostingProcessor.class, "calculateOperationClass", pst);
        Assert.assertEquals(BACK_VALUE, operClass);
//        Assert.assertEquals(ClosedPeriod.getValue(), pst.getReason());

    }

    @Test
    public void testCreatBackValueOperation() throws SQLException {

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810%1");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810%3");

        BigDecimal amt = new BigDecimal("98.76");

        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(getOperday().getCurrentDate());
        cal.add(Calendar.DATE, -7);
        Date valueDate = cal.getTime();
        EtlPosting pst = createEtlPosting(valueDate, PaymentHub.getLabel(), bsaDt, RUB, amt, bsaCt, RUB, amt);
        Assert.assertNotNull(pst);

        GLBackValueOperation operation = (GLBackValueOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLBackValueOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(BACK_VALUE, operation.getOperClass());
        Assert.assertEquals(OperState.BLOAD, operation.getState());
        Assert.assertEquals(getOperday().getCurrentDate(), operation.getCurrentDate());
        Assert.assertEquals(pst.getValueDate(), operation.getPostDate());

    }

    @Test
    public void testCreatOperationExt() throws SQLException {

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810%1");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810%3");

        BigDecimal amt = new BigDecimal("98.76");

        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(getOperday().getCurrentDate());
        cal.add(Calendar.DATE, -7);
        Date valueDate = cal.getTime();
        EtlPosting pst = createEtlPosting(valueDate, PaymentHub.getLabel(), bsaDt, RUB, amt, bsaCt, RUB, amt);
        Assert.assertNotNull(pst);

        GLBackValueOperation operation = remoteAccess.invoke(EtlPostingController.class, "processBackValue", pst);
        Assert.assertNotNull(operation);
//        Assert.assertNotNull(operation.getOperExt());

        GLOperationExt operExt = (GLOperationExt) baseEntityRepository.findById(GLOperationExt.class, operation.getId());
        Assert.assertNotNull(operExt);

        Assert.assertEquals(BackValuePostStatus.CONTROL, operExt.getManualStatus());
        Assert.assertEquals(operation.getPostDate(), operExt.getPostDatePlan());

    }

    private EtlPosting createEtlPosting(Date valueDate, String src,
                                        String accountDebit, BankCurrency currencyDebit, BigDecimal amountDebit,
                                        String accountCredit, BankCurrency currencyCredit, BigDecimal amountCredit) {
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "BackValue");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(valueDate);
        pst.setSourcePosting(src);
        pst.setDealId("DEAL_" + stamp);

        pst.setAccountDebit(accountDebit);
        pst.setCurrencyDebit(currencyDebit);
        pst.setAmountDebit(amountDebit);

        pst.setAccountCredit(accountCredit);
        pst.setCurrencyCredit(currencyCredit);
        pst.setAmountCredit(amountCredit);

        return (EtlPosting) baseEntityRepository.save(pst);
    }

}
