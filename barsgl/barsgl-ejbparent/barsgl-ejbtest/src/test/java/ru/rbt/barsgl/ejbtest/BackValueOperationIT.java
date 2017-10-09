package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.BankCalendarDay;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.*;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.ClosedReportPeriodView;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.*;
import ru.rbt.barsgl.ejb.entity.sec.GLErrorRecord;
import ru.rbt.barsgl.ejb.integr.bg.BackValueOperationController;
import ru.rbt.barsgl.ejb.integr.bg.EtlPostingController;
import ru.rbt.barsgl.ejb.integr.bg.ReprocessPostingService;
import ru.rbt.barsgl.ejb.integr.oper.IncomingPostingProcessor;
import ru.rbt.barsgl.ejb.repository.BackValueOperationRepository;
import ru.rbt.barsgl.ejb.repository.GLErrorRepository;
import ru.rbt.barsgl.ejb.repository.dict.BVSouceCachedRepository;
import ru.rbt.barsgl.ejb.repository.dict.ClosedPeriodCashedRepository;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.BackValuePostStatus;
import ru.rbt.barsgl.shared.enums.ErrorCorrectType;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.CLOSED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.BUFFER;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.AUD;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.USD;
import static ru.rbt.barsgl.ejb.entity.etl.EtlPackage.PackageState.LOADED;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperClass.AUTOMATIC;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperClass.BV_MANUAL;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperationExt.BackValueReason.ClosedPeriod;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperationExt.BackValueReason.OverDepth;
import static ru.rbt.barsgl.ejbtest.ValidationIT.checkOperErrorRecord;
import static ru.rbt.barsgl.shared.enums.BackValuePostStatus.COMPLETED;
import static ru.rbt.barsgl.shared.enums.BackValuePostStatus.CONTROL;
import static ru.rbt.barsgl.shared.enums.DealSource.*;
import static ru.rbt.barsgl.shared.enums.OperState.*;

/**
 * Created by er18837 on 26.06.2017.
 */
public class BackValueOperationIT extends AbstractTimerJobIT {

    public static final Logger log = Logger.getLogger(BackValueOperationIT.class.getName());
    private static final String daysCriteria = " CCY = 'RUR' and DAT between '2015-01-31' and '2015-02-28' ";
    private static final String holidays =
            "'2015-02-01', '2015-02-07', '2015-02-08', '2015-02-14', '2015-02-15', '2015-02-21', '2015-02-22', '2015-02-23', '2015-02-28'";

