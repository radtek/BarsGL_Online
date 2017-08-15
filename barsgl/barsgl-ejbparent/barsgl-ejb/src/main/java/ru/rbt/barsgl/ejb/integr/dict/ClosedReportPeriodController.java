package ru.rbt.barsgl.ejb.integr.dict;

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
    private UserContext userContext;

    @Override
    public RpcRes_Base<ClosedReportPeriodWrapper> create(ClosedReportPeriodWrapper wrapper) {
        return null;
    }

    @Override
    public RpcRes_Base<ClosedReportPeriodWrapper> update(ClosedReportPeriodWrapper wrapper) {
        return null;
    }

    @Override
    public RpcRes_Base<ClosedReportPeriodWrapper> delete(ClosedReportPeriodWrapper wrapper) {
        return null;
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

            // TODO
            // cutDate >= текущий ОД
            // cutDate > lastDate
            //

        } catch (Exception e) {
            errorList.add(e.getMessage());
        }
        return StringUtils.listToString(errorList, "\n");    }
}
