package ru.rbt.barsgl.ejbtest;

import org.apache.log4j.Logger;
import org.junit.*;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.sec.GLErrorRecord;
import ru.rbt.barsgl.ejb.integr.bg.ReprocessPostingService;
import ru.rbt.barsgl.ejb.repository.GLErrorRepository;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.ErrorCorrectType;
import ru.rbt.ejbcore.mapping.YesNo;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;

/**
 * Created by ER18837 on 01.03.17.
 */
public class ReprocessErrorIT extends AbstractTimerJobIT {

    private static final Logger log = Logger.getLogger(ValidationIT.class);

    @BeforeClass
    public static void beforeClass() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    @Before
    public void before() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    @After
    public void after() {
        restoreOperday();
    }

    /**
     * Проверка закрытия одной ошибки
     */
    @Test
    public void testCloseOne() throws SQLException {
        EtlPackage pkg = newPackage(System.currentTimeMillis(), "WithError");
        Assert.assertTrue(pkg.getId() > 0);
        EtlPosting pst = createPostingBad(pkg, "SECMOD", "40817036%1", "40817036%2", BankCurrency.AUD, new BigDecimal("315.45") );
        Assert.assertTrue(pst.getId() > 0);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNull(operation);                                               // операция не должна быть создана
        pst = (EtlPosting) baseEntityRepository.refresh(pst, true);
        Assert.assertEquals(1, pst.getErrorCode().intValue());

        EtlPackage pkg1 = newPackage(System.currentTimeMillis(), "WithoutError");
        Assert.assertTrue(pkg.getId() > 0);
        EtlPosting pst1 = createPosting(pkg, pst.getSourcePosting(), pst.getAccountDebit(), pst.getAccountCredit(), pst.getCurrencyDebit(), new BigDecimal("315.45"),
                    pst.getEventId(), pst.getDealId(), pst.getPaymentRefernce());
        Assert.assertTrue(pst.getId() > 0);

        operation = (GLOperation) postingController.processMessage(pst1);
        Assert.assertNotNull(operation);                                               // операция не должна быть создана
        pst1 = (EtlPosting) baseEntityRepository.refresh(pst1, true);
        Assert.assertEquals(0, pst1.getErrorCode().intValue());

        GLErrorRecord err = getPostingErrorRecord(pst);
        List<Long> errorIdList = new ArrayList<>();
        errorIdList.add(err.getId());
        // correctErrors (List<Long> errorIdList, String comment, String idPstCorr, ErrorCorrectType correctType)
        RpcRes_Base<Integer> res = remoteAccess.invoke(ReprocessPostingService.class, "correctErrors",
                errorIdList, "testCloseOne", pst1.getAePostingId(), ErrorCorrectType.CLOSE_ONE);

        Assert.assertFalse(res.isError());
        Assert.assertEquals(1, res.getResult().intValue());
        System.out.println(res.getMessage());

        err = (GLErrorRecord) baseEntityRepository.refresh(err, true);
        Assert.assertEquals(YesNo.Y, err.getCorrect());
        Assert.assertNotNull(err.getComment());
        Assert.assertNotNull(err.getAePostingIdNew());
        System.out.println(String.format("Comment: '%s' ID_NEW: '%s'", err.getComment(), err.getAePostingIdNew()));

        pst = (EtlPosting) baseEntityRepository.refresh(pst, true);
        Assert.assertEquals(1, pst.getErrorCode().intValue());
    }

    /**
     * Проверка переобработки одной ошибки
     */
    @Test
    public void testReprocessOne() throws SQLException {
        EtlPackage pkg = newPackage(System.currentTimeMillis(), "WithError");
        Assert.assertTrue(pkg.getId() > 0);
        EtlPosting pst = createPostingBad(pkg, "ARMPRO", "40817036%3", "40817036%4", BankCurrency.AUD, new BigDecimal("415.45") );
        Assert.assertTrue(pst.getId() > 0);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNull(operation);                                               // операция не должна быть создана
        pst = (EtlPosting) baseEntityRepository.refresh(pst, true);
        Assert.assertEquals(1, pst.getErrorCode().intValue());

        GLErrorRecord err = getPostingErrorRecord(pst);
        List<Long> errorIdList = new ArrayList<>();
        errorIdList.add(err.getId());
        // correctErrors (List<Long> errorIdList, String comment, String idPstCorr, ErrorCorrectType correctType)
        RpcRes_Base<Integer> res = remoteAccess.invoke(ReprocessPostingService.class, "correctErrors",
                errorIdList, "testReprocessOne", null, ErrorCorrectType.REPROCESS_ONE);

        Assert.assertFalse(res.isError());
        Assert.assertEquals(1, res.getResult().intValue());
        System.out.println(res.getMessage());

        err = (GLErrorRecord) baseEntityRepository.refresh(err, true);
        System.out.println(String.format("Comment: '%s' ID_NEW: '%s'", err.getComment(), err.getAePostingIdNew()));
        Assert.assertEquals(YesNo.Y, err.getCorrect());
        Assert.assertNotNull(err.getComment());
        Assert.assertNull(err.getAePostingIdNew());

        pst = (EtlPosting) baseEntityRepository.refresh(pst, true);
        Assert.assertNull(pst.getErrorCode());

        pkg = (EtlPackage) baseEntityRepository.refresh(pkg, true);
        Assert.assertEquals(EtlPackage.PackageState.LOADED, pkg.getPackageState());
    }

