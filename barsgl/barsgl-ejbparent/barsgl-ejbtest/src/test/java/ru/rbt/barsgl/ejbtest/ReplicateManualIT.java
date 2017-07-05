package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.ReplicateManualTask;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;

import java.text.ParseException;
import java.util.Date;

import static org.apache.commons.lang3.time.DateUtils.parseDate;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;

/**
 * Created by ER22317 on 03.03.2016.
 */
public class ReplicateManualIT extends AbstractTimerJobIT{

    @Test
    public void replicateManual() throws Exception {

        setOperday(parseDate("07072014", "ddMMyyyy"), parseDate("04072014", "ddMMyyyy"), ONLINE, OPEN);

        SingleActionJobBuilder ReplicTaskBuilder = SingleActionJobBuilder.create().withClass(ReplicateManualTask.class);
        jobService.executeJob(ReplicTaskBuilder.build());
    }

    @Test
    public void testCheckrun() throws ParseException {
        // init
        Date sysdate10 = parseDate("2014-01-10", "yyyy-MM-dd");
        Date workday9 = parseDate("2014-01-09", "yyyy-MM-dd");
        Assert.assertEquals(workday9, getWorkdayBefore(sysdate10));
//        Assert.assertTrue(isWorkday(parseDate("2014-01-10 06", "yyyy-MM-dd HH")));
//        Assert.assertTrue(isWorkday(parseDate("2014-01-09 06", "yyyy-MM-dd HH")));
//        Assert.assertFalse(isWorkday(parseDate("2014-01-08 06", "yyyy-MM-dd HH")));
        Assert.assertTrue(isWorkday(parseDate("2014-01-10", "yyyy-MM-dd")));
        Assert.assertTrue(isWorkday(parseDate("2014-01-09", "yyyy-MM-dd")));
        Assert.assertFalse(isWorkday(parseDate("2014-01-08", "yyyy-MM-dd")));

        Date sysdate8 = parseDate("2014-01-09", "yyyy-MM-dd");
        Date workday1 = parseDate("2014-01-01", "yyyy-MM-dd");
        Assert.assertEquals(workday1, getWorkdayBefore(sysdate8));

//        Assert.assertTrue(check(parseDate("2014-01-09 06", "yyyy-MM-dd HH")));
//        Assert.assertFalse(check(parseDate("2014-01-08 06", "yyyy-MM-dd HH")));
//        Assert.assertFalse(check(parseDate("2014-01-10 06", "yyyy-MM-dd HH")));
        Assert.assertTrue(check(parseDate("2014-01-09", "yyyy-MM-dd")));
        Assert.assertFalse(check(parseDate("2014-01-08", "yyyy-MM-dd")));
        Assert.assertFalse(check(parseDate("2014-01-10", "yyyy-MM-dd")));
    }

    private boolean check(Date sysdate) {
        return remoteAccess.invoke(ReplicateManualTask.class, "checkRun", sysdate);
    }

}
