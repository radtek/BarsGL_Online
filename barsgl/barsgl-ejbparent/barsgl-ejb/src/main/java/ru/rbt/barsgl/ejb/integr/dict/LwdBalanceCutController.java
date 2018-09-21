package ru.rbt.barsgl.ejb.integr.dict;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.entity.dict.LwdBalanceCut;
import ru.rbt.barsgl.ejb.entity.dict.LwdBalanceCutView;
import ru.rbt.barsgl.ejb.repository.dict.LwdBalanceCutRepository;
import ru.rbt.barsgl.ejb.repository.dict.LwdCutCachedRepository;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.operday.LwdBalanceCutWrapper;
import ru.rbt.ejbcore.util.StringUtils;

import javax.ejb.*;
import javax.inject.Inject;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MINUTES;
import static javax.ejb.LockType.READ;
import static javax.ejb.LockType.WRITE;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by er18837 on 16.08.2017.
 */
@Singleton
@AccessTimeout(value = 15, unit = MINUTES)
public class LwdBalanceCutController extends BaseDictionaryController<LwdBalanceCutWrapper, Date, LwdBalanceCut, LwdBalanceCutRepository>{
    public static final String timePatternMask = "([0-1]?\\d|2[0-3]):([0-5]?\\d)";
    private static final Pattern timePattern = Pattern.compile(timePatternMask);

    @EJB
    LwdBalanceCutRepository repository;

    @EJB
    LwdCutCachedRepository cachedRepository;

    @Inject
    private BankCalendarDayRepository calendarDayRepository;

    @Inject
    OperdayController operdayController;

    @Inject
    private UserContext userContext;

    @Inject
    ru.rbt.ejbcore.util.DateUtils dateUtils;

    @Lock(READ)
    public RpcRes_Base<LwdBalanceCutWrapper> get() {
        LwdBalanceCutView record = cachedRepository.getRecord();
        LwdBalanceCutWrapper wrapper = new LwdBalanceCutWrapper();
        if (null != record) {
            Date runDate = record.getRunDate();
            Date closeDate;
            try {
                closeDate = calendarDayRepository.getWorkDateBefore(runDate, false);
            } catch (SQLException e) {
                return new RpcRes_Base<>(wrapper, true, e.getMessage());
            }
            wrapper.setRunDateStr(new SimpleDateFormat(wrapper.getDateFormat()).format(record.getRunDate()));
            wrapper.setCloseDateStr(new SimpleDateFormat(wrapper.getDateFormat()).format(closeDate));
            wrapper.setCutTimeStr(record.getCutTime());
        }
        return new RpcRes_Base<>(wrapper, false,
                String.format("Время отсечения предыдущего рабочего дня: дата опердня '%s', время '%s'"
                , wrapper.getRunDateStr(), wrapper.getCutTimeStr()));
    }

    @Override
    @Lock(WRITE)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public RpcRes_Base<LwdBalanceCutWrapper> create(LwdBalanceCutWrapper wrapper) {
        String errorMessage = validate(wrapper);
        if(!isEmpty(errorMessage)){
            return new RpcRes_Base<>(wrapper, true, errorMessage);
        }

        try {
            repository.executeUpdate("delete from LwdBalanceCut b");
            LwdBalanceCut record = new LwdBalanceCut(wrapper.getRunDate(), wrapper.getCutTimeStr(), userContext.getUserName());
            repository.save(record);
            cachedRepository.init();

            String msg = String.format("Создана установка времени отсечения предыдущего рабочего дня: дата опердня '%s', время '%s'"
                    , wrapper.getRunDateStr(), wrapper.getCutTimeStr());
            return auditInfo(wrapper, msg);
        } catch (Exception e) {
            String msg = String.format("Ошибка создания установки времени отсечения предыдущего рабочего дня"
                    , wrapper.getRunDateStr(), wrapper.getCutTimeStr());
            return auditError(wrapper, msg, e);
        }
    }

    private String parseDates(LwdBalanceCutWrapper wrapper) {
        if (isEmpty(wrapper.getRunDateStr()))
            return "Не задана дата начала действия";

        try {
            wrapper.setRunDate(new SimpleDateFormat(wrapper.getDateFormat()).parse(wrapper.getRunDateStr()));
        } catch (Exception e) {
            return format("Неверный формат даты '%s', требуется '%s'", wrapper.getRunDateStr(), wrapper.getDateFormat());
        }

        try {
            new SimpleDateFormat(wrapper.getTimeFormat()).parse(wrapper.getCutTimeStr());
            if (!timePattern.matcher(wrapper.getCutTimeStr()).matches())
                return format("Неверное время '%s', должно быть от '00:00' до '23:59'", wrapper.getCutTimeStr());
        } catch (Exception e) {
            return format("Неверный формат времени '%s', требуется '%s'", wrapper.getCutTimeStr(), wrapper.getTimeFormat());
        }
        return "";
    }

    private String validate(LwdBalanceCutWrapper wrapper) {
        ArrayList<String> errorList = new ArrayList<String>();
        try {
            String parseError = parseDates(wrapper);
            if (!isEmpty(parseError))
                return parseError;

            Date runDate = wrapper.getRunDate();
            Date lwDate = operdayController.getOperday().getLastWorkingDay();
            // runDate >= предыдущий ОД + 1
            if (!lwDate.before(runDate))
                errorList.add(format("Дата отсечения '%s' должна быть > предыдущего рабочего дня '%s'"
                        , wrapper.getRunDateStr(), dateUtils.onlyDateString(lwDate)));

            // runDate существует и рабочий день
            if (calendarDayRepository.getCalendarDays(runDate, runDate).isEmpty())
                errorList.add(format("Дата отсечения '%s' отсутствует в календаре банка", wrapper.getRunDateStr()));
//            else if (!calendarDayRepository.isWorkday(runDate))
//                errorList.add(format("Дата отсечения '%s' - выходной день", wrapper.getRunDateStr()));

        } catch (Exception e) {
            errorList.add(e.getMessage());
        }
        return StringUtils.listToString(errorList, "\n");
    }

    @Override
    public RpcRes_Base<LwdBalanceCutWrapper> update(LwdBalanceCutWrapper wrapper) {
        return null;
    }

    @Override
    public RpcRes_Base<LwdBalanceCutWrapper> delete(LwdBalanceCutWrapper wrapper) {
        return null;
    }

}
