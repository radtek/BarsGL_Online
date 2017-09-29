package ru.rbt.barsgl.ejb.controller.operday.task;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.etc.AS400ProcedureRunner;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.etc.SshProcedureRunner;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejb.repository.properties.PropertiesRepository;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountQuery;


/**
 * Created by ER22228 on 14.03.2016.
 */
public class LoadProcessRunAsyncTask implements ParamsAwareRunnable {

    private static final Logger log = Logger.getLogger(LoadProcessRunAsyncTask.class);

    @EJB
    private AuditController auditController;

    @EJB
    private PropertiesRepository propertiesRepository;

    @Inject
    private AS400ProcedureRunner as400ProcedureRunner;

    @Inject
    private SshProcedureRunner sshProcedureRunner;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        try {
            List objectsList = new ArrayList();
            properties.stringPropertyNames().stream().forEach(key -> objectsList.add(properties.get(key)));

            String barsGlLoaderType = propertiesRepository.getString(PropertyName.BARSGL_LOADER_TYPE.getName());
            if("ssh".equals(barsGlLoaderType)){
                String host = propertiesRepository.getString(PropertyName.BARSGL_LOADER_SSH_HOST.getName());
                Long portObj = propertiesRepository.getNumber(PropertyName.BARSGL_LOADER_SSH_PORT.getName());
                int port = (portObj == null) ? 22 : portObj.intValue();
                String user = propertiesRepository.getString(PropertyName.BARSGL_LOADER_SSH_USER.getName());
                String pswd = propertiesRepository.getString(PropertyName.BARSGL_LOADER_SSH_PSWD.getName());
                String cmd = propertiesRepository.getString(PropertyName.BARSGL_LOADER_SSH_RUN_CMD.getName());
                sshProcedureRunner.executeSshCommand(host, user, pswd, port, cmd, null);
            } else {
                as400ProcedureRunner.callAsyncGl("/GCP/bank.jar", "lv.gcpartners.bank.util.LoadProcessNew", new Object[]{});
            }

            as400ProcedureRunner.callAsyncGl("/GCP/bank.jar", "lv.gcpartners.bank.util.LoadProcessNew", objectsList.toArray());
//            runner.callAsync("/GCP/async.jar", "ru.rb.test.Example", objectsList.toArray());
//            runner.callSynhro("/GCP","/GCP/bank.jar", "lv.gcpartners.bank.util.LoadProcessNew", objectsList.toArray());
        } catch (Exception e) {
            log.info(jobName, e);
            auditController.error(AccountQuery, "Ошибка при выполнении задачи LoadProcessRunAsyncTask", null, e);
        }
    }
}
