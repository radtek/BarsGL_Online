package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.stamt.*;
import ru.rbt.barsgl.ejb.entity.acc.AccRlnId;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.StamtUnloadParam;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejb.repository.BankCurrencyRepository;
import ru.rbt.barsgl.ejb.repository.BackvalueJournalRepository;
import ru.rbt.barsgl.ejb.repository.PdRepository;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.ejbtesting.ServerTestingFacade;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

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
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.CLOSED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.*;
import static ru.rbt.barsgl.ejb.controller.operday.task.stamt.StamtUnloadController.STAMT_UNLOAD_FULL_DATE_KEY;
import static ru.rbt.barsgl.ejb.controller.operday.task.stamt.UnloadStamtParams.*;
import static ru.rbt.barsgl.ejb.entity.acc.AccountKeysBuilder.create;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;
import static ru.rbt.barsgl.ejbtest.utl.Utl4Tests.*;
import static ru.rbt.barsgl.shared.enums.StamtUnloadParamType.B;
import static ru.rbt.barsgl.shared.enums.StamtUnloadParamTypeCheck.INCLUDE;

/**
 * Created by ER18837 on 19.03.15.
 * Выгрузка данных о мемориальных ордерах в STAMT
 * @fsd 8.1
 */
public class StamtUnloadIT extends AbstractTimerJobIT {

    private static String UNLOAD_STAMTD_PARAM_NAME = UnloadStamtParams.DELTA_POSTING.getParamName();
    private static String UNLOAD_STAMTD_PARAM_DESC = UnloadStamtParams.DELTA_POSTING.getParamDesc();

    public static final Logger log = Logger.getLogger(StamtUnloadIT.class.getName());

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
        baseEntityRepository.executeNativeUpdate("insert into gl_stmparm (account, INCLUDE,acctype,INCLUDEBLN) values (?, '1', 'B','0')"
                , pds.get(0).getBsaAcid().substring(0,5));
        unloadStamtFull(unloadDate);
        Assert.assertTrue(1 == baseEntityRepository.selectOne("select count(1) cnt from GL_ETLSTM").getInteger("cnt"));
        // исключаем счет 1 , вкл счет 2
        cleanHeader();
        baseEntityRepository.executeNativeUpdate("insert into gl_stmparm (account, INCLUDE,acctype,INCLUDEBLN) values (?, '0', 'A','0')"
                , pds.get(0).getBsaAcid());
        baseEntityRepository.executeNativeUpdate("insert into gl_stmparm (account, INCLUDE,acctype,INCLUDEBLN) values (?, '1', 'A','0')"
                , pds.get(1).getBsaAcid());
        unloadStamtFull(unloadDate);
        Assert.assertTrue("" + baseEntityRepository.selectOne("select count(1) cnt from GL_ETLSTM").getInteger("cnt")
                , 1 == baseEntityRepository.selectOne("select count(1) cnt from GL_ETLSTM").getInteger("cnt"));
        // исключаем два счета
        cleanHeader();
        baseEntityRepository.executeNativeUpdate("delete from gl_stmparm");
        baseEntityRepository.executeNativeUpdate("insert into gl_stmparm (account, INCLUDE,acctype,INCLUDEBLN) values (?, '0', 'A','0')"
                , pds.get(0).getBsaAcid());
        baseEntityRepository.executeNativeUpdate("insert into gl_stmparm (account, INCLUDE,acctype,INCLUDEBLN) values (?, '0', 'A','0')"
                , pds.get(1).getBsaAcid());
        unloadStamtFull(unloadDate);
        Assert.assertTrue("" + baseEntityRepository.selectOne("select count(1) cnt from GL_ETLSTM").getInteger("cnt")
                ,0 == baseEntityRepository.selectOne("select count(1) cnt from GL_ETLSTM").getInteger("cnt"));
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

        baseEntityRepository.executeNativeUpdate("update gl_oper set postdate = vdate, procdate = procdate - 10");
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
        Assert.assertTrue(2 <= excl.size());
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

        baseEntityRepository.executeNativeUpdate("delete from gl_sched_h where sched_name = ?"
                , StamtUnloadBalanceFlexTask.class.getSimpleName());