    @BeforeClass
    public static void beforeAll() {

        try {
            setOperday(DateUtils.parseDate("27.02.2015", "dd.MM.yyyy"), DateUtils.parseDate("25.02.2015", "dd.MM.yyyy"), ONLINE, OPEN, BUFFER);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        setCalendar2015_02();

//        saveTable("GL_BVPARM");
//        saveTable("GL_CRPRD");

        setBVparams();
    }

/*
    @AfterClass
    public static  void  restoreTables() {
        restoreTable("GL_BVPARM");
        restoreTable("GL_CRPRD");
    }

    public static void saveTable(String tblName) {
        String tmpName = tblName + "_tmp";
        try {
            baseEntityRepository.executeNativeUpdate("drop table " + tmpName);
        } catch(Exception e) {}
        baseEntityRepository.executeNativeUpdate("create table " + tmpName + " as (select * from " + tblName + ") with data");
    }

    public static void restoreTable(String tblName) {
        String tmpName = tblName + "_tmp";
        baseEntityRepository.executeNativeUpdate("delete from " + tblName);
        baseEntityRepository.executeNativeUpdate("insert into " + tblName + " (select * from " + tmpName + ")");
        baseEntityRepository.executeNativeUpdate("drop table " + tmpName);
    }
*/

    // заполнить корректно календарь
    public static void setCalendar2015_02() {
        baseEntityRepository.executeNativeUpdate("update CAL set HOL = ' ', THOL = ' ' where " + daysCriteria + " and DAT not in (" + holidays + ")");
        baseEntityRepository.executeNativeUpdate("update CAL set HOL = 'X', THOL = 'X' where " + daysCriteria + " and DAT in (" + holidays + ")");
        baseEntityRepository.executeNativeUpdate("update CAL set HOL = ' ', THOL = 'T' where " + daysCriteria + " and DAT in ('2015-01-31')");
    }

    // заполнить параметры BackValue
    public static void setBVparams() {
        baseEntityRepository.executeNativeUpdate("delete from GL_BVPARM");
        baseEntityRepository.executeNativeUpdate("insert into GL_BVPARM (ID_SRC, BV_SHIFT, DTB, DTE) values ('ARMPRO',  3, '2015-01-01', '2016-12-31')");
        baseEntityRepository.executeNativeUpdate("insert into GL_BVPARM (ID_SRC, BV_SHIFT, DTB, DTE) values ('SECMOD', 14, '2015-01-01', '2016-12-31')");
        baseEntityRepository.executeNativeUpdate("insert into GL_BVPARM (ID_SRC, BV_SHIFT, DTB, DTE) values ('AXAPTA', 14, '2015-01-01', '2016-12-31')");
        baseEntityRepository.executeNativeUpdate("insert into GL_BVPARM (ID_SRC, BV_SHIFT, DTB, DTE) values ('ARMPRO',  2, '2017-01-01', null)");
        baseEntityRepository.executeNativeUpdate("insert into GL_BVPARM (ID_SRC, BV_SHIFT, DTB, DTE) values ('SECMOD',  5, '2017-01-01', null)");
        baseEntityRepository.executeNativeUpdate("insert into GL_BVPARM (ID_SRC, BV_SHIFT, DTB, DTE) values ('AXAPTA',  5, '2017-01-01', null)");

        baseEntityRepository.executeNativeUpdate("delete from GL_CRPRD");
        baseEntityRepository.executeNativeUpdate("insert into GL_CRPRD (PRD_LDATE, PRD_CUTDATE) values ('2014-12-31', '2015-01-15')");
        baseEntityRepository.executeNativeUpdate("insert into GL_CRPRD (PRD_LDATE, PRD_CUTDATE) values ('2015-01-31', '2015-02-03')");
        baseEntityRepository.executeNativeUpdate("insert into GL_CRPRD (PRD_LDATE, PRD_CUTDATE) values ('2015-02-28', '2015-03-04')");
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

        ClosedReportPeriodView period = remoteAccess.invoke(ClosedPeriodCashedRepository.class, "getPeriod");

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

        // ARMPRO на глубину Back Value
        Integer shiftArm = remoteAccess.invoke(BVSouceCachedRepository.class, "getDepth", ARMPRO.getLabel());
        Assert.assertNotNull(shiftArm);
        vdateARM = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", vdateARM, shiftArm, true);
        pst = createEtlPosting(vdateARM, ARMPRO.getLabel(), bsaDt, RUB, amt, bsaCt, RUB, amt);
        operClass = remoteAccess.invoke(IncomingPostingProcessor.class, "calculateOperationClass", pst);
        Assert.assertEquals(AUTOMATIC, operClass);

        // ARMPRO на глубину > Back Value
        vdateARM = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", vdateARM, 1, true);
        pst = createEtlPosting(vdateARM, ARMPRO.getLabel(), bsaDt, RUB, amt, bsaCt, RUB, amt);
        operClass = remoteAccess.invoke(IncomingPostingProcessor.class, "calculateOperationClass", pst);
        Assert.assertEquals(BV_MANUAL, operClass);

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
        Assert.assertEquals(BV_MANUAL, operClass);

        // SECMOD в закрытый период
        Integer shiftSEC = remoteAccess.invoke(BVSouceCachedRepository.class, "getDepth", SECMOD.getLabel());
        Assert.assertNotNull(shiftSEC);
        ClosedReportPeriodView period = remoteAccess.invoke(ClosedPeriodCashedRepository.class, "getPeriod");
        Assert.assertNotNull(period);
        Date operday = period.getCutDate();
        Date lwdate = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", operday, 1, false);
        Date cutDate = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", operday, shiftSEC, false);
        Date vdateSEC = period.getLastDate();
        Assert.assertTrue(cutDate.before(vdateSEC));

        setOperday(operday, lwdate, ONLINE, Operday.LastWorkdayStatus.OPEN);
        pst = createEtlPosting(vdateSEC, SECMOD.getLabel(), bsaDt, RUB, amt, bsaCt, RUB, amt);
        operClass = remoteAccess.invoke(IncomingPostingProcessor.class, "calculateOperationClass", pst);
        Assert.assertEquals(BV_MANUAL, operClass);

    }

    @Test
    public void testCreateBackValueOperation() throws SQLException {

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810%1");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810%3");
        BigDecimal amt = new BigDecimal("98.76");

        ClosedReportPeriodView period = remoteAccess.invoke(ClosedPeriodCashedRepository.class, "getPeriod");
        Assert.assertNotNull(period);

        Operday operday = getOperday();

        // PH неделю назад
        Date vdatePH = DateUtils.addDays(operday.getCurrentDate(), -7);
        EtlPosting pst = createEtlPosting(vdatePH, PaymentHub.getLabel(), bsaDt, RUB, amt, bsaCt, RUB, amt);
        Assert.assertNotNull(pst);

        GLBackValueOperation operation = (GLBackValueOperation) postingController.processMessage(pst);
        operation = (GLBackValueOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(BV_MANUAL, operation.getOperClass());
        Assert.assertEquals(BLOAD, operation.getState());
        Assert.assertEquals(pst.getValueDate(), operation.getPostDate());

        Assert.assertNotNull(operation.getOperExt());
        Assert.assertEquals(CONTROL, operation.getOperExt().getManualStatus());
        GLOperationExt operExt = (GLOperationExt) baseEntityRepository.findById(GLOperationExt.class, operation.getId());
        Assert.assertNotNull(operExt);
        Assert.assertEquals(operation.getPostDate(), operExt.getPostDatePlan());

        Assert.assertEquals(OverDepth.getValue(), operExt.getManualReason());
        Assert.assertEquals(operday.getLastWorkingDay(), operExt.getDepthCutDate());
        Assert.assertEquals(period.getCutDate(), operExt.getCloseCutDate());
        Assert.assertEquals(period.getLastDate(), operExt.getCloseLastDate());

        // SECMOD в закрытый период
        Integer shiftSEC = remoteAccess.invoke(BVSouceCachedRepository.class, "getDepth", SECMOD.getLabel());
        Assert.assertNotNull(shiftSEC);
        Date curdate = period.getCutDate();
        Date lwdate = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", curdate, 1, false);
        Date cutDate = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", curdate, shiftSEC, false);
        Date vdateSEC = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", period.getLastDate(), 1, false);
        Assert.assertTrue(cutDate.before(vdateSEC));

        setOperday(curdate, lwdate, ONLINE, Operday.LastWorkdayStatus.OPEN);

        pst.setSourcePosting(SECMOD.getLabel());
        pst.setValueDate(vdateSEC);
        operation = (GLBackValueOperation) postingController.processMessage(pst);
        operation = (GLBackValueOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(BV_MANUAL, operation.getOperClass());
        Assert.assertEquals(BLOAD, operation.getState());
        Assert.assertEquals(pst.getValueDate(), operation.getPostDate());

        Assert.assertNotNull(operation.getOperExt());
        Assert.assertEquals(CONTROL, operation.getOperExt().getManualStatus());
        operExt = (GLOperationExt) baseEntityRepository.findById(GLOperationExt.class, operation.getId());
        Assert.assertEquals(operation.getPostDate(), operExt.getPostDatePlan());

        Assert.assertEquals(ClosedPeriod.getValue(), operExt.getManualReason());
        Assert.assertEquals(cutDate, operExt.getDepthCutDate());
        Assert.assertEquals(period.getCutDate(), operExt.getCloseCutDate());
        Assert.assertEquals(period.getLastDate(), operExt.getCloseLastDate());

    }

    @Test
    public void testCreateBackWtacOperation() throws SQLException {

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810%1");
        String bsaCt = "00000810000000000000";
        BigDecimal amt = new BigDecimal("98.76");

        ClosedReportPeriodView period = remoteAccess.invoke(ClosedPeriodCashedRepository.class, "getPeriod");
        Assert.assertNotNull(period);

        Operday operday = getOperday();

        // PH неделю назад
        Date vdatePH = DateUtils.addDays(operday.getCurrentDate(), -7);
        EtlPosting pst = createEtlPosting(vdatePH, PaymentHub.getLabel(), bsaDt, RUB, amt, bsaCt, RUB, amt);
        Assert.assertNotNull(pst);

        GLBackValueOperation operation = (GLBackValueOperation) postingController.processMessage(pst);
        operation = (GLBackValueOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(BV_MANUAL, operation.getOperClass());
        Assert.assertEquals(BWTAC, operation.getState());
        Assert.assertNotNull(operation.getErrorMessage());
        System.out.println(operation.getErrorMessage());
        Assert.assertEquals(pst.getValueDate(), operation.getPostDate());

        Assert.assertNotNull(operation.getOperExt());
        Assert.assertEquals(CONTROL, operation.getOperExt().getManualStatus());
        GLOperationExt operExt = (GLOperationExt) baseEntityRepository.findById(GLOperationExt.class, operation.getId());
        Assert.assertNotNull(operExt);
        Assert.assertEquals(operation.getPostDate(), operExt.getPostDatePlan());

        Assert.assertEquals(OverDepth.getValue(), operExt.getManualReason());
        Assert.assertEquals(operday.getLastWorkingDay(), operExt.getDepthCutDate());
        Assert.assertEquals(period.getCutDate(), operExt.getCloseCutDate());
        Assert.assertEquals(period.getLastDate(), operExt.getCloseLastDate());

    }

    /**
     * формирование даты проводки, если дата валютирования - рабочий день
     */
    @Test
    public void testPostingWorkDate() throws Exception {
        EtlPosting postings[];
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817036%3");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817036%4");
        BigDecimal amt = new BigDecimal("76.54");
        BankCurrency currency = AUD;

        Date tomonth = DateUtils.parseDate("26.01.2015", "dd.MM.yyyy");
        Date prprev = DateUtils.parseDate("24.02.2015", "dd.MM.yyyy");
        Date prev = DateUtils.parseDate("25.02.2015", "dd.MM.yyyy");
        Date curr = DateUtils.parseDate("26.02.2015", "dd.MM.yyyy");

        postings = new EtlPosting[3];
        postings[0] = createEtlPosting(curr, KondorPlus.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);     // нестандартная
        postings[1] = createEtlPosting(curr, SECMOD.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);         // из справочника
        postings[2] = createEtlPosting(curr, PaymentHub.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);     // не из справочника

        // VDATE = сегодня: POSTDATE = опердень
        setOperday(curr, prev, ONLINE, OPEN);
        getOperday();
        GLOperation operation = (GLOperation) postingController.processMessage(postings[1]);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(POST, operation.getState());
        Assert.assertEquals(curr, operation.getPostDate());

        // VDATE = вчера, баланс открыт: POSTDATE = для всех вчера
        for(EtlPosting pst : postings) {
            pst.setValueDate(prev);
            operation = (GLOperation) postingController.processMessage(pst);
            operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
            Assert.assertEquals(POST, operation.getState());
            Assert.assertEquals(prev, operation.getPostDate());
        }

        // VDATE = вчера, баланс закрыт:
        setOperday(curr, prev, ONLINE, CLOSED);
        operation = (GLOperation) postingController.processMessage(postings[0]);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(POST, operation.getState());
        Assert.assertEquals(curr, operation.getPostDate());     // K+: POSTDATE = сегодня

        operation = (GLOperation) postingController.processMessage(postings[1]);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(POST, operation.getState());
        Assert.assertEquals(prev, operation.getPostDate());     // справ: POSTDATE = вчера

        operation = (GLOperation) postingController.processMessage(postings[2]);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(POST, operation.getState());
        Assert.assertEquals(curr, operation.getPostDate());     // не справ: POSTDATE = сегодня

        // VDATE = позавчера
        Date vdate = prprev;
        for(EtlPosting pst : postings)
            pst.setValueDate(vdate);

        operation = (GLOperation) postingController.processMessage(postings[0]);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(POST, operation.getState());
        Assert.assertEquals(curr, operation.getPostDate());     // K+: POSTDATE = сегодня, автоматическая обработка

        operation = (GLOperation) postingController.processMessage(postings[1]);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(POST, operation.getState());
        Assert.assertEquals(vdate, operation.getPostDate());     // справ: POSTDATE = VDATE, автоматическая обработка

        operation = (GLOperation) postingController.processMessage(postings[2]);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(BLOAD, operation.getState());
        Assert.assertEquals(vdate, operation.getPostDate());     // не справ: POSTDATE = VDATE, ручная обработка

        // VDATE = месяц назад
        vdate = tomonth;
        for(EtlPosting pst : postings)
            pst.setValueDate(vdate);

        operation = (GLOperation) postingController.processMessage(postings[0]);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(POST, operation.getState());
        Assert.assertEquals(curr, operation.getPostDate());     // K+: POSTDATE = сегодня, автоматическая обработка

        operation = (GLOperation) postingController.processMessage(postings[1]);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(BLOAD, operation.getState());
        Assert.assertEquals(vdate, operation.getPostDate());     // справ: POSTDATE = VDATE, ручная обработка

        operation = (GLOperation) postingController.processMessage(postings[2]);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(BLOAD, operation.getState());
        Assert.assertEquals(vdate, operation.getPostDate());     // не справ: POSTDATE = VDATE, ручная обработка
    }

    /**
     * формирование даты проводки, если дата валютирования - выходной
     */
    @Test
    public void testPostingHolidays() throws Exception {

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817036%7");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817036%8");
        BigDecimal amt = new BigDecimal("87.65");
        BankCurrency currency = AUD;

        // TODO подготовить календарь на январь-февраль 2015
        Date prev = DateUtils.parseDate("20.02.2015", "dd.MM.yyyy");
        Date hold = DateUtils.parseDate("22.02.2015", "dd.MM.yyyy");
        Date curr = DateUtils.parseDate("24.02.2015", "dd.MM.yyyy");

        List<DataRecord> days = baseEntityRepository.select("select * from cal where dat between ? and ? and ccy = 'RUR' and thol <> 'X'"
                , prev, curr);
        Assert.assertEquals(2, days.size());
        final Date finalCurr = curr;
        Assert.assertEquals(2, days.stream().filter(rec
                -> rec.getDate("dat").equals(prev) || rec.getDate("dat").equals(finalCurr)).collect(Collectors.toList()).size());

        setOperday(curr, prev, ONLINE, OPEN);

        // выходные между текущим и предыдущим ОД: текущий ОД
        EtlPosting pst = createEtlPosting(hold, PaymentHub.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);
        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(POST, operation.getState());
        Assert.assertEquals(getOperday().getCurrentDate(), operation.getCurrentDate());
        Assert.assertEquals(getOperday().getLastWorkdayStatus(), operation.getLastWorkdayStatus());
        Assert.assertEquals(operation.getPostDate()+"",curr, operation.getPostDate());

        // далеко назад выходной: следующий Рабочий день
        final Date longPrev = DateUtils.parseDate("07.02.2015", "dd.MM.yyyy");
        Assert.assertFalse(remoteAccess.invoke(BankCalendarDayRepository.class, "isWorkday", longPrev));
        pst.setValueDate(longPrev);
        operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(BLOAD, operation.getState());
        Assert.assertEquals(getOperday().getCurrentDate(), operation.getCurrentDate());
        Assert.assertEquals(getOperday().getLastWorkdayStatus(), operation.getLastWorkdayStatus());
        Date nextWork = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateAfter", longPrev, false);
        Assert.assertEquals(operation.getPostDate()+"", nextWork, operation.getPostDate());

        // последний день месяца: предыдущий Рабочий день
        final Date monthPrev = DateUtils.parseDate("31.01.2015", "dd.MM.yyyy");
        pst.setSourcePosting(KondorPlus.getLabel());    // для K+ дата формируется также, но проводка не задерживается
        pst.setValueDate(monthPrev);
        operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(POST, operation.getState());
        Assert.assertEquals(getOperday().getCurrentDate(), operation.getCurrentDate());
        Assert.assertEquals(getOperday().getLastWorkdayStatus(), operation.getLastWorkdayStatus());
        Date prevWork = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", monthPrev, false);
        Assert.assertEquals(operation.getPostDate()+"", prevWork, operation.getPostDate());

        // технический раб день для ARMPRO: остается техн раб день
        pst.setSourcePosting(ARMPRO.getLabel());
        operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(BLOAD, operation.getState());
        Assert.assertEquals(getOperday().getCurrentDate(), operation.getCurrentDate());
        Assert.assertEquals(getOperday().getLastWorkdayStatus(), operation.getLastWorkdayStatus());
        Assert.assertEquals(operation.getPostDate()+"", operation.getValueDate(), operation.getPostDate());

        // дата в будущем  - ошибка!!
        pst.setValueDate(DateUtils.addDays(curr, 1));
        operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(ERCHK, operation.getState());
        Assert.assertNotNull(operation.getProcDate());
        Assert.assertEquals(curr, operation.getProcDate());
    }

    /**
     * создвет операции в статусе BLOAD
     */
    @Test
    public void testPostingBV() throws Exception {
        EtlPosting postings[];
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817036%3");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817036%4");
        BigDecimal amt = new BigDecimal("76.54");
        BankCurrency currency = AUD;

        Date prprev = DateUtils.parseDate("24.02.2015", "dd.MM.yyyy");
        Date prev = DateUtils.parseDate("25.02.2015", "dd.MM.yyyy");
        Date curr = DateUtils.parseDate("26.02.2015", "dd.MM.yyyy");
        Date tomonth = DateUtils.parseDate("31.01.2015", "dd.MM.yyyy");
        Date vdmonth = DateUtils.parseDate("30.01.2015", "dd.MM.yyyy");

        postings = new EtlPosting[3];
        postings[0] = createEtlPosting(prprev, ARMPRO.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);        // ARMPRO
        postings[1] = createEtlPosting(prprev, SECMOD.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);        // из справочника
        postings[2] = createEtlPosting(prprev, PaymentHub.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);    // не из справочника

        setOperday(curr, prev, ONLINE, CLOSED);

        // VDATE = позавчера
        GLOperation operation = (GLOperation) postingController.processMessage(postings[2]);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(BLOAD, operation.getState());
        Assert.assertEquals(prprev, operation.getPostDate());     // не справ: POSTDATE = VDATE, ручная обработка

        // VDATE = конец прошлого месяца
        for(EtlPosting pst : postings)
            pst.setValueDate(tomonth);

        operation = (GLOperation) postingController.processMessage(postings[0]);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(BLOAD, operation.getState());
        Assert.assertEquals(tomonth, operation.getPostDate());     // справ: POSTDATE = VDATE, ручная обработка

        operation = (GLOperation) postingController.processMessage(postings[1]);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(BLOAD, operation.getState());
        Assert.assertEquals(vdmonth, operation.getPostDate());     // справ: POSTDATE = VDATE, ручная обработка

        operation = (GLOperation) postingController.processMessage(postings[2]);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(BLOAD, operation.getState());
        Assert.assertEquals(vdmonth, operation.getPostDate());     // не справ: POSTDATE = VDATE, ручная обработка

    }

