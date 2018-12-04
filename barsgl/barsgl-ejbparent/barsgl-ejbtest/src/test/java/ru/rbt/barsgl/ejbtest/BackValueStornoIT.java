package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.BankCalendarDay;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.OpenOperdayTask;
import ru.rbt.barsgl.ejb.controller.operday.task.PdSyncTask;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLBackValueOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejb.integr.bg.BackValueOperationController;
import ru.rbt.barsgl.ejbcore.CacheController;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.enums.DealSource;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.tasks.ejb.entity.task.JobHistory;
import ru.rbt.tasks.ejb.repository.JobHistoryRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.BUFFER;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.AUD;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;
import static ru.rbt.barsgl.ejbtest.BackValueOperationIT.createEtlPosting;
import static ru.rbt.barsgl.ejbtest.BackValueOperationIT.createEtlPostingNotSaved;
import static ru.rbt.barsgl.shared.enums.BackValuePostStatus.COMPLETED;
import static ru.rbt.barsgl.shared.enums.DealSource.ARMPRO;
import static ru.rbt.barsgl.shared.enums.DealSource.PaymentHub;
import static ru.rbt.barsgl.shared.enums.DealSource.SECMOD;
import static ru.rbt.barsgl.shared.enums.OperState.*;

/**
 * Created by er18837 on 26.11.2018.
 */
public class BackValueStornoIT extends AbstractTimerJobIT {
    private static final Logger log = Logger.getLogger(BackValueStornoIT.class.getName());

    @BeforeClass
    public static void beforeClass() {
        try {
            setOperday(DateUtils.parseDate("03.07.2018", "dd.MM.yyyy"), DateUtils.parseDate("02.07.2018", "dd.MM.yyyy"), ONLINE, OPEN, DIRECT);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        setBVparams();
    }

    @AfterClass
    public static void afterClass() {
        restoreOperday();
    }

    // заполнить параметры BackValue
    public static void setBVparams() {
        baseEntityRepository.executeNativeUpdate("delete from GL_BVPARM");
        baseEntityRepository.executeNativeUpdate("insert into GL_BVPARM (ID_SRC, BV_SHIFT, BVSTRN_INVISIBLE, DTB, DTE) values ('ARMPRO', 4, 'Y', '2017-01-01', null)");
        baseEntityRepository.executeNativeUpdate("insert into GL_BVPARM (ID_SRC, BV_SHIFT, BVSTRN_INVISIBLE, DTB, DTE) values ('SECMOD', 2, 'N', '2017-01-01', null)");

        baseEntityRepository.executeNativeUpdate("delete from GL_CRPRD");
//        baseEntityRepository.executeNativeUpdate("insert into GL_CRPRD (PRD_LDATE, PRD_CUTDATE) values ('2015-02-28', '2015-03-04')");

        remoteAccess.invoke(CacheController.class, "flushAllCaches");
    }

    @Test
    public void testStrotnoInvisNDirect() throws SQLException {
        updateOperday(ONLINE, OPEN, DIRECT);
        testStrotnoInvisN(SECMOD, 2);
    }

    @Test
    public void testStrotnoInvisNBuffer() throws SQLException {
        updateOperday(ONLINE, OPEN, BUFFER);
        testStrotnoInvisN(SECMOD, 2);
    }

    @Test
    public void testStrotnoNoparamDirect() throws SQLException {
        updateOperday(ONLINE, OPEN, DIRECT);
        testStrotnoInvisN(PaymentHub, 1);
    }

    @Test
    public void testStrotnoNoparamBuffer() throws SQLException {
        updateOperday(ONLINE, OPEN, BUFFER);
        testStrotnoInvisN(PaymentHub, 1);
    }

    public void testStrotnoInvisN(DealSource src, int depth) throws SQLException {
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20209810_0001%1");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20209810_0001%2");
        BigDecimal amt = new BigDecimal("1223.45");
        BankCurrency currency = RUB;
        Date od = getOperday().getCurrentDate();
        Date vdate = getWorkDateBefore(od, depth);
        Date sdate = vdate;

        // SECMOD прямая
        EtlPosting pst = createEtlPosting(vdate, src.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);
        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());

        // сторно
        EtlPosting pstS = createStornoPosting(sdate, pst);
        GLOperation operationS = (GLOperation) postingController.processMessage(pstS);
        operationS = (GLOperation) baseEntityRepository.findById(operationS.getClass(), operationS.getId());

