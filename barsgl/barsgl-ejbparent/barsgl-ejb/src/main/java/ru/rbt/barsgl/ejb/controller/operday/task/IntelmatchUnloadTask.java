package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rb.cfg.CryptoUtil;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.etc.SshCommand;
import ru.rbt.barsgl.ejb.etc.SshCommandBuilder;
import ru.rbt.barsgl.ejb.etc.SshProcedureRunner;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Task;

/**
 * Created by Ivan Sevastyanov on 07.09.2018.
 */
public class IntelmatchUnloadTask extends AbstractJobHistoryAwareTask {

    @EJB
    private PropertiesRepository propertiesRepository;

    @Inject
    private SshProcedureRunner sshProcedureRunner;

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        try {
            SshCommand cmd = SshCommandBuilder.create()
                    .withUser(propertiesRepository.getString(PropertyName.BARSREP_REPL_SSH_USER.getName()))
                    .withEncryptedPassword(propertiesRepository.getString(PropertyName.BARSREP_REPL_SSH_PSWD.getName()))
                    .withHost(propertiesRepository.getString(PropertyName.BARSREP_REPL_SSH_HOST.getName()))
                    .withPort(Optional.ofNullable(propertiesRepository.getNumber(PropertyName.BARSREP_REPL_SSH_PORT.getName()))
                        .map(Long::intValue).orElse(22))
                    .withCommand(propertiesRepository.getString(PropertyName.INTELMATCH_SSH_CMD.getName())).build();
            auditController.info(Task, format("Запуск процедуры выгрузки Intelmatch c параметрами: %s", cmd));
            sshProcedureRunner.executeSshCommand(cmd);
        } catch (Throwable e) {
            auditController.error(Task, "Ошибка при запуске процедуры выгрущки Intelmatch", null, e);
            return false;
        }
        return true;
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        return true;
    }

    @Override
    protected void initExec(String jobName, Properties properties) throws Exception {}

    public void encrypt() {
        System.out.println("Encrypted password: '" + CryptoUtil.encrypt("4Uh98586") + "'");
    }
}
