package ru.rbt.barsgl.ejb.common.controller.od;

import com.google.web.bindery.autobean.shared.AutoBean;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.shared.operday.DatesWrapper;
import ru.rbt.ejb.repository.properties.PropertiesRepository;

import javax.inject.Inject;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

/**
 * Created by er18837 on 12.09.2018.
 */
public class Rep47422Controller {
    public static final String REG47422_DEPTH = "reg47422.def.depth";
    public static final int REG47422_DEF_DEPTH = 4;

    @Inject
    private OperdayController operdayController;

    @Inject
    BankCalendarDayRepository calendarDayRepository;

    @Inject
    PropertiesRepository propertiesRepository;

    public DatesWrapper getDatesReport() throws SQLException {
        Operday od = operdayController.getOperday();
        int depth = (int)(long)propertiesRepository.getNumberDef(REG47422_DEPTH, (long)REG47422_DEF_DEPTH);

        DatesWrapper wrapper = new DatesWrapper();
        wrapper.setDateFrom(calendarDayRepository.getWorkDateBefore(od.getCurrentDate(), depth > 0 ? depth : 1, false));
        wrapper.setDateTo(od.getLastWorkingDay());

        SimpleDateFormat format = new SimpleDateFormat(wrapper.dateFormat);
        wrapper.setDateFromStr(format.format(wrapper.getDateFrom()));
        wrapper.setDateToStr(format.format(wrapper.getDateTo()));
        return wrapper;
    }
}
