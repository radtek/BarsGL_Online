package ru.rbt.barsgl.ejb.controller.cob;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.operday.task.ExecutePreCOBTaskFake;
import ru.rbt.barsgl.ejb.controller.operday.task.ExecutePreCOBTaskNew;
import ru.rbt.barsgl.ejb.entity.cob.CobStepStatistics;
import ru.rbt.barsgl.ejb.entity.sec.AuditRecord;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.barsgl.ejb.repository.JobHistoryRepository;
import ru.rbt.barsgl.ejb.repository.cob.CobStatRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.repository.PropertiesRepository;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbcore.util.StringUtils;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.ExceptionUtils;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.cob.CobStepItem;
import ru.rbt.barsgl.shared.cob.CobWrapper;
import ru.rbt.barsgl.shared.enums.CobStepStatus;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.math.BigDecimal;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by ER18837 on 10.03.17.
 */
public class CobStatService {
    public static final String COB_TASK_NAME = ExecutePreCOBTaskNew.class.getSimpleName();
    public static final String COB_FAKE_NAME = ExecutePreCOBTaskFake.class.getSimpleName();

    private static final BigDecimal ALL = new BigDecimal(100);
    private static final BigDecimal INC = new BigDecimal("1.1");

    @EJB
    private CobStatRecalculator recalculator;

    @EJB
    private CobStatRepository statRepository;

    @Inject
    private OperdayController operdayController;

    @EJB
    private PropertiesRepository propertiesRepository;

    @Inject
    private JobHistoryRepository historyRepository;

    @EJB
    private AuditController auditController;

    @Inject
    DateUtils dateUtils;

    public RpcRes_Base<CobWrapper> calculateCob() {
        CobWrapper wrapper = new CobWrapper();
        try {
            Date curdate = operdayController.getOperday().getCurrentDate();
            if (statRepository.getRunCobStatus(curdate) == CobStepStatus.Running) {
                return new RpcRes_Base<CobWrapper>(wrapper, true,
                        new ValidationError(ErrorCode.COB_IS_RUNNING, dateUtils.onlyDateString(curdate)).getMessage()
                );
            }

            Long idCob = recalculator.calculateCob();
            if (null == idCob) {
                return new RpcRes_Base<CobWrapper>(wrapper, true, "Не удалось рассчитать COB");
            }
            return getCobInfo(idCob);
        } catch (Throwable t) {
            auditController.error(AuditRecord.LogCode.PreCob, "Ошибка расчета длительности СОВ",
                    null, null, new ValidationError(ErrorCode.COB_STEP_ERROR, getErrorMessage(t)));
            return new RpcRes_Base<CobWrapper>(wrapper, true, t.getMessage());
        }
    }

    public RpcRes_Base<CobWrapper> getCobInfo(Long idCob) {
        CobWrapper wrapper = new CobWrapper();
        try {
            if (null == idCob)
                idCob = statRepository.getMaxRunCobId();
            if (null == idCob)
                idCob = statRepository.getMaxCobId();
            if (null == idCob) {
                return new RpcRes_Base<CobWrapper>(wrapper, true, "Нет ни одной записи о расчете COB");
            }
            List<CobStepStatistics> stepList = statRepository.getCobSteps(idCob);
            fillWrapper(wrapper, idCob, stepList);
            return new RpcRes_Base<CobWrapper>(wrapper, false, "");
        } catch (Throwable t) {
            auditController.error(AuditRecord.LogCode.PreCob, String.format("Ошибка получения информации для интерфейса СОВ (ID_COB = %d)", idCob),
                    null, null, new ValidationError(ErrorCode.COB_STEP_ERROR, getErrorMessage(t)));
            return new RpcRes_Base<CobWrapper>(wrapper, true, t.getMessage());
        }
    }

    private boolean fillWrapper(CobWrapper wrapper, Long idCob, List<CobStepStatistics> stepList) throws Exception {
        try {
            Long systime = getDateInMillis(operdayController.getSystemDateTime());
            wrapper.setIdCob(idCob);
            List<CobStepItem> stepItemList = new ArrayList<>();
            wrapper.setStepList(stepItemList);
            List<String> errorList = new ArrayList<>();
            CobStepItem itemTotal = new CobStepItem(0, "Всего: ");
            wrapper.setTotal(itemTotal);
            itemTotal.setDuration(BigDecimal.ZERO);
            itemTotal.setEstimation(BigDecimal.ZERO);
            for (CobStepStatistics stepStat : stepList) {
                CobStepItem item = new CobStepItem(stepStat.getPhaseNo(), stepStat.getPhaseName());
                item.setStatus(stepStat.getStatus());
                item.setMessage(stepStat.getMessage());
                if (!StringUtils.isEmpty(stepStat.getErrorMsg()))
                    errorList.add(String.format("Шаг %d: %s", stepStat.getPhaseNo(), stepStat.getErrorMsg()));
                calcStepParameters(stepStat, item, systime);
                itemTotal.setDuration(itemTotal.getDuration().add(item.getDuration()));
                itemTotal.setEstimation(itemTotal.getEstimation().add(item.getEstimation()));
                stepItemList.add(item);
            }
            itemTotal.setStatus(statRepository.getCobStatus(stepList));
            switch (itemTotal.getStatus()) {
                case NotStart:
                    itemTotal.setPercent(BigDecimal.ZERO);
                    break;
                case Success:
                    itemTotal.setPercent(ALL);
                    break;
                default:
                    itemTotal.setPercent(getPercent(itemTotal.getDuration(), itemTotal.getEstimation()));  // percent
                    break;
            }
            setIntValues(itemTotal);

//        wrapper.setStartTimer(Running == itemTotal.getStatus());       // TODO это пока
            wrapper.setStartTimer(isRunning());       // TODO это пока
            if (itemTotal.getStatus() == CobStepStatus.Halt)
                errorList.add("Обработка прервана");
            wrapper.setErrorMessage(StringUtils.listToString(errorList, ";\n "));
            return true;
        } catch (Throwable t) {
            auditController.error(AuditRecord.LogCode.PreCob,  String.format("Ошибка заполнения информации для интерфейса СОВ (ID_COB = %d STEP = %d)",
                    idCob, wrapper.getStepList() != null ? wrapper.getStepList().size() + 1 : 0 ),
                    null, null, new ValidationError(ErrorCode.COB_STEP_ERROR, getErrorMessage(t)));
            return false;
        }
    }

