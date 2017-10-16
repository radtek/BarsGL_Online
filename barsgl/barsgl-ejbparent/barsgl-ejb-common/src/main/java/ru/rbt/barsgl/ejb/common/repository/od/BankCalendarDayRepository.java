package ru.rbt.barsgl.ejb.common.repository.od;

import com.google.gwt.i18n.shared.impl.DateRecord;
import ru.rbt.barsgl.ejb.common.mapping.od.BankCalendarDay;
import ru.rbt.barsgl.ejb.common.mapping.od.BankCalendarDayId;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static org.apache.commons.lang3.time.DateUtils.addDays;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless
public class BankCalendarDayRepository extends AbstractBaseEntityRepository<BankCalendarDay,BankCalendarDayId> {

    private static final String BANK_CALENDAR_CODE = "RUR";

    public BankCalendarDay getWorkdayBefore(Date date) {
        return findNativeFirst(BankCalendarDay.class,
                "select * from cal c1 \n" +
                " where c1.dat = (\n" +
                "   select max(dat) from cal\n" +
                "   where dat < ?1 and ccy = ?2\n" +
                "     and nvl(thol, 'W') not in ('X', 'T') ) " +
                "and c1.ccy = ?2", date, BANK_CALENDAR_CODE);
    }

    public BankCalendarDay getWorkdayAfter(Date date) {
        return findNativeFirst(BankCalendarDay.class,
                "select * from cal c1 \n" +
                " where c1.dat = (\n" +
                "   select min(dat) from cal\n" +
                "   where dat > ?1 and dat < ?1 + 20 and ccy = ?2\n" +
                "     and nvl(thol, 'W') not in ('X', 'T') ) " +
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
                        "  and nvl(thol, 'W') thol not in ('X', 'T')  order by dat", 100, BANK_CALENDAR_CODE, fromDay, toDay);
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
                    "select * from cal where dat = ? and ccy = ?  and nvl(thol, 'W') not in ('X', 'T') "
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
                    "select * from cal where dat = ? and ccy = ? and nvl(thol, 'W') <> 'X'"
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
                    "select * from cal where dat = ? and ccy = ?  and nvl(thol, 'W') not in (" + tholIn + ") "
                    , date, BANK_CALENDAR_CODE);
        } catch (SQLException e) {
            return false;
        }
    }

    public Date getWorkDateBefore(Date dateTo, int days, boolean withTech) throws SQLException {
        String tholIn = withTech ? "'X'" : "'X', 'T'";
/*
        DataRecord data = selectFirst(String.format("select min(d.dat) as dat from (" +
                " select DAT from CAL where CCY = ? and nvl(THOL, 'W') not in (" + tholIn +
                ") and dat < ? order by 1 desc fetch first %d rows only) d", days), BANK_CALENDAR_CODE, dateTo);
*/
        int interval = 15;
        if (days > 3) interval += days *2;
        Date dateMin = addDays(dateTo, -interval);
        DataRecord data = selectFirst("select DAT from (" +
                "select c.DAT, row_number() over (order by DAT desc) rn" +
                "  from CAL c where CCY = ? and nvl(THOL, 'W') not in (" + tholIn +
                ") and c.dat > ? and c.dat < ?" +
                ") where rn = ?", BANK_CALENDAR_CODE, dateMin, dateTo, days);
        return null != data ? data.getDate(0) : null;
    }

    public Date getWorkDateAfter(Date dateFrom, int days, boolean withTech) throws SQLException {
        String tholIn = withTech ? "'X'" : "'X', 'T'";
/*
        DataRecord data = selectFirst(String.format("select max(d.dat) as dat from (" +
                " select DAT from CAL where CCY = ? and nvl(THOL, 'W') not in (" + tholIn +
                ") and dat > ? order by 1 fetch first %d rows only) d", days), BANK_CALENDAR_CODE, dateFrom);
*/
        int interval = 15;
        if (days > 3) interval += days *2;
        Date dateMax = addDays(dateFrom, interval);
        DataRecord data = selectFirst("select DAT from (" +
                "select c.DAT, row_number() over (order by DAT) rn" +
                "  from CAL c where CCY = ? and nvl(THOL, 'W') not in (" + tholIn +
                ") and c.dat > ? and c.dat < ?" +
                ") where rn = ?", BANK_CALENDAR_CODE, dateFrom, dateMax, days);
        return null != data ? data.getDate(0) : null;
    }

    public Date getWorkDateBefore(Date dateFrom, boolean withTech) throws SQLException {
        String tholIn = withTech ? "'X'" : "'X', 'T'";
        DataRecord data = selectFirst("select max(dat) from CAL where CCY = ? and nvl(THOL, 'W') not in (" + tholIn + ") and dat < ? "
                , BANK_CALENDAR_CODE, dateFrom);
        return null != data ? data.getDate(0) : null;
    }

    public Date getWorkDateAfter(Date dateFrom, boolean withTech) throws SQLException {
        String tholIn = withTech ? "'X'" : "'X', 'T'";
        DataRecord data = selectFirst("select min(dat) from CAL where CCY = ? and nvl(THOL, 'W') not in (" + tholIn + ") and dat > ? "
                , BANK_CALENDAR_CODE, dateFrom);
        return null != data ? data.getDate(0) : null;
    }
}
