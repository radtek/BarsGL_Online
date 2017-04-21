package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.common.repository.od.OperdayRepository;
import ru.rbt.barsgl.ejb.controller.od.OperdaySynchronizationController;
import ru.rbt.barsgl.ejb.controller.operday.PreCobStepController;
//import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.barsgl.ejb.entity.dict.CurrencyRate;
//import ru.rbt.barsgl.ejb.entity.task.JobHistory;
import ru.rbt.barsgl.ejb.integr.bg.EtlPostingController;
import ru.rbt.barsgl.ejb.repository.RateRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.job.BackgroundJobService;
//import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;
import ru.rbt.barsgl.ejb.controller.operday.task.ReprocessWtacOparationsTask;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.CLOSED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.USD;
import static ru.rbt.audit.entity.AuditRecord.LogCode.OpenOperday;
import static ru.rbt.ejbcore.validation.ErrorCode.OPEN_OPERDAY_ERROR;
import ru.rbt.barsgl.ejb.controller.operday.task.cmn.AbstractJobHistoryAwareTask;
import ru.rbt.tasks.ejb.entity.task.JobHistory;

/**
 * Created by Ivan Sevastyanov
 * Открытие операционного дня
 */
public class OpenOperdayTask extends AbstractJobHistoryAwareTask {

    private static final Logger log = Logger.getLogger(OpenOperdayTask.class.getName());

    public static final String FLEX_FINAL_STEP_RESULT_OK = "O";
    public static final String FLEX_FINAL_STEP_KEY = "flexStepName";
    public static final String FLEX_FINAL_MSG_OK = "MI5GL";

    public static final String PD_MODE_KEY = "pdMode";
    public static final String PD_MODE_DEFAULT = Operday.PdMode.BUFFER.name();

    @Inject
    private RateRepository rateRepository;

//    @Inject
//    private OperdayController operdayController;
//
//    @Inject
//    private DateUtils dateUtils;

    @Inject
    private BankCalendarDayRepository calendarDayRepository;

    @Inject
    private OperdayRepository operdayRepository;

//    @EJB
//    private EtlPostingController etlPostingController;
//
//    @EJB
//    private PreCobStepController preCobStepController;
//
//    @EJB
//    private AuditController auditController;

    @EJB
    private BackgroundJobService backgroundJobService;

    @EJB
    private OperdaySynchronizationController synchronizationController;

    @EJB
    private CoreRepository coreRepository;

    public enum OpenOperdayContextKey {
        CURRENT_OD
        , OPERDAY_TO_OPEN
        , TARGET_PD_MODE
    }

    @Override
    public boolean execWork (JobHistory jobHistory, Properties properties) throws Exception {
        Operday currentOperday = (Operday) properties.get(OpenOperdayContextKey.CURRENT_OD);
        Date operDayToOpen = (Date) properties.get(OpenOperdayContextKey.OPERDAY_TO_OPEN);

        // желаемый режим обработки проводок
        Operday.PdMode targetPdMode = (Operday.PdMode) properties.get(OpenOperdayContextKey.TARGET_PD_MODE);

        auditController.info(OpenOperday, format("Открытие операционного дня '%s'", dateUtils.onlyDateString(operDayToOpen)));
        auditController.info(OpenOperday, format(
                "\nТекущий операционный день '%s', фаза '%s', " +
                        "\nпредыдущий рабочий опердень '%s', статус баланса предыдущего опердня '%s'"
                , dateUtils.onlyDateString(currentOperday.getCurrentDate()), currentOperday.getPhase()
                , dateUtils.onlyDateString(currentOperday.getLastWorkingDay()), currentOperday.getLastWorkdayStatus()));

        setPdMode(targetPdMode);
        restartGlPdSequence();

        // открыть следующий опердень
        operdayController.openNextOperDay();

        // обработка операций WTAC (только если созданы условия)
        backgroundJobService.executeJob(ReprocessWtacOparationsTask.JOB_NAME);

        auditController.info(OpenOperday, format("Открытие операционного дня '%s' завершено успешно", dateUtils.onlyDateString(operDayToOpen)));

        return true;
    }

    @Override
    protected void initExec(String jobName, Properties properties) {
        properties.put(OpenOperdayContextKey.CURRENT_OD, operdayController.getOperday());
        properties.put(OpenOperdayContextKey.OPERDAY_TO_OPEN, calendarDayRepository
                .getWorkdayAfter(operdayController.getOperday().getCurrentDate()).getId().getCalendarDate());
        properties.put(OpenOperdayContextKey.TARGET_PD_MODE, calculatePdMode(properties));
    }

    @Override
    public boolean checkRun(String jobName, Properties properties) throws Exception {
        if (TaskUtils.getCheckRun(properties, true)) {
            Operday currentOperday = (Operday) properties.get(OpenOperdayContextKey.CURRENT_OD);
            Date operDayToOpen = (Date) properties.get(OpenOperdayContextKey.OPERDAY_TO_OPEN);
            boolean resultCurrentOdState =
                    checkOperdayCurrentStateForOpen(currentOperday, operDayToOpen)
                        && checkRates(operDayToOpen);

            Operday.PdMode targetPdMode = (Operday.PdMode) properties.get(OpenOperdayContextKey.TARGET_PD_MODE);

            boolean resultPdMode = true;
            if (DIRECT == targetPdMode) {
                resultPdMode = checkFlexSucceded(operDayToOpen, currentOperday.getCurrentDate(), properties);
            }
            return resultCurrentOdState && resultPdMode;
        }
        return true;
    }

