package ru.rbt.barsgl.ejbtest.aq;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.od.OperdaySynchronizationController;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.gl.BackvalueJournal;
import ru.rbt.barsgl.ejbtest.AbstractRemoteIT;
import ru.rbt.ejbcore.datarec.DBParam;
import ru.rbt.ejbcore.datarec.DBParams;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CalcBalanceAsyncIT extends AbstractRemoteIT {

    private static final Logger log = Logger.getLogger(CalcBalanceAsyncIT.class.getName());

    private static final Long zero = new Long(0);

    private static final String pbrGibrid = "@@GL-K+";
    private static String pbrOnline;

    static {
        try {
            pbrOnline = baseEntityRepository.selectFirst("select * from GL_AQPBR").getString("PBRVALUE");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private enum QUEUE {
        BAL_QUEUE, AQ$_BAL_QUEUE_TAB_E
    }

    @Test public void testGibrid() throws SQLException {
        stopListeningQueue();
        purgeQueueTable();

        Operday operday = getOperday();

        // отключены все триггера, кроме AQ

        setGibridMode();

        List<DataRecord> triggers = baseEntityRepository.select("select * from user_triggers where table_name = ? ", "PST");
        Assert.assertTrue(triggers.stream().anyMatch(r -> "ENABLED".equals(r.getString("status"))));
        Assert.assertEquals(Operday.BalanceMode.GIBRID, Operday.BalanceMode.valueOf(baseEntityRepository.selectFirst("select GLAQ_PKG_UTL.GET_CURRENT_BAL_STATE st from dual").getString("st")));

        // удаление baltur по счету
        GLAccount account = findAccount("40702810%");
        log.info("Account " + account.getBsaAcid());
        baseEntityRepository.executeNativeUpdate("delete from baltur where bsaacid = ?", account.getBsaAcid());

        Long amnt = 100L; Long amntbc = amnt + 1;
        long id = baseEntityRepository.nextId("PD_SEQ");
        log.info("PST id= " + id);
        createPosting(id, id, account.getAcid(), account.getBsaAcid(), amnt, amntbc, pbrGibrid, operday.getCurrentDate(), operday.getCurrentDate(), BankCurrency.RUB.getCurrencyCode(), null);

        // проверка остатков - нет изменений - включен гибридный режим
        List<DataRecord> balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertEquals(0, balturs.size());

        checkMessageCount(QUEUE.BAL_QUEUE, 1);
        dequeueProcessOne();

        // есть изменения (проверка сумм!)
        balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertEquals(1, balturs.size());
        Assert.assertEquals(zero, balturs.get(0).getLong("obac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("obbc"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtbc"));
        Assert.assertEquals(amnt, balturs.get(0).getLong("ctac"));
        Assert.assertEquals(amntbc, balturs.get(0).getLong("ctbc"));

        // invisible
        baseEntityRepository.executeNativeUpdate("update pst set invisible = '1' where id = ?", id);

        checkMessageCount(QUEUE.BAL_QUEUE, 1);
        dequeueProcessOne();

        balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertEquals(1, balturs.size());
        Assert.assertEquals(zero, balturs.get(0).getLong("obac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("obbc"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtbc"));
        Assert.assertEquals(zero, balturs.get(0).getLong("ctac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("ctbc"));

        // insert already invisible
        long id2 = baseEntityRepository.nextId("PD_SEQ");
        createPosting(id2,id2, account.getAcid(), account.getBsaAcid(),amnt, amntbc, pbrGibrid, operday.getCurrentDate(), operday.getCurrentDate(), BankCurrency.RUB.getCurrencyCode(), "1");

        checkMessageCount(QUEUE.BAL_QUEUE, 0);

        // balance is zero
        balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertEquals(1, balturs.size());
        Assert.assertEquals(zero, balturs.get(0).getLong("obac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("obbc"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtbc"));
        Assert.assertEquals(zero, balturs.get(0).getLong("ctac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("ctbc"));

    }

    @Test
    public void testOnline() throws SQLException {
        setOnlineMode();
        stopListeningQueue();
        purgeQueueTable();

        GLAccount account = findAccount("40702810%");
        log.info("Account " + account.getBsaAcid());
        baseEntityRepository.executeNativeUpdate("delete from baltur where bsaacid = ?", account.getBsaAcid());

        Long amnt = 100L;
        Long amntbc = 101L;

        // онлайн пересчет остатков, в очередь ничего не поступает, остатки считаются в триггерах

        Operday operday = getOperday();
        long id = baseEntityRepository.nextId("PD_SEQ");
        createPosting(id, id, account.getAcid(), account.getBsaAcid(), amnt, amntbc, pbrGibrid, operday.getCurrentDate(), operday.getCurrentDate(), BankCurrency.RUB.getCurrencyCode(), "1");


        checkMessageCount(QUEUE.BAL_QUEUE, 0);

        List<DataRecord> balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertEquals(0, balturs.size());

        baseEntityRepository.executeNativeUpdate("update pst set invisible = '0' where id = ?", id);
        balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertEquals(1, balturs.size());
        Assert.assertEquals(zero, balturs.get(0).getLong("obac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("obbc"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtbc"));
        Assert.assertEquals(amnt, balturs.get(0).getLong("ctac"));
        Assert.assertEquals(amntbc, balturs.get(0).getLong("ctbc"));
    }

    @Test public void testOndemand() throws SQLException {
        setGibridMode();
        setOndemanMode();

        List<DataRecord> triggers = baseEntityRepository.select("select * from user_triggers where table_name = ? ", "PST");
        List<DataRecord> enabled = triggers.stream().filter(r -> "ENABLED".equals(r.getString("status"))).collect(Collectors.toList());
        Assert.assertEquals(0, enabled.size());
    }

    @Test public void testGibridOnSpecificPBR() throws SQLException {
        setGibridMode();
        stopListeningQueue();
        purgeQueueTable();

        GLAccount account = findAccount("40702810%");
        log.info("Account " + account.getBsaAcid());
        baseEntityRepository.executeNativeUpdate("delete from baltur where bsaacid = ?", account.getBsaAcid());

        Long amnt = 100L;
        Long amntbc = 101L;

        // онлайн пересчет остатков, в очередь ничего не поступает, остатки считаются в триггерах

        Operday operday = getOperday();
        long id = baseEntityRepository.nextId("PD_SEQ");
        createPosting(id, id, account.getAcid(), account.getBsaAcid(), amnt, amntbc, pbrOnline, operday.getCurrentDate(), operday.getCurrentDate(), BankCurrency.RUB.getCurrencyCode(), "0");

        checkMessageCount(QUEUE.BAL_QUEUE, 0);

        List<DataRecord> balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertEquals(1, balturs.size());

        balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertEquals(1, balturs.size());
        Assert.assertEquals(zero, balturs.get(0).getLong("obac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("obbc"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtac"));
        Assert.assertEquals(zero, balturs.get(0).getLong("dtbc"));
        Assert.assertEquals(amnt, balturs.get(0).getLong("ctac"));
        Assert.assertEquals(amntbc, balturs.get(0).getLong("ctbc"));

    }

    @Test public void testBvjrnl() throws Exception {

        GLAccount account1 = findAccount("40702810%");
        Date pod0 = DateUtils.parseDate("2018-09-01", "yyyy-MM-dd");
        Date pod1 = DateUtils.addDays(pod0, 1);
        Date pod2 = DateUtils.addDays(pod0, 2);
        cleanBvjrnlRecord(account1);
        insertBvJrnl(account1, BackvalueJournal.BackvalueJournalState.NEW, pod0);
        insertBvJrnl(account1, BackvalueJournal.BackvalueJournalState.NEW, pod1);
        insertBvJrnl(account1, BackvalueJournal.BackvalueJournalState.NEW, pod2);

        List<DataRecord> bvs = baseEntityRepository.select("select * from gl_bvjrnl where bsaacid = ?", account1.getBsaAcid());
        Assert.assertTrue(bvs.stream().anyMatch(r -> r.getLong("seq") != null));

        GLAccount account2 = findAccount("40701810%");
        cleanBvjrnlRecord(account2);
        insertBvJrnl(account2, BackvalueJournal.BackvalueJournalState.NEW, pod0);

        DBParams params = DBParams.createParams(new DBParam(Types.INTEGER, DBParam.DBParamDirectionType.OUT)
                ,new DBParam(Types.INTEGER, DBParam.DBParamDirectionType.OUT));
        params = baseEntityRepository.executeCallable(
                "declare\n" +
                "    l_cnt number;\n" +
                "    l_tot number;\n" +
                "begin\n" +
                "    PKG_LOCAL.INS_TO_LOCAL(l_cnt, l_tot);\n" +
                "    \n" +
                "    ? := l_cnt;\n" +
                "    ? := l_tot;\n" +
                "end;", params);
        Assert.assertEquals(2, params.getParams().get(0).getValue());
        Assert.assertEquals(4, params.getParams().get(1).getValue());
        // проверка статусов обработки процессом локализации
        // проверяем
        DBParams params2 = DBParams.createParams(new DBParam(Types.INTEGER, DBParam.DBParamDirectionType.OUT));
        params2 = baseEntityRepository.executeCallable(
                "declare\n" +
                "    l_cnt number;\n" +
                "begin\n" +
                "    PKG_LOCAL.UPDATE_BVJRNL(l_cnt, '1');\n" +
                "    ? := l_cnt;\n" +
                "end;", params2);
        // проверка статусов обработки процессом локализации
        // не проверяем
        Assert.assertEquals(0, params2.getParams().get(0).getValue());
        params2 = baseEntityRepository.executeCallable(
                "declare\n" +
                "    l_cnt number;\n" +
                "begin\n" +
                "    PKG_LOCAL.UPDATE_BVJRNL(l_cnt);\n" +
                "    ? := l_cnt;\n" +
                "end;", params2);
        Assert.assertEquals(4, params2.getParams().get(0).getValue());
    }

    @Test public void testRestoreTriggersState() throws SQLException {
        setGibridMode();
        checkCurrentMode(Operday.BalanceMode.GIBRID);

        setOndemanMode();
        checkCurrentMode(Operday.BalanceMode.ONDEMAND);

        restorePreviousTriggersState();
        checkCurrentMode(Operday.BalanceMode.GIBRID);
    }

    @Test public void testSwitchBalanceModeOnFlushBuffer() {
        Assert.fail("Переключение режима обработки отстатков после сброса буфера");
    }

    @Test public void testSwitchBalanceModeOnOpenOperday() {
        Assert.fail("Переключение режима обработки отстатков при открытии ОД");
    }

    private void createPosting (long id, long pcid, String acid, String bsaacid, long amount, long amountbc, String pbr, Date pod, Date vald, String ccy, String invisible) {
        String insert = "insert into pst (id,pcid,acid,bsaacid,amnt,amntbc,pbr,pod,vald,ccy, invisible) values (?,?,?,?,?,?,?,?,?,?,?)";
        baseEntityRepository.executeNativeUpdate(insert, id, pcid, acid, bsaacid, amount, amountbc, pbr, pod, vald, ccy, invisible);
    }

    private void purgeQueueTable() {
        baseEntityRepository.executeNativeUpdate(
                "DECLARE\n" +
                "    PRAGMA AUTONOMOUS_TRANSACTION;\n" +
                "\n" +
                "    PURGE_OPTIONS DBMS_AQADM.AQ$_PURGE_OPTIONS_T;\n" +
                "    \n" +
                "BEGIN\n" +
                "\n" +
                "    DBMS_AQADM.PURGE_QUEUE_TABLE(GLAQ_PKG_CONST.C_NORMAL_QUEUE_TAB_NAME, NULL, PURGE_OPTIONS);\n" +
                "    COMMIT;\n" +
                "END;");
    }

    private void dequeueProcessOne() {
        baseEntityRepository.executeNativeUpdate("begin GLAQ_PKG.DEQUEUE_PROCESS_ONE(GLAQ_PKG_CONST.C_NORMAL_QUEUE_NAME); end;");
    }

    private void checkMessageCount(QUEUE queue, long expect) throws SQLException {
        List<DataRecord> records = baseEntityRepository.select("select queue from AQ$BAL_QUEUE_TAB where queue = ?", queue.name());
        Assert.assertEquals(expect, records.size());
    }

    private void setGibridMode() {
        remoteAccess.invoke(OperdaySynchronizationController.class, "setGibridBalanceCalc");
    }

    private void setOnlineMode() {
        remoteAccess.invoke(OperdaySynchronizationController.class, "setOnlineBalanceCalc");
    }

    private void setOndemanMode() {
        remoteAccess.invoke(OperdaySynchronizationController.class, "setOndemandBalanceCalc");
    }

    private void restorePreviousTriggersState() {
        remoteAccess.invoke(OperdaySynchronizationController.class, "restorePreviousTriggersState");
    }

    private void stopListeningQueue() {
        baseEntityRepository.executeNativeUpdate(
                "begin\n" +
                "    for nn in (select * from user_scheduler_running_jobs where job_name like '%LISTEN%') loop\n" +
                "        dbms_scheduler.stop_job(nn.job_name, true);\n" +
                "    end loop;\n" +
                "end;");
    }

    private void insertBvJrnl(GLAccount account, BackvalueJournal.BackvalueJournalState state, Date pod) {
        baseEntityRepository.executeNativeUpdate("insert into gl_bvjrnl (bsaacid,acid,pod,state) values (?, ?, ?, ?)",
                account.getBsaAcid(), account.getAcid(), pod, state.name());
    }

    private void cleanBvjrnlRecord(GLAccount account) {
        baseEntityRepository.executeNativeUpdate("delete from gl_bvjrnl where bsaacid = ?", account.getBsaAcid());
    }

    private void checkCurrentMode(Operday.BalanceMode mode) throws SQLException {
        Assert.assertEquals(mode, Operday.BalanceMode.valueOf(baseEntityRepository
                .selectFirst("select GLAQ_PKG_UTL.GET_CURRENT_BAL_STATE st from dual").getString("st")));

    }
}
