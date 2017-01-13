package ru.rbt.barsgl.ejb.entity.lg;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * Created by Ivan Sevastyanov on 18.10.2016.
 */
@Embeddable
public class LongRunningTaskStepId implements Serializable {

    @Column(name = "ID_STEP")
    private Long idStepPattern;

    @Column(name = "ID_HIST")
    private Long idHistory;

    public LongRunningTaskStepId() {
    }

    public LongRunningTaskStepId(Long idStepPattern, Long idHistory) {
        this.idStepPattern = idStepPattern;
        this.idHistory = idHistory;
    }

    public Long getIdStepPattern() {
        return idStepPattern;
    }

    public void setIdStepPattern(Long idStepPattern) {
        this.idStepPattern = idStepPattern;
    }

    public Long getIdHistory() {
        return idHistory;
    }

    public void setIdHistory(Long idHistory) {
        this.idHistory = idHistory;
    }

    @Override
    public String toString() {
        return "LongRunningTaskStepId{" +
                "idStepPattern=" + idStepPattern +
                ", idHistory=" + idHistory +
                '}';
    }
}
