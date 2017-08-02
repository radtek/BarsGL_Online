package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.EtlStructureMonitorTask;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.SourcesDeals;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLBackValueOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperationExt;
import ru.rbt.barsgl.ejb.integr.bg.BackValueOperationController;
import ru.rbt.barsgl.ejb.integr.bg.BackValuePostingController;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.criteria.*;
import ru.rbt.barsgl.shared.enums.*;
import ru.rbt.barsgl.shared.operation.BackValueWrapper;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.AUD;
import static ru.rbt.barsgl.ejb.entity.etl.EtlPackage.PackageState.LOADED;
import static ru.rbt.barsgl.ejbtest.BackValueOperationTest.*;
import static ru.rbt.barsgl.shared.criteria.CriterionColumn.createCriterion;
import static ru.rbt.barsgl.shared.enums.BackValueAction.SIGN;
import static ru.rbt.barsgl.shared.enums.BackValueAction.STAT;
import static ru.rbt.barsgl.shared.enums.BackValueAction.TO_HOLD;
import static ru.rbt.barsgl.shared.enums.BackValueMode.ALL;
import static ru.rbt.barsgl.shared.enums.BackValueMode.ONE;
import static ru.rbt.barsgl.shared.enums.BackValueMode.VISIBLE;
import static ru.rbt.barsgl.shared.enums.BackValuePostStatus.*;
import static ru.rbt.barsgl.shared.enums.DealSource.ARMPRO;
import static ru.rbt.barsgl.shared.enums.DealSource.PaymentHub;

/**
 * Created by er18837 on 20.07.2017.
 */
public class BackValueAuthTest extends AbstractTimerJobTest {

    public static final Logger log = Logger.getLogger(BackValueAuthTest.class.getName());
    private final Long USER_ID = 2L;

