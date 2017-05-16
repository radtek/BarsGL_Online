package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.Overvalue2dwh;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;

import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by ER22317 on 18.05.2016.
 * Выгрузка данных переоценки в DWH
 */
public class Overvalue2dwhTest extends AbstractTimerJobTest{
    @Test
    public void test() throws Exception {
        baseEntityRepository.executeNativeUpdate("delete from gl_etldwhs where pardesc in ('PSTR_ACC_R LOAD','GLVD_PSTR_UD','GLVD_PSTR LOAD','GLVD_BAL_R')");
        baseEntityRepository.executeNativeUpdate("delete from GLVD_PSTR_DH");
        baseEntityRepository.executeNativeUpdate("delete from GLVD_PSTR_UH");

        SingleActionJobBuilder RemoteBuilder = SingleActionJobBuilder.create().withClass(Overvalue2dwh.class);
        Properties prop = new Properties();
        prop.setProperty("OperDay","2016-03-23");
        prop.setProperty("WorkDay","2016-03-23");
        prop.setProperty("pdidMax","1");
        prop.setProperty("skipMI4GL","true");
        baseEntityRepository.executeNativeUpdate("update gl_pdjchgr set unf ='N' where operday ='2016-03-23' and  pod < '2016-03-23'");
        jobService.executeJob(RemoteBuilder.build(), prop);
        checkHeadersSuccessCount(1);
        checkFillPstrTabs();
    }

    private static void checkHeadersSuccessCount(int cnt) throws SQLException {
        DataRecord rec = baseEntityRepository.selectFirst("select count (1) cnt from GL_ETLDWHS where PARDESC = 'GLVD_BAL_R' and parvalue = 1");

        Assert.assertTrue(rec.getInteger("cnt") + "", cnt == rec.getInteger("cnt"));
    }
    private static void checkFillPstrTabs() throws SQLException {
        DataRecord rec = baseEntityRepository.selectFirst("select count(*) cnt from GLVD_PSTR_U");
        Assert.assertTrue( "count GLVD_PSTR_U = 0", rec.getInteger("cnt") > 0 );
        rec = baseEntityRepository.selectFirst("select count(*) cnt from GLVD_PSTR_D");
        Assert.assertTrue( "count GLVD_PSTR_D = 0", rec.getInteger("cnt") > 0 );
    }
}
