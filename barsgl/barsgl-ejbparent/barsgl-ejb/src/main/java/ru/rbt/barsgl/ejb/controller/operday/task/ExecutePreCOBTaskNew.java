package ru.rbt.barsgl.ejb.controller.operday.task;

/**
 * Created by ER18837 on 16.03.17.
 */

import org.apache.log4j.Logger;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.bt.BalturRecalculator;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.BackvalueJournalController;
import ru.rbt.barsgl.ejb.controller.cob.CobRunningStepWork;
import ru.rbt.barsgl.ejb.controller.cob.CobRunningTaskController;
import ru.rbt.barsgl.ejb.controller.cob.CobStatRecalculator;
import ru.rbt.barsgl.ejb.controller.cob.CobStepResult;
import ru.rbt.barsgl.ejb.controller.od.DatLCorrector;
import ru.rbt.barsgl.ejb.controller.od.OperdaySynchronizationController;
import ru.rbt.barsgl.ejb.controller.operday.PreCobStepController;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.integr.bg.EtlPostingController;
import ru.rbt.barsgl.ejb.integr.oper.SuppressStornoTboController;
import ru.rbt.barsgl.ejb.repository.BatchPostingRepository;
import ru.rbt.barsgl.ejb.repository.EtlPackageRepository;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.job.TimerJobRepository;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.*;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.shared.ExceptionUtils;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.io.StringReader;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.*;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.PreCob;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Task;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.CLOSED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.*;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.BUFFER;
import static ru.rbt.barsgl.shared.enums.CobPhase.*;
import static ru.rbt.barsgl.shared.enums.CobStepStatus.Error;
import static ru.rbt.barsgl.shared.enums.CobStepStatus.Halt;
import static ru.rbt.ejbcore.validation.ErrorCode.*;

/**
 * Created by Ivan Sevastyanov
 * Перевод опердня в состояние PRE_COB
 */
public class ExecutePreCOBTaskNew extends AbstractJobHistoryAwareTask {

    public static final String TIME_LOAD_BEFORE_KEY = "timeLoadBefore";
    public static final String CHECK_CHRONOLOGY_KEY = "chronology";
    public static final String CHECK_PACKAGES_KEY = "checkPackages";

    private static final Logger log = Logger.getLogger(ExecutePreCOBTaskNew.class);

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

    @Inject
    private TimerJobRepository jobRepository;

    @Inject
    private CobRunningTaskController taskController;

    @EJB
    private CobStatRecalculator statRecalculator;

    @EJB
    private DatLCorrector balturCorrector;