    @BeforeClass
    public static void beforeAll() {

        try {
            setOperday(DateUtils.parseDate("27.02.2015", "dd.MM.yyyy"), DateUtils.parseDate("25.02.2015", "dd.MM.yyyy"), ONLINE, OPEN);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        setCalendar2015_02();

        saveTable("GL_BVPARM");
        saveTable("GL_CRPRD");

        setBVparams();
    }

    /**
     * Задержать одну операцию
     * @throws SQLException
     */
    @Test
    public void testHoldOne() throws SQLException {
        /**
         * создать операцию BV в статусе CONTROL
         * авторизовать
         * запустить обработку
         */
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817036%5");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817036%7");
        BigDecimal amt = new BigDecimal("44.88");
        BankCurrency currency = AUD;

        Date vdate = DateUtils.addDays(getOperday().getCurrentDate(), -7);
        EtlPosting pst = createEtlPosting(vdate, PaymentHub.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);
        Assert.assertNotNull(pst);

        GLBackValueOperation operation = (GLBackValueOperation) postingController.processMessage(pst);
        operation = (GLBackValueOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(pst.getValueDate(), operation.getPostDate());
        Assert.assertEquals(CONTROL, operation.getOperExt().getManualStatus());

        BackValueWrapper wrapper = createWrapper(vdate, CONTROL, TO_HOLD, ONE, new GLOperation[]{operation});
        wrapper.setComment("Тест 'testHoldOne'");
        RpcRes_Base<Integer> res = remoteAccess.invoke(BackValuePostingController.class, "processOperationBv", wrapper, null);
        System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        Assert.assertEquals(1, res.getResult().intValue());

        operation = (GLBackValueOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(pst.getValueDate(), operation.getPostDate());
        GLOperationExt operationExt = operation.getOperExt();
        Assert.assertEquals(HOLD, operationExt.getManualStatus());
        Assert.assertNotNull(operationExt.getConfirmTimestamp());       // TODO надо ?
        Assert.assertEquals(wrapper.getComment(), operationExt.getManualNarrative());
    }

    /**
     * авторизовать список операций
     * @throws SQLException
     */
    @Test
    public void testSignList() throws SQLException {
        /**
         * создать операции BV в статусе CONTROL
         * авторизовать
         * запустить обработку
         */
        BigDecimal amt = new BigDecimal("12.10");
        BigDecimal plus = new BigDecimal("0.1");
        Date vdate = DateUtils.addDays(getOperday().getCurrentDate(), -7);

        EtlPosting[] postings = new EtlPosting[3];
        GLBackValueOperation[] operations = new GLBackValueOperation[postings.length];
        createOperationsBv(postings, operations, PaymentHub, vdate, amt, plus);

        Date postdate = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateAfter", vdate, false);
        List<DataRecord> data = baseEntityRepository.select("select RATE from CURRATES where CCY = ? and DAT in (?, ?) order by DAT",
                postings[0].getCurrencyCredit().getCurrencyCode(), vdate, postdate);
        BigDecimal rate1 = data.get(0).getBigDecimal(0);
        BigDecimal rate2 = data.get(1).getBigDecimal(0);
        Assert.assertNotEquals(rate1, rate2);

        for (int i = 0; i < operations.length; i++) {
            Assert.assertEquals(rate1, operations[i].getRateCredit());
        }

        BackValueWrapper wrapper = createWrapper(postdate, CONTROL, SIGN, VISIBLE, operations);
        RpcRes_Base<Integer> res = remoteAccess.invoke(BackValuePostingController.class, "processOperationBv", wrapper, null);
        System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        Assert.assertEquals(postings.length, res.getResult().intValue());

        for (int i = 0; i < operations.length; i++) {
            GLBackValueOperation operation = (GLBackValueOperation) baseEntityRepository.findById(operations[i].getClass(), operations[i].getId());
            Assert.assertEquals(postdate, operation.getPostDate());
            Assert.assertEquals(SIGNEDDATE, operation.getOperExt().getManualStatus());
            Assert.assertNotNull(operation.getOperExt().getConfirmTimestamp());

            remoteAccess.invoke(BackValueOperationController.class, "processBackValueOperation", operation);
            operation = (GLBackValueOperation) baseEntityRepository.findById(GLBackValueOperation.class, operation.getId());
            Assert.assertEquals(OperState.POST, operation.getState());
            Assert.assertEquals(COMPLETED, operation.getOperExt().getManualStatus());
            Assert.assertEquals(rate2, operation.getRateCredit());
        }
    }

    /**
     * задержать, затем авторизовать операции по фильтру, получить статистику по фильтру
     * @throws SQLException
     */
    @Test
    public void testFilter() throws Exception {
        /**
         * создать операции BV в статусе CONTROL
         * авторизовать
         * запустить обработку
         */
        BigDecimal amt = new BigDecimal("15.50");
        BigDecimal plus = new BigDecimal("0.5");
        Date vdate = DateUtils.addDays(getOperday().getCurrentDate(), -14);
        DealSource src = ARMPRO;

        EtlPosting[] postings = new EtlPosting[3];
        GLBackValueOperation[] operations = new GLBackValueOperation[postings.length];
        createOperationsBv(postings, operations, src, vdate, amt, plus);

        String sqlFmt = "select o.*, e.MNL_STATUS from GL_OPER o join GL_OPEREXT e on o.GLOID = e.GLOID where e.MNL_STATUS = '%s'";

        String sql = String.format(sqlFmt, CONTROL.name());
        DataRecord data = baseEntityRepository.selectOne("select count(1) from (" + sql + " and o.SRC_PST = ? and o.VDATE = ?) T", src.getLabel(), vdate);
        int holdCount = data.getInteger(0);
        Assert.assertTrue(holdCount >= postings.length);

        BackValueWrapper wrapper = createWrapper(vdate, CONTROL, TO_HOLD, ALL, operations);
        wrapper.setSql(sql);
        Criteria criteria = createCriteria(src, vdate);
        RpcRes_Base<Integer> res = remoteAccess.invoke(BackValuePostingController.class, "processOperationBv", wrapper, criteria);
        System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        Assert.assertEquals(holdCount, res.getResult().intValue());

        for (int i = 0; i < operations.length; i++) {
            GLBackValueOperation operation = (GLBackValueOperation) baseEntityRepository.findById(operations[i].getClass(), operations[i].getId());
            Assert.assertEquals(HOLD, operation.getOperExt().getManualStatus());
        }

        sql = String.format(sqlFmt, HOLD.name());
        data = baseEntityRepository.selectOne("select count(1) from (" + sql + " and o.SRC_PST = ? and o.VDATE = ? and o.AMT_DR > ?) T", src.getLabel(), vdate, amt);
        int signCount = data.getInteger(0);
        Assert.assertTrue(signCount >= 2);

        Date postdate = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateAfter", vdate, 2, false);
        List<DataRecord> dataList = baseEntityRepository.select("select RATE from CURRATES where CCY = ? and DAT in (?, ?) order by DAT",
                postings[0].getCurrencyCredit().getCurrencyCode(), vdate, postdate);
        BigDecimal rate1 = dataList.get(0).getBigDecimal(0);
        BigDecimal rate2 = dataList.get(1).getBigDecimal(0);
        Assert.assertNotEquals(rate1, rate2);

        BackValueWrapper wrapper1 = createWrapper(postdate, HOLD, SIGN, ALL, operations);
        wrapper1.setSql(sql);
        Criteria criteria1 = createCriteria(src, vdate, createCriterion("AMT_DR", Operator.GT, amt));
        res = remoteAccess.invoke(BackValuePostingController.class, "processOperationBv", wrapper1, criteria1);
        System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        Assert.assertEquals(signCount, res.getResult().intValue());

        GLBackValueOperation operation = (GLBackValueOperation) baseEntityRepository.findById(operations[0].getClass(), operations[0].getId());
        Assert.assertEquals(vdate, operation.getPostDate());
        Assert.assertEquals(HOLD, operation.getOperExt().getManualStatus());
        for (int i = 1; i < operations.length; i++) {
            operation = (GLBackValueOperation) baseEntityRepository.findById(operations[i].getClass(), operations[i].getId());
            Assert.assertEquals(postdate, operation.getPostDate());
            Assert.assertEquals(SIGNEDDATE, operation.getOperExt().getManualStatus());
        }

        sql = String.format(sqlFmt, SIGNEDDATE.name());
        data = baseEntityRepository.selectOne("select count(1) from (" + sql + " and o.SRC_PST = ? and o.VDATE = ? and o.AMT_DR > ?) T", src.getLabel(), vdate, amt);
        int statCount = data.getInteger(0);
        Assert.assertTrue(statCount >= signCount);

        BackValueWrapper wrapper2 = createWrapper(postdate, SIGNEDDATE, STAT, ALL, operations);
        wrapper2.setSql(sql);
        res = remoteAccess.invoke(BackValuePostingController.class, "processOperationBv", wrapper2, criteria1);
        System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        Assert.assertEquals(statCount, res.getResult().intValue());

        // обработка авторизованных bv-операций
        TimerJob mon = SingleActionJobBuilder.create().withClass(EtlStructureMonitorTask.class).build();
        jobService.executeJob(mon);

        for (int i = 1; i < operations.length; i++) {
            operation = (GLBackValueOperation) baseEntityRepository.findById(operations[i].getClass(), operations[i].getId());
            Assert.assertEquals(OperState.POST, operation.getState());
            Assert.assertEquals(COMPLETED, operation.getOperExt().getManualStatus());
            Assert.assertEquals(rate2, operation.getRateCredit());
        }
    }

    private void createOperationsBv(EtlPosting[] postings, GLBackValueOperation operations[], DealSource src, Date vdate, BigDecimal amt, BigDecimal plus) throws SQLException {
        BankCurrency currency = AUD;
        for (int i = 0; i < postings.length; i++) {
            String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817036%" + i);
            String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817036%" + i + 5);
            EtlPosting pst = createEtlPosting(vdate, src.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);
            Assert.assertNotNull(pst);
            amt = amt.add(plus);

            GLBackValueOperation operation = (GLBackValueOperation) postingController.processMessage(pst);
            operation = (GLBackValueOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
            Assert.assertEquals(pst.getValueDate(), operation.getPostDate());
            Assert.assertEquals(CONTROL, operation.getOperExt().getManualStatus());
            postings[i] = pst;
            operations[i] = operation;
        }

    }

    private BackValueWrapper createWrapper(Date postDate, BackValuePostStatus statusOld, BackValueAction action, BackValueMode mode, GLOperation[] opers) {
        BackValueWrapper wrapper = new BackValueWrapper();
        wrapper.setBvStatus(statusOld);
        wrapper.setPostDateStr(new SimpleDateFormat(wrapper.getDateFormat()).format(postDate));
        wrapper.setAction(action);
        wrapper.setMode(mode);
        wrapper.setGloIDs(Arrays.asList(opers).stream().map(operation -> operation.getId()).collect(Collectors.toList()));
        wrapper.setUserId(USER_ID);
        return wrapper;
    }

    private Criteria createCriteria(DealSource src, Date vdate, CriterionColumn ... columns) {
        List<Criterion> criterionList = new ArrayList<>();
        criterionList.add(createCriterion("SRC_PST", Operator.EQ, src.getLabel()));
        criterionList.add(createCriterion("VDATE", Operator.EQ, vdate));
        if (null != columns)
            for (CriterionColumn column : columns)
                criterionList.add(column);
        Criteria criteria = new Criteria(CriteriaLogic.AND, criterionList);
        return criteria;
    }
}
