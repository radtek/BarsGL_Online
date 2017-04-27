package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.operday.task.AccountBalanceUnloadTask;
import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadParams;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.barsgl.ejb.repository.WorkprocRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus.ERROR;
import static ru.rbt.audit.entity.AuditRecord.LogCode.OverValueAcc2GlAcc;

/**
 * Created by ER22317 on 01.11.2016.
 */
public class OverValueAcc2GlAccTask implements ParamsAwareRunnable {
    @EJB
    private CoreRepository repository;
    @EJB
    private OperdayController operdayController;
    @EJB
    private AuditController auditController;
    @Inject
    private DateUtils dateUtils;
    @Inject
    private WorkprocRepository workprocRepository;
    @Inject
    private TaskUtils taskUtils;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        Date executeDate = TaskUtils.getDateFromGLOD(properties, repository, operdayController.getOperday());
        auditController.info(OverValueAcc2GlAcc
                , format("Начало добавления счетов переоценки и курсовой разницы в gl_acc за дату: '%s'", dateUtils.onlyDateString(executeDate)));

        long headerId = 0;
        try {
            if (checkRun(properties, executeDate)) {
                headerId = taskUtils.createHeaders(DwhUnloadParams.UnloadOverValueAcc, executeDate);
                DataRecord ret = repository.selectFirst("select GL_OVERVALUE_ACC() from sysibm.sysdummy1", new Object[]{});
                auditController.info(OverValueAcc2GlAcc, format("Добавлено %d счетов переоценки", ret.getBigInteger(0)));
                ret = repository.selectFirst("select GL_EXCHANGE_ACC() from sysibm.sysdummy1", new Object[]{});
                auditController.info(OverValueAcc2GlAcc, format("Добавлено %d счетов курсовой", ret.getBigInteger(0)));

                taskUtils.setResultStatus(headerId, DwhUnloadStatus.SUCCEDED);

            }
        }catch (Exception e) {
            auditController.error(OverValueAcc2GlAcc
                    , format("Ошибка добавления счетов переоценки и курсовой разницы в gl_acc за дату: '%s'", dateUtils.onlyDateString(executeDate)), null, e);
            if (0 < headerId) {
                taskUtils.setResultStatus(headerId, ERROR);
            }

        }

        auditController.info(OverValueAcc2GlAcc
                , format("Окончание добавления счетов переоценки и курсовой разницы в gl_acc за дату: '%s'", dateUtils.onlyDateString(executeDate)));

    }

    public boolean checkRun(Properties properties, Date executeDate) throws Exception{
        try {
            if (TaskUtils.getCheckRun(properties, true)) {
                Assert.isTrue(0 == TaskUtils.getDwhAlreadyHeaderCount(executeDate, DwhUnloadParams.UnloadOverValueAcc, repository)
                                , () -> new ValidationError(ErrorCode.ALREADY_UNLOADED
                                , format("Уже запущена или выполнено в текущем ОД (%s)", dateUtils.onlyDateString(executeDate))));

                final String stepName = Optional.ofNullable(properties.getProperty("stepName")).orElse("P9").trim();
                final boolean isStepOk = workprocRepository.isStepOK(stepName, TaskUtils.getExecuteDate("operday", properties, executeDate));
                Assert.isTrue(isStepOk, () -> new ValidationError(ErrorCode.TASK_ERROR
                        , format("Не завершен шаг '%s' в ОД '%s'", stepName, dateUtils.onlyDateString(executeDate))));

                return true;
            } else {
                return true;
            }
        } catch (ValidationError validationError) {
            auditController.warning(OverValueAcc2GlAcc, "Ошибка запуска счетов переоценки и курсовой разницы", null, validationError);
            return false;
        }
    }

}
