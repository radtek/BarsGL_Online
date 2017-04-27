package ru.rbt.barsgl.ejb.common.mapping.od;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import static ru.rbt.barsgl.ejb.common.mapping.od.BankCalendarDay.HolidayFlag.X;

/**
 * Created by Ivan Sevastyanov
 */
@Entity
@Table(name = "CAL")
public class BankCalendarDay extends BaseEntity<BankCalendarDayId>{

    public enum HolidayFlag {
        X
    }

    @EmbeddedId
    private BankCalendarDayId id;

    @Column(name = "HOL")
    private String holiday;

    @Override
    public BankCalendarDayId getId() {
        return id;
    }

    @Override
    public void setId(BankCalendarDayId id) {
        this.id = id;
    }

    public String getHoliday() {
        return holiday;
    }

    public void setHoliday(String holiday) {
        this.holiday = holiday;
    }

    public boolean isHoliday() {
        return X.name().equals(this.holiday);
    }
}