    /**
     * Проверка переобработки списка ошибок
     */
    @Test
    public void testReprocessList() throws SQLException {
        EtlPackage[] pkgs = new EtlPackage[2];
        EtlPosting[] psts = new EtlPosting[3];
        pkgs[0] = newPackage(System.currentTimeMillis(), "WithError1");
        Assert.assertTrue(pkgs[0].getId() > 0);
        psts[0] = createPostingBad(pkgs[0], "ARMPRO", "40817036%5", "40817036%6", BankCurrency.AUD, new BigDecimal("315.43") );
        Assert.assertTrue(psts[0].getId() > 0);
        psts[1] = createPostingBad(pkgs[0], "ARMPRO", "40817036%7", "40817036%8", BankCurrency.AUD, new BigDecimal("915.43") );
        Assert.assertTrue(psts[1].getId() > 0);
        pkgs[1] = newPackage(System.currentTimeMillis(), "WithError2");
        Assert.assertTrue(pkgs[1].getId() > 0);
        psts[2] = createPostingBad(pkgs[1], "ARMPRO", "40817036%9", "40817036%0", BankCurrency.AUD, new BigDecimal("815.43") );
        Assert.assertTrue(psts[2].getId() > 0);

        for (EtlPosting pst: psts) {
            Assert.assertNull(postingController.processMessage(pst));
            pst = (EtlPosting) baseEntityRepository.refresh(pst, true);
            Assert.assertEquals(1, pst.getErrorCode().intValue());
        }

        List<Long> errorIdList = new ArrayList<>();
        for (EtlPosting pst: psts) {
            errorIdList.add(getPostingErrorRecord(pst).getId());
        }
        // correctErrors (List<Long> errorIdList, String comment, String idPstCorr, ErrorCorrectType correctType)
        RpcRes_Base<Integer> res = remoteAccess.invoke(ReprocessPostingService.class, "correctErrors",
                errorIdList, "testReprocessList", null, ErrorCorrectType.REPROCESS_LIST);

        Assert.assertFalse(res.isError());
        Assert.assertEquals(3, res.getResult().intValue());
        System.out.println(res.getMessage());

        for (Long errId : errorIdList ) {
            GLErrorRecord err = (GLErrorRecord) baseEntityRepository.findById(GLErrorRecord.class, errId);
            Assert.assertEquals(YesNo.Y, err.getCorrect());
            Assert.assertNotNull(err.getComment());
            Assert.assertNull(err.getAePostingIdNew());
        }

        for (EtlPosting pst: psts) {
            pst = (EtlPosting) baseEntityRepository.refresh(pst, true);
            Assert.assertNull(pst.getErrorCode());
        }

        for (EtlPackage pkg: pkgs) {
            pkg = (EtlPackage) baseEntityRepository.refresh(pkg, true);
            Assert.assertEquals(EtlPackage.PackageState.LOADED, pkg.getPackageState());
        }
    }

