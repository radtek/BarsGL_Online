/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.barsgl.ejb.controller.operday.task;

import java.util.Properties;
import javax.ejb.EJB;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import org.apache.log4j.Logger;
import ru.rbt.barsgl.audit.controller.AuditController;
import static ru.rbt.barsgl.audit.entity.AuditRecord.LogCode.AccountQuery;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.CommonQueueProcessor4;
import ru.rbt.barsgl.ejb.jms.MQConnectionManager;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

/**
 *
 * @author Andrew Samsonov
 */
public class AsyncAccountQueryTaskMT implements ParamsAwareRunnable, ExceptionListener {

    private static final Logger log = Logger.getLogger(AsyncAccountQueryTaskMT.class);

    @EJB
    private AuditController auditController;

    @EJB
    private CommonQueueProcessor4 queueProcessor;

    @EJB
    private MQConnectionManager connectionManager;
    
    private Properties properties;
    
    @Override
    public void run(String jobName, Properties properties) throws Exception {
      try {
        this.properties = properties;
        startJMS();        
      } catch (Exception e) {
        log.info(jobName, e);
        auditController.error(AccountQuery, "Ошибка при выполнении задачи AccountQueryTask", null, e);
      }
    }

  private void startJMS() throws Exception, JMSException {
    connectionManager.start(properties, null, this);
    
    queueProcessor.setSession(connectionManager.createOutgouingSession());
    queueProcessor.setQueueProperties(properties);
    
    connectionManager.setMessageListener(queueProcessor);
  }

    @Override
    public void onException(JMSException exception) {
      log.info("\n\nonException calling");
      try {
        connectionManager.stop();
        reconnect();
      } catch (Exception ex) {
        log.error(ex);
      }
    }        

    private void reconnect() {
      // Restore connection by timer?
    }    
}
