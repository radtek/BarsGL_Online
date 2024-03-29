package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.PreCobStepController;
import ru.rbt.barsgl.ejb.controller.operday.task.EtlStructureMonitorTask;
import ru.rbt.barsgl.ejb.controller.operday.task.ProcessFlexFanTask;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.flx.FanNdsPosting;
import ru.rbt.barsgl.ejb.entity.flx.NdsPosting;
import ru.rbt.barsgl.ejb.entity.flx.TransitNdsReference;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.mapping.YesNo;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by Ivan Sevastyanov on 23.11.2016.
 */
public class FanNdsPostingIT extends AbstractRemoteIT {

    private static final Logger log = Logger.getLogger(FanNdsPostingIT.class.getName());

    private static final BigDecimal oneHundred = new BigDecimal(100);

    @Test
    public void testOk() throws Exception {
        baseEntityRepository.executeNativeUpdate("delete from GL_NDS");
        baseEntityRepository.executeNativeUpdate("insert into GL_NDS (ID, TAX, DTB, DTE) values (1, 18, to_date('2001-01-01' , 'YYYY-MM-DD'), to_date('2018-12-31' , 'YYYY-MM-DD'))");
        baseEntityRepository.executeNativeUpdate("insert into GL_NDS (ID, TAX, DTB, DTE) values (2, 20, to_date('2019-01-01' , 'YYYY-MM-DD'), null)");
        Date workday = DateUtils.parseDate("25.02.2017", "dd.MM.yyyy");
        Date operday = DateUtils.parseDate("26.02.2017", "dd.MM.yyyy");
        testOneDay(operday, workday, new BigDecimal(18));
        workday = DateUtils.parseDate("25.02.2019", "dd.MM.yyyy");
        operday = DateUtils.parseDate("26.02.2019", "dd.MM.yyyy");
        testOneDay(operday, workday, new BigDecimal(20));
    }

    @Test
    public void testNewYear() throws Exception {
        baseEntityRepository.executeNativeUpdate("delete from GL_NDS");
        baseEntityRepository.executeNativeUpdate("insert into GL_NDS (ID, TAX, DTB, DTE) values (1, 18, to_date('2001-01-01' , 'YYYY-MM-DD'), to_date('2018-12-31' , 'YYYY-MM-DD'))");
        baseEntityRepository.executeNativeUpdate("insert into GL_NDS (ID, TAX, DTB, DTE) values (2, 20, to_date('2019-01-01' , 'YYYY-MM-DD'), null)");
        Date workday = DateUtils.parseDate("31.12.2018", "dd.MM.yyyy");
        Date operday = DateUtils.parseDate("01.01.2019", "dd.MM.yyyy");
        testOneDay(operday, workday, new BigDecimal(18));
        workday = DateUtils.parseDate("01.01.2019", "dd.MM.yyyy");
        operday = DateUtils.parseDate("02.01.2019", "dd.MM.yyyy");
        testOneDay(operday, workday, new BigDecimal(20));
    }

