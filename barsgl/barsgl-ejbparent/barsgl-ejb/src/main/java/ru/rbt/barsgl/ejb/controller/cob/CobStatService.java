package ru.rbt.barsgl.ejb.controller.cob;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.operday.task.ExecutePreCOBTaskFake;
import ru.rbt.barsgl.ejb.entity.cob.CobStepStatistics;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.barsgl.ejb.repository.JobHistoryRepository;
import ru.rbt.barsgl.ejb.repository.cob.CobStatRepository;
import ru.rbt.barsgl.ejbcore.repository.PropertiesRepository;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbcore.util.StringUtils;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.cob.CobStepItem;
import ru.rbt.barsgl.shared.cob.CobWrapper;
import ru.rbt.barsgl.shared.enums.CobStepStatus;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by ER18837 on 10.03.17.
 */
public class CobStatService {
    public static final String COB_TASK_NAME = ExecutePreCOBTaskFake.class.getSimpleName();

    private static final BigDecimal ALL = new BigDecimal(100);
    private static final BigDecimal INC = new BigDecimal("1.05");

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
            return new RpcRes_Base<CobWrapper>(wrapper, true, t.getMessage());
        }
    }

    public RpcRes_Base<CobWrapper> getCobInfo(Long idCob) {
        CobWrapper wrapper = new CobWrapper();
        try {
            if (null == idCob)
                idCob = statRepository.getMaxRunCobId();
            if (null == idCob) {
                return new RpcRes_Base<CobWrapper>(wrapper, true, "Нет ни одной записи о расчете COB");
            }
            List<CobStepStatistics> stepList = statRepository.getCobSteps(idCob);
            fillWrapper(wrapper, idCob, stepList);
            return new RpcRes_Base<CobWrapper>(wrapper, false, "");
        } catch (Throwable t) {
            return new RpcRes_Base<CobWrapper>(wrapper, true, t.getMessage());
        }
    }

    private boolean fillWrapper(CobWrapper wrapper, Long idCob, List<CobStepStatistics> stepList) throws Exception {
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
        wrapper.setErrorMessage(StringUtils.listToString(errorList, "; "));
        return true;
    }

    private void calcStepParameters(CobStepStatistics stepStat, CobStepItem item, Long systime) throws Exception {
        switch(stepStat.getStatus()) {
            case NotStart:
            case Skipped:
                item.setEstimation(stepStat.getEstimated());
                item.setDuration(BigDecimal.ZERO);
                item.setPercent(BigDecimal.ZERO);
                break;
            case Running:
                Long timeBegin = getDateInMillis(stepStat.getStartTimestamp());
                if (timeBegin == 0L) {
                    item.setEstimation(stepStat.getEstimated());
                    item.setDuration(BigDecimal.ZERO);
                    item.setPercent(BigDecimal.ZERO);
                } else {
                    BigDecimal estimate = stepStat.getEstimated().max(stepStat.getDuration());
                    BigDecimal duration = (new BigDecimal(systime - timeBegin)).scaleByPowerOfTen(-3);    // seconds
                    if(duration.compareTo(estimate) > 0) {
                        BigDecimal koef = propertiesRepository.getDecimalDef(PropertyName.COB_STAT_INC.getName(),INC);
                        BigDecimal newEstimate = duration.multiply(koef);
                        statRepository.executeInNewTransaction((persistence ->
                                statRepository.increaseStepEstimate(stepStat.getIdCob(), stepStat.getPhaseNo(), newEstimate, stepStat.getDuration())));
                        statRepository.refresh(stepStat, true);
                        estimate = stepStat.getDuration();
                    }
                    item.setEstimation(estimate);
                    item.setDuration(duration);
                    item.setPercent(getPercent(duration, estimate));  // percent
                }
                break;
            case Success:
                item.setEstimation(stepStat.getDuration());
                item.setDuration(stepStat.getDuration());
                item.setPercent(ALL);
                break;
            case Error:
            case Halt:
                BigDecimal estimate = stepStat.getEstimated().max(stepStat.getDuration());
                item.setEstimation(estimate);
                item.setDuration(stepStat.getDuration());
                item.setPercent(getPercent(stepStat.getDuration(), estimate));  // percent
                break;
            default:
                break;
        }
        setIntValues(item);
    }

    private void setIntValues(CobStepItem item){
        item.setIntDuration(Optional.ofNullable(item.getDuration()).orElse(BigDecimal.ZERO).intValue());
        item.setIntEstimation(Optional.ofNullable(item.getEstimation()).orElse(BigDecimal.ZERO).intValue());
        item.setIntPercent(Optional.ofNullable(item.getPercent()).orElse(BigDecimal.ZERO).intValue());
    }

    private BigDecimal getPercent(BigDecimal value, BigDecimal whole) {
        return value.scaleByPowerOfTen(2).divide(whole, 6, BigDecimal.ROUND_CEILING);  // percent

    }

    private Long getDateInMillis(Date dateTime) {
        if (null == dateTime)
            return 0L;
        Calendar curTime = Calendar.getInstance();
        curTime.setTime(dateTime);
        return curTime.getTimeInMillis();
    }

    private boolean isRunning() {
        return historyRepository.isAlreadyRunningLike(null, COB_TASK_NAME);
    }
}
