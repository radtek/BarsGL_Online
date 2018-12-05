package ru.rbt.barsgl.ejbtest;

import org.junit.*;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.controller.operday.task.Exclude47422Task;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
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
import java.util.*;
import java.util.stream.Collectors;

import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.EUR;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.USD;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperSide.C;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperSide.D;
import static ru.rbt.barsgl.ejbtest.AbstractTimerJobIT.restoreOperday;
import static ru.rbt.barsgl.ejbtest.Exclude47422IT.Filial.EKB;
import static ru.rbt.barsgl.ejbtest.Exclude47422IT.Filial.MOS;
import static ru.rbt.barsgl.shared.enums.DealSource.*;
import static ru.rbt.barsgl.shared.enums.Reg47422State.SKIP_SRC;

/**
 * Created by er18837 on 06.09.2018.
 */
public class Exclude47422IT extends AbstractTimerJobIT {

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
    public static void beforeClass() throws SQLException {
        defineTech();
        addBvParm();
        clearReg47422();
    }

    public static void addBvParm() throws SQLException {
        //       insert into gl_bvparm (ID_SRC, BV_SHIFT, DTB) values ('PH', 30, '2017-01-01');
        baseEntityRepository.executeNativeUpdate("delete from gl_bvparm where ID_SRC in (?, ?)", PaymentHub.getLabel(), Flex12.getLabel());
        baseEntityRepository.executeNativeUpdate("insert into gl_bvparm (ID_SRC, BV_SHIFT, DTB) values (?, 30, TO_DATE('2017-01-01', 'YYYY-MM-DD'))", PaymentHub.getLabel());
        baseEntityRepository.executeNativeUpdate("insert into gl_bvparm (ID_SRC, BV_SHIFT, DTB) values (?, 30, TO_DATE('2017-01-01', 'YYYY-MM-DD'))", Flex12.getLabel());
    }

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

