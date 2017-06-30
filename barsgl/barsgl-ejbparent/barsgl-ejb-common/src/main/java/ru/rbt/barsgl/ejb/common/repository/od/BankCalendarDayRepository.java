package ru.rbt.barsgl.ejb.common.repository.od;

import com.google.gwt.i18n.shared.impl.DateRecord;
import ru.rbt.barsgl.ejb.common.mapping.od.BankCalendarDay;
import ru.rbt.barsgl.ejb.common.mapping.od.BankCalendarDayId;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import ru.rbt.ejbcore.datarec.DataRecord;

/**
 * Created by Ivan Sevastyanov
 */
public class BankCalendarDayRepository extends AbstractBaseEntityRepository<BankCalendarDay,BankCalendarDayId> {

    private static final String BANK_CALENDAR_CODE = "RUR";

    public BankCalendarDay getWorkdayBefore(Date date) {
        return findNativeFirst(BankCalendarDay.class,
                "select * from cal c1 \n" +
                " where c1.dat = (\n" +
                "   select max(dat) from cal\n" +
                "   where dat < ?1 and ccy = ?2\n" +
                "     and thol not in ('X', 'T') ) " +
                "and c1.ccy = ?2", date, BANK_CALENDAR_CODE);
    }

    public BankCalendarDay getWorkdayAfter(Date date) {
        return findNativeFirst(BankCalendarDay.class,
                "select * from cal c1 \n" +
                " where c1.dat = (\n" +
                "   select min(dat) from cal\n" +
                "   where dat > ?1 and ccy = ?2\n" +
                "     and thol not in ('X', 'T') ) " +
                "and c1.ccy = ?2", date, BANK_CALENDAR_CODE);
    }

    /**
     * Рабочие дни
     * @param fromDay с даты (включительно)
     * @param toDay по дату (включительно)
     * @return отсортированный в порядке возрастания список рабочих дней
     */
    public List<BankCalendarDay> getWorkdays(Date fromDay, Date toDay) {
        return findNative(BankCalendarDay.class,
                "select * from cal " +
                        "where ccy = ?1 and dat >= ?2 " +
                        "  and dat <= ?3 " +
                        "  and thol not in ('X', 'T')  order by dat", 100, BANK_CALENDAR_CODE, fromDay, toDay);
    }

    /**
     * все дни календаря
     * @param fromDay с даты (включительно)
     * @param toDay по дату (включительно)
     * @return отсортированный в порядке возрастания список всех дней календаря
     */
    public List<BankCalendarDay> getCalendarDays(Date fromDay, Date toDay) {
        return findNative(BankCalendarDay.class,
                "select * from cal \n" +
                "where ccy = ?1 and dat >= ?2 \n" +
                "  and dat <= ?3 \n" +
                "order by dat", 100, BANK_CALENDAR_CODE, fromDay, toDay);
    }

    /**
     * является ли день рабочим
     * @param date
     * @return
     */
    public boolean isWorkday(Date date) {
        try {
            return null != selectFirst(
                    "select * from cal where dat = ? and ccy = ?  and thol not in ('X', 'T') "
                        , date, BANK_CALENDAR_CODE);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * является ли день рабочим (включая технический раб день)
     * @param date
     * @return
     */
    public boolean isWorkdayWithTech(Date date) {
        try {
            return null != selectFirst(
                    "select * from cal where dat = ? and ccy = ? and thol <> 'X'"
                        , date, BANK_CALENDAR_CODE);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * является ли день рабочим
     * @param date
     * @return
     */
    public String getDayType(Date date) {
        try {
            DataRecord data = selectFirst(
                    "select THOL from cal where dat = ? and ccy = ?"
                        , date, BANK_CALENDAR_CODE);
            return (null == data ? "" : data.getString(0));
        } catch (SQLException e) {
            return "";
        }
    }

    public boolean isWorkday(Date date, boolean withTech) {
        String tholIn = withTech ? "'X'" : "'X', 'T'";
        try {
            return null != selectFirst(
                    "select * from cal where dat = ? and ccy = ?  and thol not in (" + tholIn + ") "
                    , date, BANK_CALENDAR_CODE);
        } catch (SQLException e) {
            return false;
        }
    }

    public Date getWorkDateBefore(Date dateFrom, int days, boolean withTech) throws SQLException {
        String tholIn = withTech ? "'X'" : "'X', 'T'";
        DataRecord data = selectFirst(String.format("select min(d.dat) as dat from (" +
                " select DAT from CAL where CCY = ? and THOL not in (" + tholIn +
                ") and dat < ? order by 1 desc fetch first %d rows only) d", days), BANK_CALENDAR_CODE, dateFrom);
        return null != data ? data.getDate(0) : null;
    }

    public Date getWorkDateAfter(Date dateFrom, int days, boolean withTech) throws SQLException {
        String tholIn = withTech ? "'X'" : "'X', 'T'";
        DataRecord data = selectFirst(String.format("select max(d.dat) as dat from (" +
                " select DAT from CAL where CCY = ? and THOL not in (" + tholIn +
                ") and dat > ? order by 1 fetch first %d rows only) d", days), BANK_CALENDAR_CODE, dateFrom);
        return null != data ? data.getDate(0) : null;
    }

    public Date getWorkDateBefore(Date dateFrom, boolean withTech) throws SQLException {
        String tholIn = withTech ? "'X'" : "'X', 'T'";
        DataRecord data = selectFirst("select max(dat) from CAL where CCY = ? and THOL not in (" + tholIn + ") and dat < ? "
                , BANK_CALENDAR_CODE, dateFrom);
        return null != data ? data.getDate(0) : null;
    }

    public Date getWorkDateAfter(Date dateFrom, boolean withTech) throws SQLException {
        String tholIn = withTech ? "'X'" : "'X', 'T'";
        DataRecord data = selectFirst("select min(dat) from CAL where CCY = ? and THOL not in (" + tholIn + ") and dat > ? "
                , BANK_CALENDAR_CODE, dateFrom);
        return null != data ? data.getDate(0) : null;
    }
}