        Assert.assertTrue(operationS.isStorno());
        Assert.assertEquals(POST, operation.getState());
        Assert.assertEquals(POST, operationS.getState());
        Assert.assertEquals(operation.getId(), operationS.getStornoOperation().getId());
    }

    @Test
    public void testStrotnoInvisYDirect22() throws SQLException {
        testStrotnoInvisYDirect(2, 2, true);
    }

    @Test
    public void testStrotnoInvisYDirect21() throws SQLException {
        testStrotnoInvisYDirect(2, 1, true);
    }

    @Test
    public void testStrotnoInvisYDirect20() throws SQLException {
        testStrotnoInvisYDirect(2, 0, true);
    }

    @Test
    public void testStrotnoInvisYDirect52() throws SQLException {
        testStrotnoInvisYDirect(5, 2, false);
    }

    public void testStrotnoInvisYDirect(int vdepth, int sdepth, boolean cans) throws SQLException {
        Operday odWas = getOperday();

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20209810_0001%3");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20209810_0001%4");
        BigDecimal amt = new BigDecimal("1233.45");
        BankCurrency currency = RUB;
        Date od = getOperday().getCurrentDate();
        Date vdate = getWorkDateBefore(od, vdepth);
        Date sdate = getWorkDateBefore(od, sdepth);

        // ARMPRO прямая
        setOperday(vdate, DateUtils.addDays(vdate, -1), ONLINE, OPEN, DIRECT);
        EtlPosting pst = createEtlPosting(vdate, ARMPRO.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);
        GLOperation operation = (GLOperation) postingController.processMessage(pst);

        // сторно
