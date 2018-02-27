package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.EtlStructureMonitorTask;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.*;
import ru.rbt.barsgl.ejb.integr.bg.BackValueOperationController;
import ru.rbt.barsgl.ejb.integr.bg.BackValuePostingController;
import ru.rbt.barsgl.ejb.integr.bg.EditPostingController;
import ru.rbt.barsgl.ejb.integr.oper.EditPostingGLPdProcessor;
import ru.rbt.barsgl.ejb.integr.oper.EditPostingPdProcessor;
import ru.rbt.barsgl.ejb.integr.oper.EditPostingProcessor;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.criteria.*;
import ru.rbt.barsgl.shared.enums.*;
import ru.rbt.barsgl.shared.operation.BackValueWrapper;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.StringUtils;

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
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.BUFFER;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.AUD;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.USD;
import static ru.rbt.barsgl.ejbtest.BackValueOperationIT.*;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;
import static ru.rbt.barsgl.shared.criteria.CriterionColumn.createCriterion;
import static ru.rbt.barsgl.shared.enums.BackValueAction.*;
import static ru.rbt.barsgl.shared.enums.BackValueMode.ALL;
import static ru.rbt.barsgl.shared.enums.BackValueMode.ONE;
import static ru.rbt.barsgl.shared.enums.BackValueMode.VISIBLE;
import static ru.rbt.barsgl.shared.enums.BackValuePostStatus.*;
import static ru.rbt.barsgl.shared.enums.DealSource.ARMPRO;
import static ru.rbt.barsgl.shared.enums.DealSource.KondorPlus;
import static ru.rbt.barsgl.shared.enums.DealSource.PaymentHub;
import static ru.rbt.barsgl.shared.enums.OperState.POST;

/**
 * Created by er18837 on 20.07.2017.
 */
public class BackValueAuthIT extends AbstractTimerJobIT {

    public static final Logger log = Logger.getLogger(BackValueAuthIT.class.getName());
    private final static Long USER_ID = 2L;
    private final static Long ROLE_H3 = 10L;