    @Test
    public void testProcessOperation() throws SQLException {

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817036%5");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817036%6");
        BigDecimal amt = new BigDecimal("65.43");
        BankCurrency currency = AUD;

        Date vdate = DateUtils.addDays(getOperday().getCurrentDate(), -7);
        EtlPosting pst = createEtlPosting(vdate, PaymentHub.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);
        Assert.assertNotNull(pst);

        GLBackValueOperation operation = (GLBackValueOperation) postingController.processMessage(pst);
        operation = (GLBackValueOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(pst.getValueDate(), operation.getPostDate());

        baseEntityRepository.executeUpdate("update GLOperation o set o.postDate = ?1, o.equivalentDebit = ?2, o.equivalentCredit = ?3 where o.id = ?4",
                getOperday().getLastWorkingDay(), null, null, operation.getId());

        remoteAccess.invoke(BackValueOperationController.class, "processBackValueOperation", operation);
        operation = (GLBackValueOperation) baseEntityRepository.findById(GLBackValueOperation.class, operation.getId());
        Assert.assertEquals("GLOID = " + operation.getId(), POST, operation.getState());
        Assert.assertEquals("GLOID = " + operation.getId(), COMPLETED, operation.getOperExt().getManualStatus());

    }

    @Test
    public  void testProcessingTask() throws Exception {
        testPostingBV();
        Date curdate = getOperday().getCurrentDate();

        List<GLBackValueOperation> gloids = baseEntityRepository.select(GLBackValueOperation.class, "from GLBackValueOperation o where o.state = ?1 and o.operExt.manualStatus = ?2",
                OperState.BLOAD, CONTROL);
        Assert.assertFalse(gloids.isEmpty());
        String ops = gloids.stream().map(op -> " " + op.getId()).collect(Collectors.joining(","));
        System.out.println(ops);
        baseEntityRepository.executeUpdate("update GLOperation o set o.postDate = ?1, o.equivalentDebit = null, o.equivalentCredit = null, o.bsChapter = null where o.id in (" + ops + ")", //
                getOperday().getCurrentDate());
        baseEntityRepository.executeUpdate("update GLOperationExt e set e.manualStatus = ?1 where e.id in (" + ops + ")", BackValuePostStatus.SIGNEDDATE);

        List<GLBackValueOperation> operations = remoteAccess.invoke(BackValueOperationRepository.class, "getOperationsForProcessing", 0, curdate);
        int size = operations.size();
        Assert.assertTrue(size > 0);

        // обработка созданных BV операций
        jobService.executeJob(SingleActionJobBuilder.create().withClass(ProcessBackValueOperationsTask.class).build());

        for (GLBackValueOperation operation : operations) {
            GLBackValueOperation oper = (GLBackValueOperation) baseEntityRepository.findById(GLBackValueOperation.class, operation.getId());
            Assert.assertEquals("GLOID = " + oper.getId(), POST, oper.getState());
            Assert.assertEquals("GLOID = " + oper.getId(), COMPLETED, oper.getOperExt().getManualStatus());
            List<GLPosting> postList = getPostings(operation);
            for (GLPosting post: postList) {                // в каждой проводке:
                List<Pd> pdList = getPostingPd(post);
                for (Pd pd : pdList)
                    Assert.assertEquals(curdate, pd.getPod());
            }
        }
    }

