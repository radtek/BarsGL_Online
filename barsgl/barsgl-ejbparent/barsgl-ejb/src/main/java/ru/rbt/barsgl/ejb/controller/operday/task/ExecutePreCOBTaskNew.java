package ru.rbt.barsgl.ejb.controller.operday.task;

/**
 * Created by ER18837 on 16.03.17.
 */

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.bt.BalturRecalculator;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.BackvalueJournalController;
import ru.rbt.barsgl.ejb.controller.cob.CobRunningStepWork;
import ru.rbt.barsgl.ejb.controller.cob.CobRunningTaskController;
import ru.rbt.barsgl.ejb.controller.cob.CobStatRecalculator;
import ru.rbt.barsgl.ejb.controller.cob.CobStepResult;
import ru.rbt.barsgl.ejb.controller.od.OperdaySynchronizationController;
import ru.rbt.barsgl.ejb.controller.operday.PreCobStepController;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.entity.cob.CobStepStatistics;
import ru.rbt.barsgl.ejb.entity.sec.AuditRecord;
import ru.rbt.barsgl.ejb.entity.task.JobHistory;
import ru.rbt.barsgl.ejb.integr.bg.EtlPostingController;
import ru.rbt.barsgl.ejb.integr.oper.SuppressStornoTboController;
import ru.rbt.barsgl.ejb.repository.BatchPostingRepository;
import ru.rbt.barsgl.ejb.repository.EtlPackageRepository;
import ru.rbt.barsgl.ejb.repository.JobHistoryRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.job.TimerJobRepository;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbcore.util.StringUtils;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.Assert;
import ru.rbt.barsgl.shared.ExceptionUtils;
import ru.rbt.barsgl.shared.enums.*;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.io.StringReader;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.*;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.CLOSED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.PRE_COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.BUFFER;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.*;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.CLOSE_OPERDAY_ERROR;

/**
 * Created by Ivan Sevastyanov
 * Перевод опердня в состояние PRE_COB
 */
public class ExecutePreCOBTaskNew extends AbstractJobHistoryAwareTask {

    public static final String TIME_LOAD_BEFORE_KEY = "timeLoadBefore";
    public static final String CHECK_CHRONOLOGY_KEY = "chronology";
    public static final String CHECK_PACKAGES_KEY = "checkPackages";

    public static final String NAME = "ExecutePreCOBTask";

    private static final Logger log = Logger.getLogger(ExecutePreCOBTask.class);


    @Inject
    private OperdayController operdayController;

    @Inject
    private DateUtils dateUtils;

    @Inject
    private EtlPackageRepository packageRepository;

    @Inject
    private BatchPostingRepository postingRepository;

    @EJB
    private PreCobStepController preCobStepController;

    @EJB
    private EtlPostingController etlPostingController;

    @EJB
    BalturRecalculator balturRecalculator;

    @EJB
    private AuditController auditController;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @Inject
    private CloseLastWorkdayBalanceTask closeLastWorkdayBalanceTask;

    @Inject
    private EtlStructureMonitorTask monitorTask;

    @Inject
    private PreCobBatchPostingTask preCobBatchPostingTask;

    @Inject
    private SuppressStornoTboController suppressDuplication;

    @EJB
    private BackvalueJournalController backvalueJournalController;

    @EJB
    private OperdaySynchronizationController synchronizationController;

    @EJB
    private CoreRepository repository;

    @EJB
    private JobHistoryRepository jobHistoryRepository;

    @Inject
    private TimerJobRepository jobRepository;

    @Inject
    private CobRunningTaskController taskController;

