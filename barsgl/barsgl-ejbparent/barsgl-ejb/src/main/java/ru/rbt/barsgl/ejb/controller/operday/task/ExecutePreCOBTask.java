package ru.rbt.barsgl.ejb.controller.operday.task;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.bt.BalturRecalculator;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.BackvalueJournalController;
import ru.rbt.barsgl.ejb.controller.od.OperdaySynchronizationController;
import ru.rbt.barsgl.ejb.controller.operday.PreCobStepController;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.integr.bg.EtlPostingController;
import ru.rbt.barsgl.ejb.integr.oper.SuppressStornoTboController;
import ru.rbt.barsgl.ejb.repository.EtlPackageRepository;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.job.TimerJobRepository;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.InvisibleType;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.*;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.CLOSED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.*;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.BUFFER;
import static ru.rbt.ejbcore.validation.ErrorCode.CLOSE_OPERDAY_ERROR;

/**
 * Created by Ivan Sevastyanov
 * Перевод опердня в состояние PRE_COB
 */
@Deprecated
public class ExecutePreCOBTask extends AbstractJobHistoryAwareTask {

    public static final String TIME_LOAD_BEFORE_KEY = "timeLoadBefore";
    public static final String CHECK_CHRONOLOGY_KEY = "chronology";
    public static final String CHECK_PACKAGES_KEY = "checkPackages";

    public static final String NAME = "ExecutePreCOBTask";

    private static final Logger log = Logger.getLogger(ExecutePreCOBTask.class);


//    @Inject
//    private OperdayController operdayController;
//
//    @Inject
//    private DateUtils dateUtils;

    @Inject
    private EtlPackageRepository packageRepository;

//    @Inject
//    private BatchPostingRepository postingRepository;

    @EJB
    private PreCobStepController preCobStepController;

    @EJB
    private EtlPostingController etlPostingController;

    @EJB
    BalturRecalculator balturRecalculator;

//    @EJB
//    private AuditController auditController;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @Inject
    private CloseLastWorkdayBalanceTask closeLastWorkdayBalanceTask;

//    @Inject
//    private EtlStructureMonitorTask monitorTask;

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

//    @EJB
//    private JobHistoryRepository jobHistoryRepository;

    @Inject
    private TimerJobRepository jobRepository;

    @Override
    protected boolean checkRun(String jobName, Properties properties) throws Exception {
        return !(!checkChronology(operdayController.getOperday().getCurrentDate()
                    , operdayController.getSystemDateTime(), properties)
                || !checkPackagesToloadExists(properties));

    }