//        setOperday(sdate, DateUtils.addDays(sdate, -1), ONLINE, OPEN, DIRECT);
        setOperday(odWas.getCurrentDate(), odWas.getLastWorkingDay(), ONLINE, OPEN, DIRECT);
        EtlPosting pstS = createStornoPosting(sdate, pst);
        GLOperation operationS = (GLOperation) postingController.processMessage(pstS);

        if(cans) {
            checkSocancOperation(operationS.getId(), operation.getId());
            checkCancOperation(operation.getId());
        } else {
            checkPostOperations(operationS.getId(), operation.getId());
        }
    }

    @Test
    public void testBloadStrotnoDirect() throws SQLException {
        updateOperday(ONLINE, OPEN, DIRECT);

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20209810_0001%7");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20209810_0001%8");
        BigDecimal amt = new BigDecimal("1234.45");
        BankCurrency currency = RUB;
        Date od = getOperday().getCurrentDate();
        Date vdate = getWorkDateBefore(od, 5);
        Date sdate = getWorkDateBefore(od, 2);

        // ARMPRO прямая
        EtlPosting pst = createEtlPosting(vdate, ARMPRO.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);
        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertEquals(BLOAD, operation.getState());
        GLBackValueOperation bvOperation = processBVOperation(operation);
        Assert.assertEquals(POST, bvOperation.getState());

        // сторно
        EtlPosting pstS = createStornoPosting(sdate, pst);
        GLOperation operationS = (GLOperation) postingController.processMessage(pstS);
        operationS = (GLOperation) baseEntityRepository.findById(operationS.getClass(), operationS.getId());

        Assert.assertTrue(operationS.isStorno());
        Assert.assertEquals(POST, operationS.getState());
        Assert.assertEquals(bvOperation.getId(), operationS.getStornoOperation().getId());
    }

    @Test
    public void testPostStrotnoBloadDirect() throws SQLException {
        updateOperday(ONLINE, OPEN, DIRECT);

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20209810_0001%7");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20209810_0001%8");
        BigDecimal amt = new BigDecimal("1234.45");
        BankCurrency currency = RUB;
        Date od = getOperday().getCurrentDate();
        Date vdate = getWorkDateBefore(od, 2);
        Date sdate = getWorkDateBefore(od, 5);

        // ARMPRO прямая
        EtlPosting pst = createEtlPosting(vdate, ARMPRO.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);
        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(POST, operation.getState());

        // сторно
        EtlPosting pstS = createStornoPosting(sdate, pst);
        GLOperation operationS = (GLOperation) postingController.processMessage(pstS);
        Assert.assertEquals(BLOAD, operationS.getState());
        GLBackValueOperation bvOperationS = processBVOperation(operationS);
        Assert.assertEquals(POST, bvOperationS.getState());

        Assert.assertTrue(bvOperationS.isStorno());
        Assert.assertEquals(POST, bvOperationS.getState());
        Assert.assertEquals(operation.getId(), bvOperationS.getStornoOperation().getId());
    }

    private GLBackValueOperation processBVOperation(GLOperation operation) {
        baseEntityRepository.executeUpdate("update GLOperation o set o.postDate = ?1, o.equivalentDebit = ?2, o.equivalentCredit = ?3 where o.id = ?4",
                operation.getValueDate(), null, null, operation.getId());

        remoteAccess.invoke(BackValueOperationController.class, "processBackValueOperation", operation);
        GLBackValueOperation bvOperation = (GLBackValueOperation) baseEntityRepository.findById(GLBackValueOperation.class, operation.getId());
        Assert.assertEquals("GLOID = " + bvOperation.getId(), POST, bvOperation.getState());
        Assert.assertEquals("GLOID = " + bvOperation.getId(), COMPLETED, bvOperation.getOperExt().getManualStatus());
        return bvOperation;
    }

    @Test
    public void testStrotnoInvisYBuffer22() throws SQLException {
        testStrotnoInvisYBuffer(2, 2);
    }

    @Test
    public void testStrotnoInvisYBuffer21() throws SQLException {
        testStrotnoInvisYBuffer(2, 1);
    }

    @Test
    public void testStrotnoInvisYBuffer20() throws SQLException {
        testStrotnoInvisYBuffer(2, 0);
    }

    public void testStrotnoInvisYBuffer(int vdepth, int sdepth) throws SQLException {
        updateOperday(ONLINE, OPEN, BUFFER);

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20209810_0001%5");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20209810_0001%6");
        BigDecimal amt = new BigDecimal("1123.45");
        BankCurrency currency = RUB;
        Date od = getOperday().getCurrentDate();
        Date vdate = getWorkDateBefore(od, vdepth);
        Date sdate = getWorkDateBefore(od, sdepth);

        // ARMPRO прямая
        EtlPosting pst = createEtlPosting(vdate, ARMPRO.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);
        GLOperation operation = (GLOperation) postingController.processMessage(pst);

        // сторно
        EtlPosting pstS = createStornoPosting(sdate, pst);
        GLOperation operationS = (GLOperation) postingController.processMessage(pstS);
        operationS = (GLOperation) baseEntityRepository.findById(operationS.getClass(), operationS.getId());
        Assert.assertTrue(operationS.isStorno());
        Assert.assertEquals(STRN_WAIT, operationS.getState());

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(POST, operation.getState());
    }

    @Test
    public void testSuppressBuffer() throws Exception {
        baseEntityRepository.executeNativeUpdate("update GL_OPER set STATE = ? where STATE = ?", ERCHK.name(), STRN_WAIT.name());
        testStrotnoInvisYBuffer(2,2);
        List<GLOperation> operations = baseEntityRepository.select(GLOperation.class, "from GLOperation o where o.state = ?1", STRN_WAIT);
        Assert.assertEquals(1, operations.size());

//        remoteAccess.invoke(PdSyncTask.class, "processStornoBackvalue", getOperday().getCurrentDate(), null);
        Operday operday = getOperday();
        baseEntityRepository.executeNativeUpdate("update gl_od set prc = 'STOPPED'");
        baseEntityRepository.executeNativeUpdate("delete from workproc where dat = ?", operday.getLastWorkingDay());
        baseEntityRepository.executeNativeUpdate("insert into workproc (dat,id,result,count,msg) values (?,?,?,?,?)"
                , operday.getLastWorkingDay(), "IFLEX","O", "0", "MI5GL");

        log.info("deleted " + baseEntityRepository.executeUpdate("delete from JobHistory h where h.jobName like ?1", PdSyncTask.class.getSimpleName() + "%"));
        JobHistory history = remoteAccess.invoke(JobHistoryRepository.class
                , "createHeader", PdSyncTask.class.getSimpleName(), getOperday().getCurrentDate());

        SingleActionJob syncJob = SingleActionJobBuilder.create()
                .withProps(AbstractJobHistoryAwareTask.JobHistoryContext.HISTORY_ID.name() + "=" + history.getId() + "\n" +
                        OpenOperdayTask.PD_MODE_KEY+"="+ Operday.PdMode.DIRECT)
                .withClass(PdSyncTask.class)
                .withName("PdSyncTask001").build();
        jobService.executeJob(syncJob);

        List<GLOperation> opers = baseEntityRepository.select(GLOperation.class, "from GLOperation o where o.state = ?1", STRN_WAIT);
        Assert.assertEquals(0, opers.size());

        GLOperation operationS = operations.get(0);
        GLOperation operation = operationS.getStornoOperation();
        checkSocancOperation(operationS.getId(), operation.getId());
        checkCancOperation(operation.getId());

    }

    // TODO test Direct -> POST -> Buffer -> STRN_WAIT -> PdSyncTask -> Suppress

    public EtlPosting createEtlPosting(Date valueDate, String src,
                                              String accountDebit, BankCurrency currencyDebit, BigDecimal amountDebit,
                                              String accountCredit, BankCurrency currencyCredit, BigDecimal amountCredit) {
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "BackValuePackage");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = createEtlPostingNotSaved(pkg, valueDate, src,
                accountDebit, currencyDebit, amountDebit,
                accountCredit, currencyCredit, amountCredit);

        return (EtlPosting) baseEntityRepository.save(pst);
    }

    public EtlPosting createStornoPosting(Date valueDateStorno, EtlPosting pst) {
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "BackValuePackage");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pstS = newPosting(stamp, pkg);
        // идентификаторы
        pstS.setSourcePosting(pst.getSourcePosting());
        pstS.setStornoReference(pst.getEventId());
        pstS.setDealId(pst.getDealId());
        pstS.setPaymentRefernce(pst.getPaymentRefernce());
        pstS.setStorno(YesNo.Y);
        // данные по вееру
        pstS.setFan(pst.getFan());
        pstS.setParentReference(pst.getParentReference());
        // данные проводки (инвертированы)
        pstS.setAccountDebit(pst.getAccountCredit());
        pstS.setCurrencyDebit(pst.getCurrencyCredit());
        pstS.setAmountDebit(pst.getAmountCredit());
        pstS.setAccountCredit(pst.getAccountDebit());
        pstS.setCurrencyCredit(pst.getCurrencyDebit());
        pstS.setAmountCredit(pst.getAmountDebit());

        pstS.setValueDate(valueDateStorno);
        return (EtlPosting) baseEntityRepository.save(pstS);
    }

    private void checkSocancOperation(Long gloStorno, Long gloParent) throws SQLException {
        GLOperation operationS = (GLOperation) baseEntityRepository.findById(GLOperation.class, gloStorno);
        Assert.assertTrue(operationS.isStorno());
        Assert.assertEquals(SOCANC, operationS.getState());
        Assert.assertEquals(GLOperation.OperType.ST, operationS.getPstScheme());
        Assert.assertEquals(GLOperation.StornoType.C, operationS.getStornoRegistration());
        Assert.assertEquals(gloParent, operationS.getStornoOperation().getId());        // ссылка на сторно операцию
        List<GLPosting> postList = getPostings(operationS);
        Assert.assertTrue(postList.isEmpty());                    // нет своих проводки
    }

    private void checkCancOperation(Long gloCanc) throws SQLException {
        GLOperation operation = (GLOperation) baseEntityRepository.findById(GLOperation.class, gloCanc);
        Assert.assertEquals(OperState.CANC, operation.getState());
        List<GLPosting> postList = getPostings(operation);

        Assert.assertNotNull(postList);
        Assert.assertFalse(postList.isEmpty());         // 2 проводки

        for (GLPosting posting: postList) {
            List<Pd> pdList = getPostingPd( posting );
            for (Pd pd: pdList) {
                Assert.assertEquals(pd.getInvisible(), "1");
                Assert.assertNotNull(baseEntityRepository.selectFirst("select * from GL_BVJRNL where acid = ? and bsaacid = ? and pod = ?",
                        pd.getAcid(), pd.getBsaAcid(), pd.getPod()));
            }
        }
    }

    private void checkPostOperations(Long gloStorno, Long gloParent) throws SQLException {
        GLOperation operation = (GLOperation) baseEntityRepository.findById(GLOperation.class, gloParent);
        Assert.assertEquals(POST, operation.getState());
        GLOperation operationS = (GLOperation) baseEntityRepository.findById(GLOperation.class, gloStorno);
        Assert.assertTrue(operationS.isStorno());
        Assert.assertEquals(POST, operationS.getState());
        Assert.assertEquals(operation.getId(), operationS.getStornoOperation().getId());
    }

    public static Date getWorkDateBefore(Date dateTo, int days, boolean withTech) {
        return remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", dateTo, days, withTech);
    }

    public static Date getWorkDateBefore(Date dateTo, int days) {
        return days == 0 ? dateTo :
                remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", dateTo, days, false);
    }
}
