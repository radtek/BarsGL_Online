package ru.rbt.barsgl.ejb.common.repository.od;

import ru.rbt.barsgl.ejb.common.mapping.od.BankCalendarDay;
import ru.rbt.barsgl.ejb.common.mapping.od.BankCalendarDayId;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.Stateless;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

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

}