    private boolean checkOperdayCurrentStateForOpen(Operday currentOperday, Date operDayToOpen) {
        boolean isCob = COB == currentOperday.getPhase();
        boolean isLwdClosed = CLOSED == currentOperday.getLastWorkdayStatus();
        if (!isCob || !isLwdClosed) {
            auditController.warning(OpenOperday
                    , format("Невозможно открыть следующий ОД: '%s'. ", dateUtils.onlyDateString(operDayToOpen)), null
                    , format("Неверный статус ОД. Текущий операционный день '%s' должен быть фазе '%s' <%s>, " +
                        "баланс предыдущего операционного дня '%s' в статусе '%s' <%s>", dateUtils.onlyDateString(currentOperday.getCurrentDate()), COB, isCob
                        , dateUtils.onlyDateString(currentOperday.getLastWorkingDay()), CLOSED, isLwdClosed));
            return false;
        } else {
            return true;
        }
    }

    private boolean checkRates(Date operDayToOpen) {
        CurrencyRate rate = rateRepository.findRate(USD, operDayToOpen);
        boolean isRateUsdExixts = null != rate;
        if (!isRateUsdExixts) {
            auditController.warning(OpenOperday
                    , format("Невозможно открыть следующий ОД: '%s'. ", dateUtils.onlyDateString(operDayToOpen)), null
                    , format("Курс валюты '%s' на день '%s' не установлен", USD.getId(), dateUtils.onlyDateString(operDayToOpen)));
            return false;
        } else {
            return true;
        }
    }

    private boolean checkFlexSucceded(Date operDayToOpen, Date currentOperday, Properties properties) throws SQLException {
        final String flexStepName = properties.getProperty(FLEX_FINAL_STEP_KEY);
        try {
            Assert.isTrue(!isEmpty(flexStepName), format("Не задано название завершающего шага загрузки FLEX"));
            DataRecord work = operdayRepository.findWorkprocStep(flexStepName, currentOperday);
            Assert.isTrue(null != work, format("Не завершена загрузка FLEX. Нет завершающего шага '%s' на дату '%s'"
                    , flexStepName, dateUtils.onlyDateString(currentOperday)));
            Assert.isTrue(FLEX_FINAL_STEP_RESULT_OK.equalsIgnoreCase(work.getString("result")),
                    format("Загрузка FLEX в недопустимом статусе '%s'", work.getString("result")));
            Assert.isTrue(FLEX_FINAL_MSG_OK.equalsIgnoreCase(work.getString("MSG")),
                    format("Загрузка FLEX не выполнена, msg = '%s'", work.getString("MSG")));
        } catch (IllegalArgumentException e) {
            ValidationError error = new ValidationError(OPEN_OPERDAY_ERROR
                    , format("Невозможно открыть следующий ОД: '%s'. %s", dateUtils.onlyDateString(operDayToOpen), e.getMessage()));
            auditController.warning(OpenOperday, "Невозможно открыть следующий ОД", null, error);
            return false;
        }
        return true;
    }

    /**
     * установка режима загрузки, если не соответствует установленному в настройках задачи
     */
    private void setPdMode(Operday.PdMode targetPdMode) throws Exception {
        if (operdayController.getOperday().getPdMode() != targetPdMode) {
            auditController.warning(OpenOperday
                    , format("Текущий режим загрузки '%s' не соответствует установленному в настройках задачи '%s'. Переключаем."
                            , operdayController.getOperday().getPdMode(), targetPdMode), null, "");
            operdayController.swithPdMode(operdayController.getOperday().getPdMode());
        }
    }

    public Operday.PdMode calculatePdMode(Properties properties) {
        final String pdModeString = Optional.ofNullable(properties
                .getProperty(PD_MODE_KEY)).orElse("").trim().toUpperCase();
        log.info(String.format("Установлен режим загрузки: '%s'", pdModeString));

        if (!StringUtils.isEmpty(pdModeString)) {
            return Operday.PdMode.valueOf(pdModeString);
        } else {
            Date currentday = org.apache.commons.lang3.time.DateUtils.truncate(new Date(), Calendar.DATE);
            log.info(String.format("Текущий системный день: '%s'", dateUtils.fullDateString(currentday)));
            Date operDayToOpen = calendarDayRepository
                    .getWorkdayAfter(operdayController.getOperday().getCurrentDate()).getId().getCalendarDate();
            log.info(String.format("Следующий системный день к открытию: '%s'", dateUtils.fullDateString(operDayToOpen)));
            if (operDayToOpen.after(currentday)) {
                return Operday.PdMode.DIRECT;
            } else {
                return Operday.PdMode.BUFFER;
            }
        }
    }

    /**
     * Начальное значение последовательности SEQ_GL_PD0 при открытии ОД
     * используется для записи в gl_pd (BUFFER MODE)
     */
    private void restartGlPdSequence() throws Exception {
        coreRepository.executeInNewTransaction(conn -> {synchronizationController.restartSequenceGLPD(1); return null;});
    }

}
