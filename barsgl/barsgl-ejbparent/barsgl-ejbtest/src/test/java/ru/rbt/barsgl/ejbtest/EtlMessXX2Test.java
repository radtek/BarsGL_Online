package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.shared.enums.OperState;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;

/**
 * Created by ER22317 on 28.02.2017.
 */
public class EtlMessXX2Test extends AbstractTimerJobTest {
    static final String BSAACID0 = "30424810500014588436";
    static final String ACID = "00400038RUR104902001";

    @Before
    public void beforeClass() throws Exception{
        Date operday = DateUtils.parseDate("2017-02-10", "yyyy-MM-dd");
        setOperday(operday, DateUtils.addDays(operday, -1), ONLINE, OPEN);
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        testInitTable();
    }

    public void testInitTable() throws Exception {
        int count;
        if (null == baseEntityRepository.selectFirst("select 1 from GL_ACTPARM where ACCTYPE = '131060102' and CUSTYPE = '00' and term='00' and acc2='30424'")) {
            count = baseEntityRepository.executeNativeUpdate("insert into GL_ACTPARM (ACCTYPE,CUSTYPE,term,acc2,plcode,acod,ac_SQ,DTB) " +
                    "values('131060102','00','00','30424',null,'1049','02',TO_DATE(TO_DATE('2014-05-01','RRRR-MM-DD'),'RRRR-MM-DD'))");
            System.out.println(count + ": insert values('131060102','00','00','30424',null,'1049','02',TO_DATE('2014-05-01','RRRR-MM-DD'))");
        }
        if (null == baseEntityRepository.selectFirst("select 1 from GL_ACTPARM where ACCTYPE = '131060102' and CUSTYPE = '9' and term='00' and acc2='30424'")) {
            count = baseEntityRepository.executeNativeUpdate("insert into GL_ACTPARM (ACCTYPE,CUSTYPE,term,acc2,plcode,acod,ac_SQ,DTB) " +
                    "values('131060102','9','00','30424',null,'1049','02',TO_DATE('2014-05-01','RRRR-MM-DD'))");
            System.out.println(count + ": insert values('131060102','9','00','30424',null,'1049','02',TO_DATE('2014-05-01','RRRR-MM-DD'))");
        }
        if (null == baseEntityRepository.selectFirst("select 1 from GL_ACTPARM where ACCTYPE = '712010100' and CUSTYPE = '21' and term='05' and acc2='70606'")) {
            count = baseEntityRepository.executeNativeUpdate("insert into GL_ACTPARM (ACCTYPE,CUSTYPE,term,acc2,plcode,acod,ac_SQ,DTB) " +
                    "values('712010100','21','05','70606','31318','8107','01',TO_DATE('2014-05-01','RRRR-MM-DD'))");
            System.out.println(count + ": insert values('712010100','21','05','70606','31318','8107','01',TO_DATE('2014-05-01','RRRR-MM-DD'))");
        }

        if (null == baseEntityRepository.selectFirst("select 1 from GL_SRCPST where ID_SRC = 'AXAPTA'")) {
            count = baseEntityRepository.executeNativeUpdate("insert into GL_SRCPST (ID_SRC, SHNM, LGNM,fl_dealid) values('AXAPTA','AXT','Внутренняя бухгалтерия', 'N')");
            System.out.println(count + ": insert into GL_SRCPST (ID_SRC, SHNM, LGNM,fl_dealid) values('AXAPTA','AXT','Внутренняя бухгалтерия', 'N')");
        }

         if (null == baseEntityRepository.selectFirst("select * from  GL_ACTNAME where ACCTYPE = '131060102'")){
            count = baseEntityRepository.executeNativeUpdate("insert into GL_ACTNAME(acctype, accname, pl_act, fl_ctrl) values('131060102', 'EtlMessXX2Test', 'N', 'N')");
             System.out.println(count + ": insert into GL_ACTNAME(acctype, accname, pl_act, fl_ctrl) values('131060102', 'EtlMessXX2Test', 'N', 'N')");
         }
        //предполагается существование 1 счета 30424810500014588436, 00400038RUR104902001 с rlntype='0'
        //и любого количества других счетов 00400038RUR104902001 с rlntype='4'
        //и любого количества других счетов 00400038RUR104902001 с остальными rlntype
        //начальное состояние gl_acc задается в glAccBeginState()
    }

