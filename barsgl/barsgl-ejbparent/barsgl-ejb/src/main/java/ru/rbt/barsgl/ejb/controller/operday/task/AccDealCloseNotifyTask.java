package ru.rbt.barsgl.ejb.controller.operday.task;

import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.CustomerNotifyQueueController;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

import javax.ejb.EJB;
import java.util.Optional;
import java.util.Properties;

import static ru.rbt.audit.entity.AuditRecord.LogCode.CustomerDetailsNotify;

/**
 * Created by er18837 on 20.03.2018.
 */
public class AccDealCloseNotifyTask implements ParamsAwareRunnable {
    private static final Logger log = Logger.getLogger(AccDealCloseNotifyTask.class);

    @EJB
    private AuditController auditController;

    @EJB
    private CustomerNotifyQueueController queueController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        try {
            queueController.process(properties);
        } catch (Exception e) {
            String topics = Optional.ofNullable(properties.getProperty("mq.topics")).orElse("");
            log.info(jobName + " " + topics,e);
            auditController.error(CustomerDetailsNotify, "Ошибка при выполнении задачи AccDealCloseNotifyTask, topics:" + topics, null, e);
        }
    }
}
