package ru.rbt.barsgl.ejbtest;

import com.google.common.collect.Iterables;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.ReconcilationTask;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static ru.rbt.barsgl.ejbtest.utl.Utl4Tests.getPds;

/**
 * Created by Ivan Sevastyanov
 * Формирование реконсиляционного отчета
 * @fsd
 */
public class ReconciliationTest extends AbstractRemoteTest {

    private enum Pbr {
        GL_PH("@@GL-PH"), PH("PH");

        private String src;

        Pbr(String src) {
            this.src = src;
        }

        public String getSource() {
            return src;
        }
    }

    private static final Logger log = Logger.getLogger(ReconciliationTest.class);

    @Before
    public void before() {
        baseEntityRepository.executeNativeUpdate("update gl_oper set curdate = curdate - 1 day where curdate = ?", getOperday().getCurrentDate());
        baseEntityRepository.executeNativeUpdate("update pd set pod = pod - 1 day where pod = ? and pbr = ? and invisible = '1'"
                , getOperday().getCurrentDate(), Pbr.PH.getSource());
    }

    /**
     * Формирование реконсиляционного отчета при формировании простых проводок
     * @fsd
     * @throws SQLException
     */
    @Test public void testSimple() throws SQLException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "Проверка реконсиляции");

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());
        pst.setEventId(pst.getEventId());

        pst.setAccountCredit("40817036200012959997");
        pst.setAccountDebit("40817036250010000018");
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);

        List<Pd> pds = getPds(baseEntityRepository, operation);
        Assert.assertEquals(2, pds.size());
        setPbr(pds);

        List<Pd> mdPds = createPhPds(pds);

        remoteAccess.invoke(ReconcilationTask.class, "execute", getOperday().getCurrentDate());

        List<DataRecord> recsAe = getRecsAe();
        Assert.assertEquals(2, recsAe.size());

        List<DataRecord> recsMd = getRecsMd();
        Assert.assertEquals(2, recsMd.size());

        Assert.assertTrue(Iterables.all(recsAe, predicate -> "Y".equals(predicate.getString("rec_yn"))));
        Assert.assertTrue(Iterables.all(recsMd, predicate -> null != predicate.getLong("rec_pdid")));

        baseEntityRepository.executeNativeUpdate("delete from pd where id = ?", mdPds.get(0).getId());
        baseEntityRepository.executeNativeUpdate("delete from gl_recpdmd where reg_date = ?", getOperday().getCurrentDate());
        baseEntityRepository.executeNativeUpdate("delete from gl_recpdae where reg_date = ?", getOperday().getCurrentDate());

        remoteAccess.invoke(ReconcilationTask.class, "execute", getOperday().getCurrentDate());
        recsAe = baseEntityRepository.select("select * from gl_recpdae");
        Assert.assertEquals(2, recsAe.size());

        recsMd = baseEntityRepository.select("select * from gl_recpdmd");
        Assert.assertEquals(1, recsMd.size());

        Assert.assertFalse(Iterables.all(recsAe, predicate -> "Y".equals(predicate.getString("rec_yn"))));
        Assert.assertTrue(Iterables.all(recsMd, predicate -> null != predicate.getLong("rec_pdid")));

    }

    /**
     * Формирование реконсиляционного отчета при формировании веерных проводок
     * @fsd
     * @throws SQLException
     */
    @Test public void testFan() throws SQLException {
        List<Pd> pdsAe = createFanPds();
        setPbr(pdsAe);
        List<Pd> pdsMd = createPhPds(pdsAe);

        remoteAccess.invoke(ReconcilationTask.class, "execute", getOperday().getCurrentDate());

        List<DataRecord> recsAe = getRecsAe();
        Assert.assertTrue(0 < pdsAe.size());
        Assert.assertEquals(pdsAe.size(), recsAe.size());

        List<DataRecord> recsMd = getRecsMd();
        Assert.assertTrue(0 < pdsMd.size());
        Assert.assertEquals(pdsMd.size(), recsMd.size());

        Assert.assertTrue(Iterables.all(recsAe, predicate -> "Y".equals(predicate.getString("fan_yn")) && "N".equals(predicate.getString("mfo_yn"))));
        Assert.assertTrue(Iterables.all(recsAe, predicate -> "Y".equals(predicate.getString("rec_yn"))));
        Assert.assertTrue(Iterables.all(recsMd, predicate -> null != predicate.getLong("rec_pdid")));
    }

    /**
     * Формирование реконсиляционного отчета при формировании межфилиальных проводок
     * @fsd
     * @throws SQLException
     */
    @Test public void testMfo() throws SQLException {

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "MFO");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

