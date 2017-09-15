package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rb.cfg.CryptoUtil;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.etc.AS400ProcedureRunner;
import ru.rbt.barsgl.ejb.etc.SshProcedureRunner;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.barsgl.ejb.repository.WorkmodRepository;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.shared.enums.Repository;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.Date;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.StartLoaderTask;
import static ru.rbt.audit.entity.AuditRecord.LogCode.StartReplicationTask;

/**
 * Created by ER19391 on 11.09.2017.
 */
public class StartReplicationTask  extends AbstractJobHistoryAwareTask {

    public static final String REPL = "REPL";

    @Inject
    private WorkmodRepository workmod;

    @EJB
    private PropertiesRepository propertiesRepository;

    @Inject
    private AS400ProcedureRunner as400ProcedureRunner;

    @Inject
    private SshProcedureRunner sshProcedureRunner;

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        try {
            String barsGlLoaderType = propertiesRepository.getString(PropertyName.BARSREP_REPL_TYPE.getName());
            if(ru.rbt.barsgl.ejb.controller.operday.task.StartLoaderTask.StartLoaderType.ssh.name().equals(barsGlLoaderType)){
                String host = propertiesRepository.getString(PropertyName.BARSREP_REPL_SSH_HOST.getName());
                Long portObj = propertiesRepository.getNumber(PropertyName.BARSREP_REPL_SSH_PORT.getName());
                int port = (portObj == null) ? 22 : portObj.intValue();
                String user = propertiesRepository.getString(PropertyName.BARSREP_REPL_SSH_USER.getName());
                String ecryptedPswd = propertiesRepository.getString(PropertyName.BARSREP_REPL_SSH_PSWD.getName());
                String pswd = CryptoUtil.decrypt(ecryptedPswd);
                String cmd = propertiesRepository.getString(PropertyName.BARSREP_REPL_SSH_RUN_CMD.getName());
                sshProcedureRunner.executeSshCommand(host, user, pswd, port, cmd, null);
            } else {
                as400ProcedureRunner.callAsyncGl("/GCP/bank.jar", "ru.rb.ucb.loader.replication.BARSReplicatorNew", new Object[]{});
            }
            return true;
        } catch (Throwable e) {
            auditController.error(StartLoaderTask
                    , "Ошибка при запуске репликации"
                    , null
                    , e
            );
            return false;
        }
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        if(workmod.isStepAlreadyRunning(Repository.BARSREP, REPL)) {
            auditController.warning(StartReplicationTask
                    , "Невозможно запустить процесс репликации"
                    , null
                    , "Репликация уже работает"
            );
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected void initExec(String jobName, Properties properties) {
    }
}
