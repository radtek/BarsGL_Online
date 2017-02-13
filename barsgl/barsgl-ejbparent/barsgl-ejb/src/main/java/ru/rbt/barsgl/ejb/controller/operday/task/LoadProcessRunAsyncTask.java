package ru.rbt.barsgl.ejb.controller.operday.task;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.etc.AS400ProcedureRunner;
import ru.rbt.barsgl.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static ru.rbt.barsgl.audit.entity.AuditRecord.LogCode.AccountQuery;


/**
 * Created by ER22228 on 14.03.2016.
 */
public class LoadProcessRunAsyncTask implements ParamsAwareRunnable {

    private static final Logger log = Logger.getLogger(LoadProcessRunAsyncTask.class);

    @EJB
    private AuditController auditController;

    @Inject
    private AS400ProcedureRunner runner;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        try {
            List objectsList = new ArrayList();
            properties.stringPropertyNames().stream().forEach(key -> objectsList.add(properties.get(key)));
            runner.callAsyncGl("/GCP/bank.jar", "lv.gcpartners.bank.util.LoadProcessNew", objectsList.toArray());
//            runner.callAsync("/GCP/async.jar", "ru.rb.test.Example", objectsList.toArray());
//            runner.callSynhro("/GCP","/GCP/bank.jar", "lv.gcpartners.bank.util.LoadProcessNew", objectsList.toArray());
        } catch (Exception e) {
            log.info(jobName, e);
            auditController.error(AccountQuery, "Ошибка при выполнении задачи LoadProcessRunAsyncTask", null, e);
        }
    }
}