    @Test
    public void testNoNds() throws Exception {
        baseEntityRepository.executeNativeUpdate("delete from GL_NDS");
        Date workday = DateUtils.parseDate("25.02.2017", "dd.MM.yyyy");
        Date operday = DateUtils.parseDate("26.02.2017", "dd.MM.yyyy");
        try {
            testOneDay(operday, workday, new BigDecimal(18));
            Assert.assertFalse(true);
        } catch (Throwable e) {
            System.out.println(e.getMessage());
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testTwoNds() throws Exception {
        baseEntityRepository.executeNativeUpdate("delete from GL_NDS");
        baseEntityRepository.executeNativeUpdate("insert into GL_NDS (ID, TAX, DTB, DTE) values (1, 18, to_date('2001-01-01' , 'YYYY-MM-DD'), to_date('2019-01-01' , 'YYYY-MM-DD') - 1)");
        baseEntityRepository.executeNativeUpdate("insert into GL_NDS (ID, TAX, DTB, DTE) values (2, 20, to_date('2019-01-01' , 'YYYY-MM-DD'), null)");
        Date workday = DateUtils.parseDate("25.02.2019", "dd.MM.yyyy");
        Date operday = DateUtils.parseDate("26.02.2019", "dd.MM.yyyy");
        try {
            testOneDay(operday, workday, new BigDecimal(18));
            Assert.assertFalse(true);
        } catch (Throwable e) {
            System.out.println(e.getMessage());
            Assert.assertTrue(true);
        }
    }

    private void testOneDay(Date operday, Date workday, BigDecimal rateNds) throws Exception {

        setOperday(operday, workday, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);

        try{
            baseEntityRepository.executeNativeUpdate("update gl_etlpkg set state = 'ERROR' where state = 'LOADED'");

            baseEntityRepository.executeNativeUpdate("update workday set workday = ?", workday);
            emulateWorkprocStep(workday, "MI3GL");

            TransitNdsReference reference = createTransitAccount();
            Assert.assertNotNull(reference);

            String transAcid = baseEntityRepository.selectFirst("select acid from gl_acc where bsaacid = ?"
                    , reference.getTransitAccount()).getString(0);
            Assert.assertTrue(!StringUtils.isEmpty(transAcid));

            Pd pd = createPd(workday, reference.getTransitAccount(), transAcid, rateNds);
            Assert.assertNotNull(pd);

            jobService.executeJob(SingleActionJobBuilder.create().withClass(ProcessFlexFanTask.class).build());

            List<NdsPosting> postings = baseEntityRepository.select(NdsPosting.class, "from NdsPosting p where p.id = ?1", pd.getId());
            Assert.assertEquals(1, postings.size());

            List<FanNdsPosting> drafts = baseEntityRepository.select(FanNdsPosting.class, "from FanNdsPosting p where p.evtId = ?1", pd.getId());
            Assert.assertEquals(2, drafts.size());
            Assert.assertTrue(drafts.stream().allMatch(r -> r.getProcessed() == YesNo.Y));

            List<EtlPosting> psts = baseEntityRepository.select(EtlPosting.class, "from EtlPosting p where p.eventId like ?1", pd.getId()+"%");
            Assert.assertEquals(2, psts.size());

            // проверяем размер НДС
            int indNds = (psts.get(0).getEventId().equals(psts.get(0).getPaymentRefernce())) ? 0 : 1;
            Assert.assertEquals("Неверно рассчитана сумма НДС", rateNds, psts.get(indNds).getAmountCredit());
            Assert.assertEquals("Неверно рассчитана сумма комиссии без НДС", oneHundred, psts.get(indNds ^ 1).getAmountCredit());

            jobService.executeJob(SingleActionJobBuilder.create().withClass(EtlStructureMonitorTask.class).build());

            psts = baseEntityRepository.select(EtlPosting.class, "from EtlPosting p where p.eventId like ?1", pd.getId()+"%");
            Assert.assertTrue(psts.stream().allMatch(r -> 0 == r.getErrorCode()));

            List<GLOperation> opers = baseEntityRepository.select(GLOperation.class, "from GLOperation o where o.eventId like ?1", pd.getId()+"%");
            Assert.assertTrue(opers.stream().map(o-> o.getId() + ":" + o.getState()).collect(Collectors.joining("!"))
                    , opers.stream().allMatch(r -> r.getState() == OperState.LOAD));

            remoteAccess.invoke(PreCobStepController.class, "processFan");
            opers = baseEntityRepository.select(GLOperation.class, "from GLOperation o where o.eventId like ?1", pd.getId()+"%");
            Assert.assertTrue(opers.stream().allMatch(r -> r.getState() == OperState.POST));
        }finally{
            // удаляем все - не должно падать
            log.info("deleted: " + baseEntityRepository.executeNativeUpdate("delete from pst where pbr like '@@IF%' and pod = ?", workday));
            jobService.executeJob(SingleActionJobBuilder.create().withClass(ProcessFlexFanTask.class).build());
        }
    }

    private TransitNdsReference createTransitAccount() throws SQLException {
        String transitAccount = findBsaAccount("47422810%");
        String profitAccount = findBsaAccount("70601810199021111296");
        String ndsAccount = findBsaAccount("60309810%");

        TransitNdsReference ref = (TransitNdsReference) baseEntityRepository.selectFirst(TransitNdsReference.class, "from TransitNdsReference t where t.transitAccount = ?1"
                , transitAccount);
        if (null == ref) {
            ref = new TransitNdsReference(transitAccount, profitAccount, ndsAccount, "1");
            ref = (TransitNdsReference) baseEntityRepository.save(ref);
        }
        return ref;
    }

    private Pd createPd(Date operday, String transBsaacid, String transAcid, BigDecimal rateNds) throws SQLException {
        long id = baseEntityRepository.selectFirst("select PD_SEQ.nextval id from DUAL").getLong(0);
        BigDecimal amount = oneHundred.add(rateNds).movePointRight(2);   // сумма комиссии с НДС в копейках
        baseEntityRepository.executeNativeUpdate("insert into pst (id,pod,vald,acid,bsaacid,ccy,amnt,amntbc,pbr,pnar, rnarlng,docn, pref) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", id, operday, operday, transAcid, transBsaacid,"RUR", amount, amount, "@@IF123", "1234", "12345", "", id+"ref");

//        baseEntityRepository.executeNativeUpdate("insert into pdext2 (id, rnarlng, docn) values (?, ?, ?)", id, "12345", "");
//                "values (?, ?, ?)", id, "12345", ru.rbt.ejbcore.util.StringUtils.rsubstr(System.currentTimeMillis()+"", 5));
//        baseEntityRepository.executeNativeUpdate("insert into pdext (id,pref) values (?,?)", id, id+"ref");
//        baseEntityRepository.executeNativeUpdate("insert into pdext3 (id) values (?)", id);
//        baseEntityRepository.executeNativeUpdate("insert into pdext5 (id) values (?)", id);

        return (Pd) baseEntityRepository.selectFirst(Pd.class, "from Pd d where d.id = ?1", id);
    }


}
