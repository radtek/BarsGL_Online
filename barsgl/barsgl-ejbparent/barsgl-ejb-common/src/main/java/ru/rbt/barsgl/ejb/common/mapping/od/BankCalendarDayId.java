package ru.rbt.barsgl.ejb.common.mapping.od;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

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
}