//        pst.setAccountCredit("40817978550160000066");     // "CHL" клиент
        pst.setAccountCredit("47411978750020010096");       // "SPB" не клиент
        pst.setAccountDebit("47427978400404502369");        // "EKB" клиент

        pst.setAmountCredit(new BigDecimal("321.56"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.EUR);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);

        List<Pd> pdsAe = getPds(baseEntityRepository, operation);
        setPbr(pdsAe);
        List<Pd> pdsMd = createPhPds(pdsAe);

        remoteAccess.invoke(ReconcilationTask.class, "execute", getOperday().getCurrentDate());

        List<DataRecord> recsAe = baseEntityRepository.select("select * from gl_recpdae where bsaacid like '3030%'");
        Assert.assertTrue(0 < pdsAe.size());

        List<DataRecord> recsMd = baseEntityRepository.select("select * from gl_recpdmd where bsaacid like '3030%'");
        Assert.assertTrue(0 < pdsMd.size());

        //выверка не прошла
        Assert.assertTrue(Iterables.all(recsAe, predicate -> "Y".equals(predicate.getString("mfo_yn")) && "N".equals(predicate.getString("fan_yn"))));
        Assert.assertTrue(Iterables.all(recsAe, predicate -> "N".equals(predicate.getString("rec_yn"))));
        Assert.assertFalse(Iterables.all(recsMd, predicate -> null != predicate.getLong("rec_pdid")));
    }

    private List<Pd> createFanPds() {
        final long st = System.currentTimeMillis();
        EtlPackage etlPackage = newPackageNotSaved(st, "Checking fan posting logic");
        etlPackage.setAccountCnt(2);                        // число перьев в веере
        baseEntityRepository.save(etlPackage);

        EtlPosting pst = newPosting(st, etlPackage);
        pst.setValueDate(getOperday().getCurrentDate());
        pst.setFan(YesNo.Y);
        String parentRef = pst.getPaymentRefernce();
        pst.setParentReference(parentRef);

        pst.setAccountDebit("40806810700010000465");        // MOS, RUB
        pst.setAccountCredit("40702810100013995679");       // MOS, RUB
        pst.setAmountCredit(new BigDecimal("1100.010"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setCurrencyDebit(pst.getCurrencyCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());
        Assert.assertEquals(YesNo.Y, operation.getFan());
        Assert.assertEquals(pst.getParentReference(), operation.getParentReference());

        pst = newFanPosting(System.currentTimeMillis(), pst, "C", new BigDecimal("1200.020"),
                "40702810900010002613", null, null);
        pst = (EtlPosting) baseEntityRepository.save(pst);

        operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());
        Assert.assertEquals(YesNo.Y, operation.getFan());
        Assert.assertEquals(pst.getParentReference(), operation.getParentReference());

        pst = newFanPosting(System.currentTimeMillis(), pst, "C", new BigDecimal("1300.030"),
                "40702810900010002613", null, null);
        pst = (EtlPosting) baseEntityRepository.save(pst);

        operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertTrue(0 < operation.getId());
        Assert.assertEquals(YesNo.Y, operation.getFan());
        Assert.assertEquals(pst.getParentReference(), operation.getParentReference());

        // processing


        List<GLOperation> operList = fanPostingController.processOperations(parentRef);
        Assert.assertTrue(!operList.isEmpty());

        List<Pd> allPds = new ArrayList<>();
        for (GLOperation fanOper : operList) {
            allPds.addAll(getPds(baseEntityRepository, fanOper));
        }
        return allPds;
    }

    private List<Pd> createPhPds(List<Pd> pds) {
        List<Pd> result = new ArrayList<>();
        for (Pd pd : pds) {
            long id = baseEntityRepository.nextId("PD_SEQ");
            pd.setId(id);
            pd.setPbr(Pbr.PH.getSource());
            pd.setInvisible("1");
            pd.setPcId(0L);
            result.add((Pd) baseEntityRepository.save(pd));
        }
        return result;
    }

    private void setPbr(List<Pd> pds) {
        for (Pd pd : pds) {
            log.info("pcid = " + pd.getPcId());
            baseEntityRepository.executeNativeUpdate("update pd p set p.pbr = ?1 where p.id = ?2", Pbr.GL_PH.getSource(), pd.getId());
        }
    }

    private List<DataRecord> getRecsAe() throws SQLException {
        return  baseEntityRepository.select("select * from gl_recpdae");
    }

    private List<DataRecord> getRecsMd() throws SQLException {
        return baseEntityRepository.select("select * from gl_recpdmd");
    }
}
