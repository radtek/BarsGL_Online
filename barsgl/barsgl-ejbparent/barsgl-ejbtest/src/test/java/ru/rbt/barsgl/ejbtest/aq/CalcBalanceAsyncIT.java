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
import java.util.logging.Logger;

public class CalcBalanceAsyncIT extends AbstractRemoteIT {

    private static final Logger log = Logger.getLogger(CalcBalanceAsyncIT.class.getName());

    private enum QUEUE {
        BAL_QUEUE, AQ$_BAL_QUEUE_TAB_E
    }

    @Test public void test() throws SQLException {
        purgeQueueTable();

        Operday operday = getOperday();

        // отключены все триггера, кроме AQ

        remoteAccess.invoke(OperdaySynchronizationController.class, "setGibridBalanceCalc");

        // удаление baltur по счету
        GLAccount account = findAccount("40702810%");
        log.info("Account " + account.getBsaAcid());
        baseEntityRepository.executeNativeUpdate("delete from baltur where bsaacid = ?", account.getBsaAcid());

        Long amnt = 100L; Long amntbc = amnt + 1;
        long id = baseEntityRepository.nextId("PD_SEQ");
        log.info("PST id= " + id);
        final String pbr = "@@GL-K+";
        createPosting(id, id, account.getAcid(), account.getBsaAcid(), amnt, amntbc, pbr, operday.getCurrentDate(), operday.getCurrentDate(), BankCurrency.RUB.getCurrencyCode(), null);

        // проверка остатков - нет изменений - включен гибридный режим
        List<DataRecord> balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertEquals(0, balturs.size());

        checkMessageCount(QUEUE.BAL_QUEUE, 1);
        dequeueProcessOne();

        // есть изменения (проверка сумм!)
        balturs = baseEntityRepository.select("select * from baltur where bsaacid = ?", account.getBsaAcid());
        Assert.assertEquals(1, balturs.size());
        final Long zero = new Long(0);
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
        createPosting(id2,id2, account.getAcid(), account.getBsaAcid(),amnt, amntbc, pbr, operday.getCurrentDate(), operday.getCurrentDate(), BankCurrency.RUB.getCurrencyCode(), "1");

        checkMessageCount(QUEUE.BAL_QUEUE, 0);

        dequeueProcessOne();
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

}
