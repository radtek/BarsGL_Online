package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.BackvalueJournalController;
import ru.rbt.barsgl.ejb.controller.operday.task.EtlStructureMonitorTask;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.*;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.StringUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static ru.rbt.barsgl.ejb.entity.etl.EtlPackage.PackageState.LOADED;
import static ru.rbt.barsgl.ejb.entity.gl.BackvalueJournal.BackvalueJournalState.NEW;
import static ru.rbt.barsgl.shared.enums.DealSource.KondorPlus;

/**
 * Created by Ivan Sevastyanov
 * Пересчет баланса для проводок с прошедшей датой
 */
public class BackvalueIT extends AbstractTimerJobIT {

    public static final Logger log = Logger.getLogger(BackvalueIT.class.getName());

    @BeforeClass
    public static void beforeClass() throws SQLException {
        Date midasOperday = baseEntityRepository.selectOne("select workday from workday").getDate("workday");
        setOperday(getWorkdayAfter(midasOperday), midasOperday, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
    }

    /**
     * Пересчет баланса для простых проводок (в одном филиале, в одной валюте) с backvalue
     * @throws ParseException
     */
    @Test
    public void testBackValue() throws ParseException, SQLException {

        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN, Operday.PdMode.DIRECT);

        List<DataRecord> accounts = baseEntityRepository.select("select * from gl_acc where bsaacid like '40702036%' and rownum <= 2 and dtc is null");
        Assert.assertEquals(2, accounts.size());
        final String bsaAcidCt = accounts.get(0).getString("bsaacid");
        final String bsaAcidDt = accounts.get(1).getString("bsaacid");

        baseEntityRepository.executeUpdate("delete from BackvalueJournal j where j.id.bsaAcid = ?1", bsaAcidCt);
        baseEntityRepository.executeUpdate("delete from BackvalueJournal j where j.id.bsaAcid = ?1", bsaAcidDt);

        // удалить бэквалуе журнал
        BackvalueJournal journalDt = (BackvalueJournal) baseEntityRepository.selectFirst(BackvalueJournal.class,
                "from BackvalueJournal j where j.id.bsaAcid = ?1", bsaAcidDt);
        Assert.assertNull(journalDt);

        BackvalueJournal journalCt = (BackvalueJournal) baseEntityRepository.selectFirst(BackvalueJournal.class,
                "from BackvalueJournal j where j.id.bsaAcid = ?1", bsaAcidCt);
        Assert.assertNull(journalCt);

        long stamp = System.currentTimeMillis();

        baseEntityRepository.executeUpdate("update EtlPackage p set p.packageState = ?1 where p.packageState = ?2"
                , EtlPackage.PackageState.ERROR, EtlPackage.PackageState.LOADED);

        EtlPackage pkg = newPackageNotSaved(stamp, "SIMPLE");
        pkg.setPackageState(LOADED);
        pkg.setDateLoad(getSystemDateTime());
        pkg = (EtlPackage) baseEntityRepository.save(pkg);
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getLastWorkingDay());