        checkCreateStep("MI3GL", operdate, "O");

        DataRecord header = getLastUnloadHeader(UnloadStamtParams.BALANCE_DELTA_FLEX);
        Assert.assertNull(header);

        List<DataRecord> accs = baseEntityRepository
                .select("select * from gl_acc ac where ac.dto <= ? and nvl(ac.dtc, ?) >= ? and rownum <= 2"
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

        List<DataRecord> pds = baseEntityRepository.select("select d.* from pd d, pcid_mo m, bsaacc b where d.bsaacid = b.id and d.pcid = m.pcid and rownum <= 4");
        Assert.assertEquals(4, pds.size());

        String bsaacid1 = pds.get(0).getString("bsaacid");
        String bsaacid2 = pds.get(1).getString("bsaacid");
        String bsaacid3 = pds.get(0).getString("bsaacid");
        String bsaacid4 = pds.get(1).getString("bsaacid");

        registerForStamtUnload(bsaacid1);
        registerForStamtUnload(bsaacid2);

        long pcid1 = remoteAccess.invoke(PdRepository.class, "getNextId");
        long pcid2 = remoteAccess.invoke(PdRepository.class, "getNextId");

        // два раза чтоб отработал триггер по старому значению PCID
        // проводки с мемордерами
        baseEntityRepository.executeNativeUpdate("update pd set bsaacid = ?, pcid = ?, amntbc = -10, amnt = -10, pbr = '@@IBR' where id = ?", bsaacid1, pcid1, pds.get(0).getLong("id"));
        baseEntityRepository.executeNativeUpdate("update pd set bsaacid = ?, pcid = ?, amntbc = 10, amnt = 10, pbr = '@@IBR' where id = ?", bsaacid2, pcid1, pds.get(1).getLong("id"));
        baseEntityRepository.executeNativeUpdate("update pd set bsaacid = ?, pcid = ?, amntbc = -10, amnt = -10, pbr = '@@IBR' where id = ?", bsaacid1, pcid1, pds.get(0).getLong("id"));
        baseEntityRepository.executeNativeUpdate("update pd set bsaacid = ?, pcid = ?, amntbc = 10, amnt = 10, pbr = '@@IBR' where id = ?", bsaacid2, pcid1, pds.get(1).getLong("id"));

        DataRecord pcidMo = baseEntityRepository.selectFirst("select * from pcid_mo where rownum < 2");
        Assert.assertNotNull(pcidMo);

        baseEntityRepository.executeNativeUpdate("update pcid_mo set pcid = ? where pcid = ?", pcid1, pcidMo.getLong("pcid"));

        // проводки без мемордеров
        baseEntityRepository.executeNativeUpdate("update pd set bsaacid = ?, pcid = ?, amntbc = -20, amnt = -20, pbr = '@@IBR' where id = ?", bsaacid3, pcid2, pds.get(2).getLong("id"));
        baseEntityRepository.executeNativeUpdate("update pd set bsaacid = ?, pcid = ?, amntbc = 20, amnt = 20, pbr = '@@IBR' where id = ?", bsaacid4, pcid2, pds.get(3).getLong("id"));
        baseEntityRepository.executeNativeUpdate("update pd set bsaacid = ?, pcid = ?, amntbc = -20, amnt = -20, pbr = '@@IBR' where id = ?", bsaacid3, pcid2, pds.get(2).getLong("id"));
        baseEntityRepository.executeNativeUpdate("update pd set bsaacid = ?, pcid = ?, amntbc = 20, amnt = 20, pbr = '@@IBR' where id = ?", bsaacid4, pcid2, pds.get(3).getLong("id"));

        long ts = System.currentTimeMillis();

        baseEntityRepository.executeNativeUpdate(String.format("update pcid_mo set mo_no = '@T%s' where pcid in (?)"
                , StringUtils.rsubstr(ts + "", 7)), pds.get(2).getLong("pcid"));
        baseEntityRepository.executeNativeUpdate(String.format("update pcid_mo set mo_no = '@T%s' where pcid in (?)"
                , StringUtils.rsubstr((ts+1) + "", 7)), pds.get(3).getLong("pcid"));
        baseEntityRepository.executeNativeUpdate("delete from pcid_mo where pcid in (?, ?)", pds.get(2).getLong("pcid"), pds.get(3).getLong("pcid"));

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
        Assert.assertEquals(2, records.size());
        Assert.assertEquals(bsaacid1, records.get(0).getString("dcbaccount"));
        Assert.assertEquals(bsaacid2, records.get(0).getString("ccbaccount"));
        Assert.assertEquals(pcid1 + "", records.get(0).getString("pcid"));
        Assert.assertEquals(pcid2 + "", records.get(1).getString("pcid"));

        // повторный запуск в текущем ОД не производится
        jobService.executeJob(job);
        DataRecord header2 = getLastUnloadHeader(UnloadStamtParams.POSTING_TECHOVER);
        Assert.assertEquals(header.getLong("id"), header2.getLong("id"));
    }

