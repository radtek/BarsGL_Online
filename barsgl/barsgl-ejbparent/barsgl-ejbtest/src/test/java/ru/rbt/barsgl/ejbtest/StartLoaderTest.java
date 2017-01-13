package ru.rbt.barsgl.ejbtest;
import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.operday.task.StartLoaderTask;
import ru.rbt.barsgl.ejb.repository.WorkdayRepository;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;

import java.util.Date;
import java.util.Properties;
import ru.rbt.barsgl.ejbcore.util.DateUtils;

/**
 * Created by ER22317 on 30.11.2016.
 */
public class StartLoaderTest extends AbstractTimerJobTest{
    @Test
    public void testWithOnline() throws Exception {
//        Date currentDate = getOperday().getCurrentDate();
        Date lastWorkingDay = getOperday().getLastWorkingDay();
        Operday.LastWorkdayStatus status = getOperday().getLastWorkdayStatus();
        Date workday = remoteAccess.invoke(WorkdayRepository.class, "getWorkday");
        Date newLastWorkingDay = DateUtils.addDay(workday, 1);
        Date currentDate = DateUtils.addDay(workday, 1);;

        try{
//            remoteAccess.invoke(TaskUtils.class, "clearGlEtldwhs4Pardesc", DwhUnloadParams.SetWorkday, currentDate );
            setOperday(currentDate, newLastWorkingDay ,Operday.OperdayPhase.ONLINE, status);
            Properties properties = new Properties();
            SingleActionJobBuilder builder = SingleActionJobBuilder.create().withClass(StartLoaderTask.class).withProps(properties);
            jobService.executeJob(builder.build());

            Date afterSetWorkday = remoteAccess.invoke(WorkdayRepository.class, "getWorkday");
            Assert.assertTrue("newLastWorkingDay = " + new DateUtils().dbDateString(newLastWorkingDay) + ";afterSetWorkday = " + new DateUtils().dbDateString(afterSetWorkday),
                              afterSetWorkday.compareTo(newLastWorkingDay) == 0);

        }finally {
            restoreOperday();
            remoteAccess.invoke(WorkdayRepository.class, "setWorkday", workday );
        }

    }

    @Test
    // currentDate = workday
    public void testErr1() throws Exception {
        Date currentDate = getOperday().getCurrentDate();
        Date newWorkday = getOperday().getCurrentDate();
        Date lastWorkingDay = getOperday().getLastWorkingDay();
        Operday.LastWorkdayStatus status = getOperday().getLastWorkdayStatus();
        Date workday = remoteAccess.invoke(WorkdayRepository.class, "getWorkday");
//        Date newLastWorkingDay = DateUtils.addDay(workday, 1);

        try{
//            remoteAccess.invoke(TaskUtils.class, "clearGlEtldwhs4Pardesc", DwhUnloadParams.SetWorkday, currentDate );
            setOperday(currentDate, lastWorkingDay ,Operday.OperdayPhase.ONLINE, status);
            remoteAccess.invoke(WorkdayRepository.class, "setWorkday", newWorkday );

            Properties properties = new Properties();
            SingleActionJobBuilder builder = SingleActionJobBuilder.create().withClass(StartLoaderTask.class).withProps(properties);
            jobService.executeJob(builder.build());

            Date afterSetWorkday = remoteAccess.invoke(WorkdayRepository.class, "getWorkday");
            Assert.assertTrue("newWorkday = " + new DateUtils().dbDateString(newWorkday) + ";afterSetWorkday = " + new DateUtils().dbDateString(afterSetWorkday),
                    afterSetWorkday.compareTo(newWorkday) == 0);

        }finally {
            restoreOperday();
            remoteAccess.invoke(WorkdayRepository.class, "setWorkday", workday );
        }

    }