        pst.setAccountCredit(bsaAcidCt);
        pst.setAccountDebit(bsaAcidDt);
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setAePostingId("PSTID_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 5));

        pst = (EtlPosting) baseEntityRepository.save(pst);

        remoteAccess.invoke(EtlStructureMonitorTask.class, "executeWork");

        GLOperation operation = (GLOperation) baseEntityRepository
                .selectFirst(GLOperation.class, "from GLOperation o where o.aePostingId = ?1", pst.getAePostingId());
        Assert.assertNotNull(operation);
        Assert.assertEquals(OperState.POST, operation.getState());

        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 1 проводка
        Assert.assertEquals(postList.size(), 1);

        List<Pd> pdList = getPostingPd(postList.get(0));
        Pd pdDr = pdList.get(0);
        Pd pdCr = pdList.get(1);
        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
        Assert.assertTrue(pdCr.getCcy().equals(pdDr.getCcy()));                 // валюта одинаковый

        Assert.assertTrue(pdCr.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит

        journalDt = (BackvalueJournal) baseEntityRepository.findById(BackvalueJournal.class,
                new BackvalueJournalId(pdDr.getAcid(), pdDr.getBsaAcid(), pdDr.getPod()));
        Assert.assertNotNull(journalDt);
        Assert.assertEquals(BackvalueJournal.BackvalueJournalState.PROCESSED, journalDt.getState());

        journalCt = (BackvalueJournal) baseEntityRepository.findById(BackvalueJournal.class,
                new BackvalueJournalId(pdCr.getAcid(), pdCr.getBsaAcid(), pdCr.getPod()));
        Assert.assertNotNull(journalCt);
        Assert.assertEquals(BackvalueJournal.BackvalueJournalState.PROCESSED, journalCt.getState());

    }

    /**
     * Пересчет баланса для простых проводок (в одном филиале, в одной валюте) с backvalue в режиме BUFFER
     * @throws ParseException
     */
    @Test
    public void testBackValueBuffer() throws Exception {

        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN, Operday.PdMode.BUFFER);
        List<DataRecord> accounts = baseEntityRepository.select("select * from gl_acc where bsaacid like '40702036%' and rownum <= 2 and dtc is null");
        Assert.assertEquals(2, accounts.size());
        final String bsaAcidCt = accounts.get(0).getString("bsaacid");
        final String bsaAcidDt = accounts.get(1).getString("bsaacid");


        baseEntityRepository.executeUpdate("delete from BackvalueJournal j where j.id.bsaAcid = ?1", bsaAcidCt);
        baseEntityRepository.executeUpdate("delete from BackvalueJournal j where j.id.bsaAcid = ?1", bsaAcidDt);

        // удалить бэквалуе журнал
        BackvalueJournal journalDt = (BackvalueJournal) baseEntityRepository.selectFirst(BackvalueJournal.class,
                "from BackvalueJournal j where j.id.bsaAcid = ?1", bsaAcidDt);
        Assert.assertNull(journalDt);

        BackvalueJournal journalCt = (BackvalueJournal) baseEntityRepository.selectFirst(BackvalueJournal.class,
                "from BackvalueJournal j where j.id.bsaAcid = ?1", bsaAcidCt);
        Assert.assertNull(journalCt);

        long stamp = System.currentTimeMillis();

        baseEntityRepository.executeUpdate("update EtlPackage p set p.packageState = ?1 where p.packageState = ?2"
                , EtlPackage.PackageState.ERROR, EtlPackage.PackageState.LOADED);

        EtlPackage pkg = newPackageNotSaved(stamp, "SIMPLE");
        pkg.setPackageState(LOADED);
        pkg.setDateLoad(getSystemDateTime());
        pkg = (EtlPackage) baseEntityRepository.save(pkg);
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getLastWorkingDay());

