package ru.rbt.barsgl.shared;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.TimeZone;

import java.util.Date;

/**
 * Created by akichigi on 06.07.16.
 */
public class ClientDateUtils {
    public final static int TZ_CLIENT_OFFSET = 4 * 60;	// TZ на сервере в минутах = Москва + 1 ,
    public final static TimeZone TZ_CLIENT =  TimeZone.createTimeZone(-TZ_CLIENT_OFFSET);

    private static final String onlyDate = "dd.MM.yyyy";
    private static final String dbDate = "yyyy-MM-dd";

    public static String Date2String(Date date, String format) {
        return  date == null ? null : DateTimeFormat.getFormat(format).format(date);
    }

    public static Date String2Date(String dateStr, String format) throws IllegalArgumentException{
        return null == dateStr ? null : DateTimeFormat.getFormat(format).parse(dateStr);
    }

    public static String Date2String(Date date) {
        return Date2String(date, onlyDate);
    }

    public static Date String2Date(String dateStr) throws IllegalArgumentException{
        return String2Date(dateStr, onlyDate);
    }

    public static String DateDb2String(Date date) {
        return Date2String(date, dbDate);
    }

    public static Date String2DateDb(String dateStr) throws IllegalArgumentException{
        return String2Date(dateStr, dbDate);
    }

}
