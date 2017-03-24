package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.stamt.*;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.StamtUnloadParam;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejb.repository.PdRepository;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.ejbtesting.ServerTestingFacade;
import ru.rbt.barsgl.shared.enums.OperState;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.CLOSED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.*;
import static ru.rbt.barsgl.ejb.controller.operday.task.stamt.StamtUnloadController.STAMT_UNLOAD_FULL_DATE_KEY;
import static ru.rbt.barsgl.ejb.controller.operday.task.stamt.UnloadStamtParams.BALANCE_DELTA_INCR;
import static ru.rbt.barsgl.ejb.controller.operday.task.stamt.UnloadStamtParams.FULL_POSTING;
import static ru.rbt.barsgl.ejb.entity.acc.AccountKeysBuilder.create;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;
import static ru.rbt.barsgl.ejbtest.utl.Utl4Tests.findGlAccount;
import static ru.rbt.barsgl.ejbtest.utl.Utl4Tests.getPds;
import static ru.rbt.barsgl.shared.enums.StamtUnloadParamType.B;
import static ru.rbt.barsgl.shared.enums.StamtUnloadParamTypeCheck.INCLUDE;

/**
 * Created by ER18837 on 19.03.15.
 * Выгрузка данных о мемориальных ордерах в STAMT
 * @fsd 8.1
 */
public class StamtUnloadTest extends AbstractTimerJobTest {

    private static String UNLOAD_STAMTD_PARAM_NAME = UnloadStamtParams.DELTA_POSTING.getParamName();
    private static String UNLOAD_STAMTD_PARAM_DESC = UnloadStamtParams.DELTA_POSTING.getParamDesc();

    public static final Logger log = Logger.getLogger(StamtUnloadTest.class.getName());

    @Before
    public  void init() {
        updateOperday(COB,CLOSED);
        baseEntityRepository.executeNativeUpdate("delete from GL_ETLSTMS");
    }


    /**
     * Выгрузка полная (проводки, созданные в текущий оперднень с текущей датой)
     * @fsd 8.1.3
     * @throws Exception
     */
    @Test public void testFull() throws Exception {

        String operday = "26.02.2015";

        // настройка для включения проводки по балансовику
        GLOperation operation = getOneOper(DateUtils.parseDate(operday, "dd.MM.yyyy"));
        incldeBs2ByOperation(operation);

        baseEntityRepository.executeNativeUpdate("delete from GL_ETLSTMS");
        long id0 = Optional.ofNullable(baseEntityRepository
                .selectFirst("select max(ID) mx from GL_ETLSTMS where PARNAME = ?"
                        , FULL_POSTING.getParamName())).map(r -> r.getLong(0)).orElse(0L);
        Assert.assertEquals(0L, id0);

        Properties properties = new Properties();
        properties.load(new StringReader(STAMT_UNLOAD_FULL_DATE_KEY + "=" + operday));
        remoteAccess.invoke(StamtUnloadFullTask.class, "run", "", properties);

        Assert.assertTrue(1 <= baseEntityRepository.selectOne("select count(1) cnt from GL_ETLSTM").getInteger("cnt"));

        DataRecord rec = baseEntityRepository.selectFirst("select max(ID) from GL_ETLSTMS where PARNAME = ?", FULL_POSTING.getParamName());
        long id1 = (null == rec || null == rec.getLong(0)) ? 0 : rec.getLong(0);
        Assert.assertTrue(id0 < id1);
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED.getFlag()
                , baseEntityRepository.selectOne("select parvalue from GL_ETLSTMS where id = ?", id1).getString("parvalue"));