        pst.setAccountCredit(bsaAcidCt);
        pst.setAccountDebit(bsaAcidDt);
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setAePostingId("PSTID_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 5));

        pst = (EtlPosting) baseEntityRepository.save(pst);

        remoteAccess.invoke(EtlStructureMonitorTask.class, "executeWork");

        remoteAccess.invoke(BackvalueJournalController.class, "recalculateLocal");

        GLOperation operation = (GLOperation) baseEntityRepository
                .selectFirst(GLOperation.class, "from GLOperation o where o.aePostingId = ?1", pst.getAePostingId());
        Assert.assertNotNull(operation);
        Assert.assertEquals(OperState.POST, operation.getState());

        List<GLPosting> postList = getPostings(operation);
        Assert.assertTrue(postList.isEmpty());

        List<GLPd> pdList = getGLPostings(operation);
        GLPd pdDr = pdList.stream().filter(p -> p.getAmountBC() < 0).findFirst().orElseThrow(() -> new RuntimeException("debit not found"));
        GLPd pdCr = pdList.stream().filter(p -> p.getAmountBC() > 0).findFirst().orElseThrow(() -> new RuntimeException("credit not found"));

        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
        Assert.assertTrue(pdCr.getCcy().equals(pdDr.getCcy()));                 // валюта одинаковый

        Assert.assertTrue(pdCr.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
        Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит

        journalDt = (BackvalueJournal) baseEntityRepository.findById(BackvalueJournal.class,
                new BackvalueJournalId(pdDr.getAcid(), pdDr.getBsaAcid(), pdDr.getPod()));
        Assert.assertNotNull(journalDt);
        Assert.assertEquals(BackvalueJournal.BackvalueJournalState.PROCESSED, journalDt.getState());

        journalCt = (BackvalueJournal) baseEntityRepository.findById(BackvalueJournal.class,
                new BackvalueJournalId(pdCr.getAcid(), pdCr.getBsaAcid(), pdCr.getPod()));
        Assert.assertNotNull(journalCt);
        Assert.assertEquals(BackvalueJournal.BackvalueJournalState.PROCESSED, journalCt.getState());

    }

    /**
     * проверка обновления gl_bvjrnl.seq при последовательной обработке проводок бэквалуе в случае если строка журнала со статусом NEW
     * @throws SQLException
     */
    @Test public void testCheckSeq() throws SQLException {
        final Operday operday = getOperday();
        setOperday(operday.getCurrentDate(), operday.getLastWorkingDay(), Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN, Operday.PdMode.BUFFER);

        List<DataRecord> accounts = baseEntityRepository.select("select * from gl_acc where bsaacid like '40702036%' and rownum <= 2 and dtc is null");
        Assert.assertEquals(2, accounts.size());
        final String bsaAcidCt = accounts.get(0).getString("bsaacid");
        final String bsaAcidDt = accounts.get(1).getString("bsaacid");

        baseEntityRepository.executeUpdate("delete from BackvalueJournal j where j.id.bsaAcid = ?1", bsaAcidCt);
        baseEntityRepository.executeUpdate("delete from BackvalueJournal j where j.id.bsaAcid = ?1", bsaAcidDt);

        GLOperation operation1 = createBackvalueOper(getOperday().getLastWorkingDay(), bsaAcidCt, bsaAcidDt);
        GLPd pdDr1 = getDebit(operation1);
        GLPd pdCr1 = getCredit(operation1);

        BackvalueJournal journalDt1 = getBackvalueJournal(pdDr1);
        BackvalueJournal journalCt1 = getBackvalueJournal(pdCr1);

        Assert.assertTrue(journalDt1.getSequence() > 0);
        Assert.assertTrue(journalCt1.getSequence() > 0);

        GLOperation operation2 = createBackvalueOper(getOperday().getLastWorkingDay(), bsaAcidCt, bsaAcidDt);

        GLPd pdDr2 = getDebit(operation2);
        GLPd pdCr2 = getCredit(operation2);

        BackvalueJournal journalDt2 = getBackvalueJournal(pdDr2);
        BackvalueJournal journalCt2 = getBackvalueJournal(pdCr2);

        Assert.assertTrue(journalDt2.getSequence() > 0);
        Assert.assertTrue(journalCt2.getSequence() > 0);

        Assert.assertNotEquals(journalDt1.getSequence(), journalDt2.getSequence());
        Assert.assertNotEquals(journalCt1.getSequence(), journalCt2.getSequence());
        Assert.assertEquals(journalCt2.getState(), NEW);
        Assert.assertEquals(journalDt2.getState(), NEW);

    }

    /**
     * в случае открытия счета на небольшой период позже даты проводки - меняем дату открытия
     * @throws ParseException
     */
    @Test public void testBackvalueAccDtoChange() throws ParseException {

        final String bsaacidCt = "40817036200012959997";
        final String bsaacidDt = "40817036250010000018";

        try {
            Operday operday = getOperday();
            updateOperday(operday.getPhase(), operday.getLastWorkdayStatus(), Operday.PdMode.DIRECT);


            updateAccount(bsaacidCt, operday.getCurrentDate(), operday.getCurrentDate());
            updateAccount(bsaacidDt, operday.getCurrentDate(), operday.getCurrentDate());

            deleteClonedAccount(bsaacidCt);
            deleteClonedAccount(bsaacidDt);

            long stamp = System.currentTimeMillis();

            EtlPackage pkg = newPackage(stamp, "SIMPLE");
            Assert.assertTrue(pkg.getId() > 0);

            EtlPosting pst = newPosting(stamp, pkg);
            pst.setValueDate(getOperday().getLastWorkingDay());

            pst.setAccountCredit(bsaacidCt);
            pst.setAccountDebit(bsaacidDt);
            pst.setAmountCredit(new BigDecimal("12.0056"));
            pst.setAmountDebit(pst.getAmountCredit());
            pst.setCurrencyCredit(BankCurrency.AUD);
            pst.setCurrencyDebit(pst.getCurrencyCredit());
            pst.setSourcePosting(KondorPlus.getLabel());
            pst.setDealId("123");

            pst = (EtlPosting) baseEntityRepository.save(pst);

            GLOperation operation = (GLOperation) postingController.processMessage(pst);
            Assert.assertNotNull(operation);
            Assert.assertTrue(0 < operation.getId());
            operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
            Assert.assertEquals(OperState.POST, operation.getState());
            Assert.assertEquals(getOperday().getCurrentDate(), operation.getCurrentDate());
            Assert.assertEquals(getOperday().getLastWorkdayStatus(), operation.getLastWorkdayStatus());

            GLAccount accountCt = (GLAccount) baseEntityRepository.selectFirst(GLAccount.class, "from GLAccount a where a.bsaAcid = ?1", bsaacidCt);
            Assert.assertEquals(operday.getLastWorkingDay(), accountCt.getDateOpen());

            GLAccount accountDt = (GLAccount) baseEntityRepository.selectFirst(GLAccount.class, "from GLAccount a where a.bsaAcid = ?1", bsaacidDt);
            Assert.assertEquals(operday.getLastWorkingDay(), accountDt.getDateOpen());

            List<GLPosting> postList = getPostings(operation);
            Assert.assertNotNull(postList);                 // 1 проводка
            Assert.assertEquals(postList.size(), 1);

            List<Pd> pdList = getPostingPd(postList.get(0));
            Pd pdDr = pdList.get(0);
            Pd pdCr = pdList.get(1);
            Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
            Assert.assertEquals(accountDt.getAcid(), pdDr.getAcid());  // acid debet
            Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
            Assert.assertTrue(pdCr.getCcy().equals(pdDr.getCcy()));                 // валюта одинаковый
            Assert.assertEquals(accountCt.getAcid(), pdCr.getAcid());  // acid credit
            Assert.assertEquals(org.apache.commons.lang3.StringUtils.leftPad(pst.getDealId(), 6, "0"), pdDr.getPref().trim());
            Assert.assertEquals(org.apache.commons.lang3.StringUtils.leftPad(pst.getDealId(), 6, "0"), pdCr.getPref().trim());

            Assert.assertTrue(pdCr.getAmount() == operation.getAmountDebit().movePointRight(2).longValue());  // сумма в валюте
            Assert.assertTrue(pdCr.getAmount() == -pdDr.getAmount());       // сумма в валюте дебет - кредит
        } finally {
            deleteClonedAccount(bsaacidCt);
            deleteClonedAccount(bsaacidDt);
        }
    }

    /**
     * в случае открытия счета на небольшой период позже даты проводки - меняем дату открытия
     * при этом если есть открытый и закрытый счета попадающие в update даты открытия - получаем ERCHK
     */
    @Test public void testBackvalueAccDtoChange_ERCHK() throws ParseException {

        final String bsaacidCt = "40817036200012959997";
        final String bsaacidDt = "40817036250010000018";

        try {
            Operday operday = getOperday();
            updateOperday(operday.getPhase(), operday.getLastWorkdayStatus(), Operday.PdMode.DIRECT);
            Date tomorrow = DateUtils.addDays(operday.getCurrentDate(), 1);

            deleteClonedAccount(bsaacidCt);
            deleteClonedAccount(bsaacidDt);

            cloneAccount(bsaacidCt, tomorrow);
            cloneAccount(bsaacidDt, tomorrow);

            updateAccount(bsaacidCt, operday.getCurrentDate(), operday.getCurrentDate());
            updateAccount(bsaacidDt, operday.getCurrentDate(), operday.getCurrentDate());

            long stamp = System.currentTimeMillis();

            EtlPackage pkg = newPackage(stamp, "SIMPLE");
            Assert.assertTrue(pkg.getId() > 0);

            EtlPosting pst = newPosting(stamp, pkg);
            pst.setValueDate(getOperday().getLastWorkingDay());

            pst.setAccountCredit(bsaacidCt);
            pst.setAccountDebit(bsaacidDt);
            pst.setAmountCredit(new BigDecimal("12.0056"));
            pst.setAmountDebit(pst.getAmountCredit());
            pst.setCurrencyCredit(BankCurrency.AUD);
            pst.setCurrencyDebit(pst.getCurrencyCredit());
            pst.setSourcePosting(KondorPlus.getLabel());
            pst.setDealId("123");

            pst = (EtlPosting) baseEntityRepository.save(pst);

            GLOperation operation = (GLOperation) postingController.processMessage(pst);
            operation = (GLOperation) baseEntityRepository.findById(GLOperation.class, operation.getId());
            Assert.assertEquals(OperState.ERCHK, operation.getState());
        } finally {
            deleteClonedAccount(bsaacidCt);
            deleteClonedAccount(bsaacidDt);
        }

    }

    private void updateAccount(String bsaacid, Date openDate, Date dtoBefore) {
        baseEntityRepository.executeNativeUpdate("update gl_acc set dto = ?, dtc = null where bsaacid = ? and dto <= ?", openDate, bsaacid, dtoBefore);
    }

    private GLOperation createBackvalueOper(Date backdate, String bsaAcidCt, String bsaAcidDt) {
        EtlPosting pst = newPosting(System.currentTimeMillis(), (EtlPackage)baseEntityRepository.selectFirst(EtlPackage.class, "from EtlPackage p"));
        pst.setValueDate(getOperday().getLastWorkingDay());
        pst.setAccountCredit(bsaAcidCt);
        pst.setAccountDebit(bsaAcidDt);
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setAePostingId("PSTID_" + StringUtils.rsubstr(System.currentTimeMillis() + "", 5));

        pst = (EtlPosting) baseEntityRepository.save(pst);
        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        operation = (GLOperation) baseEntityRepository
                .selectFirst(GLOperation.class, "from GLOperation o where o.aePostingId = ?1", pst.getAePostingId());
        Assert.assertNotNull(operation);
        Assert.assertEquals(OperState.POST, operation.getState());
        return operation;
    }

    private GLPd getDebit(GLOperation operation) {
        List<GLPd> pdList = getGLPostings(operation);
        Assert.assertTrue(pdList.size() > 0);
        return pdList.stream().filter(p -> p.getAmountBC() < 0).findFirst().orElseThrow(() -> new RuntimeException("debit not found"));
    }

    private GLPd getCredit(GLOperation operation) {
        List<GLPd> pdList = getGLPostings(operation);
        Assert.assertTrue(pdList.size() > 0);
        return pdList.stream().filter(p -> p.getAmountBC() > 0).findFirst().orElseThrow(() -> new RuntimeException("credit not found"));
    }

    private BackvalueJournal getBackvalueJournal(GLPd glpd) {
        return (BackvalueJournal) baseEntityRepository.findById(BackvalueJournal.class,
                new BackvalueJournalId(glpd.getAcid(), glpd.getBsaAcid(), glpd.getPod()));
    }

    private void cloneAccount(String bsaacid, Date openCloseDt) {
        baseEntityRepository.executeNativeUpdate("insert into gl_acc (ID, BSAACID, CBCC, CBCCN, BRANCH, CCY, CUSTNO, ACCTYPE\n" +
                "        , CBCUSTTYPE, TERM, GL_SEQ, ACC2, PLCODE, ACOD, SQ\n" +
                "        , ACID, PSAV, DEALSRS, DEALID, SUBDEALID, DESCRIPTION, DTO, DTC, DTR, DTM, OPENTYPE, GLOID\n" +
                "        , GLO_DC, RLNTYPE, REV_CCY, REV_FL, ACID_DWH, TRANSACTSRC, BASE_REF_NO, SUBCCY)\n" +
                "select GL_SEQ_ACC.nextval, BSAACID, CBCC, CBCCN, BRANCH, CCY, CUSTNO, ACCTYPE\n" +
                "        , CBCUSTTYPE, TERM, GL_SEQ, ACC2, PLCODE, ACOD, SQ\n" +
                "        , ACID, PSAV, DEALSRS, DEALID, SUBDEALID, DESCRIPTION, ?, ?, DTR, DTM, OPENTYPE, GLOID\n" +
                "        , GLO_DC, RLNTYPE, REV_CCY, REV_FL, ACID_DWH, TRANSACTSRC, BASE_REF_NO, SUBCCY\n" +
                "from gl_acc where bsaacid = ?", openCloseDt, openCloseDt, bsaacid);
    }

    private void deleteClonedAccount(String bsaacid) {
        log.info("deleted = " + baseEntityRepository.executeNativeUpdate("delete from gl_acc where bsaacid = ? and dtc is not null and dto is not null", bsaacid));
    }

}
