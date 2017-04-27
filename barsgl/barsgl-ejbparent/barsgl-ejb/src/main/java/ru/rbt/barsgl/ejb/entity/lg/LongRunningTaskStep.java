package ru.rbt.barsgl.ejb.entity.lg;

import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadParams;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov on 18.10.2016.
 */
@Entity
@Table(name = "GL_LONGTSKSTEP")
public class LongRunningTaskStep extends BaseEntity<LongRunningTaskStepId> {

    @EmbeddedId
    private LongRunningTaskStepId id;

    @Column(name = "DTM_START")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDate;

    @Column(name = "DTM_END")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDate;

    @Column(name = "TSKRSLT")
    @Enumerated(EnumType.ORDINAL)
    private DwhUnloadStatus status;

    @Override
    public LongRunningTaskStepId getId() {
        return id;
    }

    @Override
    public void setId(LongRunningTaskStepId id) {
        this.id = id;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public DwhUnloadStatus getStatus() {
        return status;
    }

    public void setStatus(DwhUnloadStatus status) {
        this.status = status;
    }

    public boolean isSuccess() {
        return status == DwhUnloadStatus.SUCCEDED;
    }

    @Override
    public String toString() {
        return "LongRunningTaskStep{" +
                "id=" + id +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status=" + status +
                '}';
    }
}