        remoteAccess.invoke(StamtUnloadFullTask.class, "run", "", properties);
        rec = baseEntityRepository.selectFirst("select max(ID) from GL_ETLSTMS where PARNAME = ?", FULL_POSTING.getParamName());
        long id2 = (null == rec || null == rec.getLong(0)) ? 0 : rec.getLong(0);
        Assert.assertEquals(id1, id2);

    }

    private void incldeBs2ByOperation(GLOperation operation) throws SQLException {
        baseEntityRepository.executeNativeUpdate("delete from GL_STMPARM");
        List<Pd> pds = Utl4Tests.getPds(baseEntityRepository, operation);
        Assert.assertEquals(2, pds.size());
        for (Pd pd : pds) {
            String acc = pd.getBsaAcid().substring(0,5);
            if (baseEntityRepository.selectFirst("select count(1) cnt from gl_stmparm where account = ?", acc).getLong("cnt") == 0) {
                baseEntityRepository.executeNativeUpdate("insert into gl_stmparm (account, INCLUDE,acctype, INCLUDEBLN) values (?, '1', 'B', '1')", acc);
            }
        }
    }

    private void fixOperAccDates(GLOperation o) {
        for (Pd pd : getPds(baseEntityRepository, o)) {
            baseEntityRepository.executeNativeUpdate("update gl_acc set dto = ?, dtc = null where bsaacid = ?", getOperday().getLastWorkingDay(), pd.getBsaAcid());
        }
    }

    /**
     * проверка работы настроек из GL_STMPARM
     */
    @Test
    public void testStmParm() throws Exception {
        String operday = "26.02.2015";
        Date unloadDate = DateUtils.parseDate(operday, "dd.MM.yyyy");
        baseEntityRepository.executeNativeUpdate("delete from GL_STMPARM");
        baseEntityRepository.executeNativeUpdate("update gl_oper set procdate = ?", DateUtils.addDays(unloadDate, 10));
        GLOperation operation = getOneOper(unloadDate);


        cleanHeader();
        unloadStamtFull(unloadDate);
        Assert.assertTrue(0 == baseEntityRepository.selectOne("select count(1) cnt from GL_ETLSTM").getInteger("cnt"));
        // включаем балансовик
        cleanHeader();
        List<Pd> pds = Utl4Tests.getPds(baseEntityRepository, operation);
        Assert.assertEquals(2, pds.size());
        baseEntityRepository.executeNativeUpdate("insert into gl_stmparm (account, INCLUDE,acctype,INCLUDEBLN) values (?, '1', 'B','1')"
                , pds.get(0).getBsaAcid().substring(0,5));
        unloadStamtFull(unloadDate);
        Assert.assertTrue(1 == baseEntityRepository.selectOne("select count(1) cnt from GL_ETLSTM").getInteger("cnt"));
        // исключаем счет 1
        cleanHeader();
        baseEntityRepository.executeNativeUpdate("insert into gl_stmparm (account, INCLUDE,acctype,INCLUDEBLN) values (?, '0', 'A','1')"
                , pds.get(0).getBsaAcid());
        unloadStamtFull(unloadDate);
        Assert.assertTrue(1 == baseEntityRepository.selectOne("select count(1) cnt from GL_ETLSTM").getInteger("cnt"));
        // исключаем счет 2
        cleanHeader();
        baseEntityRepository.executeNativeUpdate("insert into gl_stmparm (account, INCLUDE,acctype,INCLUDEBLN) values (?, '0', 'A','1')"
                , pds.get(1).getBsaAcid());
        unloadStamtFull(unloadDate);
        Assert.assertTrue(0 == baseEntityRepository.selectOne("select count(1) cnt from GL_ETLSTM").getInteger("cnt"));
    }

    @Test public void testCheckRun() throws IOException {
        baseEntityRepository.executeNativeUpdate("delete from GL_ETLSTMS");

        Properties properties = new Properties();
        properties.load(new StringReader("checkRun=false"));

        updateOperday(ONLINE, CLOSED);
        Assert.assertTrue(remoteAccess.invoke(StamtUnloadFullTask.class, "checkRun", properties));

        properties = new Properties();
        Assert.assertTrue(remoteAccess.invoke(StamtUnloadFullTask.class, "checkRun", properties));

        updateOperday(PRE_COB, CLOSED);
        Assert.assertFalse(remoteAccess.invoke(StamtUnloadFullTask.class, "checkRun", properties));

        properties = new Properties();
        updateOperday(COB, CLOSED);
        Assert.assertTrue(remoteAccess.invoke(StamtUnloadFullTask.class, "checkRun", properties));

    }

    /**
     * Выгрузка дельта (проводки, созданные в текущий оперднень с прошедшей датой)
     * @fsd 8.1.4
     * @throws Exception
     */
    @Test public void testDelta() throws Exception {
        String operday = "26.02.2015";
        Date unloadDate = DateUtils.parseDate(operday, "dd.MM.yyyy");

        Date backdate = DateUtils.addDays(unloadDate, -2);
        setOperday(unloadDate, backdate, ONLINE, OPEN);
        GLOperation operation = getOneOperBackdate(getOperday());
        incldeBs2ByOperation(operation);

        baseEntityRepository.executeNativeUpdate("delete from GL_ETLSTMS where PARNAME = ? and PARDESC = ?",
                UNLOAD_STAMTD_PARAM_NAME, UNLOAD_STAMTD_PARAM_DESC);

        baseEntityRepository.executeNativeUpdate("delete from GL_ETLSTMD");
        DataRecord rec = baseEntityRepository.selectFirst("select max(ID) from GL_ETLSTMS where PARNAME = ?", UNLOAD_STAMTD_PARAM_NAME);
        long id0 = (null == rec || null == rec.getLong(0)) ? 0 : rec.getLong(0);
        Assert.assertEquals(0L, id0);

        setOperday(unloadDate, backdate, COB, CLOSED);
        unloadStamtDelta(unloadDate);
        rec = baseEntityRepository.selectFirst("select max(ID) from GL_ETLSTMS where PARNAME = ?", UNLOAD_STAMTD_PARAM_NAME);
        long id1 = (null == rec || null == rec.getLong(0)) ? 0 : rec.getLong(0);
        Assert.assertTrue(id0 < id1);
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED.getFlag()
                , baseEntityRepository.selectOne("select parvalue from GL_ETLSTMS where id = ?", id1).getString("parvalue"));
        Assert.assertTrue(1 <= baseEntityRepository.selectOne("select count(1) cnt from GL_ETLSTMD").getInteger("cnt"));

        unloadStamtDelta(unloadDate);
        rec = baseEntityRepository.selectFirst("select max(ID) from GL_ETLSTMS where PARNAME = ?", UNLOAD_STAMTD_PARAM_NAME);
        long id2 = (null == rec || null == rec.getLong(0)) ? 0 : rec.getLong(0);
        Assert.assertEquals(id1, id2);
    }

    /**
     * корректность мапинга справочника/дельта
     */
    @Test
    public void testStamtUnloadParm() {
        final String account = "28347";
        baseEntityRepository.executeUpdate("delete from StamtUnloadParam p where p.account = ?1", account);
        StamtUnloadParam param = new StamtUnloadParam(account, B, INCLUDE,INCLUDE);
        baseEntityRepository.save(param);
        Assert.assertNotNull(baseEntityRepository.findById(StamtUnloadParam.class, param.getId()));
    }

    /**
     * выгрузка остатков с STAMT полная
     * @throws Exception
     */
    @Test
    public void testStamtUnloadBalanceFull() throws Exception {
        log.info(TimeZone.getDefault().getDisplayName());
        Date operdate = DateUtils.parseDate("2015-02-26", "yyyy-MM-dd");
        Date backdate = DateUtils.addDays(operdate, -2);

        setOperday(operdate, backdate, ONLINE, OPEN);

        // операции в текущем дне
        GLOperation operationCurdate = createOperWithGLAccCredit(operdate);
        incldeBs2ByOperation(operationCurdate);
        fixOperAccDates(operationCurdate);

        baseEntityRepository.executeNativeUpdate("delete from GL_ETLSTMS");
        updateOperday(COB,CLOSED);
        unloadStamtBalanceFull(operdate);
        checkAllBalanceSucceded();
        Assert.assertTrue(baseEntityRepository.select("select * from GL_BALSTM").stream()
                .anyMatch(p -> ((DataRecord)p).getString("cbaccount").equals(operationCurdate.getAccountCredit())));

        DataRecord record = getLastUnloadHeader(UnloadStamtParams.BALANCE_FULL);
        unloadStamtBalanceFull(operdate);
        DataRecord record2 = getLastUnloadHeader(UnloadStamtParams.BALANCE_FULL);
        Assert.assertEquals(record.getLong(0), record2.getLong(0));

        backdate = operdate;
        operdate = DateUtils.addDays(operdate, 1);
        setOperday(operdate, backdate, ONLINE, OPEN);

        remoteAccess.invoke(StamtUnloadBalanceTask.class, "run", "", new Properties());
        DataRecord record3 = getLastUnloadHeader(UnloadStamtParams.BALANCE_FULL);
        Assert.assertEquals(record.getLong(0), record3.getLong(0));

        setOperday(operdate, backdate, PRE_COB, OPEN);
        remoteAccess.invoke(StamtUnloadBalanceTask.class, "run", "", new Properties());
        DataRecord record4 = getLastUnloadHeader(UnloadStamtParams.BALANCE_FULL);
        Assert.assertEquals(record.getLong(0), record4.getLong(0));
    }

    @Test public void testUnloadBackvalueIncrement() throws Exception {

        baseEntityRepository.executeNativeUpdate("update gl_oper set postdate = vdate, procdate = procdate - 10 day");
        baseEntityRepository.executeNativeUpdate("delete from gl_etlstms");
        baseEntityRepository.executeNativeUpdate("delete from gl_etlstmd");
        baseEntityRepository.executeNativeUpdate("delete from gl_etlstma");

        // одна проводка в текущем дне
        String operday = "26.02.2015";
        Date unloadDate = DateUtils.parseDate(operday, "dd.MM.yyyy");
        Date backdate = DateUtils.addDays(unloadDate, -2);
        setOperday(unloadDate, backdate, ONLINE, OPEN);

        GLOperation operation = getOneOperBackdate(getOperday());
        long pcid = getPcid(operation);
        log.info("pcid=" + pcid);
        incldeBs2ByOperation(operation);

        // выгрузка в текущем открытом дне - одна проводка

        unloadBackvalueIncrement();

        // проверяем хедер
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED.getFlag()
                , getLastUnloadHeader(BALANCE_DELTA_INCR).getString("parvalue"));

        // одна проводка в текущем дне
        List<DataRecord> bvrecs = baseEntityRepository.select("select * from gl_etlstmd");
        Assert.assertTrue("" + bvrecs.size(),1 <= bvrecs.size());
        List<DataRecord> filtered = bvrecs.stream().filter(r -> r.getLong("pcid") == pcid).collect(Collectors.toList());
        Assert.assertEquals(1, filtered.size());
        Assert.assertTrue(baseEntityRepository.select("select * from GL_BALSTMD").stream()
                .anyMatch(p -> ((DataRecord)p).getString("cbaccount").equals(operation.getAccountCredit())));
        Assert.assertTrue(baseEntityRepository.select("select * from GL_BALSTMD").stream()
                .anyMatch(p -> ((DataRecord)p).getString("cbaccount").equals(operation.getAccountDebit())));


        baseEntityRepository.executeNativeUpdate("update gl_etlstms set parvalue = '2'");

        // выгрузка в текущем открытом дне - одна проводка
        GLOperation operation2 = getOneOperBackdate(getOperday());
        long pcid2 = getPcid(operation2);;
        log.info("pcid2=" + pcid2);
        unloadBackvalueIncrement();

        // проверяем хедер
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED.getFlag()
                , getLastUnloadHeader(BALANCE_DELTA_INCR).getString("parvalue"));

        // должна быть выгружена только вторая проводка
        bvrecs = baseEntityRepository.select("select * from gl_etlstmd");
        Assert.assertEquals(1, bvrecs.size());
        filtered = bvrecs.stream().filter(r -> r.getLong("pcid") == pcid2).collect(Collectors.toList());
        Assert.assertEquals(1, filtered.size());

        // первая операция уже в исключеных
        List<DataRecord> excl = baseEntityRepository.select("select * from gl_etlstma");
        Assert.assertTrue(1 <= excl.size());
        Assert.assertEquals(1, excl.stream().filter(r -> r.getLong("pcid") == pcid).count());

        baseEntityRepository.executeNativeUpdate("update gl_etlstms set parvalue = '2'");

        // выгрузка в текущем открытом дне - одна проводка
        GLOperation operation3 = getOneOperBackdate(getOperday());
        long pcid3 = getPcid(operation3);;
        log.info("pcid3=" + pcid3);
        unloadBackvalueIncrement();

        // проверяем хедер
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED.getFlag()
                , getLastUnloadHeader(BALANCE_DELTA_INCR).getString("parvalue"));

        // должна быть выгружена только вторая проводка
        bvrecs = baseEntityRepository.select("select * from gl_etlstmd");
        Assert.assertEquals(1, bvrecs.size());
        filtered = bvrecs.stream().filter(r -> r.getLong("pcid") == pcid3).collect(Collectors.toList());
        Assert.assertEquals(1, filtered.size());

        // первая и вторая операция уже в исключеных
        excl = baseEntityRepository.select("select * from gl_etlstma");
        Assert.assertEquals(2, excl.size());
        Assert.assertEquals(1, excl.stream().filter(r -> r.getLong("pcid") == pcid).count());
        Assert.assertEquals(1, excl.stream().filter(r -> r.getLong("pcid") == pcid2).count());


        // одна проводка в текущем дне
        // полная выгрузка backvalue после закрытия дня
        GLOperation operation4 = getOneOperBackdate(getOperday());
        long pcid4 = getPcid(operation4);;
        log.info("pcid4=" + pcid4);

        setOperday(unloadDate, backdate, COB, CLOSED);

        // стандартная ночная выгрузка бэквалуе
        jobService.executeJob(SingleActionJobBuilder.create().withClass(StamtUnloadDeltaTask.class).build());

        // проверяем хедер
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED.getFlag()
                , getLastUnloadHeader(UnloadStamtParams.DELTA_POSTING).getString("parvalue"));

        // должна быть выгружена только одна посл проводка
        bvrecs = baseEntityRepository.select("select * from gl_etlstmd");
        Assert.assertEquals(1, bvrecs.size());
        filtered = bvrecs.stream().filter(r -> r.getLong("pcid") == pcid4).collect(Collectors.toList());
        Assert.assertEquals(1, filtered.size());

        Assert.assertEquals(0, baseEntityRepository.selectOne("select count(1) cnt from gl_etlstma").getInteger(0).intValue());
    }

    /**
     * выгрузка остатков с STAMT полная
     * @throws Exception
     */
    @Test
    public void testStamtUnloadBalanceStep2() throws Exception {
        log.info(TimeZone.getDefault().getDisplayName());
        Date operdate = DateUtils.parseDate("2015-02-26", "yyyy-MM-dd");
        Date backdate = DateUtils.addDays(operdate, -2);

        setOperday(operdate, backdate, ONLINE, OPEN);

        // операции в текущем дне
        GLOperation operationCurdate = createOperWithGLAccCredit(operdate);
        incldeBs2ByOperation(operationCurdate);
        fixOperAccDates(operationCurdate);

        // изменяем операцию чтоб она попала в GL_BALSTMD
        baseEntityRepository.executeNativeUpdate("delete from GL_ETLSTMS");
        baseEntityRepository.executeNativeUpdate(
                "update pd d set d.pod = ? " +
                        "where d.pcid in (select d0.pcid from pd d0 where d0.pcid in (\n" +
                        "select p.pcid from gl_oper o, gl_posting p, pd d \n" +
                        "where o.gloid = p.glo_ref and o.gloid = ? and d.pcid = p.pcid))"
                , backdate, operationCurdate.getId());
        setOperday(operdate, backdate, COB, CLOSED);
        checkCreateStep("P14", operdate, "O");
        unloadStamtBalanceDelta(operdate);
        DataRecord record = getLastUnloadHeader(UnloadStamtParams.BALANCE_DELTA);
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED.getFlag(), record.getString("parvalue"));
        Assert.assertTrue(baseEntityRepository.select("select * from GL_BALSTMD").stream()
                .anyMatch(p -> ((DataRecord)p).getString("cbaccount").equals(operationCurdate.getAccountDebit())));
        // повторная выгрузка не производился
        unloadStamtBalanceDelta(operdate);
        DataRecord record2 = getLastUnloadHeader(UnloadStamtParams.BALANCE_DELTA);
        Assert.assertEquals(record.getLong(0), record2.getLong(0));
    }

    /**
     * выгрузка в STAMT STEP3
     */
    @Test
    public void testStamtAfterFlex() throws Exception {
        Date operdate = DateUtils.parseDate("2015-02-26", "yyyy-MM-dd");
        Date backdate = DateUtils.addDays(operdate, -2);

        baseEntityRepository.executeNativeUpdate("delete from GL_BALSTMD");

        setOperday(operdate, backdate, ONLINE, OPEN);

        baseEntityRepository.executeNativeUpdate("delete from gl_sched_h where sched_name = ?1"
                , StamtUnloadBalanceFlexTask.class.getSimpleName());

        checkCreateStep("MI3GL", operdate, "O");

        DataRecord header = getLastUnloadHeader(UnloadStamtParams.BALANCE_DELTA_FLEX);
        Assert.assertNull(header);

        List<DataRecord> accs = baseEntityRepository
                .select("select * from gl_acc ac where ac.dto <= ? and value(ac.dtc, ?) >= ? fetch first 2 rows only"
                        , operdate, operdate, operdate);

        Assert.assertEquals(2, accs.size());

        updateOperday(COB, CLOSED);
        initFlexPd(operdate, "30424810820010000096", accs.get(1).getString("bsaacid"));

        unloadStamtBalanceFlex(operdate);

        DataRecord record = baseEntityRepository.selectFirst("select * from gl_sched_h where sched_name = ?"
                , StamtUnloadBalanceFlexTask.class.getSimpleName());
        Assert.assertNotNull(record);
        Assert.assertEquals("1", record.getString("SCHRSLT"));

        header = getLastUnloadHeader(UnloadStamtParams.BALANCE_DELTA_FLEX);
        Assert.assertNotNull(header);
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED.getFlag(), header.getString("parvalue"));