    @BeforeClass
    public static void beforeAll() throws SQLException {

        try {
            setOperday(DateUtils.parseDate("27.02.2015", "dd.MM.yyyy"), DateUtils.parseDate("25.02.2015", "dd.MM.yyyy"), ONLINE, OPEN, BUFFER);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        setCalendar2015_02();

//        saveTable("GL_BVPARM");
//        saveTable("GL_CRPRD");

        setBVparams();
        subUserRole(USER_ID, 1L);
        addUserRole(USER_ID, ROLE_H3);
    }

    public static void addUserRole(Long id_user, Long id_role) throws SQLException {
        DataRecord res = baseEntityRepository.selectFirst("select * from GL_AU_USRRL where ID_USER = ? and ID_ROLE = ?", id_user, id_role);
        if (null == res)
            baseEntityRepository.executeNativeUpdate("insert into GL_AU_USRRL (ID_USER, ID_ROLE, USR_AUT) values (?, ?, ?)", id_user, id_role, "sys");
    }

    public static void subUserRole(Long id_user, Long id_role) throws SQLException {
        DataRecord res = baseEntityRepository.selectFirst("select * from GL_AU_USRRL where ID_USER = ? and ID_ROLE = ?", id_user, id_role);
        if (null != res)
            baseEntityRepository.executeNativeUpdate("delete from GL_AU_USRRL where ID_USER = ? and ID_ROLE = ?", id_user, id_role);
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
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "47407840%");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "47408840%");
        BigDecimal amt = new BigDecimal("44.88");
        BankCurrency currency = USD;

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
            Assert.assertEquals(POST, operation.getState());
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
            Assert.assertEquals(POST, operation.getState());
            Assert.assertEquals(COMPLETED, operation.getOperExt().getManualStatus());
            Assert.assertEquals(rate2, operation.getRateCredit());
        }
    }

    @Test
    public void testEditOperationKP() throws SQLException, InterruptedException {

        /**
         * создать операцию BV в статусе CONTROL
         * авторизовать
         * запустить обработку
         */
        Date curdate = getOperday().getCurrentDate();
        Date vdate = DateUtils.addDays(curdate, -21);

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, vdate, "47427810_0001%");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, vdate, "47425810_0001%");
        BigDecimal amt = new BigDecimal("897.65");
        BankCurrency currency = RUB;

        EtlPosting pst = createEtlPosting(vdate, KondorPlus.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);
        Assert.assertNotNull(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(curdate, operation.getPostDate());
        Assert.assertEquals(POST, operation.getState());

        BackValueWrapper wrapper = createWrapper(vdate, null, EDIT_DATE, ONE, new GLOperation[]{operation});
        wrapper.setPostDateStr(new SimpleDateFormat(wrapper.getDateFormat()).format(vdate));
        RpcRes_Base<Integer> res = remoteAccess.invoke(BackValuePostingController.class, "processOperationBv", wrapper, null);
        System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        Assert.assertEquals(1, res.getResult().intValue());

        Thread.sleep(1000L);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(vdate, operation.getPostDate());

        List<AbstractPd> pdList = getPdList(operation.getId());
        checkPostingDate(pdList, vdate);
    }

    @Test
    public void testEditOperationFan() throws SQLException {

        /**
         * создать операцию BV в статусе CONTROL
         * авторизовать
         * запустить обработку
         */
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(),  "40802810_0001%");
        String bsaCt1 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "47425810_0001%");
        String bsaCt2 = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "47427810_0001%");
        BigDecimal amt1 = new BigDecimal("897.65");
        BigDecimal amt2 = new BigDecimal("100.01");
        BankCurrency currency = RUB;

        Date curdate = getOperday().getCurrentDate();
        Date vdate = DateUtils.addDays(curdate, -14);
        EtlPackage pkg = newPackage(System.currentTimeMillis(), "BackValueFan");
        EtlPosting pst1 = createEtlPostingFan(pkg, vdate, PaymentHub.getLabel(), bsaDt, currency, amt1, bsaCt1, currency, amt1, null);
        Assert.assertNotNull(pst1);
        EtlPosting pst2 = createEtlPostingFan(pkg, vdate, PaymentHub.getLabel(), bsaDt, currency, amt2, bsaCt2, currency, amt2, pst1.getParentReference());
        Assert.assertNotNull(pst2);

        GLOperation[] opers = new GLOperation[2];
        opers[0] = (GLOperation) postingController.processMessage(pst1);
        opers[1] = (GLOperation) postingController.processMessage(pst2);

        // process fan fully
        updateOperdayMode(DIRECT, ProcessingStatus.STARTED);
        List<GLOperation> operList = fanPostingController.processOperations(pst1.getParentReference());
        Assert.assertEquals(2, operList.size());
        updateOperdayMode(BUFFER, ProcessingStatus.STARTED);

        for (GLOperation operation : opers) {
            operation = (GLOperation) baseEntityRepository.findById(GLOperation.class, operation.getId());
            Assert.assertEquals(curdate, operation.getPostDate());
            Assert.assertEquals(POST, operation.getState());
        }

        BackValueWrapper wrapper = createWrapper(vdate, null, EDIT_DATE, ONE, new GLOperation[]{opers[1]});
        wrapper.setPostDateStr(new SimpleDateFormat(wrapper.getDateFormat()).format(vdate));
        RpcRes_Base<Integer> res = remoteAccess.invoke(BackValuePostingController.class, "processOperationBv", wrapper, null);
        System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        Assert.assertEquals(1, res.getResult().intValue());

        for (GLOperation operation : opers) {
            operation = (GLOperation) baseEntityRepository.findById(GLOperation.class, operation.getId());
            Assert.assertEquals(vdate, operation.getPostDate());
        }

        List<AbstractPd> pdList = getPdList(opers[0].getId());
        Assert.assertEquals(3, pdList.size());
        checkPostingDate(pdList, vdate);
    }

    @Test
    public void testEditWithClosedPeriod() throws SQLException {
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "47425810_0001%3");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "47425810_0001%4");
        BigDecimal amt = new BigDecimal("404.04");
        BankCurrency currency = RUB;

        Date curdate = getOperday().getCurrentDate();
        Date vdate = DateUtils.addDays(curdate, -28);
        EtlPosting pst = createEtlPosting(vdate, KondorPlus.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);
        Assert.assertNotNull(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(curdate, operation.getPostDate());
        Assert.assertEquals(POST, operation.getState());

        BackValueWrapper bvWrapper = createWrapper(vdate, null, EDIT_DATE, ONE, new GLOperation[]{operation});
        bvWrapper.setUserId(USER_ID);
        String valueDateStr = (new SimpleDateFormat(bvWrapper.getDateFormat()).format(vdate));
        bvWrapper.setPostDateStr(valueDateStr);
        RpcRes_Base<Integer> res = remoteAccess.invoke(BackValuePostingController.class, "processOperationBv", bvWrapper, null);
        System.out.println(res.getMessage());
        Assert.assertTrue(res.isError());
        Assert.assertTrue(res.getMessage().contains("закрыт"));

        ManualOperationWrapper operWrapper = newOperationWrapper(operation);
        operWrapper.setUserId(USER_ID);
        operWrapper.setPdIdList(getPdIdList(operation.getId()));
        operWrapper.setPostingChoice(PostingChoice.PST_ALL);
        operWrapper.setPdMode(getOperday().getPdMode().name());
        operWrapper.setValueDateStr(valueDateStr);
        operWrapper.setPostDateStr(valueDateStr);
        RpcRes_Base<ManualOperationWrapper> res2 = remoteAccess.invoke(EditPostingController.class, "updatePostingsWrapper", operWrapper);
        System.out.println(res2.getMessage());
        Assert.assertTrue(res2.isError());
        Assert.assertTrue(res2.getMessage().contains("закрыт"));

    }

    public static EtlPosting createEtlPostingFan(EtlPackage pkg, Date valueDate, String src,
                                                      String accountDebit, BankCurrency currencyDebit, BigDecimal amountDebit,
                                                      String accountCredit, BankCurrency currencyCredit, BigDecimal amountCredit, String parentRef) {

        EtlPosting pst = createEtlPostingNotSaved(pkg, valueDate, src, accountDebit, currencyDebit, amountDebit, accountCredit, currencyCredit, amountCredit);
        Assert.assertNotNull(pst);
        pst.setFan(YesNo.Y);
        pst.setParentReference(!isEmpty(parentRef) ? parentRef : pst.getPaymentRefernce());

        return (EtlPosting) baseEntityRepository.save(pst);
    }

    private void createOperationsBv(EtlPosting[] postings, GLBackValueOperation operations[], DealSource src, Date vdate, BigDecimal amt, BigDecimal plus) throws SQLException {
        BankCurrency currency = USD;
        for (int i = 0; i < postings.length; i++) {
            String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "47408840%" + i);
            String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "47408840%" + (i + 5));
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

    private List<AbstractPd> getPdList(Long parentId) {
        Class<? extends EditPostingProcessor> editPostingProcessor = EditPostingGLPdProcessor.class;
        List<Long> pdIdList = remoteAccess.invoke(editPostingProcessor, "getOperationPdIdList", parentId);
        if (pdIdList.size() == 0) {
            editPostingProcessor = EditPostingPdProcessor.class;
            pdIdList = remoteAccess.invoke(editPostingProcessor, "getOperationPdIdList", parentId);
        }
        Assert.assertNotEquals(0, pdIdList.size());
        return remoteAccess.invoke(editPostingProcessor, "getOperationPdList", pdIdList);
    }

    private ArrayList<Long> getPdIdList(Long parentId) {
        Class<? extends EditPostingProcessor> editPostingProcessor = EditPostingGLPdProcessor.class;
        ArrayList<Long> pdIdList = remoteAccess.invoke(editPostingProcessor, "getOperationPdIdList", parentId);
        if (pdIdList.size() == 0) {
            editPostingProcessor = EditPostingPdProcessor.class;
            pdIdList = remoteAccess.invoke(editPostingProcessor, "getOperationPdIdList", parentId);
        }
        return pdIdList;
    }

    private void checkPostingDate(List<AbstractPd> pdList, Date postDate) throws SQLException {
        pdList.forEach(pd -> Assert.assertEquals(postDate, pd.getPod()));

        String pcids = StringUtils.listToString(pdList, ",");
        List<DataRecord> res = baseEntityRepository.select("select POD, MO_NO from PCID_MO where PCID in (" + pcids + ")");
        res.forEach(mo-> Assert.assertEquals(postDate, mo.getDate(0)));
    }


}