    @Test
    public void testReprocessWtac() throws Exception {
        Operday od = getOperday();
        BankCalendarDay prevWork = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkdayBefore", od.getLastWorkingDay());
        setOperday(od.getLastWorkingDay(), prevWork.getId().getCalendarDate(), ONLINE, OPEN);

        testCreateBackWtacOperation();
        testCreateBackWtacOperation();

        List<GLBackValueOperation> gloids = baseEntityRepository.select(GLBackValueOperation.class,
                "from GLBackValueOperation o where o.state = ?1 and o.operExt.manualStatus = ?2 and o.currentDate = ?3 ",
                OperState.BWTAC, CONTROL, od.getLastWorkingDay());
        Assert.assertTrue(gloids.size() >= 2);

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810%2");
        String ops = gloids.stream().map(op -> " " + op.getId()).collect(Collectors.joining(","));
        baseEntityRepository.executeUpdate("update GLOperation o set o.accountCredit = ?1 where o.id in (" + ops + ")", bsaDt);
        baseEntityRepository.executeUpdate("update GLOperationExt e set e.manualStatus = ?1 where e.id in (" + gloids.get(0).getId() + ")", BackValuePostStatus.SIGNEDDATE);

        // обработка WTAC
        setOperday(od.getCurrentDate(), od.getLastWorkingDay(), ONLINE, OPEN);
        jobService.executeJob(SingleActionJobBuilder.create().withClass(ReprocessWtacOparationsTask.class).build());

        Thread.sleep(1000L);
        GLBackValueOperation oper1 = (GLBackValueOperation) baseEntityRepository.findById(GLBackValueOperation.class, gloids.get(0).getId());
        Assert.assertNotNull(oper1);
        Assert.assertEquals("GLOID = " + oper1.getId(), BLOAD, oper1.getState());

        GLBackValueOperation oper2 = (GLBackValueOperation) baseEntityRepository.findById(GLBackValueOperation.class, gloids.get(1).getId());
        Assert.assertNotNull(oper2);
        Assert.assertEquals("GLOID = " + oper2.getId(), BLOAD, oper2.getState());

        baseEntityRepository.executeUpdate("update GLOperationExt e set e.manualStatus = ?1 where e.id in (" + gloids.get(1).getId() + ")", BackValuePostStatus.SIGNEDDATE);

        // обработка BLOAD
        jobService.executeJob(SingleActionJobBuilder.create().withClass(ProcessBackValueOperationsTask.class).build());
        oper1 = (GLBackValueOperation) baseEntityRepository.findById(GLBackValueOperation.class, oper1.getId());
        Assert.assertNotNull(oper1);
        Assert.assertEquals("GLOID = " + oper1.getId(), POST, oper1.getState());
        Assert.assertEquals("GLOID = " + oper1.getId(), COMPLETED, oper1.getOperExt().getManualStatus());

        oper2 = (GLBackValueOperation) baseEntityRepository.findById(GLBackValueOperation.class, oper2.getId());
        Assert.assertNotNull(oper2);
        Assert.assertEquals("GLOID = " + oper2.getId(), POST, oper2.getState());
        Assert.assertEquals("GLOID = " + oper2.getId(), COMPLETED, oper2.getOperExt().getManualStatus());

    }

