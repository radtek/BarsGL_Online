package ru.rbt.tasks.ejb.job;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejbcore.job.BackgroundJobService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Created by Ivan Sevastyanov
 */
@Singleton
@Startup
public class BackgroundJobsStarter {

    private static final Logger log = Logger.getLogger(BackgroundJobsStarter.class);

    @EJB
    private BackgroundJobService jobService;

    @EJB
    private OperdayController operdayController;

    @PostConstruct
    public void init() {
        log.info("Запуск всех автоматических фоновых задач");
        jobService.startupAll();
    }

    @PreDestroy
    public void tearDown() {
        log.info("Остановка всех фоновых задач");
        jobService.shutdownAll();
    }
}