    @Test public void testDeleted() throws Exception {
        initCorrectOperday();
        updateOperday(ONLINE, OPEN);
        Date operday = getOperday().getCurrentDate();
        Date lwday = getOperday().getLastWorkingDay();
        log.info("operday = " + operday);
        baseEntityRepository.executeNativeUpdate("delete from GL_STMDEL");

        List<AccRlnId> rlnIds = findBsaacidRlns(baseEntityRepository, getOperday(), "40817%", 2);
        AccRlnId rlnId1 = rlnIds.get(0);
        AccRlnId rlnId2 = rlnIds.get(1);
        log.info("bsaacid1: " + rlnId1.getBsaAcid());
        log.info("bsaacid2: " + rlnId2.getBsaAcid());
        Assert.assertEquals(2, rlnIds.size());

        List<DataRecord> opers = baseEntityRepository.select("select gloid, p.pcid from gl_oper o, gl_posting p where o.gloid = p.glo_ref and rownum <= 2");
        Assert.assertEquals(2, opers.size());

        registerForStamtUnload(rlnId1.getBsaAcid());
        registerForStamtUnload(rlnId2.getBsaAcid());

        // ручное подавление - две и более записи по счету в gl_pdjchg
        // первая проводка
        long id1 = createPd(operday, rlnId1.getAcid(), rlnId1.getBsaAcid(), BankCurrency.EUR.getCurrencyCode()
                , "@@GL" + ru.rbt.ejbcore.util.StringUtils.rsubstr(System.currentTimeMillis() + "", 3), -100, -200);
        long id2 = createPd(operday, rlnId2.getAcid(), rlnId2.getBsaAcid(), BankCurrency.EUR.getCurrencyCode()
                , "@@GL" + ru.rbt.ejbcore.util.StringUtils.rsubstr(System.currentTimeMillis() + "", 3), 100, 200);
        long pcid1 = baseEntityRepository.nextId("PD_SEQ");
        baseEntityRepository.executeNativeUpdate("update pd set pcid = ? where id in (?, ?)", pcid1, id1, id2);
        baseEntityRepository.executeNativeUpdate("update gl_posting set pcid = ? where glo_ref = ?", pcid1, opers.get(0).getLong("gloid"));

        // вторая проводка
        long id1_1 = createPd(operday, rlnId1.getAcid(), rlnId1.getBsaAcid(), BankCurrency.EUR.getCurrencyCode()
                , "@@GL" + ru.rbt.ejbcore.util.StringUtils.rsubstr(System.currentTimeMillis() + "", 3), -100, -200);
        long id2_1 = createPd(operday, rlnId2.getAcid(), rlnId2.getBsaAcid(), BankCurrency.EUR.getCurrencyCode()
                , "@@GL" + ru.rbt.ejbcore.util.StringUtils.rsubstr(System.currentTimeMillis() + "", 3), 100, 200);
        long pcid1_1 = baseEntityRepository.nextId("PD_SEQ");
        baseEntityRepository.executeNativeUpdate("update pd set pcid = ? where id in (?, ?)", pcid1_1, id1_1, id2_1);
        baseEntityRepository.executeNativeUpdate("update gl_posting set pcid = ? where glo_ref = ?", pcid1_1, opers.get(1).getLong("gloid"));

        log.info("count gl_pdjchg deleted " + baseEntityRepository.executeNativeUpdate("delete from gl_pdjchg"));

        // проводки GL
        // подавление/удаление в БД
        Assert.assertEquals(1, baseEntityRepository.executeNativeUpdate("update pd set invisible = '1' where id = ?", id1));
        Assert.assertEquals(1, baseEntityRepository.executeNativeUpdate("update pd set invisible = '1' where id = ?", id2));
        Assert.assertEquals(2, baseEntityRepository.executeNativeUpdate("delete from pd where pcid = ?", pcid1_1));

        List<DataRecord> glpds = baseEntityRepository.select("select * from GL_PDJCHG where id in (?, ?, ?, ?)", id1, id2, id1_1, id2_1);
        Assert.assertEquals(4, glpds.size());
        Assert.assertTrue(glpds.stream().allMatch(r -> r.getLong("id") == id1 || r.getLong("id") == id2 || r.getLong("id") == id1_1 || (r.getLong("id") == id2_1)));

        // теховеры
        long id3 = createPd(operday, rlnId1.getAcid(), rlnId1.getBsaAcid(), BankCurrency.EUR.getCurrencyCode(), "@@IBR", -100, -200);
        long id4 = createPd(operday, rlnId2.getAcid(), rlnId2.getBsaAcid(), BankCurrency.EUR.getCurrencyCode(), "@@IBR", 100, 200);
        long pcid2 = baseEntityRepository.nextId("PD_SEQ");
        baseEntityRepository.executeNativeUpdate("update pd set pcid = ? where id in (?, ?)", pcid2, id3, id4);

        // теховеры вторая проводка
        long id1_2 = createPd(operday, rlnId1.getAcid(), rlnId1.getBsaAcid(), BankCurrency.EUR.getCurrencyCode(), "@@IBR", -100, -200);
        long id2_2 = createPd(operday, rlnId2.getAcid(), rlnId2.getBsaAcid(), BankCurrency.EUR.getCurrencyCode(), "@@IBR", 100, 200);
        long pcid2_1 = baseEntityRepository.nextId("PD_SEQ");
        baseEntityRepository.executeNativeUpdate("update pd set pcid = ? where id in (?, ?)", pcid2_1, id1_2, id2_2);

        log.info("count gl_pdjover deleted " + baseEntityRepository.executeNativeUpdate("delete from gl_pdjover"));
        Assert.assertEquals(1, baseEntityRepository.executeNativeUpdate("update pd set invisible = '1' where id = ?", id3));
        Assert.assertEquals(1, baseEntityRepository.executeNativeUpdate("update pd set invisible = '1' where id = ?", id4));
        Assert.assertEquals(2, baseEntityRepository.executeNativeUpdate("delete from pd where pcid = ?", pcid2_1));

        List<DataRecord> pdOver = baseEntityRepository.select("select * from gl_pdjover where idpd in (?, ?, ?, ?)", id3, id4, id1_2, id2_2);
        Assert.assertEquals(4, pdOver.size());
        Assert.assertTrue(pdOver.stream().allMatch(r -> r.getLong("idpd") == id3 || r.getLong("idpd") == id4 || r.getLong("idpd") == id1_2 || r.getLong("idpd") == id2_2));

        setOperday(DateUtils.addDays(operday, 1), operday, ONLINE, OPEN);

        // формирование выгрузки в STAMT
        SingleActionJob job = SingleActionJobBuilder.create().withClass(StamtUnloadDeletedTask.class)
                .withName(StamtUnloadDeletedTask.class.getSimpleName()).build();
        jobService.executeJob(job);

        // проверка заголовков
        DataRecord header1 = getLastUnloadHeader(POSTING_DELETE);
        Assert.assertNotNull(header1);
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED.getFlag(), header1.getString("parvalue"));