//        Assert.assertTrue(0 < baseEntityRepository.selectFirst("select count(1) cnt from GL_BALSTMD").getInteger(0));

        unloadStamtBalanceFlex(operdate);
        DataRecord header2 = getLastUnloadHeader(UnloadStamtParams.BALANCE_DELTA_FLEX);
        Assert.assertEquals(header.getLong(0), header2.getLong(0));
    }

    /**
     * выгрузка в СТАМТ технических проводок по исправлению красного сальдо
     */
    @Test public void testTechover() throws Exception {

        baseEntityRepository.executeNativeUpdate("delete from gl_etlstmd");
        baseEntityRepository.executeNativeUpdate("delete from gl_stmparm");
        baseEntityRepository.executeNativeUpdate("delete from gl_pdjover");
        baseEntityRepository.executeNativeUpdate("delete from gl_balstmd");
        baseEntityRepository.executeNativeUpdate("delete from gl_etlstma");

        // регистр проводки для выгрузки
        Date operdate = DateUtils.parseDate("2015-02-26", "yyyy-MM-dd");
        Date lwdate = DateUtils.addDays(operdate, -1);
        setOperday(operdate, lwdate, ONLINE, OPEN);

        List<DataRecord> pds = baseEntityRepository.select("select d.* from pd d, pcid_mo m, bsaacc b where d.bsaacid = b.id and d.pcid = m.pcid fetch first 2 rows only");
        Assert.assertEquals(2, pds.size());

        String bsaacid1 = pds.get(0).getString("bsaacid");
        String bsaacid2 = pds.get(0).getString("bsaacid");

        registerForStamtUnliad(bsaacid1);
        registerForStamtUnliad(bsaacid2);

        long pcid = remoteAccess.invoke(PdRepository.class, "getNextId");

        // два раза чтоб отработал триггер по старому значению PCID
        baseEntityRepository.executeNativeUpdate("update pd set bsaacid = ?, pcid = ?, amntbc = -10, amnt = -10, pbr = '@@IBR' where id = ?", bsaacid1, pcid, pds.get(0).getLong("id"));
        baseEntityRepository.executeNativeUpdate("update pd set bsaacid = ?, pcid = ?, amntbc = 10, amnt = 10, pbr = '@@IBR' where id = ?", bsaacid2, pcid, pds.get(1).getLong("id"));
        baseEntityRepository.executeNativeUpdate("update pd set bsaacid = ?, pcid = ?, amntbc = -10, amnt = -10, pbr = '@@IBR' where id = ?", bsaacid1, pcid, pds.get(0).getLong("id"));
        baseEntityRepository.executeNativeUpdate("update pd set bsaacid = ?, pcid = ?, amntbc = 10, amnt = 10, pbr = '@@IBR' where id = ?", bsaacid2, pcid, pds.get(1).getLong("id"));

        DataRecord pcidMo = baseEntityRepository.selectFirst("select * from pcid_mo fetch first 1 rows only");
        Assert.assertNotNull(pcidMo);

        baseEntityRepository.executeNativeUpdate("update pcid_mo set pcid = ? where pcid = ?", pcid, pcidMo.getLong("pcid"));

        checkCreateStep("MI4GL", lwdate, "O");

        SingleActionJob job = SingleActionJobBuilder.create().withClass(StamtUnloadTechoverTask.class).build();
        jobService.executeJob(job);
        DataRecord header = getLastUnloadHeader(UnloadStamtParams.BALANCE_TECHOVER);
        Assert.assertNotNull(header);
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED.getFlag(), header.getString("parvalue"));

        header = getLastUnloadHeader(UnloadStamtParams.POSTING_TECHOVER);
        Assert.assertNotNull(header);
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED.getFlag(), header.getString("parvalue"));

        List<DataRecord> records = baseEntityRepository.select("select * from gl_etlstmd");
        Assert.assertEquals(1, records.size());
        Assert.assertEquals(bsaacid1, records.get(0).getString("dcbaccount"));
        Assert.assertEquals(bsaacid2, records.get(0).getString("ccbaccount"));
        jobService.executeJob(job);
        DataRecord header2 = getLastUnloadHeader(UnloadStamtParams.POSTING_TECHOVER);
        Assert.assertEquals(header.getLong("id"), header2.getLong("id"));
    }

    private void registerForStamtUnliad(String bsaacid) {
        try {
            baseEntityRepository.executeNativeUpdate("insert into gl_stmparm (account, INCLUDE,acctype,INCLUDEBLN) values (?, '1', 'B','1')"
                    , bsaacid.substring(0,5));
        } catch (Exception e) {
            log.log(Level.SEVERE, "ошибка вставки в параметры", e);
        }
    }

    private void initFlexPd(Date operday, String accDt, String accCt) throws SQLException {
        log.info("accDt=" + accDt);
        log.info("accCt=" + accCt);
        baseEntityRepository.executeNativeUpdate("delete from gl_stmparm");
        GLOperation operation = getOneOper(operday);
        List<Pd> pds = Utl4Tests.getPds(baseEntityRepository, operation);
        for (Pd pd : pds) {
            registerForStamtUnliad(pd.getBsaAcid());

        }
        try {
            DataRecord record = baseEntityRepository.selectFirst("select * from gl_acc where bsaacid = ?", accDt);
            //Assert.assertNotNull(record);
            if (null != record) {
                log.info("acctype=" + record.getString("acctype"));
                baseEntityRepository.executeNativeUpdate("insert into GL_BALACC values (?)", record.getString("acctype"));
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "ошибка вставки GL_BALACC", e);
        }
        log.info("pdid1=" + pds.get(0).getId());
        log.info("pdid2=" + pds.get(1).getId());
        baseEntityRepository.executeNativeUpdate("update pd p set p.pbr = '@@IF1', bsaacid = ?1, pod = ?3 where p.id = ?2", accDt, pds.get(0).getId(), operday);
        baseEntityRepository.executeNativeUpdate("update pd p set p.pbr = '@@IF1', bsaacid = ?1, pod = ?3 where p.id = ?2", accCt, pds.get(1).getId(), operday);
    }

    private void checkAllBalanceSucceded() throws SQLException {
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED.getFlag(), getLastUnloadHeader(UnloadStamtParams.BALANCE_FULL).getString("parvalue"));
    }

    private DataRecord getLastUnloadHeader(UnloadStamtParams params) throws SQLException {
        return Optional.ofNullable(baseEntityRepository
                .selectFirst("select * from gl_etlstms where parname = ? and pardesc = ? order by id desc"
                        , params.getParamName(), params.getParamDesc())).orElse(null);
    }

    /**
     * Операция с датой проводки в текущей дате
     * @param operday
     * @return
     * @throws SQLException
     */
    private GLOperation getOneOper(Date operday) throws SQLException {
        List<DataRecord> records = baseEntityRepository.selectMaxRows(
                "select o.* from gl_oper o where exists (select 1 from gl_posting ps, pd pd " +
                        " where ps.pcid = pd.pcid and pd.invisible <> '1' and o.gloid = ps.glo_ref) order by 1 desc", 1, new Object[]{});
        GLOperation operation = (GLOperation) baseEntityRepository.findById(GLOperation.class, records.get(0).getLong("gloid"));
        Assert.assertNotNull(operation);
        baseEntityRepository.executeNativeUpdate("update gl_oper o set o.postDate = '"
                        + Utl4Tests.toString(operday, "yyyy-MM-dd") +"', o.procDate = '" + Utl4Tests.toString(operday, "yyyy-MM-dd") + "' where o.gloid = ?1"
                , operation.getId());
        return (GLOperation) baseEntityRepository.findById(GLOperation.class, operation.getId());
    }

    /**
     * Операция с датой проводки в текущей дате со счетом в GL_ACC
     * @param operday
     * @return
     * @throws SQLException
     */
    private GLOperation getOneOperAcc(Date operday) throws SQLException {
        List<DataRecord> records = baseEntityRepository.selectMaxRows(
                "select o.* from gl_oper o where exists (select 1 from gl_posting ps, pd pd " +
                        " where ps.pcid = pd.pcid and pd.invisible <> '1' and o.gloid = ps.glo_ref) order by 1 desc", 1, new Object[]{});
        GLOperation operation = (GLOperation) baseEntityRepository.findById(GLOperation.class, records.get(0).getLong("gloid"));
        Assert.assertNotNull(operation);
        baseEntityRepository.executeUpdate("update GLOperation o set o.postDate = ?1, o.procDate = ?1 where o.id = ?2", operday, operation.getId());
        return (GLOperation) baseEntityRepository.findById(GLOperation.class, operation.getId());
    }

    /**
     * Операция с датой проводки в прошлой дате
     * @param operday
     * @return
     * @throws SQLException
     */
    private GLOperation getOneOperBackdate(Operday operday) throws SQLException {
        GLOperation operation = createOperation();
        Assert.assertNotNull(operation);
        baseEntityRepository.executeUpdate("update GLOperation o set o.postDate = '"
                        + Utl4Tests.toString(operday.getLastWorkingDay(), "yyyy-MM-dd") + "', o.procDate = '"
                        + Utl4Tests.toString(operday.getCurrentDate(), "yyyy-MM-dd") + "' where o.id = ?1"
                , operation.getId());
        baseEntityRepository.executeNativeUpdate(
                "update pd d set d.pod = ?, d.vald = ? " +
                        "where d.pcid = (select pcid from gl_oper o, gl_posting p where o.gloid = p.glo_ref and o.gloid = ?)"
                , operday.getLastWorkingDay(), operday.getCurrentDate(), operation.getId());
        return (GLOperation) baseEntityRepository.findById(GLOperation.class, operation.getId());
    }

    private void cleanHeader() {
        baseEntityRepository.executeNativeUpdate("delete from GL_ETLSTMS");
    }

    private void unloadStamtFull(Date operday) throws Exception {
        Properties properties = new Properties();
        properties.load(new StringReader(STAMT_UNLOAD_FULL_DATE_KEY + "=" + new SimpleDateFormat("dd.MM.yyyy").format(operday)));
        remoteAccess.invoke(StamtUnloadFullTask.class, "run", "", properties);
    }

    private void unloadStamtDelta(Date operday) throws Exception {
        Properties properties = new Properties();
        properties.load(new StringReader(STAMT_UNLOAD_FULL_DATE_KEY + "=" + new SimpleDateFormat("dd.MM.yyyy").format(operday)));
        remoteAccess.invoke(StamtUnloadDeltaTask.class, "run", "", properties);
    }

    private void unloadStamtBalanceFull(Date operday) throws Exception {
        Properties properties = new Properties();
        properties.load(new StringReader(STAMT_UNLOAD_FULL_DATE_KEY + "=" + new SimpleDateFormat("dd.MM.yyyy").format(operday)));
        remoteAccess.invoke(StamtUnloadBalanceTask.class, "run", "", properties);
    }

    private void unloadStamtBalanceDelta(Date operday) throws Exception {
        Properties properties = new Properties();
        properties.load(new StringReader(STAMT_UNLOAD_FULL_DATE_KEY + "=" + new SimpleDateFormat("dd.MM.yyyy").format(operday)));
        remoteAccess.invoke(StamtUnloadBalanceStep2Task.class, "run", "", properties);
    }

    private void unloadStamtBalanceFlex(Date operday) throws Exception {
        Properties properties = new Properties();
//        properties.load(new StringReader(STAMT_UNLOAD_FULL_DATE_KEY + "=" + new SimpleDateFormat("dd.MM.yyyy").format(operday)));
        remoteAccess.invoke(StamtUnloadBalanceFlexTask.class, "run", StamtUnloadBalanceFlexTask.class.getSimpleName(), properties);
    }

    /**
     * открывается счет по операции, т.е. движения по счету кредит будут и счет будет открыт
     * @param valueDate
     * @return
     * @throws SQLException
     */
    private GLOperation createOperWithGLAccCredit(Date valueDate) throws SQLException {

        final AccountKeys acCt
                = create()
                .withBranch("001").withCurrency(RUB.getCurrencyCode()).withCustomerNumber("01584414")
                .withAccountType("131060102").withCustomerType("00").withTerm("00")
                .withGlSequence("123457").withAcc2("30424").withAccountCode("1049").withAccSequence("02")
                .build();

        GLAccount acc0 = remoteAccess.invoke(ServerTestingFacade.class, "findGLAccountAEnoLock", acCt);
        if (null != acc0) {
            Date dtc = DateUtils.addDays(valueDate, 1);
            baseEntityRepository.executeNativeUpdate("update BSAACC set BSAACO = ?, BSAACC = ? where id = ?"
                    , valueDate, dtc, acc0.getBsaAcid());
            baseEntityRepository.executeNativeUpdate("update gl_acc set dto = ?, dtc = ? where bsaacid = ?"
                    , valueDate, dtc, acc0.getBsaAcid());
        }

        updateOperday(ONLINE,OPEN);
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "MIDAS");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(valueDate);

        pst.setCurrencyCredit(new BankCurrency(acCt.getCurrency()));
        pst.setCurrencyDebit(RUB);

        pst.setAccountCredit("");
        pst.setAccountDebit(Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "47408810%40"));

        pst.setAccountKeyCredit(acCt.toString());

        pst.setAmountCredit(new BigDecimal("13.99"));
        pst.setAmountDebit(pst.getAmountCredit());

        pst = (EtlPosting) baseEntityRepository.save(pst);

