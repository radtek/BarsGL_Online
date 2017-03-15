package ru.rbt.barsgl.ejb.controller.cob;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.cob.CobStatistics;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.barsgl.ejb.repository.cob.CobStatRepository;
import ru.rbt.barsgl.ejbcore.repository.PropertiesRepository;
import ru.rbt.barsgl.ejbcore.util.StringUtils;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.cob.CobStepItem;
import ru.rbt.barsgl.shared.cob.CobWrapper;
import ru.rbt.barsgl.shared.enums.CobStepStatus;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static ru.rbt.barsgl.shared.enums.CobStepStatus.*;

/**
 * Created by ER18837 on 10.03.17.
 */
public class CobStatService {
    final static BigDecimal ALL = new BigDecimal(100);
    final static BigDecimal INC = new BigDecimal("1.05");

    @EJB
    private CobStatRecalculator recalculator;

    @Inject
    private CobStatRepository statRepository;

    @Inject
    private OperdayController operdayController;

    @EJB
    private PropertiesRepository propertiesRepository;

    public RpcRes_Base<CobWrapper> calculateCob() {
        CobWrapper wrapper = new CobWrapper();
        try {
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
                idCob = statRepository.getMaxCobId();
            if (null == idCob) {
                return new RpcRes_Base<CobWrapper>(wrapper, true, "Нет ни одной записи о расчете COB");
            }
            List<CobStatistics> stepList = statRepository.getCobSteps(idCob);
            fillWrapper(wrapper, idCob, stepList);
            return new RpcRes_Base<CobWrapper>(wrapper, false, "");
        } catch (Throwable t) {
            return new RpcRes_Base<CobWrapper>(wrapper, true, t.getMessage());
        }
    }

    private boolean fillWrapper(CobWrapper wrapper, Long idCob, List<CobStatistics> stepList) {
        Long systime = getDateInMillis(operdayController.getSystemDateTime());
        wrapper.setIdCob(idCob);
        List<CobStepItem> stepItemList = new ArrayList<>();
        wrapper.setStepList(stepItemList);
        List<String> errorList = new ArrayList<>();
        CobStepItem itemTolal = new CobStepItem(0, "Всего: ");
        wrapper.setTotal(itemTolal);
        itemTolal.setDuration(BigDecimal.ZERO);
        itemTolal.setEstimation(BigDecimal.ZERO);
        for (CobStatistics stepStat : stepList) {
            CobStepItem item = new CobStepItem(stepStat.getPhaseNo(), stepStat.getPhaseName());
            item.setStatus(stepStat.getStatus());
            item.setMessage(stepStat.getMessage());
            if (!StringUtils.isEmpty(stepStat.getErrorMsg()))
                errorList.add(String.format("Шаг %d: %s", stepStat.getPhaseNo(), stepStat.getErrorMsg()));
            calcStepDuration(stepStat, item, systime);
            itemTolal.setDuration(itemTolal.getDuration().add(item.getDuration()));
            itemTolal.setEstimation(itemTolal.getEstimation().add(item.getEstimation()));
            stepItemList.add(item);
        }
        CobStepStatus firstStatus = stepItemList.get(0).getStatus();
        CobStepStatus lastStatus = stepItemList.get(stepItemList.size() - 1).getStatus();
        if (firstStatus == Step_NotStart) {
            itemTolal.setStatus(Step_NotStart);
            itemTolal.setPercent(BigDecimal.ZERO);
        } else if (lastStatus == Step_Success || lastStatus == Step_Error){
            itemTolal.setStatus(lastStatus);
            itemTolal.setPercent(BigDecimal.ZERO);
        } else {
            itemTolal.setStatus(Step_Running);
            itemTolal.setPercent(getPercent(itemTolal.getDuration(), itemTolal.getEstimation()));  // percent
        }

        wrapper.setErrorMessage(StringUtils.listToString(errorList, "; "));
        return true;
    }

    private void calcStepDuration(CobStatistics stepStat, CobStepItem item, Long systime){
        switch(stepStat.getStatus()) {
            case Step_NotStart:
                item.setEstimation(stepStat.getEstimated());
                item.setDuration(BigDecimal.ZERO);
                item.setPercent(BigDecimal.ZERO);
                break;
            case Step_Running:
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
                        statRepository.increaseStepEstimate(stepStat.getIdCob(), stepStat.getPhaseNo(), newEstimate, stepStat.getDuration());
                        statRepository.refresh(stepStat, true);
                        estimate = stepStat.getDuration();
                    }
                    item.setEstimation(estimate);
                    item.setDuration(duration);
                    item.setPercent(getPercent(duration, estimate));  // percent
                }
                break;
            case Step_Success:
            case Step_Error:
                item.setEstimation(stepStat.getDuration());
                item.setDuration(stepStat.getDuration());
                item.setPercent(ALL);
                break;
            default:
                break;
        }
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
}
