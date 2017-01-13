package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.entity.task.JobHistory;
import ru.rbt.barsgl.ejb.etc.AS400ProcedureRunner;
import ru.rbt.barsgl.ejbcore.util.DateUtils;

import javax.inject.Inject;
import javax.xml.bind.ValidationException;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.PlClose707Del;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.PlClose707Create;

import java.util.Date;
import java.util.Properties;

/**
 * Created by ER22317 on 09.12.2016.
 */
public class PlCloseTask extends AbstractJobHistoryAwareTask {
    @Inject
    private AS400ProcedureRunner as400ProcedureRunner;

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        Date d = null;
        //0 - OverValue707("Переоценка"), 1 - Exchangre707("Курсовая разница"), 2 - Other707("Остальное"),
        //3 - Alone("Для одного счета"), 4 - All("Все типы");
        int type707 = 0;
        String task = "";
        try {
            task = (String)properties.get("Task");
            if (task == null || task.isEmpty())
                throw new ValidationException("Задана пустая задача");
            if (task.equals("Create")){
                String s = (String)properties.get("StartDate");
                if (s == null || s.isEmpty()) d = getOperday(properties);
                else d = DateUtils.dbDateParse(s);
                s = (String)properties.get("Create707");
                if (s != null && !s.isEmpty()) type707 = Integer.valueOf(s);
                auditController.info(PlClose707Create, "Запуск создания счетов 707, тип "+type707+", дата "+d);
                as400ProcedureRunner.callAsync("/GCP/bank.jar", "lv.gcpartners.bank.plclose.PlCloseSet707",
                        new Object[]{DateUtils.dbDateString(d),type707});
                auditController.info(PlClose707Create, "Завершение создания счетов 707");
            }else if(task.equals("Del")){
                String s = (String)properties.get("StartDate");
                if (s == null || s.isEmpty()) d = getOperday(properties);
                else d = DateUtils.dbDateParse(s);
                s = (String)properties.get("Del707");
                if (s != null && !s.isEmpty()) type707 = Integer.valueOf(s);
                auditController.info(PlClose707Del, "Запуск удаления счетов 707, тип "+type707+", дата "+d);
                as400ProcedureRunner.callAsync("/GCP/bank.jar", "lv.gcpartners.bank.plclose.PlCloseDel707",
                        new Object[]{DateUtils.dbDateString(d),type707});
                auditController.info(PlClose707Del, "Завершение удаления счетов 707");
            }else{
                throw new ValidationException("Задача не найдена "+task);
            }
            return true;
        } catch (Throwable e) {
            auditController.error(PlClose707Del, "Ошибка при запуске: задача "+task+" , тип "+type707+", дата "+d, null, e);
            return false;
        }
    }

    @Override
    protected boolean checkJobStatus(String jobName, Properties properties){
        return true;
    }


    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        return true;
    }

    @Override
    protected void initExec(String jobName, Properties properties) {

    }

}
