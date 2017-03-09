package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.tasks.ejb.entity.task.JobHistory;
import ru.rbt.barsgl.ejb.integr.bg.FanNdsPostingController;
import ru.rbt.barsgl.ejb.repository.WorkprocRepository;
import ru.rbt.barsgl.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static ru.rbt.barsgl.audit.entity.AuditRecord.LogCode.FlexNdsFan;

/**
 * Created by Ivan Sevastyanov on 28.11.2016.
 */
public class ProcessFlexFanTask extends AbstractJobHistoryAwareTask {

    @Inject
    private FanNdsPostingController fanNdsPostingController;

    @Inject
    private WorkprocRepository workprocRepository;

    @EJB
    private OperdayController operdayController;

    @EJB
    private AuditController auditController;

    @Inject
    private DateUtils dateUtils;

    private enum FlexFanContext {
        WORKDAY
    }

    @Override
    protected boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        Date workday = (Date) properties.get(FlexFanContext.WORKDAY);
        auditController.info(FlexNdsFan
                , String.format("Запуск задачи формирования вееров НДС по ФЛЕКС за дату: '%s'", dateUtils.onlyDateString(workday)));
        fanNdsPostingController.processTransitPostings(workday);
        return true;
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        try {
            Date operday = (Date) properties.get(FlexFanContext.WORKDAY);
            String waitStep = Optional.ofNullable(properties.getProperty("stepName")).orElse("MI3GL");
            DataRecord waitWorkprocRecord = workprocRepository.getWorkprocRecord(waitStep, operday);
            Assert.isTrue(null != waitWorkprocRecord && "O".equals(waitWorkprocRecord.getString("RESULT"))
                , () -> new ValidationError(ErrorCode.TASK_ERROR, String.format("Не выполнен шаг %s в дате '%s'", waitStep, dateUtils.onlyDateString(operday))));
            return true;
        } catch (ValidationError validationError) {
            auditController.error(FlexNdsFan, "Не прошла проверка возм выполнения задачи", null, validationError);
            return false;
        }
    }

    @Override
    protected void initExec(String jobName, Properties properties) {
        try {
            Date operday = TaskUtils.getExecuteDate("pDate", properties
                    , workprocRepository.selectFirst("select workday from workday").getDate("workday"));
            properties.put(FlexFanContext.WORKDAY, operday);
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }

    }
}