    /*
  Case 00: Ключ счета дебета с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 9
  (соответствует строке 9 в настройках), также пустыми оставлены атрибуты ключа COMPANY и ACC2.
  При условии, что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
  В GL_ACC имеются 3 открытых счета с одинаковым ACID = 00400038RUR104902001: 30424810500014588436
  c RLNTYPE=0 и 30424810420010000001/30424810720010000002 с RLNTYPE=4.
  Все 3 они прописаны в ACCRLN. У всех 3 счетов выставляем ссылку на сделку в GL_ACC.DEALID=123.
  Ожидается, что будет открыт новый счет, т.к. счет в GL_ACC не будет счета без привязки к сделке,
   а в ACCRLN все 3 имеющихся счета мигрированы. (Шаг 5 e.iii). А также Шаг 5-а.
  */
    @Test
    //тест создает новые счета с rltype=4 и не удаляет, что бы использовать в test5
    public void test00() throws Exception {
        long stamp = System.currentTimeMillis();

        int count = baseEntityRepository.executeNativeUpdate("update gl_acc set dealid = '123' where acid = '" + ACID + "'");
        System.out.println(count + ": update gl_acc set dealid = '123' where acid = '" + ACID + "'");
        Assert.assertTrue(count > 0);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("410.450"));
        pst1.setAmountDebit(new BigDecimal("410.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();
//        count = baseEntityRepository.executeNativeUpdate("update gl_acc set dealid = null where acid = '" + ACID + "'");
//        System.out.println(count + ": update gl_acc set dealid = null where acid = '" + ACID + "'");

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertTrue(!operation.getAccountDebit().equals(BSAACID0));
        Assert.assertNotNull(baseEntityRepository.selectFirst("select 1 from GL_ACC where bsaacid=? and ACID='" + ACID + "' and RLNTYPE='4'", operation.getAccountDebit()));
    }


    /*
    Case 1: Ключ счета дебета с GL_SEQ типа XX задан с полным набором ACOD, SQ и CUSTYPE = 9. При условии,
    что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
    Ожидается, что будет корректно найден уже открытый счет.
    (Стандартная функциональность формирования ACID и новые процедуры поиска)
    */
    @Test
    public void test01() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();
        List<DataRecord> glAcc4 = getGlAccRlnType4();
        setCloseDateGlAcc( 0, glAcc4);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("4380.450"));
        pst1.setAmountDebit(new BigDecimal("4380.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;131060102;9;;XX00000034;0001;30424;;1049;02;K+TP;;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertTrue(operation.getAccountDebit().equals(BSAACID0));
    }

    /*
    Case 2: Ключ счета дебета с GL_SEQ типа XX задан с непустым ACOD, SQ = Null и CUSTYPE = 9.
    При условии, что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
    SQ не соответствует настройкам, т.к. по GL_ACTPARM.AC_SQ = 2, а не 00, а SQ(ключ) = Null.
    Но для К+ТР работает ветка «Предупреждения», когда при SQ = Null для ACID берется настройка из
    GL_ACTPARM.AC_SQ = 2, операция считается корректной, а в аудите прописывается WARNING (Предупреждение)
     – дескать, что-то не так в операции и нужно обратить внимание.
    Ожидается, что отработает ветка «Предупреждения», которая работает ТОЛЬКО для К+ТР.
    Операция будет обработана корректно и найдется уже имеющийся счет.
    (Стандартная функциональность формирования ACID и новые процедуры поиска)
     */
    @Test
    public void test02() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();
        List<DataRecord> glAcc4 = getGlAccRlnType4();
        setCloseDateGlAcc( 0, glAcc4);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("4380.450"));
        pst1.setAmountDebit(new BigDecimal("4380.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;131060102;9;;XX00000034;0001;30424;;1049;;K+TP;;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertTrue(operation.getAccountDebit().equals(BSAACID0));
    }

    /*
    Case 3: Ключ счета дебета с GL_SEQ типа XX задан с непустым ACOD, SQ = Null и CUSTYPE = 9.
    При условии, что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
    Источник сделки AXAPTA. SQ не соответствует настройкам, т.к. по GL_ACTPARM.AC_SQ = 2, а не 00,
    а SQ(ключ) = Null (SQ(ключ) = Null допустим только для GL_ACTPARM.AC_SQ = 00,
    если источник сделки отличается от К+ТР). Ожидается, что будет диагностирована ошибка формирования
    ACID счета, т.к. SQ не соответствует настройкам и источник сделки AXAPTA, не К+ТР.
    (Стандартная функциональность формирования ACID и новые процедуры поиска)
    */
    @Test
    public void test03() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();
        List<DataRecord> glAcc4 = getGlAccRlnType4();
        setCloseDateGlAcc( 0, glAcc4);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("4380.450"));
        pst1.setAmountDebit(new BigDecimal("4380.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;131060102;9;;XX00000034;0001;30424;;1049;;AXAPTA;;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.ERCHK, operation.getState());
//        "2021"
        Assert.assertTrue(isCodeInGlAudit(operation.getId(), ErrorCode.MIDAS_PARAMS_NOT_VALID.getStrErrorCode()));
    }

    /*
    Case 4: Ключ счета дебета с GL_SEQ типа XX задан с непустым ACOD, SQ = 02 (как в настройках) и
    CUSTYPE = 21 (соответствует строке 00 в настройках). При условии, что ACCTYPE = 131060102 в GL_ACTPARM
    настроен на CUSTYPE = 9 и 00 – см. выше. Ожидается, что будет найден старый счет 30424810500014588436
    (ACID = 00400038RUR104902001), который открыт по ACID на клиента 00400038, безотносительно
    к указанию CUSTYPE(ключ) = 21. Выверка атрибутов ключа пройдет по настройке GL_ACTPARM
    с CUSTYPE=00. (Стандартная функциональность формирования ACID и новые процедуры поиска.
    Поиск ключа по ACID не чувствителен к CUSTYPE, т.к. CUSTYPE эквивалентен клиенту
    в активно-пассивных счетах; отличен от клиентов только для PL-счетов.)
     */
    @Test
    public void test04() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();
        List<DataRecord> glAcc4 = getGlAccRlnType4();
        setCloseDateGlAcc( 0, glAcc4);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("4380.450"));
        pst1.setAmountDebit(new BigDecimal("4380.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;131060102;21;;XX00000034;0001;30424;;1049;02;AXAPTA;;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertTrue(operation.getAccountDebit().equals(BSAACID0));
    }

    /*
    Case 5: Ключ счета дебета с GL_SEQ типа XX задан с пустым ACOD, SQ = 02 (как в настройках) и
    CUSTYPE = 9 (соответствует строке 9 в настройках). При условии, что ACCTYPE = 131060102
    в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше. Ожидается, что ACOD будет определен
    по настройкам GL_ACTPARM.  (Стандартная функциональность формирования ACID и новые процедуры
    поиска)
     */
    @Test
    public void test05() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();
        List<DataRecord> glAcc4 = getGlAccRlnType4();
        setCloseDateGlAcc( 0, glAcc4);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("4380.450"));
        pst1.setAmountDebit(new BigDecimal("4380.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;131060102;9;;XX00000034;0001;30424;;;02;AXAPTA;;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertTrue(operation.getAccountDebit().equals(BSAACID0));
    }

    /*
    Case 6: Ключ счета дебета с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 9
    (соответствует строке 9 в настройках). При условии, что ACCTYPE = 131060102 в GL_ACTPARM
    настроен на CUSTYPE = 9 и 00 – см. выше. Ожидается, что будет найден
    старый счет 30424810500014588436 (ACID = 00400038RUR104902001), ACOD и SQ
    будут определены из настроек GL_ACTPARM. (Стандартная функциональность формирования ACID и
    новые процедуры поиска)
     */
    @Test
    public void test06() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();
        List<DataRecord> glAcc4 = getGlAccRlnType4();
        setCloseDateGlAcc( 0, glAcc4);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("4380.450"));
        pst1.setAmountDebit(new BigDecimal("4380.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;131060102;9;;XX00000034;0001;30424;;;;AXAPTA;;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertTrue(operation.getAccountDebit().equals(BSAACID0));
    }

    /*
    Case 7: Ключ счета дебета с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 9
    (соответствует строке 9 в настройках), также пустыми оставлены атрибуты ключа COMPANY и ACC2.
    При условии, что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
    Ожидается, что будет найден старый счет 30424810500014588436 (ACID = 00400038RUR104902001),
    ACOD, SQ, COMPANY и ACC2 будут определены из настроек GL_ACTPARM.
    (Стандартная функциональность формирования ACID и новые процедуры поиска)
     */
    @Test
    public void test07() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();
        List<DataRecord> glAcc4 = getGlAccRlnType4();
        setCloseDateGlAcc( 0, glAcc4);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("4380.450"));
        pst1.setAmountDebit(new BigDecimal("4380.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertTrue(operation.getAccountDebit().equals(BSAACID0));
    }

    /*
    Case 8: Ключ счета дебета с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 9
    (соответствует строке 9 в настройках), также пустыми оставлены атрибуты ключа COMPANY и ACC2.
    При условии, что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
    В счете GL_ACC (BSAACID = 30424810500014588436) в поле DEALID установлена ‘’ (пустая строка)
    вместо Null.
    Ожидается, что будет найден старый счет 30424810500014588436 (ACID = 00400038RUR104902001),
    ACOD, SQ, COMPANY и ACC2 будут определены из настроек GL_ACTPARM, поле DEALID будет
    интерпретировано как пустое (Шаг 5 а) поиска счета)
    */
    @Test
    public void test08() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();
        List<DataRecord> glAcc4 = getGlAccRlnType4();
        setCloseDateGlAcc( 0, glAcc4);

        int count = baseEntityRepository.executeNativeUpdate("update gl_acc set dealid = '' where bsaacid = '" + BSAACID0 + "'");
        System.out.println(count + ": update gl_acc set dealid = '' where bsaacid = '" + BSAACID0 + "'");
        Assert.assertTrue(count == 1);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("4380.450"));
        pst1.setAmountDebit(new BigDecimal("4380.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertTrue(operation.getAccountDebit().equals(BSAACID0));
    }

    /*
    Case 9: Ключ счета дебета с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 9
    (соответствует строке 9 в настройках), также пустыми оставлены атрибуты ключа COMPANY и ACC2.
    При условии, что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
    В счете GL_ACC (BSAACID = 30424810500014588436) в поле DEALID установлено ‘нет’ (кириллицей)
    вместо Null.
    Ожидается, что будет найден старый счет 30424810500014588436 (ACID = 00400038RUR104902001),
    ACOD, SQ, COMPANY и ACC2 будут определены из настроек GL_ACTPARM, поле DEALID будет
    интерпретировано как пустое. (Шаг 5 а) поиска счета)
    */
    @Test
    public void test09() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();
        List<DataRecord> glAcc4 = getGlAccRlnType4();
        setCloseDateGlAcc( 0, glAcc4);

        int count = baseEntityRepository.executeNativeUpdate("update gl_acc set dealid = 'нет' where bsaacid = '" + BSAACID0 + "'");
        System.out.println(count + ": update gl_acc set dealid = 'нет' where bsaacid = '" + BSAACID0 + "'");
        Assert.assertTrue(count == 1);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("4380.450"));
        pst1.setAmountDebit(new BigDecimal("4380.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertTrue(operation.getAccountDebit().equals(BSAACID0));
    }

    /*
     Case 10: Ключ счета дебета с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 9
     (соответствует строке 9 в настройках), также пустыми оставлены атрибуты ключа COMPANY и ACC2.
     При условии, что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
     В счете GL_ACC (BSAACID = 30424810500014588436) в поле DEALID установлено ‘net’ (латиницей)
     вместо Null.
     Ожидается, что НЕ будет найден старый счет 30424810500014588436 (ACID = 00400038RUR104902001),
     т.к. поле DEALID будет интерпретировано как Непустое. Будет открыт НОВЫЙ счет с RLNTYPE = 4.
     (Шаг 5 а), затем Шаг 5 d) поиска счета, т.е. Шаг 5 e.iii)
    */
    @Test
    public void test10() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();
        List<DataRecord> glAcc4 = getGlAccRlnType4();
        setCloseDateGlAcc( 0, glAcc4);

        int count = baseEntityRepository.executeNativeUpdate("update gl_acc set dealid = 'net' where bsaacid = '" + BSAACID0 + "'");
        System.out.println(count + ": update gl_acc set dealid = 'net' where bsaacid = '" + BSAACID0 + "'");
        Assert.assertTrue(count == 1);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("4.450"));
        pst1.setAmountDebit(new BigDecimal("4.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertTrue(!operation.getAccountDebit().equals(BSAACID0));
        Assert.assertNotNull(baseEntityRepository.selectFirst("select 1 from GL_ACC where bsaacid=? and ACID='" + ACID + "' and RLNTYPE='4' and GL_SEQ is not null", operation.getAccountDebit()));
        delAccount(ACID, operation.getAccountDebit());
    }

    /*
     Case 11: Ключ счета дебета с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 9
     (соответствует строке 9 в настройках), также пустыми оставлены атрибуты ключа COMPANY и ACC2.
     При условии, что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
     В ключе имеется непустая ссылка на сделку DEALID = 1234 и подсделку SUBDEALID = 12
     Ожидается, что ключ не будет обработан, будет диагностирована ошибка некорректного задания
     ключа. (Шаг 1 а) поиска счета)
    */
    @Test
    public void test11() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("40.450"));
        pst1.setAmountDebit(new BigDecimal("40.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;1234;12");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.ERCHK, operation.getState());
        //"2045"
        Assert.assertTrue(isCodeInGlAudit(operation.getId(), ErrorCode.GL_SEQ_XX_KEY_WITH_DEAL.getStrErrorCode()));
    }

    /*
     Case 12: Ключ счета дебета с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 9
     (соответствует строке 9 в настройках), также пустыми оставлены атрибуты ключа COMPANY и ACC2.
     При условии, что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
     В ключе имеется непустая ссылка на подсделку SUBDEALID = 12
     Ожидается, что ключ не будет обработан, будет диагностирована ошибка некорректного задания
     ключа. (Шаг 1 а) поиска счета)
    */
    @Test
    public void test12() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("410.450"));
        pst1.setAmountDebit(new BigDecimal("410.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;12");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.ERCHK, operation.getState());
        //"2046"
        Assert.assertTrue(isCodeInGlAudit(operation.getId(), ErrorCode.GL_SEQ_XX_KEY_WITH_SUBDEAL.getStrErrorCode()));
    }

    /*
    Case 14: Ключ счета дебета с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 12,
    TERM = 05, также пустыми оставлены атрибуты ключа COMPANY и ACC2. При условии,
    что ACCTYPE = 712010100 в GL_ACTPARM настроен на PLCODE = 31318 – см. выше.
    В ключе ссылка на символ доходов-расходов пуста, поэтому по набору атрибутов ключ
    представлен корректно, но используемый ACCTYPE соответствует PL-счету по настройкам GL_ACTPARM.
    Ожидается, что ключ не будет обработан, будет диагностирована ошибка некорректного
    задания ключа относительно настроек GL_ACTPARM. (Шаг 1 в) поиска счета)
    */
    @Test
    public void test14() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("410.450"));
        pst1.setAmountDebit(new BigDecimal("410.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;712010100;21;05;XX00000034;;;;;;AXAPTA;;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.ERCHK, operation.getState());
        //"2050"
        Assert.assertTrue(isCodeInGlAudit(operation.getId(), ErrorCode.GL_SEQ_XX_KEY_WITH_DB_PLCODE.getStrErrorCode()));
    }


    /*
    Case 15: Ключ счета дебета с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 9
    (соответствует строке 9 в настройках), также пустыми оставлены атрибуты ключа COMPANY и ACC2.
    При условии, что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
    В GL_ACC имеются 2 счета с одинаковым ACID = 00400038RUR104902001: 30424810500014588436
    c RLNTYPE=0 и 30424810420010000001 с RLNTYPE=4. Оба они прописаны в ACCRLN.
    Закрываем оба этих счета в GL_ACC, выставляя признак GL_ACC.DTC = 2016-12-29,
    с учетом GL_OD = 2017-01-10. Одновременно оставляем оба этих счета открытыми в ACCRLN.
    Тем самым моделируем ситуацию, когда в ACCRLN существуют 2 «немигрированных» счета – один
    с RLNTYPE = 0, другой – с RLNTYPE = 4.
    Ожидается, что счета в GL_ACC счета не будут найдены, а в ACCRLN будет найден счет
    30424810500014588436 c RLNTYPE=0. (Шаг 5 e.i).
    */
    @Test
    //если нет счетов с RLNTYPE='4', то запустить test00
    public void test15() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();

        int count = baseEntityRepository.executeNativeUpdate("update gl_acc set dtc = TO_DATE('2015-12-29','RRRR-MM-DD') where acid = '" + ACID + "'");
        System.out.println(count + ": update gl_acc set dtc = TO_DATE('2015-12-29','RRRR-MM-DD') where acid = '" + ACID + "'");
        Assert.assertTrue(count > 0);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("410.450"));
        pst1.setAmountDebit(new BigDecimal("410.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertTrue(operation.getAccountDebit().equals(BSAACID0));
    }

    /*
    Case 16: Ключ счета дебета с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 9
    (соответствует строке 9 в настройках), также пустыми оставлены атрибуты ключа COMPANY и ACC2.
    При условии, что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
    В GL_ACC имеются 3 счета с одинаковым ACID = 00400038RUR104902001: 30424810500014588436 c
    RLNTYPE=0 и 30424810420010000001/30424810720010000002 с RLNTYPE=4. Все 3 они прописаны в ACCRLN.
    Закрываем оба этих счета в GL_ACC, выставляя признак GL_ACC.DTC = 2016-12-29,
    с учетом GL_OD = 2017-01-10. Одновременно оставляем все 3 этих счета открытыми в ACCRLN,
    при этом у счета 30424810420010000001 выставляем ACCRLN.RLNTYPE =’0’.
    Тем самым моделируем ситуацию, когда в ACCRLN существуют 2 «немигрированных» счета с RLNTYPE = 0,
     и один – с RLNTYPE = 4.
    Ожидается, что будет диагностирована ошибка, т.к. счет в GL_ACC счета не будут найден,
    а в ACCRLN будет найдено 2 счета с одинаковым ACID и RLNTYPE = 0. (Шаг 5 e.ii).
    */
    @Test
    //если нет счетов с RLNTYPE='4', то запустить test00
    public void test16() throws Exception {
        long stamp = System.currentTimeMillis();

        DataRecord accrln_rltype4 = baseEntityRepository.selectFirst("select * from accrln where acid='" + ACID + "' and rlntype='4'");
        Assert.assertNotNull(accrln_rltype4);
        int count = baseEntityRepository.executeNativeUpdate("update accrln set rlntype = '0' where acid = '" + ACID + "' and bsaacid=?", accrln_rltype4.getString("bsaacid"));
        System.out.println(count + ": accrln set rlntype = '0'");
        Assert.assertTrue(count == 1);

        count = baseEntityRepository.executeNativeUpdate("update gl_acc set dtc = TO_DATE('2015-12-29','RRRR-MM-DD') where acid = '" + ACID + "'");
        System.out.println(count + ": update gl_acc set dtc = TO_DATE('2015-12-29','RRRR-MM-DD') where acid = '" + ACID + "'");
        Assert.assertTrue(count > 0);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("410.450"));
        pst1.setAmountDebit(new BigDecimal("410.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();

        count = baseEntityRepository.executeNativeUpdate("update accrln set rlntype = '4' where acid = '" + ACID + "' and bsaacid=?",accrln_rltype4.getString("bsaacid"));
        System.out.println(count + ": update accrln set rlntype = '4'");
        Assert.assertTrue(count == 1);

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.ERCHK, operation.getState());
        //"2049"
        Assert.assertTrue(isCodeInGlAudit(operation.getId(), ErrorCode.GL_SEQ_XX_ACCRLN_NOT_FOUND.getStrErrorCode()));
    }

    /*
    Case 18: Ключ счета дебета с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 9
    (соответствует строке 9 в настройках), также пустыми оставлены атрибуты ключа COMPANY и ACC2.
    При условии, что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
    В GL_ACC имеются 3 открытых счета с одинаковым ACID = 00400038RUR104902001:
    30424810500014588436 c RLNTYPE=0 и 30424810420010000001/30424810720010000002 с RLNTYPE=4.
    Все 3 они прописаны в ACCRLN. У всех 3 счетов выставляем ссылку на сделку в GL_ACC.DEALID=123.
    Закрываем счета в GL_ACC 30424810500014588436 c RLNTYPE=0 и 30424810420010000001 с RLNTYPE=4.
    Закрываем все 3 счета в ACCRLN, таким образом, мы не сможем их выбрать по ветке 5-e.
    Ожидается, что будет открыт новый счет, т.к. счет в GL_ACC не будет счета без привязки к сделке,
    а в ACCRLN все 3 имеющихся счета закрыты. (Шаг 5-а). Не будет найден единственный открытый счет
    в GL_ACC 30424810720010000002 с RLNTYPE=4, т.к. у него DEALID непусто.
    */
    @Test
    //если нет или мало счетов с RLNTYPE='4', то запустить test00
    public void test18() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();

        List<DataRecord> glAcc = baseEntityRepository.select("select * from gl_acc where acid = '" + ACID + "' and rlntype='4'");
        Assert.assertTrue("мало записей, выполнить test00", glAcc.size() > 1);

        DataRecord glAcc4 = baseEntityRepository.selectFirst("select * from gl_acc where acid='" + ACID + "' and rlntype='4'");
        int count = baseEntityRepository.executeNativeUpdate("update gl_acc set dtc = TO_DATE('2015-12-29','RRRR-MM-DD') where acid = '" + ACID + "' and bsaacid!=?", glAcc4.getString("bsaacid"));
        System.out.println(count + ": update gl_acc set dtc = TO_DATE('2015-12-29','RRRR-MM-DD')");
        Assert.assertTrue(count > 0);

        count = baseEntityRepository.executeNativeUpdate("update accrln set drlnc = TO_DATE('2015-12-29','RRRR-MM-DD') where acid = '" + ACID + "'");
        System.out.println(count + ": update accrln set drlnc = TO_DATE('2015-12-29','RRRR-MM-DD') where acid = '" + ACID + "'");
        Assert.assertTrue(count > 0);

        count = baseEntityRepository.executeNativeUpdate("update gl_acc set dealid = '123' where acid = '" + ACID + "'");
        System.out.println(count + ": update gl_acc set dealid = '123' where acid = '" + ACID + "'");
        Assert.assertTrue(count > 0);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("410.450"));
        pst1.setAmountDebit(new BigDecimal("410.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();

        count = baseEntityRepository.executeNativeUpdate("update accrln set drlnc=TO_DATE('2029-01-01','RRRR-MM-DD') where acid = '" + ACID + "'");
        System.out.println(count + ": update accrln set drlnc=TO_DATE('2029-01-01','RRRR-MM-DD') where acid = '" + ACID + "'");

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        for (DataRecord rec : glAcc) {
            Assert.assertFalse("счет должен быть новым", operation.getAccountDebit().equals(rec.getString("bsaacid")));
        }
    }

    /*
    Case 19: Ключ счета кредита с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 9
    (соответствует строке 9 в настройках), также пустыми оставлены атрибуты ключа COMPANY и ACC2.
    При условии, что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
    По дебету указан ключ ‘001;RUR;00400038;351020301;9;;0001042928;0001;47408;;;;K+TP;917244;’
    В GL_ACC имеются 6 счетов с одинаковым ACID = 00400038RUR104902001: 30424810500014588436 c RLNTYPE=0 и
    30424810420010000001/30424810720010000002/30424810020010000003/ 30424810320010000004/30424810620010000005 с RLNTYPE=4.
    Все они прописаны в ACCRLN. Счета 436/1/2 – c GL_ACC.DEALID=123. Счета 3 и 4 закрыты.
    Открыт только счет 30424810620010000005 с RLNTYPE=4.
    Ожидается, что будет найден счет 30424810620010000005 с RLNTYPE=4, т.к. у него DEALID пусто. (Шаг 5-b).
     */
    @Test
    //если нет или мало счетов с RLNTYPE='4', то запустить test00
    public void test19() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();

        List<DataRecord> glAcc = getGlAccRlnType4();

        int count = baseEntityRepository.executeNativeUpdate("update gl_acc set dealid = '123' where acid = '"+ACID+"' and bsaacid=?", glAcc.get(1).getString("bsaacid"));
        System.out.println(count + ": update gl_acc set dealid = '123'");
        Assert.assertTrue(count == 1);

        count = baseEntityRepository.executeNativeUpdate("update gl_acc set dealid = '123' where acid = '"+ACID+"' and rlntype='0'");
        System.out.println(count + ": update gl_acc set dealid = '123'");
        Assert.assertTrue(count > 0);

        setCloseDateGlAcc( 2, glAcc);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("410.450"));
        pst1.setAmountDebit(new BigDecimal("410.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;351020301;9;;0001042928;0001;47408;;;;K+TP;917244;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertTrue("должен быть счет из glAcc.get(0)", operation.getAccountCredit().equals(glAcc.get(0).getString("bsaacid")));
    }

    /*
    Case 20: Ключ счета кредита с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 9
    (соответствует строке 9 в настройках), также пустыми оставлены атрибуты ключа COMPANY и ACC2.
    При условии, что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
    По дебету указан ключ ‘001;RUR;00400038;351020301;9;;0001042928;0001;47408;;;;K+TP;917244;’
    В GL_ACC имеются 6 счетов с одинаковым ACID = 00400038RUR104902001: 30424810500014588436 c
    RLNTYPE=0 и 30424810420010000001/30424810720010000002/30424810020010000003/ 30424810320010000004/30424810620010000005 с RLNTYPE=4.
    Все они прописаны в ACCRLN. Счета 1/2 – c GL_ACC.DEALID=123. Счета 3 и 4 закрыты.
    Открыты только счета 30424810620010000005 с RLNTYPE=4 и 30424810500014588436 c RLNTYPE=0.
    Ожидается, что не будет найден ни один счет в GL_ACC, т.к. их 2 равнозначных по ACID и
    с DEALID/SUBDEALID – пусто в счете 30424810620010000005 и ‘нет’ в счете 30424810500014588436.
    Будет диагностирована ошибка неоднозначного определения счета. (Шаг 5-c.ii).
     */
    @Test
    //если нет или мало счетов с RLNTYPE='4', то запустить test00
    public void test20() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();

        List<DataRecord> glAcc = getGlAccRlnType4();

        DataRecord glAcc0 = baseEntityRepository.selectFirst("select * from gl_acc where acid = '"+ACID+"' and rlntype='0'");

        int count = baseEntityRepository.executeNativeUpdate("update gl_acc set acctype = ? where acid = '"+ACID+"' and bsaacid=?", glAcc.get(0).getString("acctype"), glAcc0.getString("bsaacid"));
        System.out.println(count + ": update gl_acc set acctype = " + glAcc.get(0).getString("acctype") + " and bsaacid = " + glAcc0.getString("bsaacid"));
        Assert.assertTrue(count == 1);

        count = baseEntityRepository.executeNativeUpdate("update gl_acc set dealid = '123' where acid = '"+ACID+"' and bsaacid=?", glAcc.get(1).getString("bsaacid"));
        System.out.println(count + ": update gl_acc set dealid = '123'and bsaacid = " + glAcc.get(1).getString("bsaacid"));
        Assert.assertTrue(count == 1);

        count = baseEntityRepository.executeNativeUpdate("update gl_acc set dealid = 'нет' where acid = '"+ACID+"' and rlntype='0'");
        System.out.println(count + ": update gl_acc set dealid = 'нет' where rlntype='0'");
        Assert.assertTrue(count > 0);

        setCloseDateGlAcc( 2, glAcc);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("410.450"));
        pst1.setAmountDebit(new BigDecimal("410.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;351020301;9;;0001042928;0001;47408;;;;K+TP;917244;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        System.out.println("=================");

        glAccBeginState();

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.ERCHK, operation.getState());
        //"2048"
        Assert.assertTrue(isCodeInGlAudit(operation.getId(), ErrorCode.GL_SEQ_XX_GL_ACC_NOT_FOUND.getStrErrorCode()));
    }

    /*
    Case 21: Ключ счета кредита с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 9 (соответствует строке 9 в
    настройках), также пустыми оставлены атрибуты ключа COMPANY и ACC2. При условии, что ACCTYPE = 131060102
    в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
    По дебету указан ключ ‘001;RUR;00400038;351020301;9;;0001042928;0001;47408;;;;K+TP;917244;’
    В GL_ACC имеются 6 счетов с одинаковым ACID = 00400038RUR104902001: 30424810500014588436 c RLNTYPE=0 и
    30424810420010000001/30424810720010000002/30424810020010000003/ 30424810320010000004/30424810620010000005 с RLNTYPE=4.
    Все они прописаны в ACCRLN. Счета 1/2 – c GL_ACC.DEALID=123. Счета 3 и 4 закрыты.
    Открыты только счета 30424810620010000005 с RLNTYPE=4 и 30424810500014588436 c RLNTYPE=0.
    При этом у счета 30424810500014588436 c RLNTYPE=0 ACCTYPE установлен на 131060101, что отличается от 131060102,
    используемом в ключе рассчитываемого счета. В остальном открытые счета равнозначны по ACID и
    с DEALID/SUBDEALID – пусто в счете 30424810620010000005 и ‘нет’ в счете 30424810500014588436
    Ожидается, что будет найден счет 30424810620010000005, т.к. у него единственного из открытых ACCTYPE совпадает с
    ACCTYPE ключа счета. (Шаг 5-c.i).
    */
    @Test
    //если нет или мало счетов с RLNTYPE='4', то запустить test00
    public void test21() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();

        List<DataRecord> glAcc4 = getGlAccRlnType4();

        int count = baseEntityRepository.executeNativeUpdate("update gl_acc set dealid = '123' where acid = '"+ACID+"' and bsaacid=?", glAcc4.get(1).getString("bsaacid"));
        System.out.println(count + ": update gl_acc set dealid = '123' and bsaacid = " + glAcc4.get(1).getString("bsaacid"));
        Assert.assertTrue(count == 1);

        count = baseEntityRepository.executeNativeUpdate("update gl_acc set dealid = 'нет' where acid = '"+ACID+"' and rlntype='0'");
        System.out.println(count + ": update gl_acc set dealid = 'нет' where rlntype='0'");
        Assert.assertTrue(count == 1);

        setCloseDateGlAcc(2, glAcc4);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("410.450"));
        pst1.setAmountDebit(new BigDecimal("410.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;351020301;9;;0001042928;0001;47408;;;;K+TP;917244;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertTrue("должен быть счет из glAcc.get(0)", operation.getAccountCredit().equals(glAcc4.get(0).getString("bsaacid")));
    }

    /*
    Case 22: Ключ счета кредита с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 9
    (соответствует строке 9 в настройках), также пустыми оставлены атрибуты ключа COMPANY и ACC2.
    При условии, что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
    По дебету указан ключ ‘001;RUR;00400038;351020301;9;;0001042928;0001;47408;;;;K+TP;917244;’
    В GL_ACC имеются 6 счетов с одинаковым ACID = 00400038RUR104902001: 30424810500014588436 c RLNTYPE=0 и
    30424810420010000001/30424810720010000002/30424810020010000003/ 30424810320010000004/30424810620010000005 с RLNTYPE=4.
    Все они прописаны в ACCRLN. Счета 1/2 – c GL_ACC.DEALID=123. Счета 3 и 4 закрыты.
    Открыты только счета 30424810620010000005 с RLNTYPE=4 и 30424810500014588436 c RLNTYPE=0,
    при этом у обоих открытых счетов ACCTYPE установлен на 131060101, что отличается от 131060102,
    используемом в ключе рассчитываемого счета. В остальном открытые счета равнозначны по ACID и
    с DEALID/SUBDEALID – пусто в счете 30424810620010000005 и ‘нет’ в счете 30424810500014588436.
    Ожидается, что ни один из открытых счетов не будет найден, т.к. у них ACCTYPE не совпадает с ACCTYPE ключа счета.
    Поэтому будет открыт новый счет (Шаг 5-c.iii).
     */
    @Test
    //если нет или мало счетов с RLNTYPE='4', то запустить test00
    public void test22() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();

        List<DataRecord> glAcc4 = getGlAccRlnType4();
        setGlAccDealId123(glAcc4);
        setGlAcc0DealId();

        setCloseDateGlAcc(2, glAcc4);

        setGlAcc4AccType101(glAcc4);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("410.450"));
        pst1.setAmountDebit(new BigDecimal("410.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;351020301;9;;0001042928;0001;47408;;;;K+TP;917244;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        newAccountOpen(glAcc4, operation.getAccountCredit());
    }

    /*
    Case 23: Ключ счета кредита с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 9
    (соответствует строке 9 в настройках), также пустыми оставлены атрибуты ключа COMPANY и ACC2.
    При условии, что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
    По дебету указан ключ ‘001;RUR;00400038;351020301;9;;0001042928;0001;47408;;;;K+TP;917244;’
    В GL_ACC имеются 7 счетов с одинаковым ACID = 00400038RUR104902001: 30424810500014588436 c RLNTYPE=0 и
    30424810420010000001/30424810720010000002/30424810020010000003/ 30424810320010000004/30424810620010000005
    с RLNTYPE=4. Все они прописаны в ACCRLN. Счета 1/2 – c GL_ACC.DEALID=123. Счета 3 и 4 и 6 закрыты.
    Открыты только счета 30424810620010000005 с RLNTYPE=4 и 30424810500014588436 c RLNTYPE=0,
    при этом у обоих открытых счетов ACCTYPE установлен на 131060101, что отличается от 131060102,
    используемом в ключе рассчитываемого счета. В остальном открытые счета равнозначны по ACID и
    с DEALID/SUBDEALID – пусто в счете 30424810620010000005 и ‘нет’ в счете 30424810500014588436.
    В GL_ACTNAME.ACCTYPE = 131060102 установлен признак контролируемости счета FL_CTRL = Y.
    Ожидается, что ни один из открытых счетов не будет найден, т.к. у них ACCTYPE не совпадает с ACCTYPE ключа счета.
    Поэтому будет произведена попытка открыть новый счет. Но т.к. для ACCTYPE ключа счета кредита установлен
    на контролируемый счет, тот новый счет не будет открыт, и будет диагностирована ошибка (Шаг 5-c.iii,
    Шаг 1-a открытия счета).
     */
    @Test
    //если нет или мало счетов с RLNTYPE='4', то запустить test00
    public void test23() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();

        List<DataRecord> glAcc4 = getGlAccRlnType4();
        setGlAccDealId123(glAcc4);
        setCloseDateGlAcc(2, glAcc4);
        setGlAcc0DealId();
        setGlAcc4AccType101(glAcc4);

        int count = baseEntityRepository.executeNativeUpdate("update GL_ACTNAME set FL_CTRL = 'Y' where ACCTYPE = '131060102'");
        System.out.println(count +": update GL_ACTNAME set FL_CTRL = 'Y'");
        Assert.assertTrue(count == 1);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("410.450"));
        pst1.setAmountDebit(new BigDecimal("410.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;351020301;9;;0001042928;0001;47408;;;;K+TP;917244;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.ERCHK, operation.getState());
        //"2052"
        Assert.assertTrue(isCodeInGlAudit(operation.getId(), ErrorCode.GL_SEQ_XX_KEY_WITH_FL_CTRL.getStrErrorCode()));
    }
    /*
    Case 24: Ключ счета кредита с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 9
    соответствует строке 9 в настройках), также пустыми оставлены атрибуты ключа COMPANY и ACC2.
    При условии, что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
    Но настройка на CUSTYPE=9 закрыта (DTE = ‘2016-12-29’), поэтому рабочей настройкой осталась только строка с CUSTYPE = 00.
    По дебету указан ключ ‘001;RUR;00400038;351020301;9;;0001042928;0001;47408;;;;K+TP;917244;’
    В GL_ACC имеются 7 счетов с одинаковым ACID = 00400038RUR104902001: 30424810500014588436 c RLNTYPE=0 и
    30424810420010000001/30424810720010000002/30424810020010000003/ 30424810320010000004/30424810620010000005
    с RLNTYPE=4. Все они прописаны в ACCRLN. Счета 1/2 – c GL_ACC.DEALID=123. Счета 3 и 4 и 6 закрыты.
    Открыты только счета 30424810620010000005 с RLNTYPE=4 и 30424810500014588436 c RLNTYPE=0,
    при этом у обоих открытых счетов ACCTYPE установлен на 131060101, что отличается от 131060102,
    используемом в ключе рассчитываемого счета. В остальном открытые счета равнозначны по ACID и
    с DEALID/SUBDEALID – пусто в счете 30424810620010000005 и ‘нет’ в счете 30424810500014588436.
    В GL_ACTNAME.ACCTYPE = 131060102 установлен признак неконтролируемости счета FL_CTRL = N.
    Ожидается, что будет открыт новый счет на основании параметризации GL_ACTPARM по единственной активной строке.
     */
    @Test
    //если нет или мало счетов с RLNTYPE='4', то запустить test00
    public void test24() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();

        List<DataRecord> glAcc4 = getGlAccRlnType4();
        setGlAccDealId123(glAcc4);
        setCloseDateGlAcc(2, glAcc4);
        setGlAcc0DealId();
        setGlAcc4AccType101(glAcc4);
        int count = baseEntityRepository.executeNativeUpdate("update GL_ACTPARM set dte=TO_DATE('2015-01-01','RRRR-MM-DD') where ACCTYPE = '131060102' and custype='9'");
        System.out.println(count +": update GL_ACTPARM set dte=TO_DATE('2015-01-01','RRRR-MM-DD') where ACCTYPE = '131060102' and custype='9'");
        Assert.assertTrue(count == 1);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("410.450"));
        pst1.setAmountDebit(new BigDecimal("410.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;351020301;9;;0001042928;0001;47408;;;;K+TP;917244;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        newAccountOpen(glAcc4, operation.getAccountCredit());
    }
    /*
    Case 25: Ключ счета кредита с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 9
    (соответствует строке 9 в настройках), также пустыми оставлены атрибуты ключа COMPANY и ACC2.
    При условии, что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
    Но настройка на CUSTYPE=9 закрыта (DTE = ‘2016-12-29’), поэтому рабочей настройкой осталась только строка с CUSTYPE = 00.
    Плюс изменена настройка на SQ в активной строке GL_ACTPARM.
    По дебету указан ключ ‘001;RUR;00400038;351020301;9;;0001042928;0001;47408;;;;K+TP;917244;’
    В GL_ACC имеются 7 счетов с одинаковым ACID = 00400038RUR104902001: 30424810500014588436 c RLNTYPE=0 и
    30424810420010000001/30424810720010000002/30424810020010000003/ 30424810320010000004/30424810620010000005 с RLNTYPE=4.
    Все они прописаны в ACCRLN. Счета 1/2 – c GL_ACC.DEALID=123. Счета 3 и 4 и 6 закрыты.
    Открыты только счета 30424810620010000005 с RLNTYPE=4 и 30424810500014588436 c RLNTYPE=0,
    при этом у обоих открытых счетов ACCTYPE установлен на 131060101, что отличается от 131060102,
    используемом в ключе рассчитываемого счета. В остальном открытые счета равнозначны по ACID и
    с DEALID/SUBDEALID – пусто в счете 30424810620010000005 и ‘нет’ в счете 30424810500014588436.
    В GL_ACTNAME.ACCTYPE = 131060102 установлен признак неконтролируемости счета FL_CTRL = N.
    Ожидается, что не будет открыт новый счет на основании параметризации GL_ACTPARM по единственной активной строке,
    т.к. обогащенный ключ будет содержать SQ=00, а такое значение не допускает открытие нового счета.
    Будет диагностирована ошибка по Шагу 1-б открытия счета.
     */
    @Test
    //если нет или мало счетов с RLNTYPE='4', то запустить test00
    public void test25() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();
        List<DataRecord> glAcc4 = getGlAccRlnType4();
        setGlAccDealId123(glAcc4);
        setCloseDateGlAcc(2, glAcc4);
        setGlAcc0DealId();
        setGlAcc4AccType101(glAcc4);
        int count = baseEntityRepository.executeNativeUpdate("update GL_ACTPARM set dte=TO_DATE('2015-01-01','RRRR-MM-DD') where ACCTYPE = '131060102' and custype='9'");
        System.out.println(count +": update GL_ACTPARM set dte=TO_DATE('2015-01-01','RRRR-MM-DD') where ACCTYPE = '131060102' and custype='9'");
        Assert.assertTrue(count == 1);

        count = baseEntityRepository.executeNativeUpdate("update gl_actparm set ac_sq = '00' where acctype = '131060102' and custype = '00'");
        System.out.println(count +": update gl_actparm set ac_sq = '00' where acctype = '131060102' and custype = '00'");
        Assert.assertTrue(count == 1);


        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("410.450"));
        pst1.setAmountDebit(new BigDecimal("410.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;351020301;9;;0001042928;0001;47408;;;;K+TP;917244;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        glAccBeginState();

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.ERCHK, operation.getState());
        //2051
        Assert.assertTrue(isCodeInGlAudit(operation.getId(), ErrorCode.GL_SEQ_XX_KEY_WITH_SQ_0.getStrErrorCode()));
    }

    /*
  Case 13: Ключ счета дебета с GL_SEQ типа XX задан с пустым ACOD, пустым SQ и CUSTYPE = 9
  (соответствует строке 9 в настройках), также пустыми оставлены атрибуты ключа COMPANY и ACC2.
  При условии, что ACCTYPE = 131060102 в GL_ACTPARM настроен на CUSTYPE = 9 и 00 – см. выше.
  В ключе имеется непустая ссылка на символ доходов-расходов PL = 31318.
  Ожидается, что ключ не будет обработан, будет диагностирована ошибка некорректного задания ключа.
   (Шаг 1 б) поиска счета)
  */
    @Test
    public void test13() throws Exception {
        long stamp = System.currentTimeMillis();

        glAccBeginState();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("410.450"));
        pst1.setAmountDebit(new BigDecimal("410.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;131060102;9;;XX00000034;;;31318;;;AXAPTA;;");
        pst1.setAccountKeyCredit("001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.ERCHK, operation.getState());
        //"2047"
        Assert.assertTrue(isCodeInGlAudit(operation.getId(), ErrorCode.GL_SEQ_XX_KEY_WITH_PLCODE.getStrErrorCode()));
    }


    @Test
    public void test26_SameAccount22() throws Exception {
        long stamp = System.currentTimeMillis();
        String crKeys = "001;RUR;00400038;131060102;9;;XX00000034;;;;;;AXAPTA;;";

        glAccBeginState();

        List<DataRecord> glAcc4 = getGlAccRlnType4();
        setGlAccDealId123(glAcc4);
        setGlAcc0DealId();
        setCloseDateGlAcc(2, glAcc4);
        setGlAcc4AccType101(glAcc4);

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("410.450"));
        pst1.setAmountDebit(new BigDecimal("410.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;351020301;9;;0001042928;0001;47408;;;;K+TP;917244;");
        pst1.setAccountKeyCredit(crKeys);
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        newAccountOpen(glAcc4, operation.getAccountCredit());
        String createdAcc = operation.getAccountCredit();
// повторение
        pkg = newPackage(System.currentTimeMillis(), "SIMPLE2");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(2);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("21084014");
        pst1.setEventId("ГК07248804_000000001");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("Narrative");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("410.450"));
        pst1.setAmountDebit(new BigDecimal("410.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("001;RUR;00400038;351020301;9;;0001042928;0001;47408;;;;K+TP;917244;");
        pst1.setAccountKeyCredit(crKeys);
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        operation = (GLOperation) postingController.processMessage(pst1);
        glAccBeginState();

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertEquals(createdAcc, operation.getAccountCredit());

    }
        //===============================================================
    private void setGlAcc4AccType101(List<DataRecord> glAcc4) {
        int count = baseEntityRepository.executeNativeUpdate("update gl_acc set acctype = '131060101' where acid = '"+ACID+"' and bsaacid=?", glAcc4.get(0).getString("bsaacid"));
        System.out.println(count +": update gl_acc set acctype = '131060101' and bsaacid = "+glAcc4.get(0).getString("bsaacid"));
        Assert.assertTrue(count == 1);
    }

    private void setGlAcc0DealId() {
        int count = baseEntityRepository.executeNativeUpdate("update gl_acc set dealid = 'нет' where acid = '"+ACID+"' and rlntype='0'");
        System.out.println(count +": update gl_acc set subdealid = 'нет' where rlntype='0'");
        Assert.assertTrue(count ==1);
    }

    private void setGlAccDealId123(List<DataRecord> glAcc4) {
        int count = baseEntityRepository.executeNativeUpdate("update gl_acc set dealid = '123' where acid = '"+ACID+"' and bsaacid=?", glAcc4.get(1).getString("bsaacid"));
        System.out.println(count + ": update gl_acc set dealid = '123' and bsaacid = " + glAcc4.get(1).getString("bsaacid"));
        Assert.assertTrue(count == 1);
    }

    private void newAccountOpen(List<DataRecord> glAcc, String bsaacid){
        for(DataRecord rec: glAcc){
             Assert.assertFalse("счет должен быть новым", bsaacid.equals(rec.getString("bsaacid")));
        }
        Assert.assertFalse(bsaacid.equals(BSAACID0));
    }

    private void setCloseDateGlAcc(int from, List<DataRecord> glAcc){
        for(int i = from; i < glAcc.size(); i++ ) {
            int count = baseEntityRepository.executeNativeUpdate("update gl_acc set dtc = TO_DATE('2015-12-29','RRRR-MM-DD') where acid = '"+ACID+"' and bsaacid=?", glAcc.get(i).getString("bsaacid"));
            System.out.println(count + ": update gl_acc set dtc = TO_DATE('2015-12-29','RRRR-MM-DD') where bsaacid = " + glAcc.get(i).getString("bsaacid"));
            Assert.assertTrue(count == 1);
        }
    }
    private void setCloseDateGlAcc(int from, List<DataRecord> glAcc, int to){
        for(int i = from; i < glAcc.size() - to; i++ ) {
            int count = baseEntityRepository.executeNativeUpdate("update gl_acc set dtc = TO_DATE('2015-12-29','RRRR-MM-DD') where acid = '"+ACID+"' and bsaacid=?", glAcc.get(i).getString("bsaacid"));
            System.out.println(count + ": update gl_acc set dtc = TO_DATE('2015-12-29','RRRR-MM-DD') where bsaacid = " + glAcc.get(i).getString("bsaacid"));
            Assert.assertTrue(count == 1);
        }
    }

    private List<DataRecord> getGlAccRlnType4() throws Exception{
        List<DataRecord> glAcc = null;
        glAcc = baseEntityRepository.select("select * from gl_acc where acid = '"+ACID+"' and rlntype='4' order by bsaacid");
        Assert.assertTrue("мало записей, выполнить test00", glAcc.size() > 2);
        return glAcc;
    }

    private void glAccBeginState(){
        int count = baseEntityRepository.executeNativeUpdate("update gl_acc set dealid=null,subdealid=null,dtc=null,"+
                " acctype= case when rlntype='0' then 131060101 when rlntype='4' then 131060102 else acctype end where acid = '"+ACID+"'");
        System.out.println(count + ": начальное состояние gl_acc ");

        count = baseEntityRepository.executeNativeUpdate("update GL_ACTNAME set FL_CTRL = 'N' where ACCTYPE = '131060102'");
        System.out.println(count + ": set FL_CTRL = 'N' where ACCTYPE = '131060102'");

        count = baseEntityRepository.executeNativeUpdate("update GL_ACTPARM set dte=null where ACCTYPE = '131060102' and custype='9'");
        System.out.println(count + ": set dte=null where ACCTYPE = '131060102'");

        count = baseEntityRepository.executeNativeUpdate("update gl_actparm set ac_sq = '02' where acctype = '131060102' and custype = '00'");
        System.out.println(count +": update gl_actparm set ac_sq = '02' where acctype = '131060102' and custype = '00'");

    }

    private void delAccount(String acid, String bsaacid){
        int count = baseEntityRepository.executeNativeUpdate("delete from accrlnext where bsaacid=? and acid=?", bsaacid, acid);
        System.out.println(count + ": delete from accrlnext");
        count = baseEntityRepository.executeNativeUpdate("delete from accrln where bsaacid=? and acid=?", bsaacid, acid);
        System.out.println(count + ": delete from accrln");
        count = baseEntityRepository.executeNativeUpdate("delete from gl_acc where bsaacid=? and acid=?", bsaacid, acid);
        System.out.println(count + ": delete from gl_acc");
        count = baseEntityRepository.executeNativeUpdate("delete from bsaacc where id=?", bsaacid);
        System.out.println(count + ": delete from bsaacc");

    }
    private boolean isCodeInGlAudit(long id, String code) throws Exception {
        DataRecord glAudit = baseEntityRepository.selectFirst("select * from gl_audit where entity_id=? and entitytype='GL_OPER' and sys_time >  CURRENT_TIMESTAMP - interval '1' day", id);
        System.out.println(glAudit.getString("ERRORMSG"));
        return glAudit.getString("ERRORMSG").indexOf(code) > -1;
    }
}