    @Test
    public void testReprocessStorno() throws Exception {
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817036%8");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817036%9");
        BigDecimal amtBv = new BigDecimal("18.65");
        BigDecimal amt = new BigDecimal("218.65");
        BankCurrency currency = AUD;

        Operday operday = getOperday();

        Date vdatePast = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", operday.getCurrentDate(), 10, false);
        EtlPosting pstBv = createEtlPosting(vdatePast, ARMPRO.getLabel(), bsaDt, currency, amtBv, bsaCt, currency, amtBv);        // ARMPRO
        EtlPackage pkg1 = pstBv.getEtlPackage();
        EtlPosting pstBvSt = newStornoPosting(System.currentTimeMillis(), pkg1, pstBv);
        pstBvSt.setValueDate(vdatePast);
        pstBvSt = (EtlPosting) baseEntityRepository.save(pstBvSt);

        Date vdatePrev = operday.getLastWorkingDay();
        EtlPosting pst = createEtlPosting(vdatePrev, ARMPRO.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);        // ARMPRO
        EtlPackage pkg2 = pst.getEtlPackage();
        EtlPosting pstSt = newStornoPosting(System.currentTimeMillis(), pkg1, pst);
        pstSt.setValueDate(vdatePrev);
        pstSt = (EtlPosting) baseEntityRepository.save(pstSt);

