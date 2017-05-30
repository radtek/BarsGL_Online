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
 Дата открытия счета больше даты валютирования операции
 */
public class FromEtlMessageIT extends AbstractRemoteIT {

    /**
     * меняем у одного счета
     * @throws Exception
     */
    @Test
    @Ignore
    public void pstAccUpdOpen1Acc()throws Exception {
        long pkgId = 249, pstId = 891;
        String wd = "2016-03-03", valueDate = "2016-03-02", openDate = "2016-03-15";
        Date operday = DateUtils.parseDate(wd, "yyyy-MM-dd");
//        baseEntityRepository.executeNativeUpdate("update GL_OD set CURDATE='"+wd+"'");
        setOperday(operday, DateUtils.addDays(operday, -1), ONLINE, OPEN);
        baseEntityRepository.executeNativeUpdate("delete from gl_etlpst where id = "+pstId);
        baseEntityRepository.executeNativeUpdate("delete from gl_etlpkg where ID_PKG = "+pkgId);
        baseEntityRepository.executeNativeUpdate("update gl_acc set dto='"+openDate+"' where bsaacid = '30302810700010000033'");
        baseEntityRepository.executeNativeUpdate("update accrln set drlno='"+openDate+"' where bsaacid in ('30302810700010000033')");
        baseEntityRepository.executeNativeUpdate("update bsaacc set bsaaco='"+openDate+"' where id ='30302810700010000033'");

        baseEntityRepository.executeNativeUpdate("insert into gl_etlpkg( ID_PKG, PKG_NAME, DT_LOAD, STATE, DESCRIPTION, ACC_CNT, PST_CNT, DT_PRC, MESSAGE)\n" +
                " VALUES("+pkgId+",'pkg1458917253657','2016-03-25 00:00:00','PROCESSED','MfoExchange',0,1,null,null)");
        baseEntityRepository.executeNativeUpdate("insert into gl_etlpst( ID, ID_PST, ID_PKG, SRC_PST, EVT_ID, DEAL_ID, CHNL_NAME, PMT_REF, DEPT_ID, VDATE, OTS, NRT, RNRTL, RNRTS, STRN, STRNRF, AC_DR, CCY_DR, AMT_DR, AMTRU_DR, AC_CR, CCY_CR, AMT_CR, AMTRU_CR, FAN, PAR_RF, ECODE, EMSG, ACCKEY_DR, ACCKEY_CR, EVTP)\n" +
                " VALUES("+pstId+",'id_8917253859',249,'srcpst','evtid8917253859',null,'CHN_TEST','PMT1458917253657','NGT       ','2015-02-26','2016-03-25 00:00:00','00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000nrt_1458917253657_','nrt1458917253657','nrt1458917253657','N',null,'30302810700010000033','RUR',805.12,null,'47427810550160009330','RUR',805.12,null,'N',null,0,'SUCCESS','101;RUR;00233759;171010200;21;08;0000003878;0013;47427;;;71;FC12_CL;190_A004S_15;00233759RURSA0100002','101;RUR;00001011;611010201;21;08;PL00000061;0013;70601;11114;;;FC12_CL;;','eventType')");

        EtlPackage pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, pkgId);
        pkg.setPackageState(LOADED);
        pkg.setDateLoad(new Date());
        pkg.setProcessDate(new Date());
//        pkg.setDateLoad(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(wd));
        pkg.setDateLoad(new SimpleDateFormat("yyyy-MM-dd").parse(wd));
        pkg.setProcessDate(new SimpleDateFormat("yyyy-MM-dd").parse(wd));
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst = (EtlPosting) baseEntityRepository.findById(EtlPosting.class, pstId);
        pst.setEtlPackage(pkg);
        pst.setErrorCode(null);
        pst.setErrorMessage(null);
        pst.setValueDate(new SimpleDateFormat("yyyy-MM-dd").parse(valueDate));
        baseEntityRepository.update(pst);

