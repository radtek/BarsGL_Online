package ru.rbt.barsgl.ejbcore.job;

import org.apache.log4j.Logger;
import ru.rbt.ejbcore.DefaultApplicationException;

import javax.naming.InitialContext;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

public final class JobWrapper implements Serializable {

    private static final Logger LOG = Logger.getLogger(JobWrapper.class);

    private final long creationTimestamp;
    private long execCount;

    private final JobDefinition job;

    private final AtomicBoolean inProgress = new AtomicBoolean(false);

    public JobWrapper(JobDefinition job) {
        if (job == null) {
            throw new IllegalArgumentException("Job definition cannot be null");
        }
        this.job = job;
        this.creationTimestamp = System.currentTimeMillis();
        this.execCount = 0;
    }

    public String getName() {
        return job.getName();
    }

    public long getCreateTimestamp() {
        return creationTimestamp;
    }

    public long getExecCount() {
        return execCount;
    }

    public JobDefinition getJobDefinition() {
        return job;
    }

    public void exec() throws Exception {
        if (inProgress.compareAndSet(false, true)) {
            try {
                execCount++;
                findBackgroundService().executeJob(job.getWorker().getClass(), job.getName(), job.getProperties());
            } finally {
                inProgress.set(false);
            }
        } else {
            LOG.warn(String.format("A previous execution of timer '%s' is still in progress, skipping this overlapping scheduled execution", job.getName()));
        }
    }

    @Override
    public String toString() {
        return "ru.rbt.barsgl.ejbcore.job.JobWrapper{" +
                "creationTimestamp=" + creationTimestamp +
                ", execCount=" + execCount +
                ", job=" + job +
                ", inProgress=" + inProgress +
                '}';
    }

    private BackgroundJobService findBackgroundService() {
        try {
            return (BackgroundJobService) new InitialContext().lookup("java:module/BackgroundJobServiceBean");
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

}
