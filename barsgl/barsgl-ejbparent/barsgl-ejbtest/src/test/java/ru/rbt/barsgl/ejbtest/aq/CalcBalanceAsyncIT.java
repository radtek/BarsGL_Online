package ru.rbt.barsgl.ejbtest.aq;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.od.OperdaySynchronizationController;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejbtest.AbstractRemoteIT;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.sql.SQLException;
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
        setOndemanMode();

        List<DataRecord> triggers = baseEntityRepository.select("select * from user_triggers where table_name = ? ", "PST");
        List<DataRecord> enabled = triggers.stream().filter(r -> "ENABLED".equals(r.getString("status"))).collect(Collectors.toList());
        Assert.assertEquals(3, enabled.size());
        Assert.assertTrue(enabled.stream().anyMatch(r ->
                   Objects.equals(r.getString("trigger_name"), "PST_AD_JRN")
                || Objects.equals(r.getString("trigger_name"), "PST_AI_JRN")
                || Objects.equals(r.getString("trigger_name"), "PST_AU_JRN")));

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

    private static GLAccount findAccount(String bsaacidLike) throws SQLException {
        DataRecord record = baseEntityRepository.selectFirst("select id from gl_acc where bsaacid like ?", bsaacidLike);
        if (record != null) {
            return (GLAccount) baseEntityRepository.findById(GLAccount.class, record.getLong("id"));
        } else {
            throw new RuntimeException(bsaacidLike + " not found");
        }
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

    private void stopListeningQueue() {
        baseEntityRepository.executeNativeUpdate(
                "begin\n" +
                "    for nn in (select * from user_scheduler_running_jobs where job_name like '%LISTEN%') loop\n" +
                "        dbms_scheduler.stop_job(nn.job_name, true);\n" +
                "    end loop;\n" +
                "end;");
    }
}
