package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.etc.AS400ProcedureRunner;
import ru.rbt.barsgl.ejb.etc.SshProcedureRunner;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.barsgl.ejb.repository.WorkdayRepository;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Date;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.StartLoaderTask;

/**
 * Created by ER22317 on 29.11.2016.
 */
public class StartLoaderTask extends AbstractJobHistoryAwareTask {
    @Inject
    private WorkdayRepository workdayRepository;
    @Inject
    private AS400ProcedureRunner as400ProcedureRunner;
    @Inject
    private SshProcedureRunner sshProcedureRunner;
    @EJB
    private PropertiesRepository propertiesRepository;

    public enum StartLoaderProp {
        Operday
    }
    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        try {
            Date d = (Date)properties.get(StartLoaderProp.Operday);
            workdayRepository.setGlWorkday(d);
            auditController.info(StartLoaderTask, "В BarsGl установлен workday " + dateUtils.dbDateString(d));
            workdayRepository.setRepWorkday(d);
            auditController.info(StartLoaderTask, "В BarsRep установлен workday " + dateUtils.dbDateString(d));

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

            String barsRepLoaderType = propertiesRepository.getString(PropertyName.BARSREP_LOADER_TYPE.getName());
            if("ssh".equals(barsRepLoaderType)){
                String host = propertiesRepository.getString(PropertyName.BARSREP_LOADER_SSH_HOST.getName());
                Long portObj = propertiesRepository.getNumber(PropertyName.BARSREP_LOADER_SSH_PORT.getName());
                int port = (portObj == null) ? 22 : portObj.intValue();
                String user = propertiesRepository.getString(PropertyName.BARSREP_LOADER_SSH_USER.getName());
                String pswd = propertiesRepository.getString(PropertyName.BARSREP_LOADER_SSH_PSWD.getName());
                String cmd = propertiesRepository.getString(PropertyName.BARSREP_LOADER_SSH_RUN_CMD.getName());
                sshProcedureRunner.executeSshCommand(host, user, pswd, port, cmd, null);
            } else {
                as400ProcedureRunner.callAsyncRep("/GCP/bank.jar", "lv.gcpartners.bank.util.LoadProcessNew", new Object[]{});
            }
            return true;
        } catch (Throwable e) {
            auditController.error(StartLoaderTask
                    , format("Ошибка при установке workday за '%s'", dateUtils.onlyDateString((Date)properties.get(StartLoaderProp.Operday))), null, e);
            return false;
        }
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        Date workDay = workdayRepository.getWorkday();
        try{
            Date operDay = getOperday(properties);
            Assert.isTrue(workDay.compareTo(operDay) != 0, () -> new ValidationError(ErrorCode.TASK_ERROR,
                    "od.currdate = workday("+dateUtils.dbDateString(workDay)+"), уже установленно"));
            Assert.isTrue(workDay.compareTo(operDay) < 0, () -> new ValidationError(ErrorCode.TASK_ERROR,
                    "od.currdate("+dateUtils.dbDateString(operDay)+") < workday("+dateUtils.dbDateString(workDay)+")"));
            properties.put(StartLoaderProp.Operday, operDay);

            return true;
        } catch (ValidationError validationError) {
            auditController.warning(StartLoaderTask, "Ошибка запуска "+validationError.getMessage(), null,"");
            return false;
        }
    }

    @Override
    protected void initExec(String jobName, Properties properties) {
    }

    @Override
    public Date getOperday(Properties properties) {
        try {
            return TaskUtils.getDateFromGLOD(properties, jobHistoryRepository, null);
        }catch(Exception e){
            throw new ValidationError(ErrorCode.TASK_ERROR, "Неожиданная ошибка: " + e.getMessage());
        }
    }

}