    @Test
    // currentDate < workday
    public void testErr2() throws Exception {
        Date currentDate = getOperday().getCurrentDate();
        Date newWorkday = DateUtils.addDay(getOperday().getCurrentDate(), 1);
        Date lastWorkingDay = getOperday().getLastWorkingDay();
        Operday.LastWorkdayStatus status = getOperday().getLastWorkdayStatus();
        Date workday = remoteAccess.invoke(WorkdayRepository.class, "getWorkday");
//        Date newLastWorkingDay = DateUtils.addDay(workday, 1);

        try{
//            remoteAccess.invoke(TaskUtils.class, "clearGlEtldwhs4Pardesc", DwhUnloadParams.SetWorkday, currentDate );
            setOperday(currentDate, lastWorkingDay ,Operday.OperdayPhase.ONLINE, status);
            remoteAccess.invoke(WorkdayRepository.class, "setWorkday", newWorkday );

            Properties properties = new Properties();
            SingleActionJobBuilder builder = SingleActionJobBuilder.create().withClass(StartLoaderTask.class).withProps(properties);
            jobService.executeJob(builder.build());

            Date afterSetWorkday = remoteAccess.invoke(WorkdayRepository.class, "getWorkday");
            Assert.assertTrue("newWorkday = " + new DateUtils().dbDateString(newWorkday) + ";afterSetWorkday = " + new DateUtils().dbDateString(afterSetWorkday),
                    afterSetWorkday.compareTo(newWorkday) == 0);

        }finally {
            restoreOperday();
            remoteAccess.invoke(WorkdayRepository.class, "setWorkday", workday );
        }

    }

    @Test
    public void testWithCOB() throws Exception {
        Date lastWorkingDay = getOperday().getLastWorkingDay();
        Operday.LastWorkdayStatus status = getOperday().getLastWorkdayStatus();
        Date workday = remoteAccess.invoke(WorkdayRepository.class, "getWorkday");
        Date newCurrentDate = DateUtils.addDay(workday, 1);

        try{
//            remoteAccess.invoke(TaskUtils.class, "clearGlEtldwhs4Pardesc", DwhUnloadParams.SetWorkday, newCurrentDate );
            setOperday(newCurrentDate, lastWorkingDay, Operday.OperdayPhase.COB, status);
            Properties properties = new Properties();
            SingleActionJobBuilder builder = SingleActionJobBuilder.create().withClass(StartLoaderTask.class).withProps(properties);
            jobService.executeJob(builder.build());

            Date afterSetWorkday = remoteAccess.invoke(WorkdayRepository.class, "getWorkday");
            Assert.assertTrue("newCurrentDate = " + new DateUtils().dbDateString(newCurrentDate) + ";afterSetWorkday = " + new DateUtils().dbDateString(afterSetWorkday),
                              afterSetWorkday.compareTo(newCurrentDate) == 0);

        }finally {
            restoreOperday();
            remoteAccess.invoke(WorkdayRepository.class, "setWorkday", workday );
        }

    }

    @Test
    public void testWithoutCOB() throws Exception {
        Date lastWorkingDay = getOperday().getLastWorkingDay();
        Operday.LastWorkdayStatus status = getOperday().getLastWorkdayStatus();
        Date workday = remoteAccess.invoke(WorkdayRepository.class, "getWorkday");
        Date newCurrentDate = DateUtils.addDay(workday, 1);

        try{
//            remoteAccess.invoke(TaskUtils.class, "clearGlEtldwhs4Pardesc", DwhUnloadParams.SetWorkday, newCurrentDate );
            setOperday(newCurrentDate, lastWorkingDay, Operday.OperdayPhase.PRE_COB, status);
            Properties properties = new Properties();
            SingleActionJobBuilder builder = SingleActionJobBuilder.create().withClass(StartLoaderTask.class).withProps(properties);
            jobService.executeJob(builder.build());

            Date afterSetWorkday = remoteAccess.invoke(WorkdayRepository.class, "getWorkday");
            Assert.assertTrue("workday = " + new DateUtils().dbDateString(workday) + ";afterSetWorkday = " + new DateUtils().dbDateString(afterSetWorkday),
                               afterSetWorkday.compareTo(workday) == 0);

        }finally {
            restoreOperday();
            remoteAccess.invoke(WorkdayRepository.class, "setWorkday", workday );
        }

    }

}