    /**
     * проверка нужно ли запускать задачу взависимости от того запускалась ли она в ОД  AbstractJobHistoryAwareTask#getOperday(java.util.Properties)
     * @param jobName
     * @param properties
     * @return true если проверка прошла и задача должна выполняться
     */
    @Override
    protected boolean checkJobStatus(String jobName, Properties properties) {
        return checkJobStatus(jobName, properties, null);
    }

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        return checkRun(jobName, properties, null);
    }

    @Override
    public boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        final Operday operday = operdayController.getOperday();

        auditController.info(AuditRecord.LogCode.Operday, format("Запуск процедуры закрытия ОД: '%s'", dateUtils.onlyDateString(operday.getCurrentDate())));

        List<CobRunningStepWork> works = new ArrayList<>();

        works.add(new CobRunningStepWork(CobStopEtlProc, (Long idCob, CobPhase phase) -> {
            return waitStopProcessing(operday, idCob, phase);
        }));

        works.add(new CobRunningStepWork(CobResetBuffer, (Long idCob, CobPhase phase) -> {
            return synchronizePostings(idCob, phase);
        }));

        works.add(new CobRunningStepWork(CobManualProc, (Long idCob, CobPhase phase) -> {
            return processUnprocessedBatchPostings(operday, idCob, phase);
        }));

        works.add(new CobRunningStepWork(CobStornoProc, (Long idCob, CobPhase phase) -> {
            return reprocessStorno(operday, idCob, phase);
        }));

        works.add(new CobRunningStepWork(CobCloseBalance, (Long idCob, CobPhase phase) -> {
            return closeBalance(operday, idCob, phase);
        }));

        works.add(new CobRunningStepWork(CobFanProc, (Long idCob, CobPhase phase) -> {
            return processFan(operday, idCob, phase);
        }));

        works.add(new CobRunningStepWork(CobRecalcBaltur, (Long idCob, CobPhase phase) -> {
            return recalculateAll(operday, idCob, phase);
        }));

        Long idCob = statRecalculator.calculateCob(true);
        if (taskController.execWorks(works, idCob)) {
            auditController.info(AuditRecord.LogCode.Operday, "Процедура закрытия ОД отработала успешно");
            return true;
        } else
            return false;
    }

    /**
     * step 1
     * @param operday
     * @return
     */
    public CobStepResult waitStopProcessing(Operday operday, Long idCob, CobPhase phase) {
        String msgBad = format("Не удалось остановить обработку проводок. Закрытие дня '%s' прервано"
                , dateUtils.onlyDateString(operday.getCurrentDate()));
        try {
            if (!synchronizationController.waitStopProcessingOnly()) {
                return new CobStepResult(Halt, msgBad, "Истекло время ожидания");
            }
            return new CobStepResult(CobStepStatus.Success, "Обработка проводок остановлена успешно");
        } catch (Throwable t) {
            return stepErrorResult(Halt, msgBad, t);
        }
    }

    /**
     * step 2
     * @return
     */
    public CobStepResult synchronizePostings(Long idCob, CobPhase phase) {
        if (BUFFER == operdayController.getOperday().getPdMode()) {
            statInfo(idCob, phase, format("Режим ввода проводок '%s'. Начало синхронизации проводок", BUFFER));
            try {
                if (beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) -> {
                    beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence1, connection1) -> {
                        String res = synchronizationController.syncPostings();
                        statInfo(idCob, phase, res);
                        DataRecord stats = synchronizationController.getBifferStatistic();
                        Assert.isTrue(stats.getLong("pd_cnt") == 0, () -> new DefaultApplicationException("Остались полупроводки в буфере после синхронизации"));
                        Assert.isTrue(stats.getLong("bal_cnt") == 0, () -> new DefaultApplicationException("Остались обороты в буфере после синхронизации"));
                        return null;
                    });
                    return true;
                })) {
                    beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) -> {
                        statInfo(idCob, phase, format("Перенесено проводок из буфера в архив: %s"
                                , synchronizationController.moveGLPdsToHistory(operdayController.getOperday().getCurrentDate())));
                        operdayController.swithPdMode(operdayController.getOperday().getPdMode());
                        return null;
                    });
                    return new CobStepResult(CobStepStatus.Success, "Синхронизация проводок завершена успешно");
                } else {
                    return new CobStepResult(Halt,
                            "Синхронизация проводок не завершена. Закрытие операционного дня прервано", "Превышено время обработки");
                }
            } catch (Exception e) {
                return stepErrorResult(Halt,
                        "Синхронизация проводок не завершена. Закрытие операционного дня прервано", e);
            }
        } else {
            return new CobStepResult(CobStepStatus.Skipped, format("Режим ввода проводок '%s'. Синхронизация не требуется", operdayController.getOperday().getPdMode()));
        }
    }

    /**
     * step 3
     * @param operday
     * @return
     */
    public CobStepResult processUnprocessedBatchPostings(Operday operday, Long idCob, CobPhase phase) {
        try {
            return (CobStepResult) repository.executeInNewTransaction(persistence -> {
                Date curdate = operday.getCurrentDate();
                auditController.info(PreCob, format("Проверка наличия необработанных запросов на операцию (ручных и из файла) за дату '%s'",
                        dateUtils.onlyDateString(curdate)));

                DataRecord res = packageRepository
                        .selectFirst("SELECT COUNT(1) CNT FROM GL_BATPST WHERE PROCDATE = ? and STATE <> ? and INVISIBLE = ?"
                                , curdate, BatchPostStatus.COMPLETED.name(), InvisibleType.N.name());

                if (0 != res.getLong(0)) {
                    statWarning(idCob, phase, format("Необработанных запросов на операцию за дату '%s': %d",
                            dateUtils.onlyDateString(curdate), res.getLong(0) ));
                    // это идет в своей транзакции
                    String msg = preCobBatchPostingTask.executeWork();
                    return new CobStepResult(CobStepStatus.Success, msg);
                            //format("Обработано %d запросов на операцию (ручных и из файла)", res.getLong(0)));
                } else {
                    return new CobStepResult(CobStepStatus.Skipped, format("Необработанные запросы на операцию за дату '%s' отсутствуют",
                            dateUtils.onlyDateString(curdate)));
                }
            });
        } catch (Throwable e) {
            return stepErrorResult(Error, "Ошибка обработки необработанных запросов на операцию", e);
        }
    }

    /**
     * step 4
     * @param operday
     * @return
     */
    public CobStepResult reprocessStorno(Operday operday, Long idCob, CobPhase phase) {
        try {
            return beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) -> {
//                auditController.info(AuditRecord.LogCode.Operday, "Повторная обработка сторно");
                return beanManagedProcessor.executeInNewTxWithDefaultTimeout((connection1, persistence1) ->
                        etlPostingController.reprocessErckStorno(operday.getLastWorkingDay(), operday.getCurrentDate()));
            });
        } catch (Exception e) {
            return stepErrorResult(Error, "Ошибка при повторной обработке сторно", e);
        }
    }

    /**
     * step 5
     * @param operday
     * @return
     */
    public CobStepResult closeBalance(Operday operday, Long idCob, CobPhase phase) {
        if (ONLINE == operday.getPhase() && OPEN == operday.getLastWorkdayStatus()) {
            statInfo(idCob, phase, format("Баланс предыдущего ОД открыт. Запуск процедуры закрытия БАЛАНСА предыдущего ОД из задачи ЗАКРЫТИЯ ОД: '%s'"
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
//                auditController.error(AuditRecord.LogCode.Operday, msg, null, e);
                return stepErrorResult(Halt, msg, e);
            }
        }
        return checkCurrentState(idCob, phase);
    }

    private CobStepResult checkCurrentState(Long idCob, CobPhase phase) {
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
//            auditController.warning(AuditRecord.LogCode.Operday, msg, null, new ValidationError(CLOSE_OPERDAY_ERROR, e.getMessage()));
            return stepErrorResult(Halt, msg, new ValidationError(CLOSE_OPERDAY_ERROR, e.getMessage()));
        }
        return new CobStepResult(CobStepStatus.Success, "Баланс предыдущего дня закрыт");
    }

    /**
     * step 6
     * @param operday
     * @return
     */
    public CobStepResult processFan(Operday operday, Long idCob, CobPhase phase) {
        // устанавливаем статус ОД
        try {
            // устанавливаем статус ОД
            setPreCobState(idCob, phase);
        } catch (Exception e) {
            statError(idCob, phase, format("Ошибка установки операционного дня в статус %s", PRE_COB), e);
        }
        try {
            return beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) -> {

                statInfo(idCob, phase, "Обработка вееров");
                return beanManagedProcessor.executeInNewTxWithDefaultTimeout((connection1, persistence1) -> preCobStepController.processFan());
            });
        } catch (Exception e) {
            return stepErrorResult(Error, "Ошибка при обработке вееров", e);
        }
    }

    /**
     * step 7
     * @param operday
     * @return
     */
    public CobStepResult recalculateAll(Operday operday, Long idCob, CobPhase phase) {
        try {
            beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) -> {
                // пересчет отстатков по проводкам, подавленным/перенесенным вручную
                int cnt1 = balturRecalculator.recalculateBaltur();
                statInfo(idCob, phase, format("Пересчитано остатков по подавл/перенесенным '%s'", cnt1));

                int cnt2 = suppressDuplication.suppress();
                statInfo(idCob, phase, format("Подавлено дублирующихся проводок по сделкам TBO: %s", cnt2));

                try {
                    long cnt3 = (long) repository.executeInNewTransaction(persistence1 -> balturCorrector.correctDatL());
                    statInfo(idCob, phase, format("Скорректировано BALTUR.DATL (дата последней операции): %s", cnt3));
                } catch (Throwable e) {
                    auditController.error(PreCob, "Ошибка при корректировке дат последней операции в балансе", null, e);
                }
                return null;
            });
        } catch (Exception e) {
            String msg = "Ошибка на этапе пересчета остатков / подавления дублей TBO";
            auditController.warning(AuditRecord.LogCode.Operday, msg, null, e);
            return stepErrorResult(Error, msg, e);
        }
        try {
            int cnt = (int) repository.executeInNewTransaction(persistence -> recalculateLocalization(idCob, phase));
            statInfo(idCob, phase, format("Выпонен пересчет/локализация по %d счетам", cnt));
        } catch (Exception e) {
            String msg = "Ошибка на этапе пересчета локализации";
            auditController.warning(AuditRecord.LogCode.Operday, msg, null, e);
            return stepErrorResult(Error, msg, e);
        }
        try {
            repository.executeInNewTransaction(persistence1 -> {
                operdayController.setCOB();
                statInfo(idCob, phase, format("Операционный день установлен в статус %s", COB));
                return null;
            });
        } catch (Exception e) {
            String msg = format("Ошибка при установке состояния операционного дняв статус %s", COB);
            auditController.warning(AuditRecord.LogCode.Operday, msg, null, e);
            return stepErrorResult(Halt, msg, e);
        }
        try {
            return (CobStepResult) repository.executeInNewTransaction(p -> {
                operdayController.setProcessingStatus(ProcessingStatus.ALLOWED);
                return new CobStepResult(CobStepStatus.Success, "Разрешен запуск обработки проводок");
            });
        } catch (Exception e) {
            String msg = "Ошибка разрешения запуска обработки проводок. Статус не изменен";
            auditController.warning(AuditRecord.LogCode.Operday, msg, null, e);
            return stepErrorResult(Halt, msg, e);
        }
    }

    private Integer recalculateLocalization(Long idCob, CobPhase phase) {
        String operdayString = dateUtils.onlyDateString(operdayController.getOperday().getCurrentDate());
        try {
            log.info(format("Начало пересчета/локализации при закрытии ОД '%s'", operdayString));
            int cnt = backvalueJournalController.recalculateLocal();
            log.info(format("Успешное окончание пересчета/локализации при закрытии ОД '%s'", operdayString));
            return cnt;
        } catch (Exception e) {
            statError(idCob, phase, format("Ошибка при пересчете/локализации при закрытии ОД '%s'", operdayString), e);
            return 0;
        }
    }

    private void setPreCobState(Long idCob, CobPhase phase) throws Exception {
        repository.executeInNewTransaction(pers-> {
            operdayController.setPreCOB();
            statInfo(idCob, phase, format("Операционный день установлен в статус %s", PRE_COB));
            return null;
        });
    }

