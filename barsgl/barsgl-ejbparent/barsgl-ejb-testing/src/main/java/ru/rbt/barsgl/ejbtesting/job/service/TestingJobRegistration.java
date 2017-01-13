package ru.rbt.barsgl.ejbtesting.job.service;

import javax.ejb.Local;

/**
 * Created by Ivan Sevastyanov
 */
@Local
public interface TestingJobRegistration {

    void registerJobAction(String jobName);
    int getRunCount(String jobName);
}