    @EJB
    private CobStatRecalculator statRecalculator;

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        return !(!checkChronology(operdayController.getOperday().getCurrentDate()
                , operdayController.getSystemDateTime(), properties)
                || !checkPackagesToloadExists(properties));

    }

    @Override
    public boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        final Operday operday = operdayController.getOperday();

        auditController.info(AuditRecord.LogCode.Operday, format("Запуск процедуры закрытия ОД: '%s'", dateUtils.onlyDateString(operday.getCurrentDate())));

        List<CobRunningStepWork> works = new ArrayList<>();

        works.add(new CobRunningStepWork(CobStep.CobStopEtlProc, () -> {
            return waitStopProcessing(operday);
        }));

        works.add(new CobRunningStepWork(CobStep.CobStopEtlProc, () -> {
            return synchronizePostings();
        }));

        works.add(new CobRunningStepWork(CobStep.CobStopEtlProc, () -> {
            return processUnprocessedBatchPostings(operday);
        }));

        works.add(new CobRunningStepWork(CobStep.CobStornoProc, () -> {
            return reprocessStorno(operday);
        }));

        works.add(new CobRunningStepWork(CobStep.CobStornoProc, () -> {
            return closeBalance(operday);
        }));

        works.add(new CobRunningStepWork(CobStep.CobStornoProc, () -> {
            return processFan(operday);
        }));

        works.add(new CobRunningStepWork(CobStep.CobRecalcBaltur, () -> {
            return recalculateAll(operday);
        }));

        Long idCob = statRecalculator.calculateCob(true);
        for (CobRunningStepWork work : works) {
            CobStepStatistics step = taskController.executeWithLongRunningStep(idCob, work.getStep(), work.getWork());
            if (null == step){
                auditController.error(PreCob,
                        format("Не удалось создать шаг выполнения для '%s'", work), null, new DefaultApplicationException(""));
            } else
            if (step.getStatus() == CobStepStatus.Halt) {
                auditController.error(PreCob,
                        format("Сбой выполнения COB. Шаг %s: '%s'. Процесс остановлен", step.getPhaseNo().toString(), step.getPhaseName()), null, new DefaultApplicationException(""));
                return false;
            }
        }
        auditController.info(AuditRecord.LogCode.Operday, "Процедура закрытия ОД отработала успешно");
        return true;
    }

    private CobStepResult checkCurrentState() {
        final Operday operday = operdayController.getOperday();
        try {
            Assert.isTrue(ONLINE == operday.getPhase() || PRE_COB == operday.getPhase()
                    , format("Недопустимая фаза '%s' текущего операционного дня '%s'"
                            , operday.getPhase(), dateUtils.onlyDateString(operday.getCurrentDate())));
            Assert.isTrue(CLOSED == operday.getLastWorkdayStatus()
                    , format("Недопустимый статус '%s' баланса предыдущего операционного дня '%s'"
                            , operday.getLastWorkdayStatus(), dateUtils.onlyDateString(operday.getLastWorkingDay())));
        } catch (IllegalArgumentException e) {
            String msg = format("Невозможно закрыть ОД '%s'", dateUtils.onlyDateString(operday.getCurrentDate()));
            auditController.warning(AuditRecord.LogCode.Operday, msg, null, new ValidationError(CLOSE_OPERDAY_ERROR, e.getMessage()));
            return new CobStepResult(CobStepStatus.Halt, msg, e.getMessage());
        }
        return new CobStepResult(CobStepStatus.Success, "Баланс предыдущего дня закрыт");
    }

    public CobStepResult waitStopProcessing(Operday operday) {
        String msgBad = format("Не удалось остановить обработку проводок. Закрытие дня '%s' прервано"
                , dateUtils.onlyDateString(operday.getCurrentDate()));
        try {
            if (!synchronizationController.waitStopProcessingOnly()) {
                return new CobStepResult(CobStepStatus.Halt, msgBad, "Истекло время ожидания");
            }
            return new CobStepResult(CobStepStatus.Success, "Остановлена обработка проводок");
        } catch (Throwable t) {
            return new CobStepResult(CobStepStatus.Halt, msgBad, t.getMessage());
        }
    }

    public CobStepResult synchronizePostings() {
        if (BUFFER == operdayController.getOperday().getPdMode()) {
            auditController.info(AuditRecord.LogCode.Operday, format("Режим ввода проводок '%s'. Начало синхронизации проводок", BUFFER));
            try {
                if (beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) -> {
                    try {
                        beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence1, connection1) -> {
                            synchronizationController.syncPostings();
                            DataRecord stats = synchronizationController.getBifferStatistic();
                            Assert.isTrue(stats.getLong("pd_cnt") == 0, () -> new DefaultApplicationException("Остались полупроводки в буфере после синхронизации"));
                            Assert.isTrue(stats.getLong("bal_cnt") == 0, () -> new DefaultApplicationException("Остались обороты в буфере после синхронизации"));
                            return null;
                        });
                        return true;
                    } catch (Throwable e) {
                        auditController.error(AuditRecord.LogCode.Operday, "Ошибка синхронизации проводок", null, e);
                        return false;
                    }
                })) {
                    beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) -> {
                        auditController.info(AuditRecord.LogCode.Operday, format("Перенесено проводок из буфера в архив: %s"
                                , synchronizationController.moveGLPdsToHistory(operdayController.getOperday().getCurrentDate())));
                        operdayController.swithPdMode(operdayController.getOperday().getPdMode());
                        return null;
                    });
                    return new CobStepResult(CobStepStatus.Success, "Окончание синхронизации проводок");
                } else {
                    return new CobStepResult(CobStepStatus.Halt,
                            "Синхронизация проводок не завершена. Закрытие операционного дня прервано", "Превышено время обработки");
                }
            } catch (Exception e) {
                return new CobStepResult(CobStepStatus.Halt,
                        "Синхронизация проводок не завершена. Закрытие операционного дня прервано", getErrorMessage(e));
            }
        } else {
            return new CobStepResult(CobStepStatus.Skipped, format("Режим ввода проводок '%s'. Синхронизация не требуется", operdayController.getOperday().getPdMode()));
        }
    }

    public CobStepResult processUnprocessedBatchPostings(Operday operday) {
        try {
            return (CobStepResult) repository.executeInNewTransaction(persistence -> {
                Date curdate = operday.getCurrentDate();
                auditController.info(PreCob, format("Проверка наличия необработанных запросов на операцию (ручных и из файла) за дату '%s'",
                        dateUtils.onlyDateString(curdate)));

                DataRecord res = packageRepository
                        .selectFirst("SELECT COUNT(1) CNT FROM GL_BATPST WHERE PROCDATE = ? and STATE <> ? and INVISIBLE = ?"
                                , curdate, BatchPostStatus.COMPLETED.name(), InvisibleType.N.name());

                if (0 != res.getLong(0)) {
                    auditController.warning(PreCob, format("В системе имеются необработанные запросоы ('%s') на операцию за дату '%s'",
                            res.getLong(0), curdate), null, "");
                    // это идет в своей транзакции
                    preCobBatchPostingTask.executeWork();
                    return new CobStepResult(CobStepStatus.Success, format("Обработано %d запросов на операцию (ручных и из файла)", res.getLong(0)));
                } else {
                    return new CobStepResult(CobStepStatus.Skipped, "Необработанные запросы на операцию отсутствуют");
                }
            });
        } catch (Throwable e) {
            return new CobStepResult(CobStepStatus.Error, "Ошибка обработки необработанных запросов на операцию", getErrorMessage(e));
        }
    }

    public CobStepResult reprocessStorno(Operday operday) {
        try {
            return beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) -> {
                auditController.info(AuditRecord.LogCode.Operday, "Повторная обработка сторно");
                return beanManagedProcessor.executeInNewTxWithDefaultTimeout((connection1, persistence1) ->
                        etlPostingController.reprocessErckStorno(operday.getLastWorkingDay(), operday.getCurrentDate()));
            });
        } catch (Exception e) {
            return new CobStepResult(CobStepStatus.Error, "Ошибка при повторной обработке сторно", getErrorMessage(e));
        }
    }

    public CobStepResult closeBalance(Operday operday) {
        if (ONLINE == operday.getPhase() && OPEN == operday.getLastWorkdayStatus()) {
            auditController.info(AuditRecord.LogCode.Operday, format("Баланс предыдущего ОД открыт. Запуск процедуры закрытия БАЛАНСА предыдущего ОД из задачи ЗАКРЫТИЯ ОД: '%s'"
                    , dateUtils.onlyDateString(operday.getCurrentDate())));
            try {
                beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
                    closeLastWorkdayBalanceTask.executeWork(false);
                    return null;
                }, 60 * 60);
            } catch (Exception e) {
                // пишем ошибку и выходим
                String msg = format("Ошибка запуска закрытия БАЛАНСА предыдущего ОД из задачи ЗАКРЫТИЯ ОД: '%s'"
                        , dateUtils.onlyDateString(operday.getCurrentDate()));
                auditController.error(AuditRecord.LogCode.Operday, msg, null, e);
                return new CobStepResult(CobStepStatus.Halt, msg, getErrorMessage(e));
            }
        }
        return checkCurrentState();
    }

    public CobStepResult processFan(Operday operday) {
        try {
            return beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) -> {
                // устанавливаем статус ОД
                setPreCobState();

                auditController.info(AuditRecord.LogCode.Operday, "Обработка вееров");
                return beanManagedProcessor.executeInNewTxWithDefaultTimeout((connection1, persistence1) -> preCobStepController.processFan());
            });
        } catch (Exception e) {
            return new CobStepResult(CobStepStatus.Error, "Ошибка при обработке вееров", getErrorMessage(e));
        }
    }

    public CobStepResult recalculateAll(Operday operday) {
        List<String> msgList = new ArrayList<String>();
        try {
            beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) -> {
                // устанавливаем статус ОД
                setPreCobState();

                // пересчет отстатков по проводкам, подавленным/перенесенным вручную
                String msg1 = format("Пересчитано остатков по подавл/перенесенным '%s'", balturRecalculator.recalculateBaltur());
                auditController.info(BalturRecalc, msg1);
                msgList.add(msg1);

                String msg2 = format("Подавлено дублирующихся проводок по сделкам TBO: %s", suppressDuplication.suppress());
                auditController.info(PreCob, msg2);
                msgList.add(msg2);
                return null;
            });
        } catch (Exception e) {
            String msg = "Ошибка на этапе пересчета остатков / подавления дублей TBO";
            auditController.warning(AuditRecord.LogCode.Operday, msg, null, e);
            return new CobStepResult(CobStepStatus.Halt, msg, getErrorMessage(e));
        }
        try {
            repository.executeInNewTransaction(persistence -> recalculateLocalization());
            msgList.add("Выпонен пересчет локализации");
        } catch (Exception e) {
            String msg = "Ошибка на этапе пересчета локализации";
            auditController.warning(AuditRecord.LogCode.Operday, msg, null, e);
            return new CobStepResult(CobStepStatus.Halt, msg, getErrorMessage(e));
        }
        try {
            repository.executeInNewTransaction(persistence1 -> {
                auditController.info(AuditRecord.LogCode.Operday, format("Установка состояния операционного дня в статус %s", COB));
                operdayController.setCOB();
                msgList.add(format("Операционный день установлен в статус %s", COB));
                return null;
            });
        } catch (Exception e) {
            String msg = format("Ошибка при установке состояния операционного дняв статус %s", COB);
            auditController.warning(AuditRecord.LogCode.Operday, msg, null, e);
            return new CobStepResult(CobStepStatus.Halt, msg, getErrorMessage(e));
        }
        try {
            return (CobStepResult) repository.executeInNewTransaction(p -> {
                operdayController.setProcessingStatus(ProcessingStatus.ALLOWED);
                msgList.add("Разрешен запуск обработки проводок");
                return new CobStepResult(CobStepStatus.Success, StringUtils.listToString(msgList, " \n"));
            });
        } catch (Exception e) {
            String msg = "Ошибка разрешения запуска обработки проводок. Статус не изменен";
            auditController.warning(AuditRecord.LogCode.Operday, msg, null, e);
            return new CobStepResult(CobStepStatus.Halt, msg, getErrorMessage(e));
        }
    }

    public boolean checkChronology(Date operday, Date systemDate, Properties properties) {
        String checkchronology = Optional.ofNullable(properties.getProperty(CHECK_CHRONOLOGY_KEY)).orElse("true");
        Date etalonCloseDate = org.apache.commons.lang3.time.DateUtils.addDays(operday, 1);
        if (Boolean.parseBoolean(checkchronology)) {
            if (systemDate.compareTo(etalonCloseDate) >= 0) {
                return true;
            } else {
                auditController.info(PreCob, format("Проверка хронологии закрытия ОД включена. " +
                                "Текущее системное время '%s' меньше необходимого для закрытия '%s'"
                        , dateUtils.fullDateString(systemDate)
                        , dateUtils.onlyDateString(etalonCloseDate)));
                return false;
            }
        } else {
            return true;
        }
    }

    private boolean recalculateLocalization() {
        String operdayString = dateUtils.onlyDateString(operdayController.getOperday().getCurrentDate());
        try {
            log.info(format("Начало пересчета/локализации при закрытии ОД '%s'", operdayString));
            backvalueJournalController.recalculateLocal();
            log.info(format("Успешное окончание пересчета/локализации при закрытии ОД '%s'", operdayString));
            return true;
        } catch (Exception e) {
            auditController.error(Task, format("Ошибка при пересчете/локализации при закрытии ОД '%s'"
                    , operdayString), null, e);
            return false;
        }
    }


    private void setPreCobState() throws Exception {
        repository.executeInNewTransaction(pers-> {
            auditController.info(AuditRecord.LogCode.Operday, format("Установка состояния операционного дня в статус %s", PRE_COB));
            operdayController.setPreCOB(); return null;
        });
    }

    /**
     * Используем для проверки при нажатии кнопки перевод в PRE_COB
     * @return true, если возможно
     * @throws SQLException
     */
    public boolean checkPackagesToloadExists() throws Exception {
        TimerJob job = jobRepository.selectOne(TimerJob.class, "from TimerJob j where j.name = ?1", NAME);
        Properties properties = new Properties();
        properties.load(new StringReader(job.getProperties()));
        return checkPackagesToloadExists(properties);
    }

    public boolean checkPackagesToloadExists(Properties properties) throws SQLException {
        if (Boolean.parseBoolean(Optional.ofNullable(properties.getProperty(CHECK_PACKAGES_KEY)).orElse("true"))) {
            int state = repository.selectFirst("select state from v_gla_cob_ok").getInteger("state");
            if (0 == state) {
                auditController.warning(AuditRecord.LogCode.Operday, "Невозможно закрыть ОД", null, "Есть необработанные пакеты");
                return false;
            } else
            if (1 == state){
                return true;
            } else {
                throw new DefaultApplicationException(format("Недопустимое значение флага '%s' закрытия ОД", state));
            }
        } else {
            return true;
        }
    }

    @Override
    protected void initExec(String jobName, Properties properties) {}

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class,
                PersistenceException.class);
    }

}
