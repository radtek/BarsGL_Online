package ru.rbt.ejbcore.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Ivan Sevastyanov
 */
public class DateUtils {

    private final static SimpleDateFormat databaseDate = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat onlyDate = new SimpleDateFormat("dd.MM.yyyy");
    private final SimpleDateFormat fullDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss SSS z Z");
    private final static SimpleDateFormat timeDate = new SimpleDateFormat("dd.MM.yy HH:mm:ss.SSS");

    private static final String finalDateStr = "2029-01-01";
    private static final Date finalDate = parseFinalDate();

    private static Date parseFinalDate() {
        try {
            return databaseDate.parse(finalDateStr);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Date getFinalDate() {
        return finalDate;
    }

    static public Date addDay(Date d, int days){
        return new Date(d.getTime() + (1000 * 60 * 60 * 24 * days));
    }

    public static String dateTimeString(Date d){
        return timeDate.format(d);
    }
    /**
     * преобразование даты в строку yyyy-MM-dd
     * @param from дата
     * @return строка yyyy-MM-dd
     */
    public static String dbDateString(Date from) {
        return databaseDate.format(from);
    }

    /**
     * преобразование даты в строку dd.mm.yyyy
     * @param from
     * @return строка dd.mm.yyyy
     */
    public String onlyDateString(Date from) {
        return onlyDate.format(from);
    }

    /**
     * возвращает только дату без времени
     * @param date
     * @return
     */
    public static Date onlyDate(Date date) {
        return null == date ? null
                : org.apache.commons.lang3.time.DateUtils.truncate(date, Calendar.DATE);
    }

    /**
     * преобразование даты в строку dd.MM.yyyy HH:mm:ss SSS z Z
     * @param from дата
     * @return строковое представление
     */
    public String fullDateString(Date from) {
        return fullDateFormat.format(from);
    }

    public Date onlyDateParse(String dateStr) throws ParseException {
        return null == dateStr ? null : onlyDate.parse(dateStr);
    }
    public static Date dbDateParse(String dateStr) throws ParseException {
        return null == dateStr ? null : databaseDate.parse(dateStr);
    }

    public static Date addDays(Date date, int days) {
        return org.apache.commons.lang3.time.DateUtils.addDays(date, days);
    }

    public static Date addSeconds(Date date, int seconds) {
        return org.apache.commons.lang3.time.DateUtils.addSeconds(date, seconds);
    }

    public static String formatElapsedTimeOver24h(long milliseconds) {

        // Compiler will take care of constant arithmetics
        if (24 * 60 * 60 * 1000 > milliseconds) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            return sdf.format(milliseconds);

        } else {
            SimpleDateFormat sdf = new SimpleDateFormat(":mm:ss.SSS");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            // Keep long data type
            // Compiler will take care of constant arithmetics
            long hours = milliseconds / (60L * 60L * 1000L);

            return hours + sdf.format(milliseconds);
        }
    }

    public static SimpleDateFormat getDatabaseDate() {
        return databaseDate;
    }

    public SimpleDateFormat getOnlyDate() {
        return onlyDate;
    }

    public SimpleDateFormat getFullDateFormat() {
        return fullDateFormat;
    }

    public static SimpleDateFormat getTimeDate() {
        return timeDate;
    }

    public static String getFinalDateStr() {
        return finalDateStr;
    }
}
