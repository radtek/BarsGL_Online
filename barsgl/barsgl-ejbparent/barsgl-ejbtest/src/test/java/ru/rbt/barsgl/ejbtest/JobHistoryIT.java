package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.tasks.ejb.entity.task.JobHistory;
import ru.rbt.tasks.ejb.repository.JobHistoryRepository;

import java.text.ParseException;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov on 27.06.2016.
 */
public class JobHistoryIT extends AbstractRemoteIT {

    @Test
    public void test() throws ParseException {
        final String taskName = "Name1";
        final Date operdate = DateUtils.parseDate("2015-02-26", "yyyy-MM-dd");
        baseEntityRepository.executeUpdate("delete from JobHistory h");
        Assert.assertFalse(remoteAccess.invoke(JobHistoryRepository.class, "isAlreadyRunning", taskName, operdate));
        Assert.assertFalse(remoteAccess.invoke(JobHistoryRepository.class, "isTaskOK", taskName, operdate));

        JobHistory history = remoteAccess.invoke(JobHistoryRepository.class, "createHeader", taskName, operdate);
        Assert.assertTrue(remoteAccess.invoke(JobHistoryRepository.class, "isAlreadyRunning", taskName, operdate));
        Assert.assertFalse(remoteAccess.invoke(JobHistoryRepository.class, "isTaskOK", taskName, operdate));

        remoteAccess.invoke(JobHistoryRepository.class, "updateStatus", history, DwhUnloadStatus.SUCCEDED);
        Assert.assertFalse(remoteAccess.invoke(JobHistoryRepository.class, "isAlreadyRunning", taskName, operdate));
        Assert.assertTrue(remoteAccess.invoke(JobHistoryRepository.class, "isTaskOK", taskName, operdate));

        remoteAccess.invoke(JobHistoryRepository.class, "updateStatus", history, DwhUnloadStatus.ERROR);
        Assert.assertFalse(remoteAccess.invoke(JobHistoryRepository.class, "isAlreadyRunning", taskName, operdate));
        Assert.assertFalse(remoteAccess.invoke(JobHistoryRepository.class, "isTaskOK", taskName, operdate));
    }
}