        GLOperation oper = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(oper);
        oper = getLastOperation(pst.getId());
        Assert.assertEquals(OperState.POST, oper.getState());
        Assert.assertTrue(isGlAccChangedOpenDate(oper.getAccountDebit(), oper.getValueDate()));
        Assert.assertTrue(isAccrlnChangedOpenDate(oper.getAccountDebit(), oper.getValueDate()));
        Assert.assertTrue(isBsaaccChangedOpenDate(oper.getAccountDebit(), oper.getValueDate()));
    }

    /**
     * меняем у двух счетов
     * @throws Exception
     */
    @Test
    @Ignore
    public void pstAccUpdOpen2acc()throws Exception {
        long pkgId = 197, pstId = 839;
        String wd = "2016-03-03", valueDate = "2016-03-02", openDate = "2016-03-15";
        Date operday = DateUtils.parseDate(wd, "yyyy-MM-dd");
//        baseEntityRepository.executeNativeUpdate("update GL_OD set CURDATE='"+wd+"'");
        setOperday(operday, DateUtils.addDays(operday, -1), ONLINE, OPEN);
        baseEntityRepository.executeNativeUpdate("delete from gl_etlpst where id = "+pstId);
        baseEntityRepository.executeNativeUpdate("delete from gl_etlpkg where ID_PKG = "+pkgId);
        baseEntityRepository.executeNativeUpdate("update gl_acc set dto='"+openDate+"' where bsaacid in ('47427810920130000002','70601810599131111499')");
        baseEntityRepository.executeNativeUpdate("update accrln set drlno='"+openDate+"' where bsaacid in ('47427810920130000002','70601810599131111499')");
        baseEntityRepository.executeNativeUpdate("update bsaacc set bsaaco='"+openDate+"' where id in ('47427810920130000002','70601810599131111499')");

        baseEntityRepository.executeNativeUpdate("insert into gl_etlpkg( ID_PKG, PKG_NAME, DT_LOAD, STATE, DESCRIPTION, ACC_CNT, PST_CNT, DT_PRC, MESSAGE)\n" +
                " VALUES("+pkgId+",'pkg1458909057468','2016-03-25 00:00:00','PROCESSED','TestExchange',0,1,null,null)");
        baseEntityRepository.executeNativeUpdate("insert into gl_etlpst( ID, ID_PST, ID_PKG, SRC_PST, EVT_ID, DEAL_ID, CHNL_NAME, PMT_REF, DEPT_ID, VDATE, OTS, NRT, RNRTL, RNRTS, STRN, STRNRF, AC_DR, CCY_DR, AMT_DR, AMTRU_DR, AC_CR, CCY_CR, AMT_CR, AMTRU_CR, FAN, PAR_RF, ECODE, EMSG, ACCKEY_DR, ACCKEY_CR, EVTP)\n" +
                " VALUES("+pstId+",'id_8909058761',197,'srcpst','evtid8909058761',null,'CHN_TEST','PMT1458909057468','NGT       ','2015-02-26','2016-03-25 00:00:00','00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000nrt_1458909057468_','nrt1458909057468','nrt1458909057468','N',null,'47427810920130000002','RUR',805.12,null,'70601810599131111499','RUR',805.12,null,'N',null,null,null,null,null,'eventType')");

        EtlPackage pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, pkgId);
        pkg.setPackageState(LOADED);
        pkg.setDateLoad(new Date());
        pkg.setProcessDate(new Date());
        pkg.setDateLoad(new SimpleDateFormat("yyyy-MM-dd").parse(wd));
        pkg.setProcessDate(new SimpleDateFormat("yyyy-MM-dd").parse(wd));
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst = (EtlPosting) baseEntityRepository.findById(EtlPosting.class, pstId);
        pst.setEtlPackage(pkg);
        pst.setErrorCode(null);
        pst.setErrorMessage(null);
        pst.setValueDate(new SimpleDateFormat("yyyy-MM-dd").parse(valueDate));
        baseEntityRepository.update(pst);

        GLOperation oper = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(oper);
        oper = getLastOperation(pst.getId());
        Assert.assertEquals(OperState.POST, oper.getState());
        Assert.assertTrue(isGlAccChangedOpenDate(oper.getAccountDebit(), oper.getValueDate()));
        Assert.assertTrue(isAccrlnChangedOpenDate(oper.getAccountDebit(), oper.getValueDate()));
        Assert.assertTrue(isBsaaccChangedOpenDate(oper.getAccountDebit(), oper.getValueDate()));
    }

    private boolean isGlAccChangedOpenDate(String bsaacid, Date od) throws Exception{
        return null != baseEntityRepository.selectFirst("select 1 from GL_ACC where bsaacid = ? and dto = ?", bsaacid, od);
    }
    private boolean isAccrlnChangedOpenDate(String bsaacid, Date od) throws Exception{
        return null != baseEntityRepository.selectFirst("select 1 from ACCrln where bsaacid = ? and drlno = ?", bsaacid, od);
    }
    private boolean isBsaaccChangedOpenDate(String bsaacid, Date od) throws Exception{
        return null != baseEntityRepository.selectFirst("select 1 from bsaacc where id = ? and bsaaco = ?", bsaacid, od);
    }

    /**
     * не меняем
     * @throws Exception
     */
    @Test
    @Ignore
    public void pstAccNoUpdOpen()throws Exception {
        long pkgId = 478, pstId = 1208;
        String wd = "2016-03-03", openDate = "2016-03-15";
        Date operday = DateUtils.parseDate(wd, "yyyy-MM-dd");
        setOperday(operday, DateUtils.addDays(operday, -1), ONLINE, OPEN);

        baseEntityRepository.executeNativeUpdate("delete from gl_etlpst where id = "+pstId);
        baseEntityRepository.executeNativeUpdate("delete from gl_etlpkg where ID_PKG = "+pkgId);
        baseEntityRepository.executeNativeUpdate("insert into gl_etlpkg( ID_PKG, PKG_NAME, DT_LOAD, STATE, DESCRIPTION, ACC_CNT, PST_CNT, DT_PRC, MESSAGE) "+
                "VALUES("+pkgId+",'pkg1473519464922','2016-03-03 00:00:00','PROCESSED','MfoExchange',0,1,null,null)");
        baseEntityRepository.executeNativeUpdate("insert into gl_etlpst( ID, ID_PST, ID_PKG, SRC_PST, EVT_ID, DEAL_ID, CHNL_NAME, PMT_REF, DEPT_ID, VDATE, OTS, NRT, RNRTL, RNRTS, STRN, STRNRF, AC_DR, CCY_DR, AMT_DR, AMTRU_DR, AC_CR, CCY_CR, AMT_CR, AMTRU_CR, FAN, PAR_RF, ECODE, EMSG, ACCKEY_DR, ACCKEY_CR, EVTP) "+
                "VALUES("+pstId+",'id_3519466792',478,'FC12_CL','evtid3519466792','DD_6948','CHN_TEST','PMT1473519464922','NGT       ','2016-03-03','2016-03-03 00:00:00','0000000000000000000000000000000000000000000000000000000000000000000000000000000000nrt_1473519464922_','nrt1473519464922','nrt1473519464922','N',null,'40817810000502307931','RUR',200.0,null,'40817840000452424671','USD',13000.78,null,'N',null,0,'SUCCESS',null,null,'eventType')");

        EtlPackage pkg = (EtlPackage) baseEntityRepository.findById(EtlPackage.class, pkgId);
        pkg.setPackageState(LOADED);
        pkg.setDateLoad(new Date());
        pkg.setProcessDate(new Date());
        pkg.setDateLoad(new SimpleDateFormat("yyyy-MM-dd").parse(wd));
        pkg.setProcessDate(new SimpleDateFormat("yyyy-MM-dd").parse(wd));
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst = (EtlPosting) baseEntityRepository.findById(EtlPosting.class, pstId);
        pst.setEtlPackage(pkg);
        pst.setErrorCode(null);
        pst.setErrorMessage(null);
        pst.setValueDate(new SimpleDateFormat("yyyy-MM-dd").parse(wd));
        baseEntityRepository.update(pst);

        GLOperation oper = (GLOperation) postingController.processMessage(pst);
        oper = getLastOperation(pst.getId());
        Assert.assertNotNull(oper);
        Assert.assertEquals(OperState.ERCHK, oper.getState());
    }


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