        // обработка пакета 1
        TimerJob mon = SingleActionJobBuilder.create().withClass(EtlStructureMonitorTask.class).build();
        baseEntityRepository.executeUpdate("update EtlPackage p set p.packageState = ?1 where p = ?2", LOADED, pkg1);
        jobService.executeJob(mon);

        GLBackValueOperation operBv = (GLBackValueOperation) baseEntityRepository.selectFirst(GLBackValueOperation.class,
                "from GLBackValueOperation o where o.etlPostingRef = ?1 ", pstBv.getId());
        Assert.assertEquals("GLOID = " + operBv.getId(), BLOAD, operBv.getState());
        Assert.assertEquals("GLOID = " + operBv.getId(), CONTROL, operBv.getOperExt().getManualStatus());
        GLBackValueOperation operBvSt = (GLBackValueOperation) baseEntityRepository.selectFirst(GLBackValueOperation.class,
                "from GLBackValueOperation o where o.etlPostingRef = ?1 ", pstBvSt.getId());
        Assert.assertEquals("GLOID = " + operBvSt.getId(), ERCHK, operBvSt.getState());
        Assert.assertNull("GLOID = " + operBvSt.getId(), operBvSt.getOperExt());

        GLOperation oper = (GLOperation) baseEntityRepository.selectFirst(GLOperation.class,
                "from GLOperation o where o.etlPostingRef = ?1 ", pst.getId());
        Assert.assertNull(oper);
        GLOperation operSt = (GLOperation) baseEntityRepository.selectFirst(GLOperation.class,
                "from GLOperation o where o.etlPostingRef = ?1 ", pstSt.getId());
        Assert.assertEquals("GLOID = " + operSt.getId(), ERCHK, operSt.getState());

