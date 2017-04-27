package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.RemoteOpersTask;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;

import java.io.StringReader;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

/**
 * Created by ER22317 on 19.04.2016.
 * FSBr0021 Удалённые операции для выгрузки в  DWH
 * FSBr0023 Выгрузка остатков по изменения в проводках
 */
public class RemoteOpersTest extends AbstractTimerJobTest{

    @Test
    public void test() throws Exception {

        baseEntityRepository.executeNativeUpdate("delete from gl_etldwhs where pardesc in ('GLVD_PST_DU','GLVD_BAL4')");

        Date operday = DateUtils.parseDate("2016-10-28", "yyyy-MM-dd");
//        baseEntityRepository.executeNativeUpdate("update gl_pdjchg set unf ='N' where operday ='2016-07-19'");
//        baseEntityRepository.executeNativeUpdate("update gl_pdjchg set unf ='N' where operday ='2016-07-19' and pcid in ('7440068446','7440068449')");

        setOperday(operday, operday, Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        emulateMI3GL(operday);
        Properties properties = new Properties();
        properties.load(new StringReader(TaskUtils.CHECK_RUN_KEY + "=true"));
        SingleActionJobBuilder builder = SingleActionJobBuilder.create().withClass(RemoteOpersTask.class).withProps(properties);
        jobService.executeJob(builder.build());
        checkHeadersSuccessCount(1);
        checkFillPstTabs();
    }

    @Test
    public void testBal4() throws Exception {

        baseEntityRepository.executeNativeUpdate("delete from gl_etldwhs where pardesc in ('GLVD_PST_DU','GLVD_BAL4')");
        Date operday = DateUtils.parseDate("2016-07-19", "yyyy-MM-dd");
        emulateMI3GL(operday);
        Properties properties = new Properties();
        properties.load(new StringReader(TaskUtils.CHECK_RUN_KEY + "=true"));
        SingleActionJobBuilder builder = SingleActionJobBuilder.create().withClass(RemoteOpersTask.class).withProps(properties);
        jobService.executeJob(builder.build());
    }

        @Test public void testBankomatCheck() throws Exception {
        Date operday = DateUtils.parseDate("2016-02-26", "yyyy-MM-dd");
        checkCreateIFLEX(operday);
        setIflexMessage(operday, "MI2GL");
        Properties properties = new Properties(); properties.load(new StringReader(TaskUtils.CHECK_RUN_KEY + "=false"));
        Assert.assertEquals(RemoteOpersTask.BankmatCheck.BANKMAT_NOT_NEEDED, remoteAccess.invoke(RemoteOpersTask.class, "checkRunUnloadBankomat", properties, operday));

        properties = new Properties(); properties.load(new StringReader("#" + TaskUtils.CHECK_RUN_KEY + "=false"));
        Assert.assertEquals(RemoteOpersTask.BankmatCheck.BANKMAT_NOT_NEEDED, remoteAccess.invoke(RemoteOpersTask.class, "checkRunUnloadBankomat", properties, operday));

        properties = new Properties(); properties.load(new StringReader(TaskUtils.CHECK_RUN_KEY + "=true"));
        Assert.assertEquals(RemoteOpersTask.BankmatCheck.BANKMAT_REQUIRED_N, remoteAccess.invoke(RemoteOpersTask.class, "checkRunUnloadBankomat", properties, operday));

        setIflexMessage(operday, "MI3GL");
        properties = new Properties(); properties.load(new StringReader(TaskUtils.CHECK_RUN_KEY + "=true"));
        Assert.assertEquals(RemoteOpersTask.BankmatCheck.BANKMAT_REQUIRED_Y, remoteAccess.invoke(RemoteOpersTask.class, "checkRunUnloadBankomat", properties, operday));

        setIflexMessage(operday, "MI5GL");
        properties = new Properties(); properties.load(new StringReader(TaskUtils.CHECK_RUN_KEY + "=true\n" + RemoteOpersTask.STEP_NAME_KEY + "=MI5GL"));
        Assert.assertEquals(RemoteOpersTask.BankmatCheck.BANKMAT_REQUIRED_Y, remoteAccess.invoke(RemoteOpersTask.class, "checkRunUnloadBankomat", properties, operday));
    }

    private static void checkCreateIFLEX(Date operday) throws Exception {
        int cnt = baseEntityRepository.selectFirst("select count(1) cnt from workproc where dat = ? and trim(id) = 'IFLEX'"
                , operday).getInteger(0);
        if (cnt == 1) {
            baseEntityRepository.executeNativeUpdate("update workproc set msg = 'MI3GL', result = 'O' where dat = ? and trim(id) = 'IFLEX'"
                    , operday);
        } else {
            baseEntityRepository.executeNativeUpdate("insert into workproc  values (?, 'IFLEX', current_timestamp, current_timestamp, 'O', 1, 'MI3GL')"
                    , operday);
        }
    }

    private static void setIflexMessage(Date operday, String message) {
        baseEntityRepository.executeNativeUpdate("update workproc set msg = ?, result = 'O' where dat = ? and trim(id) = 'IFLEX'"
                , message, operday);
    }

    private static void checkHeadersSuccessCount(int cnt) throws SQLException {
        DataRecord rec = baseEntityRepository.selectFirst("select count (1) cnt from GL_ETLDWHS where PARDESC = 'GLVD_BAL4' and parvalue = 1");

        Assert.assertTrue(rec.getInteger("cnt")+"", cnt == rec.getInteger("cnt"));
    }

    public static void emulateMI3GL(Date operday) throws Exception {
        int cnt = baseEntityRepository.selectFirst("select count(1) cnt from workproc where dat = ? and trim(id) = 'MI3GL'"
                , operday).getInteger(0);
        if (cnt == 1) {
            baseEntityRepository.executeNativeUpdate("update workproc set result = 'O' where dat = ? and trim(id) = 'MI3GL'"
                    , operday);
        } else {
            baseEntityRepository.executeNativeUpdate("insert into workproc  values (?, 'MI3GL', current_timestamp, current_timestamp, 'O', 1, 'MI3GL')"
                    , operday);
        }

    }

    private static void checkFillPstTabs() throws SQLException {
        DataRecord rec = baseEntityRepository.selectFirst("select count(*) cnt from GLVD_PST_U");
        Assert.assertTrue( "count GLVD_PST_U = 0", rec.getInteger("cnt") > 0 );
        rec = baseEntityRepository.selectFirst("select count(*) cnt from GLVD_PST_D");
        Assert.assertTrue( "count GLVD_PST_D = 0", rec.getInteger("cnt") > 0 );
    }
}
