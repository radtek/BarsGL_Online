package ru.rbt.barsgl.ejbcore.mapping.job;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Created by Ivan Sevastyanov
 */
@Entity
@DiscriminatorValue("CALENDAR")
public class CalendarJob extends TimerJob {

    @Column(name = "SCH_EXPR", nullable = false)
    private String scheduleExpression;

    public String getScheduleExpression() {
        return scheduleExpression;
    }

    public void setScheduleExpression(String scheduleExpression) {
        this.scheduleExpression = scheduleExpression;
    }
}