        // авторизуем operBv операцию
        baseEntityRepository.executeUpdate("update GLOperationExt e set e.manualStatus = ?1 where e.id in (" + operBv.getId() + ")", BackValuePostStatus.SIGNEDDATE);

        // обработка авторизованной операции и пакета 2
        baseEntityRepository.executeUpdate("update EtlPackage p set p.packageState = ?1 where p = ?2", LOADED, pkg2);
        jobService.executeJob(mon);

        operBv = (GLBackValueOperation) baseEntityRepository.findById(GLBackValueOperation.class, operBv.getId());
        Assert.assertEquals("GLOID = " + operBv.getId(), POST, operBv.getState());
        Assert.assertEquals("GLOID = " + operBv.getId(), COMPLETED, operBv.getOperExt().getManualStatus());

        oper = (GLOperation) baseEntityRepository.selectFirst(GLOperation.class,
                "from GLOperation o where o.etlPostingRef = ?1 ", pst.getId());
        Assert.assertEquals("GLOID = " + oper.getId(), POST, oper.getState());

        remoteAccess.invoke(CloseLwdBalanceCutTask.class, "reprocessErckStorno", operday.getLastWorkingDay(), operday.getCurrentDate());
        operBvSt = (GLBackValueOperation) baseEntityRepository.findById(GLBackValueOperation.class, operBvSt.getId());
        Assert.assertEquals("GLOID = " + operBvSt.getId(), POST, operBvSt.getState());
        Assert.assertNull("GLOID = " + operBvSt.getId(), operBvSt.getOperExt());
        Assert.assertEquals("GLOID = " + operBvSt.getId(), vdatePast, operBvSt.getValueDate());
        Assert.assertEquals("GLOID = " + operBvSt.getId(), operday.getCurrentDate(), operBvSt.getPostDate());

