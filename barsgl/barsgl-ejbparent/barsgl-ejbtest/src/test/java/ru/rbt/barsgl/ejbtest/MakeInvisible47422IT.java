package ru.rbt.barsgl.ejbtest;

import org.apache.poi.hssf.record.chart.DatRecord;
import org.junit.*;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.MakeInvisible47422Task;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.SourcesDeals;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejb.entity.gl.Reg47422Journal;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.enums.DealSource;
import ru.rbt.barsgl.shared.enums.Reg47422State;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.StringUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import static com.sun.xml.internal.ws.policy.sourcemodel.wspolicy.XmlToken.Optional;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.EUR;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;
//import static ru.rbt.barsgl.ejb.entity.gl.Reg47422Journal.Reg47422State.PROC_GL;
import static ru.rbt.barsgl.ejbtest.AbstractTimerJobIT.restoreOperday;
import static ru.rbt.barsgl.ejbtest.MakeInvisible47422IT.Filial.EKB;
import static ru.rbt.barsgl.ejbtest.MakeInvisible47422IT.Filial.MOS;
import static ru.rbt.barsgl.shared.enums.DealSource.Flex12;
import static ru.rbt.barsgl.shared.enums.DealSource.PaymentHub;
import static ru.rbt.barsgl.shared.enums.Reg47422State.ERRSRC;

/**
 * Created by er18837 on 06.09.2018.
 */
public class MakeInvisible47422IT extends AbstractRemoteIT {

    enum Filial {
        MOS("01"), SPB("02"), EKB("40"), CHL("16");
        private String cod;

        Filial(String cod) {
            this.cod = cod;
        }
    }

    enum Currency {
        RUR("810"), USD("840"), EUR("978");
        private String cod;

        Currency(String cod) {
            this.cod = cod;
        }
    }

    private static HashMap<Filial, HashMap<Currency, String>> accTechMap;

    @BeforeClass
    public static void defineTech() throws SQLException {
        accTechMap = new HashMap<>();
        String sql = "select cbcc, ccy, bsaacid from gl_acc where acc2 = '47422' and acod='4496' and sq='99' "
                + " and cbcc in (" + StringUtils.arrayToString(Filial.values(), ",", "'")
                + ") and ccy in (" + StringUtils.arrayToString(Currency.values(), ",", "'") + ") order by cbcc, ccy";
        List<DataRecord> data = baseEntityRepository.select(sql);
        for (DataRecord rec : data) {
            Filial fil = Filial.valueOf(rec.getString(0));
            HashMap<Currency, String> filMap = accTechMap.get(fil);
            if (null == filMap) {
                filMap = new HashMap<>();
                accTechMap.put(fil, filMap);
            }
            filMap.put(Currency.valueOf(rec.getString(1)), rec.getString(2));
        }
    }

    private String getAccTech(Filial fil, Currency ccy) {
        return accTechMap.get(fil).get(ccy);
    }

    private String getAccMask(String acc2, Filial filial, Currency ccy) {
        return acc2 + ccy.cod + "___" + filial.cod + "%";  // 30102810__040%
    }