        List<DataRecord> unloads = baseEntityRepository.select("select * from GL_STMDEL");
        Assert.assertEquals(4, unloads.size());
        List<DataRecord> allUnloads = unloads.stream().filter(r -> r.getLong("pcid") == pcid1 || r.getLong("pcid") == pcid1_1
                || r.getLong("pcid") == pcid2 ||r.getLong("pcid") == pcid2_1).collect(Collectors.toList());
        Assert.assertEquals(4, allUnloads.size());

        // проверка - запуск один раз в день
        jobService.executeJob(job);
        DataRecord header2 = getLastUnloadHeader(POSTING_DELETE);
        Assert.assertEquals(header1.getLong("id"), header2.getLong("id"));

        // проверка - запуск после ошибки
        setHeaderStatus(header1.getLong("id"), DwhUnloadStatus.ERROR);
        JobHistory history = getLastHistory(job.getName());
        setHistoryStatus(history.getId(), DwhUnloadStatus.ERROR);

        // удаляем одну выгруженную
        Assert.assertTrue(1 == baseEntityRepository.executeNativeUpdate("delete from GL_STMDEL where pcid = ?", pcid1));
        setHeadersStatus(DwhUnloadStatus.CONSUMED);
        jobService.executeJob(job);
        DataRecord header3 = getLastUnloadHeader(POSTING_DELETE);
        Assert.assertFalse(Objects.equals(header1.getLong("id"), header3.getLong("id")));
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED.getFlag(), header3.getString("parvalue"));

        // инкрементальная последующая выгрузка
        Long pcidNew = createPostingUpdate(operday, rlnId1, rlnId2);
        jobService.executeJob(job);

        // нет выгрузки потому что необработана предыдущая
        DataRecord header4 = getLastUnloadHeader(POSTING_DELETE);
        Assert.assertTrue(Objects.equals(header3.getLong("id"), header4.getLong("id")));
        setHeadersStatus(DwhUnloadStatus.CONSUMED);

