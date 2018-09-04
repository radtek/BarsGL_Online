package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.controller.operday.task.dwh.DwhProcessClosedDealsTask;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;
import ru.rbt.barsgl.ejbtesting.ServerTestingFacade;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.controller.operday.task.dwh.DwhProcessClosedDealsTask.*;

/**
 * загрузка данных из DWH
 */
public class DwhUnloadIT extends AbstractTimerJobIT {

    public static final Logger logger = Logger.getLogger(DwhUnloadIT.class.getName());

    private static final String DWH_STATUS_TAB = "TEST_DWH_CLOSED_DEALS_STATUS";
    private static final String DWH_DEALS_TAB = "TEST_DWH_CLOSED_DEALS_FOR_BGL";

    @BeforeClass
    public static void beforeClass() throws SQLException {
        baseEntityRepository.executeNativeUpdate("delete from GL_DEALCLOSE");
        dropTestTable(DWH_STATUS_TAB);
        dropTestTable(DWH_DEALS_TAB);
        createTableDwhStatus(DWH_STATUS_TAB);
        createTableDwhDeals(DWH_DEALS_TAB);
    }

    @Test
    public void testCloseDeals() throws Exception {
        cleanStatTable();

        final String jobName = "JOB_" + StringUtils.rsubstr(System.currentTimeMillis()+"", 5);

        fillDwhStateTable(DWH_STATUS_TAB, getOperday().getLastWorkingDay(), "KP1");

        jobService.executeJob(SingleActionJobBuilder.create().withClass(DwhProcessClosedDealsTask.class)
                .withProps(LOAD_TYPE_KEY+"="+ DwhProcessClosedDealsTask.LoadType.Full
                + "\n"+DWH_CLOSED_DEALS_STATUS_TABNAME_KEY + "=" +DWH_STATUS_TAB
                + "\n"+DWH_CLOSED_DEALS_STREAM_KEY + "= someStream"
                + "\n"+DWH_CLOSED_DEALS_TABNAME_KEY + "=" +DWH_DEALS_TAB).withName(jobName)
        .build());
        DataRecord record = baseEntityRepository.selectFirst("select * from GL_LOADSTAT");
        Assert.assertNotNull(record);
        Assert.assertEquals(getStatusOk(), record.getString("status"));
        JobHistory history = (JobHistory) baseEntityRepository.selectFirst(JobHistory.class, "from JobHistory h where h.jobName = ?1", jobName);
        Assert.assertNotNull(history);
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED, history.getResult());
    }

    @Test public void testOneDeal() throws Exception {
        cleanStatTable();

        final String streamId = "KP1";
        fillDwhStateTable(DWH_STATUS_TAB, getOperday().getLastWorkingDay(), streamId);

        final String dealId = StringUtils.rsubstr(System.currentTimeMillis() + "", 6);
        createDwhDeal(DWH_DEALS_TAB, dealId, streamId, getOperday().getLastWorkingDay());

        final String jobName = "JOB_" + StringUtils.rsubstr(System.currentTimeMillis()+"", 5);
        SingleActionJob singleActionJob = SingleActionJobBuilder.create().withClass(DwhProcessClosedDealsTask.class)
                .withProps(LOAD_TYPE_KEY+"="+ DwhProcessClosedDealsTask.LoadType.Discrete
                        + "\n"+DWH_CLOSED_DEALS_STATUS_TABNAME_KEY + "=" +DWH_STATUS_TAB
                        + "\n"+DWH_CLOSED_DEALS_STREAM_KEY + "=" + streamId
                        + "\n"+DWH_CLOSED_DEALS_TABNAME_KEY + "=" +DWH_DEALS_TAB).withName(jobName)
                .build();
        jobService.executeJob(singleActionJob);
        DataRecord record = baseEntityRepository.selectFirst("select * from GL_LOADSTAT");
        Assert.assertNotNull(record);
        Assert.assertEquals(getStatusOk(), record.getString("status"));
        JobHistory history = (JobHistory) baseEntityRepository.selectFirst(JobHistory.class, "from JobHistory h where h.jobName = ?1", jobName);
        Assert.assertNotNull(history);
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED, history.getResult());

        DataRecord dealGL = baseEntityRepository.selectFirst("select * from GL_DEALCLOSE where dealid = ?", dealId);
        Assert.assertNotNull(dealGL);

        // повторная выгрузка при успешной не происходит
        jobService.executeJob(singleActionJob);
        List<JobHistory> histories = baseEntityRepository.select(JobHistory.class, "from JobHistory h where h.jobName = ?1", jobName);
        Assert.assertEquals(1, histories.size());
    }

    @Test public void testOneDealStream() throws Exception {
        cleanStatTable();

        // stream #1
        final String streamId_1 = "KP1";
        fillDwhStateTable(DWH_STATUS_TAB, getOperday().getLastWorkingDay(), streamId_1);

        final String dealId_1 = StringUtils.rsubstr(System.currentTimeMillis() + "", 6);
        createDwhDeal(DWH_DEALS_TAB, dealId_1, streamId_1, getOperday().getLastWorkingDay());

        // stream #1
        final String streamId_2 = "KP2";
        fillDwhStateTable(DWH_STATUS_TAB, getOperday().getLastWorkingDay(), streamId_2);

        final String dealId_2 = StringUtils.rsubstr(System.currentTimeMillis() + "", 6);
        createDwhDeal(DWH_DEALS_TAB, dealId_2, streamId_2, getOperday().getLastWorkingDay());

        final String jobName = "JOB1_" + StringUtils.rsubstr(System.currentTimeMillis()+"", 5);
        SingleActionJob singleActionJob = SingleActionJobBuilder.create().withClass(DwhProcessClosedDealsTask.class)
                .withProps(LOAD_TYPE_KEY+"="+ DwhProcessClosedDealsTask.LoadType.Discrete
                        + "\n"+DWH_CLOSED_DEALS_STATUS_TABNAME_KEY + "=" +DWH_STATUS_TAB
                        + "\n"+DWH_CLOSED_DEALS_STREAM_KEY + "=" + streamId_1
                        + "\n"+DWH_CLOSED_DEALS_TABNAME_KEY + "=" +DWH_DEALS_TAB).withName(jobName)
                .build();
        jobService.executeJob(singleActionJob);
        DataRecord record = baseEntityRepository.selectFirst("select * from GL_LOADSTAT where stream_id = ?", streamId_1);
        Assert.assertNotNull(record);
        Assert.assertEquals(getStatusOk(), record.getString("status"));
        JobHistory history = (JobHistory) baseEntityRepository.selectFirst(JobHistory.class, "from JobHistory h where h.jobName = ?1", jobName);
        Assert.assertNotNull(history);
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED, history.getResult());

        List<DataRecord> dealsGL = baseEntityRepository.select("select * from GL_DEALCLOSE ");
        Assert.assertEquals(1, dealsGL.size());
        Assert.assertTrue(dealsGL.stream().anyMatch(d -> streamId_1.equals(d.getString("SOURCE"))));

        final String jobName_2 = "JOB2_" + StringUtils.rsubstr(System.currentTimeMillis()+"", 5);
        SingleActionJob singleActionJob_2 = SingleActionJobBuilder.create().withClass(DwhProcessClosedDealsTask.class)
                .withProps(LOAD_TYPE_KEY+"="+ DwhProcessClosedDealsTask.LoadType.Discrete
                        + "\n"+DWH_CLOSED_DEALS_STATUS_TABNAME_KEY + "=" +DWH_STATUS_TAB
                        + "\n"+DWH_CLOSED_DEALS_STREAM_KEY + "=" + streamId_2
                        + "\n"+DWH_CLOSED_DEALS_TABNAME_KEY + "=" +DWH_DEALS_TAB).withName(jobName_2)
                .build();
        jobService.executeJob(singleActionJob_2);
        DataRecord record_2 = baseEntityRepository.selectFirst("select * from GL_LOADSTAT where stream_id = ?", streamId_2);
        Assert.assertNotNull(record_2);
        Assert.assertEquals(getStatusOk(), record_2.getString("status"));
        JobHistory history_2 = (JobHistory) baseEntityRepository.selectFirst(JobHistory.class, "from JobHistory h where h.jobName = ?1", jobName_2);
        Assert.assertNotNull(history_2);
        Assert.assertEquals(DwhUnloadStatus.SUCCEDED, history_2.getResult());

        dealsGL = baseEntityRepository.select("select * from GL_DEALCLOSE ");
        Assert.assertEquals(2, dealsGL.size());
        Assert.assertTrue(dealsGL.stream().anyMatch(d -> streamId_1.equals(d.getString("SOURCE"))));
        Assert.assertTrue(dealsGL.stream().anyMatch(d -> streamId_2.equals(d.getString("SOURCE"))));

    }

    @Test @Ignore public void testReal() throws Exception {
        SingleActionJob singleActionJob = SingleActionJobBuilder.create().withClass(DwhProcessClosedDealsTask.class)
                .withProps(LOAD_TYPE_KEY+"="+ DwhProcessClosedDealsTask.LoadType.Full)
                .build();
        jobService.executeJob(singleActionJob);
        jobService.executeJob(singleActionJob);
    }


    private static void dropTestTable(String tableName) {
        remoteAccess.invoke(ServerTestingFacade.class, "executeUpdateNonXa",
                "begin\n" +
                        "    DB_CONF.DROP_TABLE_IF_EXISTS(user, '" + tableName + "');\n" +
                        "end;");
    }

    private static void createTableDwhStatus(String tableName) {
        remoteAccess.invoke(ServerTestingFacade.class, "executeUpdateNonXa", format(STAT_TAB_DDL, tableName));
    }

    private static void createTableDwhDeals(String tableName) {
        remoteAccess.invoke(ServerTestingFacade.class, "executeUpdateNonXa", format(DEALS_TAB_DDL, tableName));
    }

    private static void cleanStatTable() {
        baseEntityRepository.executeNativeUpdate("delete from GL_LOADSTAT");
    }

    private String getStatusOk() throws SQLException {
        return baseEntityRepository.selectFirst("select PKG_DWHDEALS.get_status_ok() col from dual").getString("col");
    }

    private void createDwhDeal(String tableName, String dealId, String streamId, Date validFrom) {
        baseEntityRepository.executeNativeUpdate(format("delete from %s where DEAL_NUMBER = ?", tableName), dealId);
        String insert = "Insert into %s (DEAL_NUMBER,SUB_DEAL_NUMBER,CNUM,CLOSE_DT,MATURITY_DT,STATUS_CD,SOURCE,STREAM_ID,VALID_FROM_DTTM,DEAL_TYPE,\"TABLE\") \n" +
                "values ('%s',null,'00674144',to_date('2015-12-28 00:00:00','yyyy-mm-dd hh24:mi:ss'),to_date('2015-12-28 00:00:00','yyyy-mm-dd hh24:mi:ss'),'C',?,'KPS_DEPOSIT',?,'KDD','DEPOSIT')";
        baseEntityRepository.executeNativeUpdate(format(insert, tableName, dealId), streamId, validFrom);
    }

    private void fillDwhStateTable(String tableName, Date asOfDate, String streamId) {
        baseEntityRepository.executeNativeUpdate("delete from " + tableName + " where stream = ?", streamId);
        baseEntityRepository.executeNativeUpdate("insert into " + tableName + " (AS_OF_DATE, stream) values (?, ?)"
                , asOfDate, streamId);
    }

    private static final String STAT_TAB_DDL = "CREATE TABLE %s \n" +
            "(AS_OF_DATE DATE, \n" +
            " STREAM CHAR(3 BYTE)\n" +
            ")";

    private static final String DEALS_TAB_DDL = "create table %s (\n" +
            "DEAL_NUMBER              VARCHAR2(42)\n" +
            ", SUB_DEAL_NUMBER          VARCHAR2(128)\n" +
            ", CNUM                     VARCHAR2(50)\n" +
            ", CLOSE_DT                 DATE\n" +
            ", MATURITY_DT              DATE\n" +
            ", STATUS_CD                VARCHAR2(3)\n" +
            ", \"SOURCE\"                   VARCHAR2(3)\n" +
            ", STREAM_ID                VARCHAR2(20)\n" +
            ", VALID_FROM_DTTM  DATE NOT NULL\n" +
            ", DEAL_TYPE                VARCHAR2(3)\n" +
            ", \"TABLE\"                    CHAR(7) \n" +
            ")";
}