    /**
     * Проверка ошибки переобработки списка ошибок
     */
    @Test
    public void testReprocessError() throws SQLException {
        EtlPackage[] pkgs = new EtlPackage[2];
        EtlPosting[] psts = new EtlPosting[3];
        pkgs[0] = newPackage(System.currentTimeMillis(), "WithError1");
        Assert.assertTrue(pkgs[0].getId() > 0);
        psts[0] = createPostingBad(pkgs[0], "SECMOD", "40817036%5", "40817036%6", BankCurrency.AUD, new BigDecimal("315.43") );
        Assert.assertTrue(psts[0].getId() > 0);
        psts[1] = createPostingBad(pkgs[0], "PH", "40817036%7", "40817036%8", BankCurrency.AUD, new BigDecimal("915.43") );
        Assert.assertTrue(psts[1].getId() > 0);
        pkgs[1] = newPackage(System.currentTimeMillis(), "WithError2");
        Assert.assertTrue(pkgs[1].getId() > 0);
        psts[2] = createPostingBad(pkgs[1], "ARMPRO", "40817036%9", "40817036%0", BankCurrency.AUD, new BigDecimal("815.43"));
        Assert.assertTrue(psts[2].getId() > 0);

        for (EtlPosting pst: psts) {
            Assert.assertNull(postingController.processMessage(pst));
            pst = (EtlPosting) baseEntityRepository.refresh(pst, true);
            Assert.assertEquals(1, pst.getErrorCode().intValue());
            setOperday(getWorkdayAfter(getOperday().getCurrentDate()), getOperday().getCurrentDate(), ONLINE, OPEN);
        }

        List<Long> errorIdList = new ArrayList<>();
        for (EtlPosting pst: psts) {
            errorIdList.add(getPostingErrorRecord(pst).getId());
        }
        // correctErrors (List<Long> errorIdList, String comment, String idPstCorr, ErrorCorrectType correctType)
        RpcRes_Base<Integer> res = remoteAccess.invoke(ReprocessPostingService.class, "correctErrors",
                errorIdList, "testReprocessError", null, ErrorCorrectType.REPROCESS_LIST);

        Assert.assertTrue(res.isError());
        Assert.assertEquals(0, res.getResult().intValue());
        System.out.println(res.getMessage());

        for (Long errId : errorIdList ) {
            GLErrorRecord err = (GLErrorRecord) baseEntityRepository.findById(GLErrorRecord.class, errId);
            Assert.assertEquals(YesNo.N, err.getCorrect());
            Assert.assertNull(err.getComment());
            Assert.assertNull(err.getAePostingIdNew());
        }

        for (EtlPosting pst: psts) {
            pst = (EtlPosting) baseEntityRepository.refresh(pst, true);
            Assert.assertEquals(1, pst.getErrorCode().intValue());
        }

        for (EtlPackage pkg: pkgs) {
            pkg = (EtlPackage) baseEntityRepository.refresh(pkg, true);
            Assert.assertEquals(EtlPackage.PackageState.PROCESSED, pkg.getPackageState());
        }
    }

    public EtlPosting createPostingBad(EtlPackage pkg, String src, String acDt, String acCt, BankCurrency ccy, BigDecimal sum) throws SQLException {
        return (EtlPosting) baseEntityRepository.save(newPosting(pkg, src, acDt, acCt, ccy, ccy, sum, sum.add(new BigDecimal("0.1"))));
    }

    public EtlPosting createPosting(EtlPackage pkg, String src, String acDt, String acCt, BankCurrency ccy, BigDecimal sum,
                                    String evtId, String dealId, String pmtRef) throws SQLException {
        EtlPosting pst = newPosting(pkg, src, acDt, acCt, ccy, ccy, sum, sum);
        pst.setDealId(dealId);
        pst.setPaymentRefernce(pmtRef);
        pst.setEventId(evtId);
        return (EtlPosting) baseEntityRepository.save(pst);
    }

    public EtlPosting newPosting(EtlPackage pkg, String src, String acDt, String acCt, BankCurrency ccyDt, BankCurrency ccyCt, BigDecimal sumDt, BigDecimal sumCt ) throws SQLException {
        Operday od = getOperday();
        long stamp = System.currentTimeMillis();

        EtlPosting pst = newPosting(stamp, pkg, src);
        pst.setValueDate(getOperday().getCurrentDate());

        String acDebit = Utl4Tests.findBsaacid(baseEntityRepository, od, acDt);
        String acCredit = Utl4Tests.findBsaacid(baseEntityRepository, od, acCt);

        pst.setAccountCredit(acCredit);
        pst.setAccountDebit(acDebit);
        pst.setCurrencyCredit(ccyCt);
        pst.setCurrencyDebit(ccyDt);
        pst.setAmountCredit(sumCt);
        pst.setAmountDebit(sumDt);

        return pst;
    }

    public static GLErrorRecord getPostingErrorRecord(EtlPosting posting) {
        Assert.assertNotNull(posting);
        Long pstRef = posting.getId();
        EtlPosting pst = (EtlPosting) baseEntityRepository.refresh(posting, true);
        Assert.assertTrue(pst.getErrorCode() == 1);
        String errorMessage = pst.getErrorMessage();
        Assert.assertNotEquals(errorMessage, "SUCCESS");

        GLErrorRecord errorRecord = remoteAccess.invoke(GLErrorRepository.class, "getRecordByRef", pstRef, null);
        Assert.assertNotNull(errorRecord);
        return errorRecord;
    }


}
