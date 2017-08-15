package ru.rbt.barsgl.ejb.integr.dict;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.dict.BVSourceDeal;
import ru.rbt.barsgl.ejb.entity.dict.BVSourceDealId;
import ru.rbt.barsgl.ejb.entity.dict.ClosedReportPeriod;
import ru.rbt.barsgl.ejb.repository.dict.ClosedReportPeriodRepository;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.ClosedReportPeriodWrapper;
import ru.rbt.ejbcore.util.StringUtils;

import javax.inject.Inject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static java.lang.String.format;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by er18837 on 14.08.2017.
 */
public class ClosedReportPeriodController extends BaseDictionaryController<ClosedReportPeriodWrapper, Date, ClosedReportPeriod, ClosedReportPeriodRepository>{

    @Inject
    private ClosedReportPeriodRepository repository;

    @Inject
    OperdayController operdayController;

    @Inject
    private UserContext userContext;

    @Inject
    ru.rbt.ejbcore.util.DateUtils dateUtils;

    @Override
    public RpcRes_Base<ClosedReportPeriodWrapper> create(ClosedReportPeriodWrapper wrapper) {
        String errorMessage = validate(wrapper);
        if(!isEmpty(errorMessage)){
            return new RpcRes_Base<>(wrapper, true, errorMessage);
        }

        return create(wrapper, repository, ClosedReportPeriod.class, wrapper.getLastDate(),
                format("Отчетный период с датой закрытия '%d' уже существует", wrapper.getLastDate()),
                format("Создан отчетный период с датой закрытия '%s' и датой отсечения '%s'", wrapper.getLastDate(), wrapper.getCutDate()),
                format("Ошибка при создании отчетного периода с датой закрытия '%s'", wrapper.getLastDate()),
                () -> new ClosedReportPeriod(wrapper.getLastDate()
                        , wrapper.getCutDate()
                        , userContext.getUserName()
                        , operdayController.getSystemDateTime()));
    }

    @Override
    public RpcRes_Base<ClosedReportPeriodWrapper> update(ClosedReportPeriodWrapper wrapper) {
        String errorMessage = validate(wrapper);
        if(!isEmpty(errorMessage)){
            return new RpcRes_Base<>(wrapper, true, errorMessage);
        }

        return update(wrapper, repository, ClosedReportPeriod.class, wrapper.getLastDate(),
                format("Отчетный период с датой закрытия '%d' не найден", wrapper.getLastDate()),
                format("Изменен отчетный период с датой закрытия '%s' и датой отсечения '%s'", wrapper.getLastDate(), wrapper.getCutDate()),
                format("Ошибка при изменении отчетного периода с датой закрытия '%s'", wrapper.getLastDate()),
                (param) -> {
                    param.setCutDate(wrapper.getCutDate());
                    param.setUser(userContext.getUserName());
                    param.setCreateTimestamp(operdayController.getSystemDateTime());
                });
    }

    @Override
    public RpcRes_Base<ClosedReportPeriodWrapper> delete(ClosedReportPeriodWrapper wrapper) {
        String parseError = parseDates(wrapper);
        if (!isEmpty(parseError))
            return new RpcRes_Base<>(wrapper, true, parseError);
        if (!wrapper.getLastDate().after(operdayController.getOperday().getCurrentDate()))
            return new RpcRes_Base<>(wrapper, true, "Нельзя удалить отчетный с датой отсечения <= текущего опердня");;

        return delete(wrapper, repository, ClosedReportPeriod.class, wrapper.getLastDate(),
                format("Отчетный период с датой закрытия '%d' не найден", wrapper.getLastDate()),
                format("Удален отчетный период с датой закрытия '%s' и датой отсечения '%s'", wrapper.getLastDate(), wrapper.getCutDate()),
                format("Ошибка при удаления отчетного периода с датой закрытия '%s'", wrapper.getLastDate()));
    }

    private String parseDates(ClosedReportPeriodWrapper wrapper) {
        if (isEmpty(wrapper.getLastDateStr()))
            return "Не задана дата закрытия отчетного периода";
        if (isEmpty(wrapper.getCutDateStr()))
            return "Не задана дата отсечения отчетного периода";

        SimpleDateFormat dateFormat = new SimpleDateFormat(wrapper.getDateFormat());
        try {
            wrapper.setLastDate(dateFormat.parse(wrapper.getLastDateStr()));
            wrapper.setCutDate(dateFormat.parse(wrapper.getCutDateStr()));
            return "";
        } catch (ParseException e) {
            return e.getMessage();
        }
    }

    private String validate(ClosedReportPeriodWrapper wrapper) {
        ArrayList<String> errorList = new ArrayList<String>();
        try {
            String parseError = parseDates(wrapper);
            if (!isEmpty(parseError))
                return parseError;

            // cutDate >= текущий ОД
            if (wrapper.getCutDate().before(operdayController.getOperday().getCurrentDate()))
                errorList.add(format(""));
            // cutDate > lastDate
            if (!wrapper.getCutDate().after(wrapper.getLastDate()))
                errorList.add(format(""));
            // проверка непересечения периодов
            ClosedReportPeriod intersected = repository.findIntersectedRecord(wrapper);
            if (null != intersected)
                errorList.add(format("Найден отчетный период с датой закрытия '%s' и датой отсечения '%s', перекрывающий заданный"
                        , dateUtils.onlyDateString(intersected.getLastDate())
                        , dateUtils.onlyDateString(intersected.getCutDate())));
        } catch (Exception e) {
            errorList.add(e.getMessage());
        }
        return StringUtils.listToString(errorList, "\n");    }
}
