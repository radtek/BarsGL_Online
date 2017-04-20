package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadParams;
import ru.rbt.barsgl.ejb.controller.operday.task.balsep.AccountBalanceUnloadThree;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by ER22228
 * Выгрузка данных о проводках и остатках в DWH
 * @fsd 8.2
 */
public class AccountBalanceUnloadTest extends AbstractTimerJobTest {

    public static final Logger logger = Logger.getLogger(AccountBalanceUnloadTest.class.getName());

    @Test @Ignore("в настоящее время выгрузка в DWH не производится")
    public void testFull() throws Exception {

        setOperday(DateUtils.dbDateParse("2017-02-09"), DateUtils.dbDateParse("2017-02-08")
                , Operday.OperdayPhase.COB, Operday.LastWorkdayStatus.CLOSED);

        checkCreateStep("P9", DateUtils.dbDateParse("2017-02-08"), "O");

        baseEntityRepository.executeNativeUpdate("update gl_etldwhs set parvalue = '2'");

        SingleActionJob job = SingleActionJobBuilder.create()
                .withClass(AccountBalanceUnloadThree.class)
                                  .withProps("operday=2016-03-12")
                                  .build();
        jobService.executeJob(job);

        Optional<DataRecord> result = Optional.ofNullable(baseEntityRepository.selectFirst(
                "select * from gl_etldwhs where PARDESC = ? and operday = ?"
                , DwhUnloadParams.UnloadBalanceThree.getParamDesc(), getOperday().getCurrentDate()));

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals("O", result.get().getString("parvalue"));

    }
}
