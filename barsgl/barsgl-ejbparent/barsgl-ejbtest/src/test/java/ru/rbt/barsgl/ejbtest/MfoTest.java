package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.shared.enums.OperState;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.entity.etl.EtlPackage.PackageState.LOADED;

/**
 * Created by Ivan Sevastyanov on 01.12.2016.
 */
public class MfoTest extends AbstractRemoteTest {
    /**
     * ***
     * @throws Exception
     */
    @Test
    @Ignore
    public void pstMfoBugTest1()throws Exception {

//        baseEntityRepository.executeNativeUpdate("update GL_OD set CURDATE='2016-03-23'");
        Date operday = DateUtils.parseDate("2016-03-23", "yyyy-MM-dd");
        setOperday(operday, DateUtils.addDays(operday, -1), ONLINE, OPEN);
        baseEntityRepository.executeNativeUpdate("delete from gl_etlpst where id = 3353248");
        baseEntityRepository.executeNativeUpdate("delete from gl_etlpkg where ID_PKG = 24560");
        baseEntityRepository.executeNativeUpdate("insert into gl_etlpkg( ID_PKG, PKG_NAME, DT_LOAD, STATE, DESCRIPTION, ACC_CNT, PST_CNT, DT_PRC, MESSAGE)\n" +
                " VALUES(24560,'24560','2016-08-15 00:00:00','ERROR','ARS_PST',0,1,'2016-08-15 00:00:00',null)");
        baseEntityRepository.executeNativeUpdate("insert into gl_etlpst( ID, ID_PST, ID_PKG, SRC_PST, EVT_ID, DEAL_ID, CHNL_NAME, PMT_REF, DEPT_ID, VDATE, OTS, NRT, RNRTL, RNRTS, STRN, STRNRF, AC_DR, CCY_DR, AMT_DR, AMTRU_DR, AC_CR, CCY_CR, AMT_CR, AMTRU_CR, FAN, PAR_RF, ECODE, EMSG, ACCKEY_DR, ACCKEY_CR, EVTP)\n" +
                " VALUES(3353248,'12167610',24560,'UCBCA','0F26','0F26',null,'Z11608240F26','CAS       ','2016-08-15','2016-08-24 00:00:00','065 Z1 160824 0F26 Cash','Зачисление на счет через кассу Банка. Вноситель: ЗЕЛЕПУХИН МИХАИЛ ВАЛЕНТИНОВИЧ.','Зачисление на счет через кассу Банка. Вноситель: ЗЕЛЕПУХИН МИХАИЛ ВАЛЕНТИНОВИЧ.','N',null,'20202810000013004300','RUR',39700.0,null,'40817978350020004553','EUR',533.96,null,'N',null,0,'SUCCESS',null,null,'Z1')");

        EtlPackage pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, 24560l);
        pkg.setPackageState(LOADED);
        pkg.setDateLoad(new Date());
        pkg.setProcessDate(new Date());
        pkg.setDateLoad(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2016-09-15 00:00:00"));
        pkg.setProcessDate(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2016-09-15 00:00:00"));
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst = (EtlPosting) baseEntityRepository.findById(EtlPosting.class, 3353248l);
        pst.setEtlPackage(pkg);
        pst.setErrorCode(null);
        pst.setErrorMessage(null);
        pst.setValueDate(new SimpleDateFormat("yyyy-MM-dd").parse("2016-03-23"));
        baseEntityRepository.update(pst);

        GLOperation oper = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(oper);
        oper = getLastOperation(pst.getId());
//        oper = (GLOperation) baseEntityRepository.findById(oper.getClass(), oper.getId());
        Assert.assertNotNull(oper);
        Assert.assertEquals(OperState.POST, oper.getState());
        System.out.println("oper.getId() = "+oper.getId());
        GLPosting posting = getPostingByOper(oper);
        Assert.assertNotNull(posting);
        System.out.println("posting.getId() = "+posting.getId());
    }

    /**
     * ***
     * @throws Exception
     */
    @Test
    @Ignore
    public void pstMfoBugTest2()throws Exception {

        EtlPackage pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, 24560l);
        pkg.setPackageState(LOADED);
        pkg.setDateLoad(new Date());
        pkg.setProcessDate(new Date());
        pkg.setDateLoad(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2016-09-15 00:00:00"));
        pkg.setProcessDate(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2016-09-15 00:00:00"));
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst = (EtlPosting) baseEntityRepository.findById(EtlPosting.class, 3353248l);
        pst.setEtlPackage(pkg);
        pst.setErrorCode(null);
        pst.setErrorMessage(null);
        pst.setValueDate(new SimpleDateFormat("yyyy-MM-dd").parse("2016-03-23"));
        pst.setAmountDebit(new BigDecimal(39700.000));
        pst.setAccountDebit("40817810000010371579");
        pst.setCurrencyDebit(BankCurrency.RUB);

        pst.setAmountCredit(new BigDecimal(533.960));
        pst.setAccountCredit("40817978350020004553");
        pst.setCurrencyCredit(BankCurrency.EUR);
        baseEntityRepository.update(pst);

        GLOperation oper = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(oper);
        oper = getLastOperation(pst.getId());
//        oper = (GLOperation) baseEntityRepository.findById(oper.getClass(), oper.getId());
        Assert.assertNotNull(oper);
        Assert.assertEquals(OperState.POST, oper.getState());
        System.out.println("oper.getId() = "+oper.getId());
        GLPosting posting = getPostingByOper(oper);
        Assert.assertNotNull(posting);
        System.out.println("posting.getId() = "+posting.getId());
    }

    /**
     * ***
     * @throws Exception
     */
    @Test
    @Ignore
    public void pstMfoBugTest3()throws Exception {

        EtlPackage pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, 24560l);
        pkg.setPackageState(LOADED);
        pkg.setDateLoad(new Date());
        pkg.setProcessDate(new Date());
        pkg.setDateLoad(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2016-09-15 00:00:00"));
        pkg.setProcessDate(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2016-09-15 00:00:00"));
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst = (EtlPosting) baseEntityRepository.findById(EtlPosting.class, 3353248l);
        pst.setEtlPackage(pkg);
        pst.setErrorCode(null);
        pst.setErrorMessage(null);
        pst.setValueDate(new SimpleDateFormat("yyyy-MM-dd").parse("2016-03-23"));
        pst.setAmountDebit(new BigDecimal(533.960));
        pst.setAccountDebit("40817978350020004553");
        pst.setCurrencyDebit(BankCurrency.EUR);

        pst.setAmountCredit(new BigDecimal(39700.000));
        pst.setAccountCredit("20202810000013004300");
        pst.setCurrencyCredit(BankCurrency.RUB);
        baseEntityRepository.update(pst);

        GLOperation oper = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(oper);
        oper = getLastOperation(pst.getId());
//        oper = (GLOperation) baseEntityRepository.findById(oper.getClass(), oper.getId());
        Assert.assertNotNull(oper);
        Assert.assertEquals(OperState.POST, oper.getState());
        System.out.println("oper.getId() = "+oper.getId());
        GLPosting posting = getPostingByOper(oper);
        Assert.assertNotNull(posting);
        System.out.println("posting.getId() = "+posting.getId());
    }

    @Test
    @Ignore
    public void pstMfoBugTest4()throws Exception {

        EtlPackage pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, 24560l);
        pkg.setPackageState(LOADED);
        pkg.setDateLoad(new Date());
        pkg.setProcessDate(new Date());
        pkg.setDateLoad(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2016-09-15 00:00:00"));
        pkg.setProcessDate(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2016-09-15 00:00:00"));
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst = (EtlPosting) baseEntityRepository.findById(EtlPosting.class, 3353248l);
        pst.setEtlPackage(pkg);
        pst.setErrorCode(null);
        pst.setErrorMessage(null);
        pst.setValueDate(new SimpleDateFormat("yyyy-MM-dd").parse("2016-03-23"));
        pst.setAmountDebit(new BigDecimal(533.960));
        pst.setAccountDebit("40817978350020004553");
        pst.setCurrencyDebit(BankCurrency.EUR);

        pst.setAmountCredit(new BigDecimal(39700.000));
        pst.setAccountCredit("40817810000010371579");
        pst.setCurrencyCredit(BankCurrency.RUB);
        baseEntityRepository.update(pst);

        GLOperation oper = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(oper);
        oper = getLastOperation(pst.getId());
//        oper = (GLOperation) baseEntityRepository.findById(oper.getClass(), oper.getId());
        Assert.assertNotNull(oper);
        Assert.assertEquals(OperState.POST, oper.getState());
        System.out.println("oper.getId() = "+oper.getId());
        GLPosting posting = getPostingByOper(oper);
        Assert.assertNotNull(posting);
        System.out.println("posting.getId() = "+posting.getId());
    }

    @Test
    @Ignore
    public void pstMfoBugTest5()throws Exception {

        EtlPackage pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, 24560l);
        pkg.setPackageState(LOADED);
        pkg.setDateLoad(new Date());
        pkg.setProcessDate(new Date());
        pkg.setDateLoad(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2016-09-15 00:00:00"));
        pkg.setProcessDate(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2016-09-15 00:00:00"));
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst = (EtlPosting) baseEntityRepository.findById(EtlPosting.class, 3353248l);
        pst.setEtlPackage(pkg);
        pst.setErrorCode(null);
        pst.setErrorMessage(null);
        pst.setValueDate(new SimpleDateFormat("yyyy-MM-dd").parse("2016-03-23"));
        pst.setAmountDebit(new BigDecimal(600));
        pst.setAccountDebit("20202840000010609639");
        pst.setCurrencyDebit(BankCurrency.USD);

        pst.setAmountCredit(new BigDecimal(533.960));
        pst.setAccountCredit("40817978350020004553");
        pst.setCurrencyCredit(BankCurrency.EUR);
        baseEntityRepository.update(pst);

        GLOperation oper = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(oper);
        oper = getLastOperation(pst.getId());
//        oper = (GLOperation) baseEntityRepository.findById(oper.getClass(), oper.getId());
        Assert.assertNotNull(oper);
        Assert.assertEquals(OperState.POST, oper.getState());
        System.out.println("oper.getId() = "+oper.getId());
        GLPosting posting = getPostingByOper(oper);
        Assert.assertNotNull(posting);
        System.out.println("posting.getId() = "+posting.getId());
    }

    @Test
    @Ignore
    public void pstMfoBugTest7()throws Exception {

        EtlPackage pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, 24560l);
        pkg.setPackageState(LOADED);
        pkg.setDateLoad(new Date());
        pkg.setProcessDate(new Date());
        pkg.setDateLoad(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2016-09-15 00:00:00"));
        pkg.setProcessDate(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2016-09-15 00:00:00"));
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst = (EtlPosting) baseEntityRepository.findById(EtlPosting.class, 3353248l);
        pst.setEtlPackage(pkg);
        pst.setErrorCode(null);
        pst.setErrorMessage(null);
        pst.setValueDate(new SimpleDateFormat("yyyy-MM-dd").parse("2016-03-23"));
        pst.setAmountDebit(new BigDecimal(39700.000));
        pst.setAccountDebit("40604810100014722965");
        pst.setCurrencyDebit(BankCurrency.RUB);

        pst.setAmountCredit(new BigDecimal(533.960));
        pst.setAccountCredit("40817978350020004553");
        pst.setCurrencyCredit(BankCurrency.EUR);
        baseEntityRepository.update(pst);

        GLOperation oper = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(oper);
        oper = getLastOperation(pst.getId());
//        oper = (GLOperation) baseEntityRepository.findById(oper.getClass(), oper.getId());
        Assert.assertNotNull(oper);
        Assert.assertEquals(OperState.POST, oper.getState());
        System.out.println("oper.getId() = "+oper.getId());
        GLPosting posting = getPostingByOper(oper);
        Assert.assertNotNull(posting);
        System.out.println("posting.getId() = "+posting.getId());
    }

    @Test
    @Ignore
    public void pstMfoBugTest8()throws Exception {

        EtlPackage pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, 24560l);
        pkg.setPackageState(LOADED);
        pkg.setDateLoad(new Date());
        pkg.setProcessDate(new Date());
        pkg.setDateLoad(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2016-09-15 00:00:00"));
        pkg.setProcessDate(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2016-09-15 00:00:00"));
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst = (EtlPosting) baseEntityRepository.findById(EtlPosting.class, 3353248l);
        pst.setEtlPackage(pkg);
        pst.setErrorCode(null);
        pst.setErrorMessage(null);
        pst.setValueDate(new SimpleDateFormat("yyyy-MM-dd").parse("2016-03-23"));
        pst.setAmountDebit(new BigDecimal(39700.000));
        pst.setAccountDebit("40604810100014722965");
        pst.setCurrencyDebit(BankCurrency.RUB);

        pst.setAmountCredit(new BigDecimal(533.960));
        pst.setAccountCredit("40901978000024588008");
        pst.setCurrencyCredit(BankCurrency.EUR);
        baseEntityRepository.update(pst);

        GLOperation oper = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(oper);
        oper = getLastOperation(pst.getId());
//        oper = (GLOperation) baseEntityRepository.findById(oper.getClass(), oper.getId());
        Assert.assertNotNull(oper);
        Assert.assertEquals(OperState.POST, oper.getState());
        System.out.println("oper.getId() = "+oper.getId());
        GLPosting posting = getPostingByOper(oper);
        Assert.assertNotNull(posting);
        System.out.println("posting.getId() = "+posting.getId());
    }

}
