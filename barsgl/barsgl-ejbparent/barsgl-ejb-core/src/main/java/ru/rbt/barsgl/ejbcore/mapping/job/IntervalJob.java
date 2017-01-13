package ru.rbt.barsgl.ejbcore.mapping.job;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Created by Ivan Sevastyanov
 */
@Entity
@DiscriminatorValue("INTERVAL")
public class IntervalJob extends TimerJob {

    /**
     * Периодичность выполнения задачи в миллисекундах
     */
    @Column(name = "INTERVAL", nullable = false)
    private Long interval;

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }
}
