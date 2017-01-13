package ru.rbt.barsgl.gwt.client.check;

import com.google.gwt.i18n.shared.DateTimeFormat;
import ru.rbt.barsgl.gwt.core.utils.AppFunction;

import java.util.Date;

/**
 * Created by ER18837 on 15.03.16.
 */
public class ConvertDateToString implements AppFunction<Date,String> {
    private DateTimeFormat dateFormat;

    public ConvertDateToString(String formatString) {
        this.dateFormat = DateTimeFormat.getFormat(formatString);
    }

    @Override
    public String apply(Date from) {
        return null == from ? null : dateFormat.format(from);
    }
}