    public static void clearReg47422() {
        baseEntityRepository.executeNativeUpdate("update GL_REG47422 set valid = 'N' where state = 'WT47416'");
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
    @Ignore
    public void makeOperations() throws SQLException {
//        makeSimpleOneday(MOS, RUB, new BigDecimal("4321.05"));
//        makeFanOneday(MOS, EUR, new BigDecimal("2448.00"));

        makeSimpleDiffday(MOS, RUB, new BigDecimal("2387"));
        makeFanDiffday(MOS, RUB, new BigDecimal("6420"));

        makeSimpleDiffday(EKB, RUB, new BigDecimal("333"));
    }

    @Test
    @Ignore
    public void testFull30() {
        Properties props = new Properties();
//        props.setProperty("depth", "30");
//        props.setProperty("withClosedPeriod", "true");
        props.setProperty("mode", "Glue");
        remoteAccess.invoke(Exclude47422Task.class, "testExec", null, props);
    }

    @Test
    public void testLoad() throws SQLException {
        Properties props = new Properties();
        props.setProperty("mode", "Load");
        remoteAccess.invoke(Exclude47422Task.class, "testExec", null, props);
        long id0 = getMaxRegId();

        Long[] gloids = makeSimpleOneday(EKB, RUB, new BigDecimal("567.89"));
        remoteAccess.invoke(Exclude47422Task.class, "testExec", null, props);
        long id1 = getMaxRegId();
        Assert.assertEquals(id0+2, id1);

        baseEntityRepository.executeNativeUpdate("update GL_REG47422 set RNARLNG = RNARLNG || '_' where id = ?", id1);
        remoteAccess.invoke(Exclude47422Task.class, "testExec", null, props);
        long id2 = getMaxRegId();
        Assert.assertEquals(id1+1, id2);

        DataRecord rec = baseEntityRepository.selectFirst("select valid from GL_REG47422 where id = ?", id1);
        Assert.assertEquals("N", rec.getString(0));
    }

    @Test
    public void testPbr() throws SQLException, InterruptedException {
        Long[] glo1 = makeSimpleOneday(MOS, USD, new BigDecimal("777"), new String[] {"47422840720010000106", "47422840720010000106"}, new DealSource[] {Manual, Manual} );
//        Long[] glo1 = makeSimpleOneday(MOS, RUB, new BigDecimal("999"), new String[] {"30102", "47427"}, new DealSource[] {Manual, Manual} );
        Long[] glo2 = makeFanOnedayCDD(MOS, EUR, new BigDecimal("444"), new String[] {"30114","47427", "45605"}, new DealSource[] {PaymentHub, Flex12, PaymentHub});

        Properties props = new Properties();
        remoteAccess.invoke(Exclude47422Task.class, "testExec", null, props);

        DataRecord rec1 = baseEntityRepository.selectFirst("select count(1) from GL_REG47422 where GLO_REF in (" + StringUtils.arrayToString(glo1, ",", "") + ") and STATE = ? and VALID = 'Y'", SKIP_SRC.name());
        Assert.assertEquals(getArrayStr("glo1: ", glo1), glo1.length, (int)rec1.getInteger(0));

        DataRecord rec2 = baseEntityRepository.selectFirst("select count(1) from GL_REG47422 where GLO_REF in (" + StringUtils.arrayToString(glo2, ",", "") + ") and STATE = ? and VALID = 'Y'", SKIP_SRC.name());
        Assert.assertEquals(getArrayStr("glo2: ", glo2), glo2.length, (int)rec2.getInteger(0));
    }

    @Test
    public void testOneDay() throws SQLException {
        Long[] glo1 = makeSimpleOneday(MOS, RUB, new BigDecimal("888.88"));
        Long[] glo2 = makeSimpleOneday(MOS, RUB, new BigDecimal("888.88"));
        Long[] glo3 = makeFanOneday(MOS, EUR, new BigDecimal("888.88"));

        Properties props = new Properties();
        remoteAccess.invoke(Exclude47422Task.class, "testExec", null, props);

        checkProcDat(glo1);
        checkProcDat(glo2);
        checkProcDat(glo3);
    }

    @Test
    public void testOneDaySimpleFcc6() throws SQLException {
        Long[] glo1 = makeSimpleOneday(MOS, RUB, new BigDecimal("12345"), new String[] {"40817", "47427"}, new DealSource[] {FCC, Flex12} );
        List<Long> pcids = getPcidListByGloid(glo1);
        baseEntityRepository.executeNativeUpdate("update PST set PBR = '@@IF-CL', GLO_REF = null where GLO_REF = ?", glo1[0]);
        baseEntityRepository.executeNativeUpdate("delete from GL_POSTING where GLO_REF = ?", glo1[0]);
        baseEntityRepository.executeNativeUpdate("delete from GL_OPER where GLOID = ?", glo1[0]);

        Properties props = new Properties();
        remoteAccess.invoke(Exclude47422Task.class, "testExec", null, props);

        checkProcDatByPcid(glo1, pcids);
    }

    @Test
    public void testOneDayFanFcc6() throws SQLException {
        Long[] glo1 = makeFanOnedayCDD(MOS, RUB, new BigDecimal("1648"), new String[] {"40817", "47427", "45203"}, new DealSource[] {FCC, Flex12, Flex12} );
        List<Long> pcid1 = getPcidListByGloid(glo1);
        baseEntityRepository.executeNativeUpdate("update PST set PBR = '@@IF-CL', GLO_REF = null where GLO_REF = ?", glo1[0]);
        baseEntityRepository.executeNativeUpdate("delete from GL_POSTING where GLO_REF = ?", glo1[0]);
        baseEntityRepository.executeNativeUpdate("delete from GL_OPER where GLOID = ?", glo1[0]);

        Long[] glo2 = makeFanOnedayCCD(MOS, RUB, new BigDecimal("1648"), new String[] {"47427", "45203", "40817"}, new DealSource[] {Flex12, Flex12, FCC} );
        List<Long> pcid2 = getPcidListByGloid(glo2);
        baseEntityRepository.executeNativeUpdate("update PST set PBR = '@@IF-CL', GLO_REF = null where GLO_REF = ?", glo2[2]);
        baseEntityRepository.executeNativeUpdate("delete from GL_POSTING where GLO_REF = ?", glo2[2]);
        baseEntityRepository.executeNativeUpdate("delete from GL_OPER where GLOID = ?", glo2[2]);

        Properties props = new Properties();
        remoteAccess.invoke(Exclude47422Task.class, "testExec", null, props);

        checkProcDatByPcid(glo1, pcid1);
        checkProcDatByPcid(glo2, pcid2);
   }

    @Test
    public void testOneDayMemorder() throws SQLException {
        Long[] glo = makeFanOnedayCDD(MOS, RUB, new BigDecimal("88888")
                , new String[] {"45203810820010000202", "30102810600010018795","40702810000014751789", ""}
                , new DealSource[] {Flex12, PaymentHub, PaymentHub});

        Properties props = new Properties();
        remoteAccess.invoke(Exclude47422Task.class, "testExec", null, props);

        checkProcDat(glo);
    }

    @Test
    public void testAbsent47416() throws SQLException {
        Long[] gloids = makeSimpleDiffday(EKB, RUB, new BigDecimal("1980"));

        Properties props = new Properties();
        remoteAccess.invoke(Exclude47422Task.class, "testExec", null, props);

        checkWt47416(gloids);
    }

    @Test
    public void testDiffDay() throws SQLException {
        Long[] glo1 = makeSimpleDiffday(MOS, RUB, new BigDecimal("7810"));
        Long[] glo2 = makeFanDiffday(MOS, RUB, new BigDecimal("4444.8"));

        Properties props = new Properties();
        remoteAccess.invoke(Exclude47422Task.class, "testExec", null, props);

        checkProcAcc(glo1);
        checkProcAcc(glo2);
    }

    private void checkProcDatByPcid(Long[] gloids, List<Long> pcids) {
        List<Reg47422Journal> regProc = getJournalListByPcid(pcids, Reg47422Journal.Reg47422Valid.Y, Reg47422State.PROC_DAT);
        checkProcDat(gloids, regProc );
    }

    private void checkProcDat(Long[] gloids ) {
        List<Reg47422Journal> regProc = getJournalListByGloid(gloids, Reg47422Journal.Reg47422Valid.Y, Reg47422State.PROC_DAT);
        checkProcDat(gloids, regProc );
    }

    private void checkProcDat(Long[] gloids, List<Reg47422Journal> regProc ) {
        Reg47422Journal regMain = regProc.stream().filter(r -> r.getPcIdNew().equals(r.getPcId())).findFirst().orElse(null);
        Assert.assertNotNull("", regMain);
        List<Pd> pdList = baseEntityRepository.select(Pd.class, "from Pd p where p.pcId = ?1 and p.invisible = '0' and not p.stornoRef is null", regMain.getPcId());
        Assert.assertEquals(gloids.length, pdList.size());
        Assert.assertNull(regProc.stream().filter(r -> !r.getParentId().equals(regMain.getId())).findFirst().orElse(null));
        long sum = pdList.stream().mapToLong(p -> p.getAmount()).sum();
        Assert.assertEquals(0, sum);

        if (null == regMain.getGlOperationId())
            return;

        List<GLPosting> postList = baseEntityRepository.select(GLPosting.class, "from GLPosting p where p.operation.id = ?1", regMain.getGlOperationId());
        Assert.assertEquals(1, postList.size());
        Assert.assertEquals(gloids.length > 2 ? "5" : "1", postList.get(0).getPostType());
    }

    private void checkProcAcc(Long[] gloids) {
        List<Reg47422Journal> regProc = getJournalListByGloid(gloids, Reg47422Journal.Reg47422Valid.Y, Reg47422State.PROC_ACC);

        Reg47422Journal regMain = regProc.stream().filter(r -> r.getParentId().equals(r.getId())).findFirst().orElse(null);
        String pcidStr = regProc.stream().map(r -> r.getPcId().toString()).collect(Collectors.joining(","));
        List<Pd> pdList = baseEntityRepository.select(Pd.class, "from Pd p where p.pcId in (" + pcidStr + ") and p.invisible = '0' and not p.stornoRef is null");
        Assert.assertEquals(gloids.length * 2, pdList.size());
        Assert.assertNull(regProc.stream().filter(r -> !r.getParentId().equals(regMain.getId())).findFirst().orElse(null));
        long sum = pdList.stream().mapToLong(p -> p.getAmount()).sum();
        Assert.assertEquals(0, sum);

        Assert.assertEquals(gloids.length, pdList.stream().filter(p -> !p.getBsaAcid().startsWith("47416")).collect(Collectors.toList()).size());
    }

    private void checkWt47416(Long[] gloids) {
        List<Reg47422Journal> regProc = getJournalListByGloid(gloids, Reg47422Journal.Reg47422Valid.Y, Reg47422State.WT47416);

        String pcidStr = regProc.stream().map(r -> r.getPcId().toString()).collect(Collectors.joining(","));
        List<Pd> pdList = baseEntityRepository.select(Pd.class, "from Pd p where p.pcId in (" + pcidStr + ") and p.invisible = '0'");
        Assert.assertEquals(gloids.length * 2, pdList.size());
        long sum = pdList.stream().mapToLong(p -> p.getAmount()).sum();
        Assert.assertEquals(0, sum);

        Assert.assertEquals(gloids.length, pdList.stream().filter(p -> !p.getBsaAcid().startsWith("47422")).collect(Collectors.toList()).size());
    }

    private List<Reg47422Journal> getJournalListByPcid(List<Long> pcids, Reg47422Journal.Reg47422Valid valid, Reg47422State state) {
        List<Reg47422Journal> regList = baseEntityRepository.select(Reg47422Journal.class,
                "from Reg47422Journal r where r.pcId in (" + StringUtils.listToString(pcids, ",", "") + ") and r.valid = ?1 and r.state = ?2", valid, state);
        Assert.assertEquals(getListStr("pcids: ", pcids), pcids.size(), regList.size() * 2);
        return regList;
    }

    private List<Reg47422Journal> getJournalListByGloid(Long[] gloids, Reg47422Journal.Reg47422Valid valid, Reg47422State state) {
        List<Reg47422Journal> regList = baseEntityRepository.select(Reg47422Journal.class,
                "from Reg47422Journal r where r.glOperationId in (" + StringUtils.arrayToString(gloids, ",", "") + ") and r.valid = ?1 and r.state = ?2", valid, state);
        Assert.assertEquals(getArrayStr("gloids: ", gloids), gloids.length, regList.size());
        return regList;
    }

    private String getArrayStr(String pref, Object[] arr) {
        return pref + StringUtils.arrayToString(arr, ",", "");
    }

    private String getListStr(String pref, List<?> lst) {
        return pref + StringUtils.listToString(lst, ",", "");
    }

    private List<Long> getPcidListByGloid(Long[] gloids) throws SQLException {
        List<DataRecord> regList = baseEntityRepository.select("select PCID from PST where GLO_REF in (" + StringUtils.arrayToString(gloids, ",", "") + ")");
        return regList.stream().map(r -> r.getLong(0)).collect(Collectors.toList());
    }

    private Long[] makeSimple(Filial filial, BankCurrency ccy, BigDecimal amnt, String[] acc2, DealSource[] src, Date[] pod  ) throws SQLException {
        String ndog = generateNdog();

        long glo1 = makeOperation(pod[0], src[0], filial, ccy, ndog, amnt, acc2[0], C);
        long glo2 = makeOperation(pod[1], src[1], filial, ccy, ndog, amnt, acc2[1], D);

        return new Long[]{glo1, glo2};

    }

    private Long[] makeSimpleDiffday(Filial filial, BankCurrency ccy, BigDecimal amnt, String[] acc2, DealSource[] src ) throws SQLException {
        Date pod = getOperday().getLastWorkingDay();
        Date pod1 = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", pod, false);
        return makeSimple(filial, ccy, amnt, acc2, src, new Date[] {pod, pod1}  );
    }

    private Long[] makeSimpleDiffday(Filial filial, BankCurrency ccy, BigDecimal amnt ) throws SQLException {
        Date pod = getOperday().getLastWorkingDay();
        Date cday = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", pod, false);
        return makeSimple(filial, ccy, amnt, new String[] {"30102", "47427"}, new DealSource[] {PaymentHub, Flex12}, new Date[] {pod, cday}  );
    }

    private Long[] makeSimpleOneday(Filial filial, BankCurrency ccy, BigDecimal amnt, String[] acc2, DealSource[] src ) throws SQLException {
        Date pod = getOperday().getLastWorkingDay();
        return makeSimple(filial, ccy, amnt, acc2, src, new Date[] {pod, pod}  );
    }

    private Long[] makeSimpleOneday(Filial filial, BankCurrency ccy, BigDecimal amnt) throws SQLException {
        return makeSimpleOneday(filial, ccy, amnt, new String[] {"30102", "47427"}, new DealSource[] {PaymentHub, Flex12} );
    }

    private Long[] makeFanCDD(Filial filial, BankCurrency ccy, BigDecimal amnt, String[] acc2, DealSource[] src, Date[] pod) throws SQLException {
        BigDecimal amnt1 = amnt.multiply(new BigDecimal(0.75));
        return makeFan(filial, ccy, new BigDecimal[] {amnt, amnt1,amnt.subtract(amnt1)},
                acc2, src, pod, new GLOperation.OperSide[] {C, D, D,});
    }

    private Long[] makeFanCCD(Filial filial, BankCurrency ccy, BigDecimal amnt, String[] acc2, DealSource[] src, Date[] pod) throws SQLException {
        BigDecimal amnt1 = amnt.multiply(new BigDecimal(0.25));
        return makeFan(filial, ccy, new BigDecimal[] {amnt1, amnt.subtract(amnt1), amnt},
                acc2, src, pod, new GLOperation.OperSide[] {C, C, D,});
    }

    private Long[] makeFan(Filial filial, BankCurrency ccy, BigDecimal[] amnt, String[] acc2, DealSource[] src, Date[] pod, GLOperation.OperSide[] operSides) throws SQLException {
        String ndog = generateNdog();

        long glo1 = makeOperation(pod[0], src[0], filial, ccy, ndog, amnt[0], acc2[0], operSides[0]);
        long glo2 = makeOperation(pod[1], src[1], filial, ccy, ndog, amnt[1], acc2[1], operSides[1]);
        long glo3 = makeOperation(pod[2], src[2], filial, ccy, ndog, amnt[2], acc2[2], operSides[2]);

        return new Long[]{glo1, glo2, glo3};
    }

    private Long[] makeFanOnedayCDD(Filial filial, BankCurrency ccy, BigDecimal amnt, String[] acc2, DealSource[] src) throws SQLException {
        Date pod = getOperday().getLastWorkingDay();
        return makeFanCDD(filial, ccy, amnt, acc2, src, new Date[] {pod, pod, pod});
    }

    private Long[] makeFanOnedayCCD(Filial filial, BankCurrency ccy, BigDecimal amnt, String[] acc2, DealSource[] src) throws SQLException {
        Date pod = getOperday().getLastWorkingDay();
        return makeFanCCD(filial, ccy, amnt, acc2, src, new Date[] {pod, pod, pod});
    }

    private Long[] makeFanOneday(Filial filial, BankCurrency ccy, BigDecimal amnt) throws SQLException {
        Date pod = getOperday().getLastWorkingDay();
        return makeFanCDD(filial, ccy, amnt, new String[] {"30114","47427", "45605"}, new DealSource[] {PaymentHub, Flex12, Flex12}, new Date[] {pod, pod, pod});
    }

    private Long[] makeFanDiffday(Filial filial, BankCurrency ccy, BigDecimal amnt) throws SQLException {
        Date pod = getOperday().getLastWorkingDay();
        Date cday = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", pod, false);
        return makeFanCDD(filial, ccy, amnt, new String[] {"30114","47427", "45605"}, new DealSource[] {PaymentHub, Flex12, Flex12}, new Date[] {pod, cday, pod});
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
        String accSrc = (acc2.length() == 20) ? acc2 : Utl4Tests.findBsaacid(baseEntityRepository, pod, getAccMask(acc2, filial, currency));
        String accDt = techSide == D ? accTech : accSrc;
        String accCt = techSide == C ? accTech : accSrc;

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