// ----------------- проверки возможности запуска

    public RpcRes_Base<Boolean> checkEnableRun(String jobName, Properties properties) throws Exception {
        List<String> errList = new ArrayList<>();
        boolean check = checkJobStatus(jobName, properties, errList)
                && checkRun(jobName, properties, errList);
        return new RpcRes_Base<>(check, false, StringUtils.listToString(errList, ";\n "));
    }


    protected boolean checkJobStatus(String jobName, Properties properties, List<String> errList) {
        try {
            Assert.isTrue(!jobHistoryRepository.isTaskOK(jobName, getOperday(properties))
                    , () -> new ValidationError(OPERDAY_TASK_ALREADY_EXC, jobName, dateUtils.onlyDateString(getOperday(properties))));
            JobHistory history = getHistory(properties);
            boolean isAlreadyRunning = (null != history) ?
                    jobHistoryRepository.isAlreadyRunning(jobName, history.getId(), getOperday(properties)) :
                    jobHistoryRepository.isAlreadyRunning(jobName, getOperday(properties));
            Assert.isTrue(!isAlreadyRunning
                    , () -> new ValidationError(OPERDAY_TASK_ALREADY_RUN, jobName, dateUtils.onlyDateString(getOperday(properties))));
            return true;
        } catch (ValidationError e) {
            auditController.warning(Task, format("Задача %s не выполнена", jobName), null, e);
            if (null != errList)
                errList.add(getErrorMessage(e));
            return false;
        }
    }

    protected boolean checkRun(String jobName, Properties properties, List<String> errList) throws Exception {
        return !(!checkChronology(operdayController.getOperday().getCurrentDate()
                , operdayController.getSystemDateTime(), properties, errList)
                || !checkPackagesToloadExists(properties, errList));

    }

    public boolean checkChronology(Date operday, Date systemDate, Properties properties, List<String> errList) {
        String checkchronology = Optional.ofNullable(properties.getProperty(CHECK_CHRONOLOGY_KEY)).orElse("true");
        Date etalonCloseDate = org.apache.commons.lang3.time.DateUtils.addDays(operday, 1);
        if (Boolean.parseBoolean(checkchronology)) {
            if (systemDate.compareTo(etalonCloseDate) >= 0) {
                return true;
            } else {
                String msg = format("Проверка хронологии закрытия ОД включена. " +
                                "Текущее системное время '%s' меньше необходимого для закрытия '%s'"
                        , dateUtils.fullDateString(systemDate)
                        , dateUtils.onlyDateString(etalonCloseDate));
                auditController.info(PreCob, msg);
                if (null != errList)
                    errList.add(msg);
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * Используем для проверки при нажатии кнопки перевод в PRE_COB
     * @return true, если возможно
     * @throws SQLException
     */
    public boolean checkPackagesToloadExists() throws Exception {
        TimerJob job = jobRepository.selectOne(TimerJob.class, "from TimerJob j where j.name = ?1", this.getClass().getSimpleName());
        Properties properties = new Properties();
        if (null != job.getProperties())
            properties.load(new StringReader(job.getProperties()));
        return checkPackagesToloadExists(properties, null);
    }

    public boolean checkPackagesToloadExists(Properties properties, List<String> errList) throws SQLException {
        if (Boolean.parseBoolean(Optional.ofNullable(properties.getProperty(CHECK_PACKAGES_KEY)).orElse("true"))) {
            int state = repository.selectFirst("select state from v_gla_cob_ok").getInteger("state");
            if (0 == state) {
                auditController.warning(AuditRecord.LogCode.Operday, "Невозможно закрыть ОД", null, "Есть необработанные пакеты");
                if (null != errList)
                    errList.add("Невозможно закрыть ОД: Есть необработанные пакеты");
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

    private String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class,
                IllegalArgumentException.class, PersistenceException.class, DefaultApplicationException.class);
    }

    private void statInfo(Long idCob, CobPhase phase, String message) {
        statRecalculator.addStepInfo(idCob, phase, message);
    };

    private void statWarning(Long idCob, CobPhase phase, String message) {
        statRecalculator.addStepWarning(idCob, phase, message);
    };

    private void statError(Long idCob, CobPhase phase, String message, Throwable e) {
        statRecalculator.addStepError(idCob, phase, message, e);
    };

    private CobStepResult stepErrorResult(CobStepStatus result, String message, Throwable e) {
        auditController.error(PreCob, message, null, null, e);
        return new CobStepResult(result, message, getErrorMessage(e));
    };
}
