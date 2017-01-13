package ru.rbt.barsgl.gwt.core.widgets;

import com.google.gwt.i18n.client.DateTimeFormat;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Field;

import java.util.Date;
import static ru.rbt.barsgl.shared.ClientDateUtils.TZ_CLIENT;

public class FieldFormatter {
	private Column column;
	
	public FieldFormatter(Column column) {
		this.column = column;
	}
	
	public String getDisplayValue(Field field) {
		return null == field ? null : getDisplayValue(field.getValue());
	}

    public String getDisplayValue(Object value) {
        String result = "";
        if (value != null) {
            switch (column.getType()) {
                case DATE:		// ставим TimeZone = Москве+1, чтобы убрать ошибку преобр даты на 1 день назад на прошлых датах
                    result = DateTimeFormat.getFormat(column.getFormat()).format((Date)value, TZ_CLIENT );
                    break;
                case DATETIME:
                    result = DateTimeFormat.getFormat(column.getFormat()).format((Date)value);
                    break;
                default:
                    result = value.toString();
                    break;
            }
        }
        return result;
    }

}
