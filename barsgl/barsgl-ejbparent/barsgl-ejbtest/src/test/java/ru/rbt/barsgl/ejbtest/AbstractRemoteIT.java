package ru.rbt.barsgl.ejbtest;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.w3c.dom.Document;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.BankCalendarDay;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.common.repository.od.OperdayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.stamt.UnloadStamtParams;
import ru.rbt.barsgl.ejb.entity.acc.AccRlnId;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.CurrencyRate;
import ru.rbt.barsgl.ejb.entity.etl.EtlAccount;
import ru.rbt.barsgl.ejb.entity.etl.EtlAccountId;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPd;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejb.integr.bg.*;
import ru.rbt.barsgl.ejb.integr.loader.ILoadManagementController;
import ru.rbt.barsgl.ejb.integr.loader.LoadManagementController;
import ru.rbt.barsgl.ejb.repository.AcDNJournalRepository;
import ru.rbt.barsgl.ejb.repository.PdRepository;
import ru.rbt.barsgl.ejb.repository.RateRepository;
import ru.rbt.barsgl.ejb.repository.WorkdayRepository;
import ru.rbt.barsgl.ejbcore.ClientSupportRepository;
import ru.rbt.barsgl.ejbcore.job.BackgroundJobService;
import ru.rbt.barsgl.ejbcore.page.SqlPageSupport;
import ru.rbt.barsgl.ejbcore.remote.ServerAccess;
import ru.rbt.barsgl.ejbtest.service.ProxyFactory;
import ru.rbt.barsgl.ejbtest.service.ServiceAccessSupport;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.ejbtesting.job.service.TestingJobRegistration;
import ru.rbt.barsgl.ejbtesting.test.GLPLAccountTesting;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.controller.etc.ITextResourceController;
import ru.rbt.ejbcore.controller.etc.TextResourceController;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.repository.BaseEntityRepository;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.google.common.collect.Iterables.find;
import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.*;
import static ru.rbt.barsgl.shared.enums.DealSource.PaymentHub;
import static ru.rbt.ejbcore.util.StringUtils.rightPad;
import static ru.rbt.ejbcore.util.StringUtils.substr;

/**
 * Created by Ivan Sevastyanov
 */
public abstract class AbstractRemoteIT  {

    private static Map<String,Object> services = Collections.synchronizedMap(new HashMap<>());

    private static final ServerAccess remoteAccessInternal;
    protected static final ServiceAccessSupport remoteAccess;
    protected static BaseEntityRepository baseEntityRepository = null;
    protected static GLOperationController fanPostingController = null;
    protected static GLOperationController fanStornoController = null;
    protected static GLOperationController fanStornoOnedayController = null;
    protected static EtlMessageController postingController = null;
    protected static TestingJobRegistration jobRegistrator;
    protected static BackgroundJobService jobService;
    protected static SqlPageSupport pagingSupport;
    protected static AcDNJournalRepository journalRepository;
    protected static ILoadManagementController loadManagementController;
    protected static ITextResourceController textResourceController;

    @BeforeClass
    public static void beforeClassRoot() {
        Operday operday = getOperday();
        checkCreateBankCurrency(operday.getCurrentDate(), EUR, new BigDecimal("63.333"));
        checkCreateBankCurrency(operday.getCurrentDate(), AUD, new BigDecimal("60.111"));
        checkCreateBankCurrency(operday.getCurrentDate(), USD, new BigDecimal("61.222"));
        checkCreateBankCurrency(operday.getLastWorkingDay(), EUR, new BigDecimal("60.333"));
        checkCreateBankCurrency(operday.getLastWorkingDay(), AUD, new BigDecimal("60.111"));
        checkCreateBankCurrency(operday.getLastWorkingDay(), USD, new BigDecimal("61.222"));

//        baseEntityRepository.executeUpdate("update AccountingType a set a.barsAllowed = ?1", N);
    }

