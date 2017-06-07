package ru.rbt.barsgl.ejb.integr.oper;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;

import javax.inject.Inject;

/**
 * Created by ER18837 on 26.02.15.
 * Обработчик веерных проводок в режиме online
 */
public class FanPostingProcessor extends IncomingPostingProcessor {

    @Inject
    private OperdayController operdayController;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

    //@Override
    /**
     * Дата проводки всегда равно текущему опердню
     * Ошибка, если valueDate > СЕГОДНЯ или valueDate между СЕГОДНЯ и ПРЕДЫДУЩИЙ раб день
     */
    /*
    // TODO удалить потом - стандарнтое определение даты проводки
    protected Date getStandardPostingDate(Date valueDate) {
        Date currentDay = operdayController.getOperday().getCurrentDate();
        Date lastWorkingDay = operdayController.getOperday().getLastWorkingDay();
        if (    valueDate.equals(currentDay)                    // valueDate = СЕГОДНЯ
            || (valueDate.compareTo(lastWorkingDay) <= 0) )     // valueDate <= ПРЕДЫДУЩИЙ раб день
        {
            return currentDay;
        } else {
            // valueDate > СЕГОДНЯ или valueDate между СЕГОДНЯ и ПРЕДЫДУЩИЙ раб день
            throw new ValidationError(DATE_NOT_VALID, "валютирования",
                    dateUtils.onlyDateString(valueDate),
                    dateUtils.onlyDateString(currentDay), operdayController.getOperday().getPhase().name(),
                    dateUtils.onlyDateString(lastWorkingDay), operdayController.getOperday().getLastWorkdayStatus().name());
        }
    }*/

    @Override
    public boolean isSupported(EtlPosting posting) {
        return null != posting
                && posting.isFan() && !posting.isStorno() && !posting.isTech();
    }


}
