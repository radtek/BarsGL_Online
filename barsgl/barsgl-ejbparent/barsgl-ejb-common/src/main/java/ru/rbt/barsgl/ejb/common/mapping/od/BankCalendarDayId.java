package ru.rbt.barsgl.ejb.common.mapping.od;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Created by Ivan Sevastyanov
 */
@Embeddable
public class BankCalendarDayId implements Serializable {

    @Column(name = "DAT")
    @Temporal(TemporalType.DATE)
    private Date calendarDate;

    @Column(name = "CCY")
    private String calendarCode;

    public BankCalendarDayId() {}

    public BankCalendarDayId(Date calendarDate, String calendarCode) {
        this.calendarDate = calendarDate;
        this.calendarCode = calendarCode;
    }

    public Date getCalendarDate() {
        return calendarDate;
    }

    public void setCalendarDate(Date calendarDate) {
        this.calendarDate = calendarDate;
    }

    public String getCalendarCode() {
        return calendarCode;
    }

    public void setCalendarCode(String calendarCode) {
        this.calendarCode = calendarCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankCalendarDayId that = (BankCalendarDayId) o;
        return Objects.equals(calendarDate, that.calendarDate) &&
                Objects.equals(calendarCode, that.calendarCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(calendarDate, calendarCode);
    }
}