//        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        GLOperation operation = remoteAccess.invoke(ServerTestingFacade.class, "processEtlPosting", pst.getId());

        Assert.assertNotNull(operation);
        Assert.assertNotNull(operation.getId());
        Assert.assertTrue(0 < operation.getId());

        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertTrue(!isEmpty(operation.getAccountKeyCredit()));
        Assert.assertEquals(OperState.POST, operation.getState());

        GLAccount result = findGlAccount(baseEntityRepository, acCt);
        Assert.assertNotNull(result);
        return operation;
    }

    private void unloadBackvalueIncrement() throws Exception {
        jobService.executeJob(SingleActionJobBuilder.create().withClass(StamtUnloadPstIncrementTask.class).build());
    }

    private EtlPosting createSimple(long stamp, EtlPackage pkg, String accCredit, String accDebit) {
        EtlPosting pst = newPosting(stamp + 1, pkg);
        pst.setAccountCredit(accCredit);
        pst.setAccountDebit(accDebit);
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setValueDate(getOperday().getCurrentDate());
        pst.setSourcePosting("K+TP");
        return (EtlPosting) baseEntityRepository.save(pst);
    }

    public GLOperation createOperation() throws SQLException {
        Long stamp = System.currentTimeMillis();
        EtlPackage pkg = newPackageNotSaved(stamp, "Тестовый пакет " + stamp);
        pkg.setDateLoad(getSystemDateTime());
        pkg.setPackageState(EtlPackage.PackageState.INPROGRESS);
        pkg.setAccountCnt(0);
        pkg.setPostingCnt(4);
        pkg = (EtlPackage) baseEntityRepository.save(pkg);

/*
        final String accCredit = "40817036200012959997";
        final String accDebit = "40817036250010000018";
*/
        List<DataRecord> bsaacids = baseEntityRepository.selectMaxRows("select * from accrln where bsaacid like '40817%' and length(acid) > 0 and ? between drlno and drlnc", 2, new Object[]{getOperday().getCurrentDate()});
        final String accCredit = bsaacids.get(0).getString("bsaacid");
        final String accDebit = bsaacids.get(1).getString("bsaacid");

        int cnt = baseEntityRepository.executeNativeUpdate("delete from GL_BSACCLK where BSAACID = ?", accCredit);
        log.info("cnt deleted cr: " + cnt);
        cnt = baseEntityRepository.executeNativeUpdate("delete from GL_BSACCLK where BSAACID = ?", accDebit);
        log.info("cnt deleted dt: " + cnt);

        EtlPosting pst1 = createSimple(stamp, pkg, accCredit, accDebit);
        GLOperation operation = (GLOperation) postingController.processMessage(pst1);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        return operation;
    }

    private long getPcid(GLOperation operation) throws SQLException {
        return baseEntityRepository.selectFirst("select pcid from gl_oper o, gl_posting p where o.gloid = p.glo_ref and o.gloid = ?"
                , operation.getId()).getLong("pcid");
    }

}
