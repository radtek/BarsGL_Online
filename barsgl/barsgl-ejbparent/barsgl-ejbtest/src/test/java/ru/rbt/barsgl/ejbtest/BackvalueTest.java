package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.BackvalueJournalController;
import ru.rbt.barsgl.ejb.controller.operday.task.EtlStructureMonitorTask;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.*;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.barsgl.shared.enums.OperState;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import static ru.rbt.barsgl.ejb.entity.etl.EtlPackage.PackageState.LOADED;

/**
 * Created by Ivan Sevastyanov
 * Пересчет баланса для проводок с прошедшей датой
 */
public class BackvalueTest extends AbstractTimerJobTest {

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
    public void testBackValue() throws ParseException {

        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN, Operday.PdMode.DIRECT);

        final String bsaAcidCt = "40817036200012959997";
        final String bsaAcidDt = "40817036250010000018";

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
    public void testBackValueBuffer() throws ParseException {

        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN, Operday.PdMode.BUFFER);

        final String bsaAcidCt = "40817036200012959997";
        final String bsaAcidDt = "40817036250010000018";

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
        remoteAccess.invoke(BackvalueJournalController.class, "recalculateBS2");

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

}
