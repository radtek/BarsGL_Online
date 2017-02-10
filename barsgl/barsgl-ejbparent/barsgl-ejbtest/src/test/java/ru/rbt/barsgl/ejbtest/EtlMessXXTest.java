package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.shared.enums.OperState;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Created by ER22317 on 06.02.2017.
 */
public class EtlMessXXTest extends AbstractTimerJobTest{
    public static final Logger log = Logger.getLogger(EtlMessXXTest.class.getName());

    @Ignore
    @Test
    public void test3Case() throws ParseException {
        long stamp = System.currentTimeMillis();

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
        pst1.setAmountCredit(new BigDecimal("22.450"));
        pst1.setAmountDebit(new BigDecimal("22.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("002;RUR;00118390;443040100;18;11;XX00000319;0002;;;4105;00;AXAPTA;;");
//        pst1.setAccountKeyDebit("002;RUR;00137476;453040100;18;11;XX00000321;0002;;;5105;00;AXAPTA;;");
        pst1.setAccountKeyCredit("002;RUR;00000151;505030201;00;00;XX00000737;0002;;;4491;01;AXAPTA;;");
//        pst1.setAccountCredit("47425810400014560229");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
    }

    @Ignore
    @Test
    public void test2Case() throws ParseException {
        long stamp = System.currentTimeMillis();

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
        pst1.setAmountCredit(new BigDecimal("22.450"));
        pst1.setAmountDebit(new BigDecimal("22.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("002;RUR;00118390;453040100;18;11;XX00000320;0002;;;5105;01;AXAPTA;;");
//        pst1.setAccountKeyDebit("002;RUR;00137476;453040100;18;11;XX00000321;0002;;;5105;00;AXAPTA;;");
        pst1.setAccountKeyCredit("002;RUR;00000151;505030201;00;00;XX00000737;0002;;;4491;01;AXAPTA;;");
//        pst1.setAccountCredit("47425810400014560229");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.ERCHK, operation.getState());
    }


    @Ignore
    @Test
    public void test1Case() throws ParseException {
        long stamp = System.currentTimeMillis();

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
        pst1.setAmountCredit(new BigDecimal("11.450"));
        pst1.setAmountDebit(new BigDecimal("11.450"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("AXAPTA");
        pst1.setFan(YesNo.N);
        pst1.setAccountKeyDebit("002;RUR;00137476;443040100;18;11;XX00000321;0002;;;4105;01;AXAPTA;;");
//        pst1.setAccountKeyDebit("002;RUR;00137476;453040100;18;11;XX00000321;0002;;;5105;00;AXAPTA;;");
        pst1.setAccountKeyCredit("002;RUR;00000151;505030201;00;00;XX00000737;0002;;;4491;01;AXAPTA;;");
//        pst1.setAccountCredit("47425810400014560229");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
    }

    @Test
    public void testGlAcc() throws ParseException {
        long stamp = System.currentTimeMillis();

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
        pst1.setAccountKeyDebit("015;RUR;00000151;505030203;00;00;XX00000738;0015;;;4491;10;AXAPTA;;");
//        '00000151RUR449110015'
        pst1.setAccountKeyCredit("015;RUR;00000151;505030201;00;00;XX00000737;0015;;;4491;01;AXAPTA;;");
//        pst1.setAccountCredit("47425810400014560229");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());

    }

    @Test
    public void testErrKey() throws ParseException {
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(4);
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
        pst1.setAccountKeyDebit("015;RUR;00000151;505030203;00;00;XX00000738;0015;;;4491;10;AXAPTA;DEALID;");
        pst1.setAccountCredit("47425810400014560229");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.ERCHK, operation.getState());

        EtlPosting pst2 = newPosting(stamp, pkg);
        pst2.setAePostingId("21084014");
        pst2.setEventId("ГК07248804_000000001");
        pst2.setValueDate(getOperday().getCurrentDate());
        pst2.setOperationTimestamp(new Date());
        pst2.setNarrative("Narrative");
        pst2.setRusNarrativeLong("RusNarrativeLong");
        pst2.setRusNarrativeShort("RusNarrativeShort");
        pst2.setStorno(YesNo.N);
        pst2.setAmountCredit(new BigDecimal("4380.450"));
        pst2.setAmountDebit(new BigDecimal("4380.450"));
        pst2.setCurrencyCredit(BankCurrency.RUB);
        pst2.setCurrencyDebit(BankCurrency.RUB);
        pst2.setSourcePosting("AXAPTA");
        pst2.setFan(YesNo.N);
        pst2.setAccountKeyDebit("015;RUR;00000151;505030203;00;00;XX00000738;0015;;;4491;10;AXAPTA;;SUBDEALID");
        pst2.setAccountCredit("47425810400014560229");
        pst2 = (EtlPosting) baseEntityRepository.save(pst2);

        operation = (GLOperation) postingController.processMessage(pst2);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.ERCHK, operation.getState());

        EtlPosting pst3 = newPosting(stamp, pkg);
        pst3.setAePostingId("21084014");
        pst3.setEventId("ГК07248804_000000001");
        pst3.setValueDate(getOperday().getCurrentDate());
        pst3.setOperationTimestamp(new Date());
        pst3.setNarrative("Narrative");
        pst3.setRusNarrativeLong("RusNarrativeLong");
        pst3.setRusNarrativeShort("RusNarrativeShort");
        pst3.setStorno(YesNo.N);
        pst3.setAmountCredit(new BigDecimal("4380.450"));
        pst3.setAmountDebit(new BigDecimal("4380.450"));
        pst3.setCurrencyCredit(BankCurrency.RUB);
        pst3.setCurrencyDebit(BankCurrency.RUB);
        pst3.setSourcePosting("AXAPTA");
        pst3.setFan(YesNo.N);
        pst3.setAccountKeyDebit("015;RUR;00000151;505030203;00;00;XX00000738;0015;;27803;4491;10;AXAPTA;;");
        pst3.setAccountCredit("47425810400014560229");
        pst3 = (EtlPosting) baseEntityRepository.save(pst3);

        operation = (GLOperation) postingController.processMessage(pst3);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.ERCHK, operation.getState());

        EtlPosting pst4 = newPosting(stamp, pkg);
        pst4.setAePostingId("21084014");
        pst4.setEventId("ГК07248804_000000001");
        pst4.setValueDate(getOperday().getCurrentDate());
        pst4.setOperationTimestamp(new Date());
        pst4.setNarrative("Narrative");
        pst4.setRusNarrativeLong("RusNarrativeLong");
        pst4.setRusNarrativeShort("RusNarrativeShort");
        pst4.setStorno(YesNo.N);
        pst4.setAmountCredit(new BigDecimal("4380.450"));
        pst4.setAmountDebit(new BigDecimal("4380.450"));
        pst4.setCurrencyCredit(BankCurrency.RUB);
        pst4.setCurrencyDebit(BankCurrency.RUB);
        pst4.setSourcePosting("AXAPTA");
        pst4.setFan(YesNo.N);
        pst4.setAccountKeyDebit("015;RUR;00000151;731050202;00;00;XX00000738;0015;;;4491;10;AXAPTA;;");
        pst4.setAccountCredit("47425810400014560229");
        pst4 = (EtlPosting) baseEntityRepository.save(pst4);

        operation = (GLOperation) postingController.processMessage(pst4);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.ERCHK, operation.getState());
    }
}