//        cleanHeader();
//        baseEntityRepository.executeNativeUpdate("delete from gl_sched_h where operday = ?", getOperday().getCurrentDate());

        // предыдущая выгрузка обработана, след должна пройти
        jobService.executeJob(job);

        header4 = getLastUnloadHeader(POSTING_DELETE);
        Assert.assertFalse(Objects.equals(header3.getLong("id"), header4.getLong("id")));
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED.getFlag(), header4.getString("parvalue"));

        unloads = baseEntityRepository.select("select * from GL_STMDEL");
        log.info("pcids: " + pcid1 + ":" + pcid1_1 + ":" + pcid2 + ":" + pcid2_1 + ":" + pcidNew);
        log.info("unloaded: " + unloads.stream().map(r->r.getLong("pcid").toString()).collect(Collectors.joining(":")));
        Assert.assertEquals(5, unloads.size());

        allUnloads = unloads.stream().filter(r -> r.getLong("pcid") == pcid1 || r.getLong("pcid") == pcid1_1
                || r.getLong("pcid") == pcid2 || r.getLong("pcid") == pcid2_1 || r.getLong("pcid") == pcidNew.longValue()).collect(Collectors.toList());
        Assert.assertEquals(allUnloads.stream().map(r->r.getLong("pcid").toString()).collect(Collectors.joining(":")), 5, allUnloads.size());

        // при выгрузке в следующем дне в первый раз проводки ранее выгруженные должны быть удалены
        setOperday(DateUtils.addDays(getOperday().getCurrentDate(), 1), DateUtils.addDays(getOperday().getLastWorkingDay(), 1), ONLINE, OPEN);
        setHeadersStatus(DwhUnloadStatus.CONSUMED);
        // должна быть первая выгрузка в текущем ОД
        baseEntityRepository.executeNativeUpdate("delete from gl_sched_h where operday = ?", getOperday().getCurrentDate());
        // удаляем одну выгруженную
        Assert.assertTrue(1 == baseEntityRepository.executeNativeUpdate("delete from GL_STMDEL where pcid = ?", pcidNew));

        // учищаем остатки GL_BALSTMD
        DataRecord balheader1 = getLastUnloadHeader(BALANCE_DELTA);
        Assert.assertNotNull(balheader1);
        baseEntityRepository.executeNativeUpdate("delete from GL_BALSTMD");
        jobService.executeJob(job);

        DataRecord header5 = getLastUnloadHeader(POSTING_DELETE);
        Assert.assertFalse(Objects.equals(header4.getLong("id"), header5.getLong("id")));
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED.getFlag(), header5.getString("parvalue"));

        unloads = baseEntityRepository.select("select * from GL_STMDEL");
        // осталась одна в прошлой дате
        Assert.assertEquals(1, unloads.size());

        unloads = baseEntityRepository.select("select * from GL_BALSTMD");
        Assert.assertTrue(2 <= unloads.size());
        Assert.assertTrue(rlnId1.getBsaAcid(), unloads.stream().anyMatch(r -> (r.getString("CBACCOUNT").equals(rlnId1.getBsaAcid())
                || r.getString("CBACCOUNT").equals(rlnId2.getBsaAcid()))));

        // заголовок по остаткам
        DataRecord balheader2 = getLastUnloadHeader(BALANCE_DELTA);
        Assert.assertFalse(Objects.equals(balheader1.getLong("id"), balheader2.getLong("id")));
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED.getFlag(), balheader2.getString("parvalue"));

        // повторную выгрузку не производим, если нет невыгруженных удаленных проводок
        setHeadersStatus(DwhUnloadStatus.CONSUMED);
        jobService.executeJob(job);
        DataRecord header6 = getLastUnloadHeader(POSTING_DELETE);
        Assert.assertTrue(Objects.equals(header5.getLong("id"), header6.getLong("id")));
    }

    @Test
    public void testRegisterChanged() throws SQLException {

        baseEntityRepository.executeNativeUpdate("delete from GL_PDJCHG");

        Date operday = getOperday().getCurrentDate();
        List<AccRlnId> rlnIds = findBsaacidRlns(baseEntityRepository, getOperday(), "40817%", 2);
        AccRlnId rlnId1 = rlnIds.get(0);
        AccRlnId rlnId2 = rlnIds.get(1);
        Long pcid = createPostingUpdate(operday, rlnId1, rlnId2);

        List<Pd> pds = (List<Pd>) baseEntityRepository.select(Pd.class, "from Pd d where d.pcId = ?1", pcid);

//        List<Pd> pds = (List<Pd>) baseEntityRepository.findNative(Pd.class, "select * from Pd d where d.pcId = ?1", 2, pcid);
        Assert.assertEquals(2, pds.size());

        for (Pd pd : pds) {
            remoteAccess.invoke(BackvalueJournalRepository.class, "registerChanged", pd);
        }
        List<DataRecord> glpds = baseEntityRepository.select("select * from GL_PDJCHG where pcid = ?", pcid);

        Assert.assertEquals(2, glpds.size());

    }

    private void registerForStamtUnload(String bsaacid) {
        try {
            baseEntityRepository.executeNativeUpdate("insert into gl_stmparm (account, INCLUDE,acctype,INCLUDEBLN) values (?, '1', 'B','1')"
                    , bsaacid.substring(0,5));
        } catch (Exception e) {
            log.log(Level.WARNING, "ошибка вставки в параметры", e.getMessage());
        }
    }

    private void initFlexPd(Date operday, String accDt, String accCt) throws SQLException {
        log.info("accDt=" + accDt);
        log.info("accCt=" + accCt);
        baseEntityRepository.executeNativeUpdate("delete from gl_stmparm");
        GLOperation operation = getOneOper(operday);
        List<Pd> pds = Utl4Tests.getPds(baseEntityRepository, operation);
        for (Pd pd : pds) {
            registerForStamtUnload(pd.getBsaAcid());

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
        baseEntityRepository.executeNativeUpdate("update pd p set p.pbr = '@@IF1', bsaacid = ?, pod = ? where p.id = ?", accDt, operday, pds.get(0).getId());
        baseEntityRepository.executeNativeUpdate("update pd p set p.pbr = '@@IF1', bsaacid = ?, pod = ? where p.id = ?", accCt, operday, pds.get(1).getId());
    }

    private void checkAllBalanceSucceded() throws SQLException {
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED.getFlag(), getLastUnloadHeader(UnloadStamtParams.BALANCE_FULL).getString("parvalue"));
    }

    protected static DataRecord getLastUnloadHeader(UnloadStamtParams params) throws SQLException {
        return Optional.ofNullable(baseEntityRepository
                .selectFirst("select * from gl_etlstms where parname = ? and pardesc = ? order by id desc"
                        , params.getParamName(), params.getParamDesc())).orElse(null);
    }

    private static void setHeaderStatus(long headerId, DwhUnloadStatus status) throws SQLException {
        Assert.assertEquals(1, baseEntityRepository.executeNativeUpdate("update gl_etlstms set parvalue = ? where id = ? "
                        , status.getFlag(), headerId));
    }

    private static void setHeadersStatus(DwhUnloadStatus status) throws SQLException {
        baseEntityRepository.executeNativeUpdate("update gl_etlstms set parvalue = ? ", status.getFlag());
    }


    private static void setHistoryStatus(long headerId, DwhUnloadStatus status) {
        baseEntityRepository.executeNativeUpdate("update gl_sched_h set SCHRSLT = ? where id_hist = ?"
            , status.getFlag(), headerId);
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
        baseEntityRepository.executeNativeUpdate("update gl_oper o set o.postDate = ?, o.procDate = ? where o.gloid = ?"
                , operday, operday, operation.getId());
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

    private EtlPosting createSimple(long stamp, EtlPackage pkg, String accCredit, String accDebit) throws SQLException {
        EtlPosting pst = newPosting(stamp + 1, pkg);
        pst.setAccountCredit(accCredit);
        pst.setAccountDebit(accDebit);
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(remoteAccess.invoke(BankCurrencyRepository.class, "getCurrency", getCurrencyCodeByDigital(accCredit.substring(5, 8))));
        pst.setCurrencyDebit(remoteAccess.invoke(BankCurrencyRepository.class, "getCurrency", getCurrencyCodeByDigital(accDebit.substring(5, 8))));
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

    private String getCurrencyCodeByDigital(String cbccy) throws SQLException {
        return baseEntityRepository.selectOne("select glccy from currency where cbccy = ?", cbccy).getString("glccy");
    }

    private static long createPd(Date pod, String acid, String bsaacid, String glccy, String pbr, long amnt, long amntbc) throws SQLException {
        long id = createPd(pod, acid, bsaacid, glccy, pbr);
        Assert.assertEquals(1, baseEntityRepository.executeNativeUpdate("update pd set amnt = ?, amntbc = ? where id = ?", amnt, amntbc, id));
        return id;
    }

    private static JobHistory getLastHistory(String jobName) throws SQLException {
        DataRecord record = Optional.ofNullable(baseEntityRepository.selectFirst("select * from gl_sched_h where SCHED_NAME = ? order by 1 desc"
            , jobName)).orElseThrow(() -> new RuntimeException("No executed jobs with name: " + jobName));
        return (JobHistory) baseEntityRepository.findById(JobHistory.class, record.getLong("id_hist"));
    }

    private static Long createPostingUpdate(Date operday, AccRlnId rlnId1, AccRlnId rlnId2) throws SQLException {
        long id1 = createPd(operday, rlnId1.getAcid(), rlnId1.getBsaAcid(), BankCurrency.EUR.getCurrencyCode(), "@@IBR", -100, -200);
        long id2 = createPd(operday, rlnId2.getAcid(), rlnId2.getBsaAcid(), BankCurrency.EUR.getCurrencyCode(), "@@IBR", 100, 200);
        Long pcid = baseEntityRepository.nextId("PD_SEQ");
        baseEntityRepository.executeNativeUpdate("update pd set pcid = ? where id in (?, ?)", pcid, id1, id2);

        baseEntityRepository.executeNativeUpdate("delete from gl_pdjover where pcid = 0");

        Assert.assertEquals(2, baseEntityRepository.executeNativeUpdate("update pd set invisible = '1' where pcid = ?", pcid));
        return pcid;
    }

}