    @Before
    public void before() {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    @After
    public void after() {
        restoreOperday();
    }

    @Test
    public void testFull30() {
        Properties props = new Properties();
//        props.setProperty("depth", "30");
//        props.setProperty("withClosedPeriod", "true");
        props.setProperty("mode", "Glue");
//        props.setProperty("mode", "Full");
        remoteAccess.invoke(MakeInvisible47422Task.class, "testExec", null, props);
    }

    @Test
    public void testLoad() throws SQLException {
        Properties props = new Properties();
        props.setProperty("mode", "Load");
        remoteAccess.invoke(MakeInvisible47422Task.class, "testExec", null, props);
        long id0 = getMaxRegId();

        Long[] gloids = makeSimpleOneday(EKB, RUB, new BigDecimal("567.89"));
        remoteAccess.invoke(MakeInvisible47422Task.class, "testExec", null, props);
        long id1 = getMaxRegId();
        Assert.assertEquals(id0+2, id1);

        baseEntityRepository.executeNativeUpdate("update GL_REG47422 set RNARLNG = RNARLNG || '_' where id = ?", id1);
        remoteAccess.invoke(MakeInvisible47422Task.class, "testExec", null, props);
        long id2 = getMaxRegId();
        Assert.assertEquals(id1+1, id2);

        DataRecord rec = baseEntityRepository.selectFirst("select valid from GL_REG47422 where id = ?", id1);
        Assert.assertEquals("N", rec.getString(0));
    }

    @Test
    public void testPbr() throws SQLException {
        Long[] glo1 = makeSimpleOneday(MOS, RUB, new BigDecimal("999"), new String[] {"30102", "47427"}, new DealSource[] {PaymentHub, PaymentHub} );
        Long[] glo2 = makeFanOneday(MOS, EUR, new BigDecimal("444"), new String[] {"30114","47427", "45605"}, new DealSource[] {PaymentHub, Flex12, PaymentHub});

        Properties props = new Properties();
        remoteAccess.invoke(MakeInvisible47422Task.class, "testExec", null, props);

        DataRecord rec1 = baseEntityRepository.selectFirst("select count(1) from GL_REG47422 where GLO_REF in (" + StringUtils.arrayToString(glo1, ",", "") + ") and STATE = ? and VALID = 'Y'", ERRSRC.name());
        Assert.assertEquals(glo1.length, (int)rec1.getInteger(0));

        DataRecord rec2 = baseEntityRepository.selectFirst("select count(1) from GL_REG47422 where GLO_REF in (" + StringUtils.arrayToString(glo2, ",", "") + ") and STATE = ? and VALID = 'Y'", ERRSRC.name());
        Assert.assertEquals(glo2.length, (int)rec2.getInteger(0));

    }

    @Test
    public void testSimpleOneday() throws SQLException {
        Long[] gloids = makeSimpleOneday(EKB, RUB, new BigDecimal("315.45"));

        Properties props = new Properties();
//        props.setProperty("mode", "Full");
        remoteAccess.invoke(MakeInvisible47422Task.class, "testExec", null, props);

        checkPostings(gloids);
    }

    @Test
    public void testFanOneday() throws SQLException {
        Long[] gloids = makeFanOneday(MOS, EUR, new BigDecimal("8421"));

        Properties props = new Properties();
//        props.setProperty("mode", "Full");
        remoteAccess.invoke(MakeInvisible47422Task.class, "testExec", null, props);

        checkPostings(gloids);
    }

    @Test
    public void testOneday() throws SQLException {
        Long[] glo1 = makeSimpleOneday(MOS, RUB, new BigDecimal("888.88"));
        Long[] glo2 = makeSimpleOneday(MOS, RUB, new BigDecimal("888.88"));
        Long[] glo3 = makeFanOneday(MOS, EUR, new BigDecimal("888.88"));

        Properties props = new Properties();
//        props.setProperty("mode", "Full");
        remoteAccess.invoke(MakeInvisible47422Task.class, "testExec", null, props);

        checkPostings(glo1);
        checkPostings(glo2);
        checkPostings(glo3);
    }

    private void checkPostings(Long[] gloids) {
        List<Reg47422Journal> regLoad = getJournalListByGloid(gloids, Reg47422Journal.Reg47422Valid.N, Reg47422State.LOAD);
        List<Reg47422Journal> regProc = getJournalListByGloid(gloids, Reg47422Journal.Reg47422Valid.Y, Reg47422State.PROC_GL);

        Reg47422Journal regMain = regProc.stream().filter(r -> r.getPcIdNew().equals(r.getPcId())).findFirst().orElse(null);
        List<Pd> pdList = baseEntityRepository.select(Pd.class, "from Pd p where p.pcId = ?1 and p.invisible = '0' and not p.stornoRef is null", regMain.getPcId());
        Assert.assertEquals(gloids.length, pdList.size());
        long sum = pdList.stream().mapToLong(p -> p.getAmount()).sum();
        Assert.assertEquals(0, sum);

        List<GLPosting> postList = baseEntityRepository.select(GLPosting.class, "from GLPosting p where p.operation.id = ?1", regMain.getGlOperationId());
        Assert.assertEquals(1, postList.size());
        Assert.assertEquals(gloids.length > 2 ? "5" : "1", postList.get(0).getPostType());
    }

    private List<Reg47422Journal> getJournalListByGloid(Long[] gloids, Reg47422Journal.Reg47422Valid valid, Reg47422State state) {
        List<Reg47422Journal> regList = baseEntityRepository.select(Reg47422Journal.class,
                "from Reg47422Journal r where r.glOperationId in (" + StringUtils.arrayToString(gloids, ",", "") + ") and r.valid = ?1 and r.state = ?2", valid, state);
        Assert.assertEquals(gloids.length, regList.size());
        return regList;
    }

    private Long[] makeSimpleOneday(Filial filial, BankCurrency ccy, BigDecimal amnt, String[] acc2, DealSource[] src ) throws SQLException {
        Date pod = getOperday().getLastWorkingDay();
        String ndog = generateNdog();

        long glo1 = makeOperation(pod, src[0], filial, ccy, ndog, amnt, acc2[0], GLOperation.OperSide.C);
        long glo2 = makeOperation(pod, src[1], filial, ccy, ndog, amnt, acc2[1], GLOperation.OperSide.D);

        return new Long[]{glo1, glo2};
    }

    private Long[] makeSimpleOneday(Filial filial, BankCurrency ccy, BigDecimal amnt) throws SQLException {
        return makeSimpleOneday(filial, ccy, amnt, new String[] {"30102", "47427"}, new DealSource[] {PaymentHub, Flex12} );
    }

    private Long[] makeFanOneday(Filial filial, BankCurrency ccy, BigDecimal amnt, String[] acc2, DealSource[] src) throws SQLException {
        Date pod = getOperday().getLastWorkingDay();
        String ndog = generateNdog();
        BigDecimal amnt1 = amnt.multiply(new BigDecimal(0.75));

        long glo1 = makeOperation(pod, src[0], filial, ccy, ndog, amnt, acc2[0], GLOperation.OperSide.C);
        long glo2 = makeOperation(pod, src[1], filial, ccy, ndog, amnt1, acc2[1], GLOperation.OperSide.D);
        long glo3 = makeOperation(pod, src[2], filial, ccy, ndog, amnt.subtract(amnt1), acc2[2], GLOperation.OperSide.D);

        return new Long[]{glo1, glo2, glo3};
    }

    private Long[] makeFanOneday(Filial filial, BankCurrency ccy, BigDecimal amnt) throws SQLException {
        return makeFanOneday(filial, ccy, amnt, new String[] {"30114","47427", "45605"}, new DealSource[] {PaymentHub, Flex12, Flex12});
    }

    private String generateNdog() throws SQLException {
        DataRecord rec = baseEntityRepository.selectFirst("select max(ID) from GL_ETLPST");
        String num = rec.getString(0);
        if (num.length() < 9) {
            num = StringUtils.leftPad(num, 9 - num.length(), "0");
        }
        return StringUtils.substr(num, 0, 3) + "/" + StringUtils.substr(num, 3, 7) + "L/" + StringUtils.substr(num, 7, 9);
    }

    private long makeOperation(Date pod, DealSource src, Filial filial, BankCurrency ccy, String ndog, BigDecimal amnt, String acc2, GLOperation.OperSide techSide) throws SQLException {

        Currency currency = Currency.valueOf(ccy.getCurrencyCode());
        String accTech = getAccTech(filial, currency);
        String accSrc = Utl4Tests.findBsaacid(baseEntityRepository, pod, getAccMask(acc2, filial, currency));
        String accDt = techSide == GLOperation.OperSide.D ? accTech : accSrc;
        String accCt = techSide == GLOperation.OperSide.C ? accTech : accSrc;

        EtlPackage pkg = newPackage(System.currentTimeMillis(), src + "47422");
        EtlPosting pst = createPosting(pkg, pod, src.getLabel(), accDt, accCt, ccy, amnt,
                src + ": оплата по договору " + ndog + " от " + new SimpleDateFormat("dd.MM.yyyy").format(new Date()));
        Assert.assertTrue(pst.getId()>0);
        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        baseEntityRepository.executeUpdate("update GLOperation o set o.procDate = ?1 where o.id = ?2",pod,operation.getId());
        baseEntityRepository.executeNativeUpdate("insert into gl_etlstma_h (PCID, TRYCNT, OPERDAY, FLAG)" +
                " values ((select PCID from GL_POSTING where GLO_REF = ?), 1, ?, 'I')", operation.getId(), pod);

        return operation.getId();
    }

    public EtlPosting createPosting(EtlPackage pkg, Date pod, String src, String acDt, String acCt, BankCurrency ccy, BigDecimal sum, String rnar) throws SQLException {
        long stamp = System.currentTimeMillis();

        EtlPosting pst = newPosting(stamp, pkg, src);
        pst.setValueDate(pod);

        pst.setAccountCredit(acCt);
        pst.setAccountDebit(acDt);
        pst.setCurrencyCredit(ccy);
        pst.setCurrencyDebit(ccy);
        pst.setAmountCredit(sum);
        pst.setAmountDebit(sum);

        pst.setRusNarrativeLong(rnar);
        return (EtlPosting) baseEntityRepository.save(pst);
    }

    private long getMaxRegId() throws SQLException {
        DataRecord rec = baseEntityRepository.selectFirst("select max(ID) from gL_REG47422");
        return rec.getLong(0);
    }
}
