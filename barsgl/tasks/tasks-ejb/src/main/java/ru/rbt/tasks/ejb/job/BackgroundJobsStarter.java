package ru.rbt.tasks.ejb.job;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejbcore.job.BackgroundJobService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;

/**
 * Created by Ivan Sevastyanov
 */
@Singleton
@Startup
@TransactionManagement(TransactionManagementType.BEAN)
public class BackgroundJobsStarter {

  private static final Logger log = Logger.getLogger(BackgroundJobsStarter.class);

  @EJB
  private BackgroundJobService jobService;

  @EJB
  private AuditController auditController;

  @EJB
  private OperdayController operdayController;

  @PostConstruct
  public void init() {
    try {
      log.info("Запуск всех автоматических фоновых задач");
      jobService.startupAll();
    } catch (Throwable e) {
      auditController.error(AuditRecord.LogCode.JobControl, "Ошибка при запуске задач", null, e);
      throw e;
    }
  }

  @PreDestroy
  public void tearDown() {
    try {
      log.info("Остановка всех фоновых задач");
      jobService.shutdownAll();
    } catch (Throwable e) {
      // Correct shutdown
      log.error("Ошибка при остановке всех фоновых задач", e);
      try{
        auditController.error(AuditRecord.LogCode.JobControl, "Ошибка при остановке задач", null, e);
      }catch (Throwable t) {
        log.error("Ошибка при записи в аудит при остановке всех фоновых задач", t);        
      }
      //throw e;
    }
  }
}