        operSt = (GLOperation) baseEntityRepository.findById(GLOperation.class, operSt.getId());
        Assert.assertEquals("GLOID = " + operSt.getId(), POST, operSt.getState());
        Assert.assertEquals("GLOID = " + operSt.getId(), vdatePrev, operSt.getValueDate());
        Assert.assertEquals("GLOID = " + operSt.getId(), vdatePrev, operSt.getPostDate());

    }

    /**
     * Проверка корреспонденции счетов - разная глава баланса (ошибка операции)
     * @fsd 7.4.1
     */
    @Test public void testReprocessBERCHK() throws Exception {

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40502840%");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "47425840%");
        String bsaCt9 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "91418840%");
        BigDecimal amt = new BigDecimal("331.56");
        BankCurrency currency = USD;

        Operday operday = getOperday();
        Date vdatePast = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", operday.getCurrentDate(), 10, false);

        EtlPosting pst = createEtlPosting(vdatePast, ARMPRO.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);        // ARMPRO
        Assert.assertNotNull(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        operation = (GLBackValueOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals("GLOID = " + operation.getId(), pst.getValueDate(), operation.getPostDate());
        Assert.assertEquals("GLOID = " + operation.getId(), BLOAD, operation.getState());

        baseEntityRepository.executeUpdate("update GLOperation o set o.accountCredit = ?1, o.bsChapter = null where o.id = ?2", bsaCt9, operation.getId());
        baseEntityRepository.executeUpdate("update GLOperationExt e set e.manualStatus = ?1 where e.id = ?2", BackValuePostStatus.SIGNEDDATE, operation.getId());
        operation = (GLBackValueOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());

        remoteAccess.invoke(BackValueOperationController.class, "processBackValueOperation", operation);
        operation = (GLBackValueOperation) baseEntityRepository.findById(GLBackValueOperation.class, operation.getId());
        checkOperErrorRecord(operation, "1005", OperState.BERCHK);

        baseEntityRepository.executeUpdate("update GLOperation o set o.accountCredit = ?1, o.bsChapter = null where o.id = ?2", bsaCt, operation.getId());

        GLErrorRecord err = getOperationErrorRecord(operation);
        List<Long> errorIdList = new ArrayList<>();
        errorIdList.add(err.getId());
        // correctErrors (List<Long> errorIdList, String comment, String idPstCorr, ErrorCorrectType correctType)
        RpcRes_Base<Integer> res = remoteAccess.invoke(ReprocessPostingService.class, "correctErrors",
                errorIdList, "testReprocessOne", null, ErrorCorrectType.REPROCESS_ONE);

        Assert.assertFalse(res.isError());
        System.out.println(res.getMessage());
        operation = (GLBackValueOperation) baseEntityRepository.findById(GLBackValueOperation.class, operation.getId());
        Assert.assertEquals("GLOID = " + operation.getId(), BLOAD, operation.getState());

        // обработка BLOAD
        jobService.executeJob(SingleActionJobBuilder.create().withClass(ProcessBackValueOperationsTask.class).build());
        GLBackValueOperation bvOperation = (GLBackValueOperation) baseEntityRepository.findById(GLBackValueOperation.class, operation.getId());
        Assert.assertNotNull(bvOperation);
        Assert.assertEquals("GLOID = " + bvOperation.getId(), POST, bvOperation.getState());
        Assert.assertEquals("GLOID = " + bvOperation.getId(), COMPLETED, bvOperation.getOperExt().getManualStatus());
    }


    public static EtlPosting createEtlPosting(Date valueDate, String src,
                                        String accountDebit, BankCurrency currencyDebit, BigDecimal amountDebit,
                                        String accountCredit, BankCurrency currencyCredit, BigDecimal amountCredit) {
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "BackValue");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = createEtlPostingNotSaved(pkg, valueDate, src,
                accountDebit, currencyDebit, amountDebit,
                accountCredit, currencyCredit, amountCredit);

        return (EtlPosting) baseEntityRepository.save(pst);
    }

    public static EtlPosting createEtlPostingNotSaved(EtlPackage pkg, Date valueDate, String src,
                                        String accountDebit, BankCurrency currencyDebit, BigDecimal amountDebit,
                                        String accountCredit, BankCurrency currencyCredit, BigDecimal amountCredit) {
        long stamp = System.currentTimeMillis();

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

        return pst;
    }

    public static GLErrorRecord getOperationErrorRecord(GLOperation operation) {
        Assert.assertNotNull(operation);
        Long gloid = operation.getId();
        GLOperation oper = (GLOperation) baseEntityRepository.refresh(operation, true);
        Assert.assertNotEquals(POST, oper.getState());
        String errorMessage = oper.getErrorMessage();
        Assert.assertNotNull(errorMessage);

        GLErrorRecord errorRecord = remoteAccess.invoke(GLErrorRepository.class, "getRecordByRef", null, gloid);
        Assert.assertNotNull(errorRecord);
        return errorRecord;
    }

}