    static {
        try {
            Properties prop = new Properties();
            prop.load(AbstractRemoteIT.class.getResourceAsStream("/ejbname.properties"));
            Context ctx = new InitialContext(prop);
            //remoteAccessInternal = (ServerAccess) ctx.lookup("ServerAccessBean#ru.rbt.barsgl.ejbcore.remote.ServerAccessEJBRemote");
            remoteAccessInternal = (ServerAccess) ctx.lookup(prop.getProperty("ejbname"));            
            remoteAccess = new ServiceAccessSupport() {
                @Override
                public <T> T invoke(Class clazz, String method, Object... params) {
                    try {
                        return remoteAccessInternal.invoke(clazz, method, params);
                    } catch (Exception e) {
                        throw new DefaultApplicationException(e);
                    }
                }

                @Override
                public <T> T invoke(String clazzName, String method, Object... params) {
                    try {
                        return remoteAccessInternal.invoke(clazzName, method, params);
                    } catch (Exception e) {
                        throw new DefaultApplicationException(e);
                    }
                }
            };
            init();

        } catch (NamingException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static void init() {
        baseEntityRepository = ProxyFactory.createProxy(ClientSupportRepository.class.getName(), BaseEntityRepository.class, remoteAccessInternal);
        postingController = ProxyFactory.createProxy(EtlPostingController.class.getName(), EtlMessageController.class, remoteAccessInternal);
        fanPostingController = ProxyFactory.createProxy(FanForwardOperationController.class.getName(), GLOperationController.class, remoteAccessInternal);
        fanStornoController = ProxyFactory.createProxy(FanStornoBackvalueOperationController.class.getName(), GLOperationController.class, remoteAccessInternal);
        fanStornoOnedayController = ProxyFactory.createProxy(FanStornoOnedayOperationController.class.getName(), GLOperationController.class, remoteAccessInternal);
        jobRegistrator = ProxyFactory.createProxy(TestingJobRegistration.class.getName(), TestingJobRegistration.class, remoteAccessInternal);
        jobService = ProxyFactory.createProxy(BackgroundJobService.class.getName(), BackgroundJobService.class, remoteAccessInternal);
        pagingSupport = ProxyFactory.createProxy(SqlPageSupport.class.getName(), SqlPageSupport.class, remoteAccessInternal);
        loadManagementController = ProxyFactory.createProxy(LoadManagementController.class.getName(), ILoadManagementController.class, remoteAccessInternal);
        textResourceController = ProxyFactory.createProxy(TextResourceController.class.getName(), ITextResourceController.class, remoteAccessInternal);
    }


    protected static Operday getOperday() {
        return remoteAccess.invoke(OperdayController.class, "getOperday");
    }

    protected static Date getSystemDateTime() {
        return remoteAccess.invoke(OperdayController.class, "getSystemDateTime");
    }

    /**
     * Получает полупроводки для проводки и выполняет базовые проверки
     * @param posting   - проводка GL
     * @return          - список полупроводок для проводки (0 - дебетб 1 - кредит)
     */
    protected List<Pd> getPostingPd(GLPosting posting) {
        GLOperation operation = posting.getOperation();
        String pbr = getPdPbr(operation, posting.getPostType());
        String pnar = getPnar(operation);
        Map<String,String> map = ImmutableMap.<String,String>builder()
                .put("javax.persistence.cache.storeMode", "REFRESH").build();
        List<Pd> pdList = baseEntityRepository.selectHinted(Pd.class, "from Pd p where p.pcId = ?1 order by p.id",
                new Object[]{posting.getId()}, map);
        Assert.assertTrue(pdList.size() == 2);      // 2 полупроводки
        String narrExch = getRusNarrLong(operation, pdList);
        for (Pd pd: pdList) {
            Assert.assertEquals(operation.getId(), pd.getGlOperationId());
            Assert.assertEquals(operation.getValueDate(), pd.getVald());
            Assert.assertEquals(operation.getPostDate(), pd.getPod());
            Assert.assertEquals(operation.getProcDate(), pd.getProcDate());
            Assert.assertEquals(pnar, pd.getPnar());
            Assert.assertEquals(narrExch, pd.getRusNarrLong());
            Assert.assertEquals(operation.getRusNarrativeShort(), pd.getRusNarrShort());
            Assert.assertEquals(pbr, pd.getPbr());
            Assert.assertTrue(null == operation.getDealId()
                    || operation.getDealId().trim().equals(pd.getDealId()));
            Assert.assertTrue(null == operation.getSubdealId()
                    || operation.getSubdealId().trim().equals(pd.getSubdealId()));
            Assert.assertEquals("09", pd.getOperReference());
            if (GLPosting.PostingType.ExchDiff.getValue().equals(posting.getPostType()))
                Assert.assertNull(pd.getEventType());
            else
                Assert.assertEquals(operation.getEventType(), pd.getEventType());
            System.out.println(format("SRC: '%s', PMT_REF: '%s', DEAL_ID: '%s', 'PREF: '%s''",
                    operation.getSourcePosting(), operation.getPaymentRefernce(), operation.getDealId(), pd.getPref()));
        }
        Pd pdDr = pdList.get(0);
        Pd pdCr = pdList.get(1);
        Assert.assertEquals(posting.getId(), pdDr.getId());                // PCID == ID debit
        Assert.assertTrue(pdDr.getAmountBC() + ":" + pdCr.getAmountBC(), -pdDr.getAmountBC() == pdCr.getAmountBC());   // сумма в рублях дебет = кредит - ВСЕГДА!
        return pdList;
    }

    private String getRusNarrLong(GLOperation operation, List<Pd> pdList)  {
        String narr = operation.getRusNarrativeLong();
        for (Pd pd: pdList) {
            if (pd.getBsaAcid().startsWith("706")) {
                String plc = '%' + pd.getBsaAcid().substring(13, 16) + '%';
                try {
                    DataRecord rec = baseEntityRepository.selectFirst("select RNARLNG from EXPDNARPARM where PLCODREGEX like ?", plc);
                    if (null != rec) {
                        narr = rec.getString(0);
                        break;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return narr;
    }

     /**
     * postings in BUFFER mode
     * @param operation
     * @return
     */
    protected List<GLPd> getGLPostingPd(GLOperation operation) {
        List<GLPd> pdList = getGLPostings(operation);
        Assert.assertTrue(pdList.size() == 2);      // 2 полупроводки
        GLPd pdDr = Utl4Tests.findDebitPd(pdList);
        GLPd pdCr = Utl4Tests.findCreditPd(pdList);

        // only for debit buffer pd
        Assert.assertNotNull(pdDr.getMemorderNumber());
        Assert.assertNotNull(pdDr.getPostType());

        // only for debit buffer pd. for credit only reference to operation
        Assert.assertNull(pdCr.getMemorderNumber());
        Assert.assertNull(pdCr.getPostType());

        String pbr = getPdPbr(operation, pdDr.getPostType());
        String pnar = getPnar(operation);
        for (GLPd pd: pdList) {
            Assert.assertEquals(operation.getId(), pd.getGlOperationId());
            Assert.assertEquals(operation.getValueDate(), pd.getVald());
            Assert.assertEquals(operation.getPostDate(), pd.getPod());
            Assert.assertEquals(operation.getProcDate(), pd.getProcDate());
            Assert.assertEquals(pnar, pd.getPnar());
            Assert.assertEquals(operation.getRusNarrativeLong(), pd.getRusNarrLong());
            Assert.assertEquals(operation.getRusNarrativeShort(), pd.getRusNarrShort());
            Assert.assertEquals(pbr, pd.getPbr());
            Assert.assertTrue(null == operation.getDealId()
                    || operation.getDealId().trim().equals(pd.getDealId()));
            Assert.assertTrue(null == operation.getSubdealId()
                    || operation.getSubdealId().trim().equals(pd.getSubdealId()));
            Assert.assertEquals("09", pd.getOperReference());
            if (GLPosting.PostingType.ExchDiff.getValue().equals(pdDr.getPostType()))
                Assert.assertNull(pd.getEventType());
            else
                Assert.assertEquals(operation.getEventType(), pd.getEventType());
            System.out.println(format("SRC: '%s', PMT_REF: '%s', DEAL_ID: '%s', 'PREF: '%s''",
                    operation.getSourcePosting(), operation.getPaymentRefernce(), operation.getDealId(), pd.getPref()));
        }
        Assert.assertTrue(pdDr.getAmountBC() + ":" + pdCr.getAmountBC(), -pdDr.getAmountBC() == pdCr.getAmountBC());   // сумма в рублях дебет = кредит - ВСЕГДА!
        return pdList;
    }


    private String getPdPbr(GLOperation operation, String postType) {
        String pbr = "@@GL";
        if (postType.equals(GLPosting.PostingType.ExchDiff.getValue())) {
            pbr = pbr + "RCA";
        } else {
            try {
                DataRecord rec = baseEntityRepository.selectFirst("select SHNM from GL_SRCPST where ID_SRC = ?", operation.getSourcePosting());
                if (null != rec) {
                    pbr = pbr + rec.getString(0);
                }
                else {
                    pbr = pbr + "-" + operation.getSourcePosting();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return substr(pbr, 7);
    }

    public static String getPnar(GLOperation operation) {
        String pref = remoteAccess.invoke(PdRepository.class, "getPref", operation);
        return remoteAccess.invoke(PdRepository.class, "getPnar", operation, pref);
    }

    public static String getPnarManual (String dealId, String subDealId, String paymentRef) {
        return remoteAccess.invoke(PdRepository.class, "getPnarManual",  dealId, subDealId, paymentRef);
    }


    /**
     * Получает полупроводки для веерной проводки и выполняет базовые проверки
     * @param posting   - проводка GL
     * @return          - список полупроводок для проводки (0 - дебетб 1 - кредит)
     */
    protected List<Pd> getFanPostingPd(GLPosting posting) {
        GLOperation operation = posting.getOperation();
        Map<String,String> map = ImmutableMap.<String,String>builder()
                .put("javax.persistence.cache.storeMode", "REFRESH").build();
        List<Pd> pdList = baseEntityRepository.selectHinted(Pd.class, "from Pd p where p.pcId = ?1 order by p.id",
                new Object[]{posting.getId()}, map);
        Assert.assertTrue(pdList.size() >= 2);      // 2 полупроводки
        for (Pd pd: pdList) {
            Assert.assertEquals(operation.getValueDate(), pd.getVald());
            Assert.assertEquals(operation.getPostDate(), pd.getPod());
        }
        return pdList;
    }

    protected List<GLPosting> getPostings(GLOperation operation) {
        return baseEntityRepository.select(GLPosting.class, "from GLPosting p where p.operation = ?1 order by p.id", new Object[]{operation});
    }

    /**
     * Создает тестовый ETL пакет, СОХРАНЯЕТ в таблице
     * @param stamp
     * @param descr
     * @return
     */
    public static EtlPackage newPackage(long stamp, String descr) {
        EtlPackage pkg = newPackageNotSaved(stamp, descr);
        pkg = (EtlPackage) baseEntityRepository.save(pkg);
        return pkg;
    }

    /**
     * Создаем пакет и НЕ сохраняемм в БД
     * @param stamp
     * @param descr
     * @return
     */
    public static EtlPackage newPackageNotSaved(long stamp, String descr) {
        EtlPackage pkg = new EtlPackage();
        pkg.setPackageName("pkg" + stamp);
        pkg.setAccountCnt(0);
        pkg.setDateLoad(new Date());
        pkg.setDescription(descr);
        pkg.setPackageState(EtlPackage.PackageState.PROCESSED);
        pkg.setPostingCnt(1);
//        pkg.setMessage("123");
        pkg.setId(baseEntityRepository.nextId("GL_SEQ_PKG"));
        return pkg;
    }

    /**
     * Создает тестовый ETL постинг (без VDATE), НЕ СОХРАНЯЕТ в таблице
     * @param stamp
     * @param pkg
     * @return
     */
    public static EtlPosting newPosting(long stamp, EtlPackage pkg) {
        return newPosting(stamp, pkg, PaymentHub.getLabel());
    }

    public static EtlPosting newPosting(long stamp, EtlPackage pkg, String src) {
        EtlPosting pst = new EtlPosting();
        String st = ("" + System.currentTimeMillis()).substring(3);
        pst.setAePostingId("id_" + st);

        // незначимые параметры
        pst.setChnlName("CHN_TEST");
        pst.setPaymentRefernce("PMT" + stamp);
//        pst.setDealId(String.valueOf(stamp).substring(0, 10));
        pst.setDeptId("NGT");
        pst.setEventId("evtid" + st);
        pst.setEventType("eventType");
        pst.setNarrative(StringUtils.leftPad("nrt_" + stamp + "_", 100, "0"));
        pst.setOperationTimestamp(new Date());
        pst.setRusNarrativeLong("nrt" + stamp);
        pst.setRusNarrativeShort("nrt" + stamp);
        pst.setSourcePosting(src);

        // могут стать значимыми
        pst.setFan(YesNo.N);
        pst.setParentReference(null);

        pst.setStorno(YesNo.N);
        pst.setStornoReference(null);

        pst.setAmountDebitRu(null);
        pst.setAmountCreditRu(null);

        pst.setEtlPackage(pkg);
        pst.setId(baseEntityRepository.nextId("GL_SEQ_PST"));
        return pst;
    }

    public static EtlPosting createFanPosting(long st
            , EtlPackage etlPackage
            , String accDt
            , String accCt
            , BigDecimal amtDt
            , BankCurrency curDt
            , BigDecimal amtCt
            , BankCurrency curCt
            , String reference
            , String parentRef
            , YesNo fan) {
        EtlPosting pst = newPosting(st, etlPackage);
        pst.setValueDate(getOperday().getCurrentDate());
        pst.setFan(fan);
        pst.setPaymentRefernce(reference);
        pst.setParentReference(parentRef);

        pst.setAccountDebit(accDt);        // MOS, RUB
        pst.setAccountCredit(accCt);       // MOS, RUB
        pst.setAmountCredit(amtCt);
        pst.setAmountDebit(amtDt);
        pst.setCurrencyCredit(curCt);
        pst.setCurrencyDebit(curDt);
        pst.setAePostingId("id_" + ("" + System.currentTimeMillis()).substring(3));

        return (EtlPosting) baseEntityRepository.save(pst);
    }

    /**
     * Создает сторно ETL для основного  (без VDATE), НЕ СОХРАНЯЕТ в таблице
     * @param stamp
     * @param pkg
     * @param pst
     * @return
     */
    public static EtlPosting newStornoPosting(long stamp, EtlPackage pkg, EtlPosting pst) {
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
        return pstS;
    }

    /**
     * Создает сторно ETL для основного  (без VDATE), НЕ СОХРАНЯЕТ в таблице
     * @param stamp
     * @param pkg
     * @param pst
     * @return
     */
    public static EtlPosting newStornoFanPosting(long stamp, EtlPackage pkg, EtlPosting pst, String parentReference, Date valueDate) {
        EtlPosting pstS = newStornoPosting(stamp, pkg, pst);
        // данные по вееру
        pstS.setFan(pst.getFan());
        pstS.setParentReference(parentReference);
        pstS.setValueDate(valueDate);
        return pstS;
    }

    /**
     * Создает ETL перо для веера, НЕ СОХРАНЯЕТ в таблице
     * @param stamp
     * @param pst
     * @return
     */
    public static EtlPosting newFanPosting(long stamp, EtlPosting pst, String fbSide, BigDecimal fbAmount,
                                           String fpAccount, BankCurrency fpCcy, BigDecimal fpAmount) {
        EtlPosting pstS = newPosting(stamp, pst.getEtlPackage());
        // ссылка
        pstS.setValueDate(pst.getValueDate());
        pstS.setParentReference(pst.getParentReference());
        pstS.setFan(YesNo.Y);
        // данные проводки
        if (fbSide.equals("D")) {
            pstS.setAccountDebit(pst.getAccountDebit());
            pstS.setCurrencyDebit(pst.getCurrencyDebit());
            pstS.setAmountDebit(fbAmount);
            pstS.setAccountCredit(fpAccount);
            pstS.setCurrencyCredit(fpCcy == null ? pst.getCurrencyDebit() : fpCcy);
            pstS.setAmountCredit(fpAmount == null ? fbAmount : fpAmount);
        } else {
            pstS.setAccountCredit(pst.getAccountCredit());
            pstS.setCurrencyCredit(pst.getCurrencyCredit());
            pstS.setAmountCredit(fbAmount);
            pstS.setAccountDebit(fpAccount);
            pstS.setCurrencyDebit(fpCcy == null ? pst.getCurrencyCredit() : fpCcy);
            pstS.setAmountDebit(fpAmount == null ? fbAmount : fpAmount);
        }
        return pstS;
    }

    public static EtlAccount newAccount(EtlPackage pkg, String bsaAcid, boolean toDelete ) throws SQLException {
        if ( toDelete ) {
                DataRecord rec = baseEntityRepository.selectFirst("select 1 from BSAACC where ID = ?", bsaAcid);
                DataRecord acidRec = baseEntityRepository.selectFirst("select ACID from ACCRLN where BSAACID = ?", bsaAcid);
                baseEntityRepository.executeUpdate("delete from GLAccount g where g.bsaAcid = ?1", bsaAcid);
                if (null != acidRec && !acidRec.getString(0).isEmpty()) {
                    String acid = acidRec.getString(0);
                    baseEntityRepository.executeUpdate("delete from GlAccRln g where g.id.acid = ?1 and g.id.bsaAcid = ?2", acid, bsaAcid);
                    baseEntityRepository.executeUpdate("delete from Acc a where a.id = ?1", acid);
                }
                if (null != rec) {
                    baseEntityRepository.executeUpdate("delete from BsaAcc b where b.id = ?1", bsaAcid);
                }
        }
        EtlAccountId id = new EtlAccountId(pkg.getId(), bsaAcid);
        EtlAccount etlAcc = new EtlAccount();
        etlAcc.setId(id);
        etlAcc.setDateOpen(new Date());
        return etlAcc;

    }

    protected static GLPosting findGLPosting(final String post_type, List<GLPosting> posts) {
        return find(posts, post -> post.getPostType().equalsIgnoreCase(post_type), null);
    }

    protected static void updateOperday(Operday.OperdayPhase phase, Operday.LastWorkdayStatus status, Operday.PdMode pdMode) {
        int cnt = baseEntityRepository.executeUpdate(
                "update Operday o set o.lastWorkdayStatus = ?1, o.phase = ?2, o.pdMode = ?3", status, phase, pdMode);
        Assert.assertEquals(1, cnt);
        refreshOperdayState();
    }

    protected static void updateOperdayMode(Operday.PdMode pdMode, ProcessingStatus processingStatus) {
    int cnt = baseEntityRepository.executeUpdate(
            "update Operday o set o.pdMode = ?1, o.processingStatus = ?2", pdMode, processingStatus);
    Assert.assertEquals(1, cnt);
    refreshOperdayState();
    }

    // ProcessingStatus

    protected static void updateOperday(Operday.OperdayPhase phase, Operday.LastWorkdayStatus status) {
        updateOperday(phase, status, Operday.PdMode.DIRECT);
    }

    protected static void Operday(Operday.OperdayPhase phase, Operday.LastWorkdayStatus status) {
        int cnt = baseEntityRepository.executeUpdate("update Operday o set o.lastWorkdayStatus = ?1, o.phase = ?2", status, phase);
        Assert.assertEquals(1, cnt);
        refreshOperdayState();
    }

    protected static void refreshOperdayState() {
        remoteAccess.invoke(OperdayController.class, "refresh");
    }

    protected static CurrencyRate findRate(BankCurrency currency, Date ondate) {
        return remoteAccess.invoke(RateRepository.class, "findRate", currency, ondate);
    }

    protected static CurrencyRate findRate(String currencyCode, Date ondate) {
        BankCurrency currency = (BankCurrency) baseEntityRepository.selectFirst(BankCurrency.class, "from BankCurrency c where c.id = ?1", currencyCode);
        Assert.assertNotNull(currency);
        return remoteAccess.invoke(RateRepository.class, "findRate", currency, ondate);
    }

    protected static Date getWorkdayAfter(Date date) {
        return ((BankCalendarDay)remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkdayAfter", date)).getId().getCalendarDate();
    }

    protected static Date getWorkdayBefore(Date date) {
        return ((BankCalendarDay)remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkdayBefore", date)).getId().getCalendarDate();
    }

    protected static boolean isWorkday(Date date) {
        return (Boolean) remoteAccess.invoke(BankCalendarDayRepository.class, "isWorkday", date);
    }

    /**
     * для USD
     * @param ondate
     */
    protected static void checkCreateBankCurrency(Date ondate) {
        checkCreateBankCurrency(ondate, USD, new BigDecimal("60.000"));
    }

    protected static void checkCreateBankCurrency(Date ondate, BankCurrency currency, BigDecimal rateAmount) {
        CurrencyRate rate = remoteAccess.invoke(RateRepository.class, "findRate", currency, ondate);
        if (null == rate) {
            rate = new CurrencyRate(currency, ondate, rateAmount, BigDecimal.ONE);
            remoteAccess.invoke(RateRepository.class, "save", rate);
        }
    }

    /**
     * Проверка ссылки на сторнируемую проводку
     * @param postS
     * @param postList
     */
    public void checkStornoRef(GLPosting postS, List<GLPosting> postList) {
        GLPosting post = findGLPosting(postS.getStornoType(), postList);
        Assert.assertNotNull(post);
        Long pcidRef = post.getId();
        Assert.assertEquals(postS.getStornoPcid(), pcidRef);
    }

    public List<GLOperation> processPostings(List<EtlPosting> etlList) {
        try {
            List<GLOperation> operList = new ArrayList<>();
            for (EtlPosting post : etlList) {
                GLOperation oper = (GLOperation) postingController.processMessage(post);
    //            Assert.assertNotNull(oper);
                if (null != oper)
                    operList.add(oper);
            }
            return operList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static void setOperday(Date curentDate, Date lastWorkingDay, Operday.OperdayPhase phase, Operday.LastWorkdayStatus status) {
        setOperday(curentDate, lastWorkingDay, phase, status, Operday.PdMode.DIRECT);
        refreshOperdayState();
    }

    protected static void setOperday(Date curentDate, Date lastWorkingDay
            , Operday.OperdayPhase phase, Operday.LastWorkdayStatus status, Operday.PdMode pdMode) {
        baseEntityRepository.executeUpdate(
                "update Operday o set o.currentDate=?1, o.lastWorkdayStatus=?2, o.lastWorkingDay=?3, o.phase=?4, o.pdMode = ?5, o.processingStatus =?6 "
                , curentDate, status, lastWorkingDay, phase, pdMode, ProcessingStatus.STARTED);
        baseEntityRepository
                .executeNativeUpdate("update cal set hol = ' ', thol = ' ' where dat in (?, ?) and ccy = 'RUR'"
                , curentDate, lastWorkingDay);
        refreshOperdayState();
    }

    protected static void initCorrectOperday() {
        Date workday = remoteAccess.invoke(WorkdayRepository.class, "getWorkday");
        BankCalendarDay current = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkdayAfter", workday);
        setOperday(current.getId().getCalendarDate(), workday, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        refreshOperdayState();
    }

    protected List<GLPd> getGLPostings(GLOperation operation) {
        return baseEntityRepository.select(GLPd.class, "from GLPd p where p.glOperationId = ?1"
                , new Object[]{operation.getId()});

    }

    protected ManualOperationWrapper newOperationWrapper(String accountDebit, String accountCredit,
                                                  String currencyDebit, BigDecimal amountDebit ) {
        return newOperationWrapper("",
                "", accountDebit, currencyDebit, amountDebit,
                "", accountCredit, currencyDebit, amountDebit);
    }

    protected ManualOperationWrapper newOperationWrapper( String bsChapter,
            String filialDebit,  String accountDebit,  String currencyDebit,  BigDecimal amountDebit,
            String filialCredit, String accountCredit, String currencyCredit, BigDecimal amountCredit  ) {

        return newOperationWrapper( getOperday().getCurrentDate(), bsChapter,
                filialDebit,  accountDebit,  currencyDebit,  amountDebit,
                filialCredit, accountCredit, currencyCredit, amountCredit  );
    }

    protected ManualOperationWrapper newOperationWrapper( Date postDate, String bsChapter,
                                                          String filialDebit,  String accountDebit,  String currencyDebit,  BigDecimal amountDebit,
                                                          String filialCredit, String accountCredit, String currencyCredit, BigDecimal amountCredit  ) {
        ManualOperationWrapper wrapper = new ManualOperationWrapper();

        wrapper.setInputMethod(InputMethod.M);
        String st = ("" + System.currentTimeMillis()).substring(6);
        wrapper.setDealId(st);
        wrapper.setPaymentRefernce(null);
        wrapper.setDeptId("AUD");
        wrapper.setDealSrc("K+TP");
        wrapper.setProfitCenter("AL");

        wrapper.setNarrative("Narrative");
        wrapper.setRusNarrativeShort("RusNarrativeShort");
        wrapper.setRusNarrativeLong("RusNarrativeLong");

        wrapper.setValueDateStr(new SimpleDateFormat(wrapper.dateFormat).format(postDate));
        wrapper.setPostDateStr(wrapper.getValueDateStr());

        wrapper.setFilialDebit(filialDebit);
        wrapper.setAccountDebit(accountDebit);
        wrapper.setCurrencyDebit(currencyDebit);
        wrapper.setAmountDebit(amountDebit);

        wrapper.setFilialCredit(filialCredit);
        wrapper.setAccountCredit(accountCredit);
        wrapper.setCurrencyCredit(currencyCredit);
        wrapper.setAmountCredit(amountCredit);

        return wrapper;
    }

    protected ManualOperationWrapper newOperationWrapper(GLOperation operation) {
        ManualOperationWrapper wrapper = new ManualOperationWrapper();

        wrapper.setId(operation.getId());
        wrapper.setInputMethod(InputMethod.AE);
        String st = ("" + System.currentTimeMillis()).substring(6);
        wrapper.setDealId(operation.getDealId());
        wrapper.setPaymentRefernce(operation.getPaymentRefernce());
        wrapper.setDeptId(operation.getDeptId());
        wrapper.setDealSrc(operation.getSourcePosting());

        wrapper.setNarrative(operation.getNarrative());
        wrapper.setRusNarrativeShort(operation.getRusNarrativeShort());
        wrapper.setRusNarrativeLong(operation.getRusNarrativeLong());

        wrapper.setValueDateStr(new SimpleDateFormat(wrapper.dateFormat).format(getOperday().getCurrentDate()));
        wrapper.setPostDateStr(wrapper.getValueDateStr());

//        wrapper.setFilialDebit(filialDebit);
        wrapper.setAccountDebit(operation.getAccountDebit());
        wrapper.setCurrencyDebit(operation.getCurrencyDebit().getCurrencyCode());
        wrapper.setAmountDebit(operation.getAmountDebit());

//        wrapper.setFilialCredit(filialCredit);
        wrapper.setAccountCredit(operation.getAccountCredit());
        wrapper.setCurrencyCredit(operation.getCurrencyCredit().getCurrencyCode());
        wrapper.setAmountCredit(operation.getAmountCredit());

        return wrapper;
    }

    public void checkCreateStep(String stepName, Date ondate, String state) {
        Optional<DataRecord> optional = Optional.ofNullable((DataRecord)remoteAccess.invoke(OperdayRepository.class
                , "findWorkprocStep", stepName, ondate));

        if (!optional.isPresent()) {
            baseEntityRepository.executeNativeUpdate(
                    "insert into workproc (dat, id, starttime, endtime, result, count, msg) values (?,?,?,?,?,?,?)"
                    , ondate, stepName, new Date(), new Date(), state, 0, "Testing 1");
        } else {
            DataRecord record = optional.get();
            if (record.getString("result") == null || !record.getString("result").trim().equals(state)) {
                baseEntityRepository.executeNativeUpdate("update workproc set result = ?, msg = ? where id = ? and dat = ?",
                        state, "Testing 1", rightPad(stepName, 10, " "), ondate);
            }
        }
    }

    public DataRecord getLastHistRecord(String jobName) throws SQLException {
        return baseEntityRepository.selectFirst("select * from gl_sched_h where sched_name = ? order by 1 desc ", jobName);
    }

    public static GLOperation getLastOperation(Long idpst) {
        return (GLOperation) baseEntityRepository.selectFirst(GLOperation.class, "from GLOperation o where o.etlPostingRef = ?1 order by 1 desc", idpst);
    }

    public static GLPosting getPosting(Long idoper) {
        return (GLPosting) baseEntityRepository.selectFirst(GLOperation.class, "from GLPosting p where p.operation = ?1 order by 1 desc", idoper);
    }

    public static GLPosting getPostingByOper(GLOperation oper) {
        return (GLPosting) baseEntityRepository.selectFirst(GLOperation.class, "from GLPosting p where p.operation = ?1 order by 1 desc", oper);
    }

    public static String findBsaAccount(String bsaacidLike) throws SQLException {
        return findBsaAccount(bsaacidLike, new Date());
    }

    public static AccRlnId findAccRln(String bsaacidLike) throws SQLException {
        return Optional.ofNullable(baseEntityRepository.selectFirst("select acid, bsaacid from accrln r, BSAACC a where r.bsaacid like ? and r.bsaacid = a.id and a.BSAACC > ?"
                , bsaacidLike, new Date()))
                .map(r -> new AccRlnId(r.getString(0), r.getString(1))).orElseThrow(() -> new DefaultApplicationException("Not found " + bsaacidLike));
    }

    public static long createPd(Date pod, String acid, String bsaacid, String glccy, String pbr) throws SQLException {
        long id = baseEntityRepository.selectFirst("select PD_SEQ.nextval id_seq from dual").getLong(0);
        baseEntityRepository.executeNativeUpdate("insert into pd (id,pod,vald,acid,bsaacid,ccy,amnt,amntbc,pbr,pnar,invisible) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", id, pod, pod, acid, bsaacid, glccy, 100,100, pbr, "1234", "0");
        return id;
    }

    public static String findBsaAccount(String bsaacidLike, Date dateClose) throws SQLException {
        return Optional.ofNullable(baseEntityRepository.selectFirst(
                "SELECT \"BSAACID\" FROM \"ACCRLN\" \"R\", \"BSAACC\" \"A\" " +
                        "WHERE \"R\".\"BSAACID\" LIKE ? AND \"R\".\"BSAACID\" = \"A\".\"ID\" AND \"A\".\"BSAACC\" > ?"
                , bsaacidLike, dateClose))
                .map(r -> r.getString(0)).orElseThrow(() -> new DefaultApplicationException("Not found " + bsaacidLike + " " + dateClose));
    }

    protected static DataRecord getLastUnloadHeader(UnloadStamtParams params) throws SQLException {
        return Optional.ofNullable(baseEntityRepository
                .selectFirst("select * from gl_etlstms where parname = ? and pardesc = ? order by id desc"
                        , params.getParamName(), params.getParamDesc())).orElse(null);
    }

    protected static void emulateWorkprocStep(Date operDay, String stepName) throws Exception {
        int cnt = baseEntityRepository.selectFirst("select count(1) cnt from workproc where dat = ? and trim(id) = ?"
                , operDay, stepName).getInteger(0);
        if (cnt == 1) {
            baseEntityRepository.executeNativeUpdate("update workproc set result = 'O' where dat = ? and trim(id) = ?"
                    , operDay, stepName);
        } else {
            baseEntityRepository.executeNativeUpdate("insert into workproc  values (?, ?, current_timestamp, current_timestamp, 'O', 1, ?)"
                    , operDay, stepName, stepName);
        }
    }

    protected static void executeAutonomic(String sql) throws Exception {
        remoteAccess.invoke(GLPLAccountTesting.class, "executeAutonomic", sql);
    }

    protected static String getCustomerNumberEmptyType() throws SQLException {
        DataRecord data = baseEntityRepository.selectFirst("select BBCUST from SDCUSTPD" +
                " where BXCTYP < 3 and not coalesce(BBCNA1, ' ') = ' ' and not coalesce(BXRUNM, ' ') = ' '");
        return data.getString(0);
    }

    protected static String getCustomerNumberByType(String custType) throws SQLException {
        DataRecord data = baseEntityRepository.selectFirst("select BBCUST from SDCUSTPD" +
                " where BXCTYP = ? and not coalesce(BBCNA1, ' ') = ' ' and not coalesce(BXRUNM, ' ') = ' '", custType);
        return data.getString(0);
    }

    protected static String getAccountType(String custType, String term) throws SQLException {
        DataRecord data = baseEntityRepository.selectFirst("select p.ACCTYPE from GL_ACTPARM p join GL_ACTNAME n on n.ACCTYPE = p.ACCTYPE" +
                " where CUSTYPE = ? and TERM = ? and TECH_ACT <> 'Y' and DTB <= ? and DTE is null", StringUtils.rightPad(custType,3), term, getOperday().getCurrentDate());
        return data.getString(0);
    }

    protected static String getAccountType(String custType, String term, String acc2) throws SQLException {
        DataRecord data = baseEntityRepository.selectFirst("select p.ACCTYPE from GL_ACTPARM p join GL_ACTNAME n on n.ACCTYPE = p.ACCTYPE" +
                " where CUSTYPE = ? and TERM = ? and ACC2 like ? and TECH_ACT <> 'Y' and DTB <= ? and DTE is null",
                StringUtils.rightPad(custType,3), term, acc2, getOperday().getCurrentDate());
        return data.getString(0);
    }

    protected String getRecourceText(String resourceName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream("/" + resourceName), "UTF-8");
    }

    protected Document getDocument(DocumentBuilder docBuilder, String message) throws IOException, org.xml.sax.SAXException {
        return docBuilder.parse(new ByteArrayInputStream(message.getBytes("UTF-8")));
    }

    protected String getXmlParam(XPath xPath, Document doc, String parentNode, String paramName) throws XPathExpressionException {
        return (String) xPath.evaluate(parentNode + "/" + paramName, doc.getDocumentElement(), XPathConstants.STRING);
    }

    protected String changeXmlParam(String message, String paramName, String oldValue, String newValue) {
        return message.replace(paramName + ">" + oldValue, paramName + ">" + newValue);
    }

    public long getAuditMaxId() throws SQLException {
        DataRecord res = baseEntityRepository.selectFirst("select max(ID_RECORD) from GL_AUDIT");
        return null == res ? 0 : res.getLong(0);
    }

}