    private void calcStepParameters(CobStepStatistics step, CobStepItem item, Long systime) throws Exception {
        BigDecimal duration = BigDecimal.ZERO;
        try {
            switch (step.getStatus()) {
                case NotStart:
                case Skipped:
                    item.setEstimation(null != step.getEstimated() ? step.getEstimated() : BigDecimal.ZERO);
                    item.setDuration(BigDecimal.ZERO);
                    item.setPercent(BigDecimal.ZERO);
                    break;
                case Running:
                    Long timeBegin = getDateInMillis(step.getStartTimestamp());
                    if (timeBegin == 0L) {
                        item.setEstimation(step.getEstimated());
                        item.setDuration(BigDecimal.ZERO);
                        item.setPercent(BigDecimal.ZERO);
                    } else {
                        BigDecimal estimate = step.getEstimated().max(step.getDuration());
                        duration = (new BigDecimal(systime - timeBegin)).scaleByPowerOfTen(-3);    // seconds
                        if (duration.compareTo(estimate) > 0) {
                            BigDecimal koef = propertiesRepository.getDecimalDef(PropertyName.COB_STAT_INC.getName(), INC);
                            BigDecimal newEstimate = duration.multiply(koef);
                            statRepository.executeInNewTransaction((persistence ->
                                    statRepository.increaseStepEstimate(step.getIdCob(), step.getPhaseNo(), newEstimate, step.getDuration())));
                            statRepository.refresh(step, true);
                            estimate = step.getDuration();
                        }
                        item.setEstimation(estimate);
                        item.setDuration(duration);
                        item.setPercent(getPercent(duration, estimate));  // percent
                    }
                    break;
                case Success:
                    item.setEstimation(step.getDuration());
                    item.setDuration(step.getDuration());
                    item.setPercent(ALL);
                    break;
                case Error:
                case Halt:
                    BigDecimal estimate = step.getEstimated().max(step.getDuration());
                    item.setEstimation(estimate);
                    item.setDuration(step.getDuration());
                    item.setPercent(getPercent(step.getDuration(), estimate));  // percent
                    break;
                default:
                    break;
            }
            setIntValues(item);
        } catch (Throwable t) {
            auditController.error(AuditRecord.LogCode.PreCob,
                    String.format("Ошибка расчета информации для интерфейса СОВ (ID_COB = %d STEP = %d Est = %s Dur = %s dur = %s)",
                    step.getId().getIdCob(), step.getId().getPhaseNo(), step.getEstimated().toString(), step.getDuration().toString(), duration.toString()),
                    null, null, new ValidationError(ErrorCode.COB_STEP_ERROR, getErrorMessage(t)));
        }

}

    private void setIntValues(CobStepItem item){
        item.setIntDuration(Optional.ofNullable(item.getDuration()).orElse(BigDecimal.ZERO).intValue());
        item.setIntEstimation(Optional.ofNullable(item.getEstimation()).orElse(BigDecimal.ZERO).intValue());
        item.setIntPercent(Optional.ofNullable(item.getPercent()).orElse(BigDecimal.ZERO).intValue());
    }

    private BigDecimal getPercent(BigDecimal value, BigDecimal whole) {
        try {
            if (0 == BigDecimal.ZERO.compareTo(whole))
                return ALL;
            else
                return value.scaleByPowerOfTen(2).divide(whole, 6, BigDecimal.ROUND_CEILING);  // percent
        } catch (Throwable t) {
            auditController.error(AuditRecord.LogCode.PreCob,
                    String.format("Ошибка вычисления процента в интерфейс СОВ value = %s, whole = %s", value.toString(), whole.toString()),
                    null, null, new ValidationError(ErrorCode.COB_STEP_ERROR, getErrorMessage(t)));
            return BigDecimal.ZERO;
        }

    }

    private Long getDateInMillis(Date dateTime) {
        if (null == dateTime)
            return 0L;
        Calendar curTime = Calendar.getInstance();
        curTime.setTime(dateTime);
        return curTime.getTimeInMillis();
    }

    private boolean isRunning() {
        return historyRepository.isAlreadyRunningLike(null, COB_TASK_NAME) || historyRepository.isAlreadyRunningLike(null, COB_FAKE_NAME);  // TODO debug
    }

    private String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, ArithmeticException.class,
                IllegalArgumentException.class, PersistenceException.class, DefaultApplicationException.class);
    }

}
