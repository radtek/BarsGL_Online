package ru.rbt.barsgl.ejbtesting.job.service;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ivan Sevastyanov
 */
@Singleton
public class TestingJobRegistrationBean implements TestingJobRegistration {

    private final Map<String, TestingJobInfo> jobs = new HashMap<>();

    @Lock(LockType.WRITE)
    public void registerJobAction(String jobName) {
        TestingJobInfo info = jobs.get(jobName);
        if (null == info) {
            info = new TestingJobInfo(jobName);
            jobs.put(jobName, info);
        }
        info.action();
    }

    @Lock(LockType.READ)
    public int getRunCount(String jobName) {
        TestingJobInfo info = jobs.get(jobName);
        return null != info ? info.getRunCount() : 0;
    }

    private class TestingJobInfo {
        private final String jobName;
        private int runCount = 0;

        public TestingJobInfo(String jobName) {
            this.jobName = jobName;
        }

        public void action() {
            runCount++;
        }

        public String getJobName() {
            return jobName;
        }

        public int getRunCount() {
            return runCount;
        }
    }

}
