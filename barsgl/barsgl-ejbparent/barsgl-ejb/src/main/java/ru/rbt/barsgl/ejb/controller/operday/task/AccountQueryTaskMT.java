package ru.rbt.barsgl.ejb.controller.operday.task;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.CommonQueueProcessor4;
import ru.rbt.barsgl.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

import javax.ejb.EJB;
import java.util.Properties;

import static ru.rbt.barsgl.audit.entity.AuditRecord.LogCode.AccountQuery;


/**
 * Created by ER22228 on 14.03.2016.
 */
public class AccountQueryTaskMT implements ParamsAwareRunnable {

    private static final Logger log = Logger.getLogger(AccountQueryTaskMT.class);

    @EJB
    private AuditController auditController;

    @EJB
    private CommonQueueProcessor4 queueProcessor;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        try {
            queueProcessor.process(properties);
        } catch (Exception e) {
            log.info(jobName,e);
            auditController.error(AccountQuery, "Ошибка при выполнении задачи AccountQueryTask", null, e);
        }
    }
}
