package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.controller.operday.task.md.DismodAccRestTask;
import ru.rbt.barsgl.ejb.controller.operday.task.md.DismodAccRestTask.DismodOutState;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import java.sql.SQLException;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.controller.operday.task.md.DismodAccRestTask.DismodOutState.S;
import static ru.rbt.barsgl.ejbtest.AbstractRemoteIT.PostDirection.C;
import static ru.rbt.barsgl.ejbtest.AbstractRemoteIT.PostDirection.D;
import static ru.rbt.ejbcore.util.StringUtils.substr;

/**
 * Created by Ivan Sevastyanov on 22.11.2018.
 */
public class DiscountIT extends AbstractRemoteIT {

    public static String OUT_ACCOUNT_BASKET_TAB;
    public static String OUT_LOG_TAB;

    static {
        try {
            OUT_ACCOUNT_BASKET_TAB = baseEntityRepository.selectFirst("select PKG_MD_ACCOUNT.GET_ACCOUNT_BASKET_TAB_NAME() nm from dual").getString("nm");
            OUT_LOG_TAB = baseEntityRepository.selectFirst("select PKG_MD_ACCOUNT.GET_OUT_LOG_TAB_NAME() nm from dual").getString("nm");
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    @Test
    public void test() throws Exception {
        createOutAccountTab();
        createOutLogTab();

        setOnlineBalanceMode();

        baseEntityRepository.executeNativeUpdate(format("delete from %s", OUT_LOG_TAB));
        baseEntityRepository.executeNativeUpdate("delete from gl_sched_h");
        baseEntityRepository.executeNativeUpdate(format("delete from %s", OUT_ACCOUNT_BASKET_TAB));
        baseEntityRepository.executeNativeUpdate("delete from GL_MD_LOG");

        baseEntityRepository.executeNativeUpdate(format("insert into %s values (?,?,?,?,sysdate,sysdate)", OUT_LOG_TAB)
            , 1, DismodAccRestTask.OUT_PROCESS_NAME, getOperday().getLastWorkingDay(), S.name());

        GLAccount account1 = findAccount("408%");
        createAccRecord(account1, substr(account1.getBsaAcid(), 5));
        GLAccount account2 = findAccount("47408810%");
        createAccRecord(account2, substr(account2.getBsaAcid(), 5));
        GLAccount account3 = findAccountLikeAndNotEquals("408%", account2.getBsaAcid());
        createAccRecord(account3, substr(account3.getBsaAcid(), 5));


        long pcid1 = baseEntityRepository.nextId("PD_SEQ");
        long pcid2 = baseEntityRepository.nextId("PD_SEQ");
        createPd(getWorkdayBefore(getOperday().getLastWorkingDay()), account1.getAcid(), account1.getBsaAcid(), account1.getCurrency().getCurrencyCode(), "@@GL-PH");
        createPd(getOperday().getLastWorkingDay(), account1.getAcid(), account1.getBsaAcid(), account1.getCurrency().getCurrencyCode(), "@@GL-PH", pcid1, C);
        createPd(getOperday().getLastWorkingDay(), account1.getAcid(), account1.getBsaAcid(), account1.getCurrency().getCurrencyCode(), "@@GL-PH", pcid2, D);
        createPd(getOperday().getCurrentDate(), account1.getAcid(), account1.getBsaAcid(), account1.getCurrency().getCurrencyCode(), "@@GL-PH");

        createPd(getWorkdayBefore(getOperday().getLastWorkingDay()),  account2.getAcid(),  account2.getBsaAcid(),  account2.getCurrency().getCurrencyCode(), "@@GL-PH");
        createPd(getOperday().getLastWorkingDay(),  account2.getAcid(),  account2.getBsaAcid(),  account2.getCurrency().getCurrencyCode(), "@@GL-PH", pcid1, D);
        createPd(getOperday().getLastWorkingDay(),  account2.getAcid(),  account2.getBsaAcid(),  account2.getCurrency().getCurrencyCode(), "@@GL-PH", pcid2, C);
        createPd(getOperday().getCurrentDate(),  account2.getAcid(),  account2.getBsaAcid(),  account2.getCurrency().getCurrencyCode(), "@@GL-PH");

        final String jobName = "Name1";
        SingleActionJob job = SingleActionJobBuilder.create().withClass(DismodAccRestTask.class).withName(jobName).build();
        jobService.executeJob(job);

        JobHistory lastHist = getLastHistRecordObject(jobName);
        Assert.assertNotNull(lastHist);
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED, lastHist.getResult());

        DataRecord mdAccount = baseEntityRepository.selectFirst("select * from GL_MD_ACC where bsaacid = ?", account1.getBsaAcid());
        Assert.assertNotNull(mdAccount);

        DataRecord mdAccount2 = baseEntityRepository.selectFirst("select * from GL_MD_ACC where bsaacid = ?", account2.getBsaAcid());
        Assert.assertNotNull(mdAccount2);

        DataRecord mdAccount3 = baseEntityRepository.selectFirst("select * from GL_MD_ACC where bsaacid = ?", account3.getBsaAcid());
        Assert.assertNotNull(mdAccount3);

        DataRecord record = baseEntityRepository.selectFirst("select * from GL_MD_LOG");
        Assert.assertEquals(DismodOutState.S, DismodOutState.valueOf(record.getString("status")));

        jobService.executeJob(job);
        JobHistory lastHist2 = getLastHistRecordObject(jobName);
        Assert.assertEquals(lastHist, lastHist2);
    }

    private void createOutAccountTab() {
        executeAutonomous(
                format("create table %s (\n" +
                "                BSAACID VARCHAR2(20)\n" +
                "                , ACCTYPE VARCHAR2(9)\n" +
                "                , CCY VARCHAR2(3)\n" +
                "                , DEAL_ID NUMBER(19)\n" +
                "                , PSAV CHAR(1)\n" +
                "                , FL_TURN CHAR(1)\n" +
                "                , EXCLUDE VARCHAR2(5)\n" +
                "            )", OUT_ACCOUNT_BASKET_TAB));
    }

    private void createOutLogTab() {
        executeAutonomous(format("CREATE TABLE %s (\n" +
                "    ID_PK NUMBER(19) NOT NULL\n" +
                "    , PROCESS_NM VARCHAR2(20) \n" +
                "    , OPERDAY DATE\n" +
                "    , STATUS CHAR(1)\n" +
                "    , START_DATE TIMESTAMP \n" +
                "    , END_DATE TIMESTAMP\n" +
                ")", OUT_LOG_TAB));
    }

    private void executeAutonomous(String sql) {
        baseEntityRepository.executeNativeUpdate(format(
                "declare\n" +
                "    pragma autonomous_transaction;\n" +
                "    l_already exception;\n" +
                "    pragma exception_init(l_already, -955);\n" +
                "    l_sql varchar2(4000) :=\n" +
                "    q'[ %s]';\n" +
                "begin\n" +
                "    execute immediate l_sql;\n" +
                "exception\n" +
                "    when l_already then null;\n" +
                "end;", sql));
    }

    private GLAccount createAccRecord(GLAccount account, String exc) throws SQLException {
        baseEntityRepository.executeNativeUpdate(format("insert into %s (BSAACID,ACCTYPE,CCY,DEAL_ID,PSAV,FL_TURN,EXCLUDE) values (?, ?, ?, ?, ?, 'Y', ?)", OUT_ACCOUNT_BASKET_TAB)
            , account.getBsaAcid(), account.getAccountType(), account.getCurrency().getCurrencyCode(), account.getId(), "А".equals(account.getPassiveActive()) ? "A" : "L", exc);
        return account;
    }

    private GLAccount createAccRecord(String like) throws SQLException {
        GLAccount account = findAccount(like);
        createAccRecord(account, null);
        return account;
    }
}