    @Override
    public boolean execWork(JobHistory jobHistory, Properties properties) throws Exception {
        final Operday operday = operdayController.getOperday();

        auditController.info(Operday, format("Запуск процедуры закрытия ОД: '%s'", dateUtils.onlyDateString(operday.getCurrentDate())));

        if (!synchronizationController.waitStopProcessing()) {
            auditController.error(Operday, format("Не удалось остановить обработку проводок. Закрытие дня '%s' прервано"
                    , dateUtils.onlyDateString(operday.getCurrentDate())), null, "");
            return false;
        }

        if (BUFFER == operdayController.getOperday().getPdMode()) {
            auditController.info(Operday, format("Режим ввода проводок '%s'. Начало синхронизации проводок", BUFFER));
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
                    auditController.error(Operday, "Ошибка синхронизации проводок", null, e);
                    return false;
                }
            })) {
                beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) -> {
                    auditController.info(Operday, format("Перенесено проводок из буфера в архив: %s"
                            , synchronizationController.moveGLPdsToHistory(operdayController.getOperday().getCurrentDate())));
                    operdayController.swithPdMode(operdayController.getOperday().getPdMode());
                    return null;
                });
                auditController.info(Operday, "Окончание синхронизации проводок");
            } else {
                auditController.error(Operday,
                        "Синхронизация проводок не завершена. Закрытие операционного дня прервано", null, new DefaultApplicationException(""));
                return false;
            }
        }

        processUnprocessedBatchPostings();

        if (ONLINE == operday.getPhase() && OPEN == operday.getLastWorkdayStatus()) {
            auditController.info(Operday, format("Баланс предыдущего ОД открыт. Запуск процедуры закрытия БАЛАНСА предыдущего ОД из задачи ЗАКРЫТИЯ ОД: '%s'"
                    , dateUtils.onlyDateString(operday.getCurrentDate())));
            try {
                beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
                    closeLastWorkdayBalanceTask.executeWork(true); return null;
                }, 60 * 60);
            } catch (Exception e) {
                // пишем ошибку и выходим
                auditController.error(Operday, format("Ошибка запуска закрытия БАЛАНСА предыдущего ОД из задачи ЗАКРЫТИЯ ОД: '%s'"
                        , dateUtils.onlyDateString(operday.getCurrentDate())), null, e);
                return false;
            }
        }

        if (!checkCurrentState()) {
            return false;
        }
        try {
            beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) -> {
                // устанавливаем статус ОД
                setPreCobState();

                auditController.info(Operday, "Повторная обработка сторно");
                beanManagedProcessor.executeInNewTxWithDefaultTimeout((connection1, persistence1) -> closeLastWorkdayBalanceTask.reprocessErckStorno(operday.getLastWorkingDay(), operday.getCurrentDate()));

                auditController.info(Operday, "Обработка вееров");
                beanManagedProcessor.executeInNewTxWithDefaultTimeout((connection1, persistence1) -> preCobStepController.processFan());

                // пересчет отстатков по проводкам, подавленным/перенесенным вручную
                auditController.info(BalturRecalc
                        , format("Пересчитано остатков по подавл/перенесенным '%s'", balturRecalculator.recalculateBaltur()));

                auditController.info(PreCob, format("Подавлено дублирующихся проводок по сделкам TBO: %s", suppressDuplication.suppress()));
                return null;
            });
        } catch (Exception e) {
            auditController.error(PreCob, "Ошибка закрытия ОД на этапе обработки сторно, вееров, подавления дублей TBO...", null, e);
            return false;
        }

        repository.executeInNewTransaction(persistence1 -> {
            repository.executeInNewTransaction(persistence -> recalculateLocalization());
            auditController.info(Operday, format("Установка состояния операционного дня в статус %s", COB));
            operdayController.setCOB();
            return null;
        });

        try {
            repository.executeInNewTransaction(p -> {operdayController.setProcessingStatus(ProcessingStatus.ALLOWED); return null;});
        } catch (Exception e) {
            auditController.warning(Operday, "Ошибка разрешения запуска обработки проводок. Статус не изменен", null, e);
        }
        auditController.info(Operday, "Процедура закрытия ОД отработала успешно");
        return true;
    }

    private boolean checkCurrentState() {
        final Operday operday = operdayController.getOperday();
        try {
            Assert.isTrue(ONLINE == operday.getPhase() || PRE_COB == operday.getPhase()
                    , format("Недопустимая фаза '%s' текущего операционного дня '%s'"
                    , operday.getPhase(), dateUtils.onlyDateString(operday.getCurrentDate())));
            Assert.isTrue(CLOSED == operday.getLastWorkdayStatus()
                    , format("Недопустимый статус '%s' баланса предыдущего операционного дня '%s'"
                    , operday.getLastWorkdayStatus(), dateUtils.onlyDateString(operday.getLastWorkingDay())));
        } catch (IllegalArgumentException e) {
            auditController.warning(Operday
                    , format("Невозможно закрыть ОД '%s'", dateUtils.onlyDateString(operday.getCurrentDate()))
                    , null, new ValidationError(CLOSE_OPERDAY_ERROR, e.getMessage()));
            return false;
        }
        return true;
    }

    public void processUnprocessedBatchPostings() throws Exception {
        try {
            repository.executeInNewTransaction(persistence -> {
                Date curdate = operdayController.getOperday().getCurrentDate();
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
                }
                return null;
            });
        } catch (Throwable e) {
            auditController.error(PreCob, "Ошибка обработки необработанных запросов на операцию", null, e);
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
            auditController.info(Operday, format("Установка состояния операционного дня в статус %s", PRE_COB));
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
                auditController.warning(Operday, "Невозможно закрыть ОД", null, "Есть необработанные пакеты");
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
}
