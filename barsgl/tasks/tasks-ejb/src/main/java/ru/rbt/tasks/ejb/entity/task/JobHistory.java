package ru.rbt.tasks.ejb.entity.task;

import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.shared.jobs.TimerJobHistoryWrapper;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov on 17.02.2016.
 */
@Entity
@Table(name = "GL_SCHED_H")
public class JobHistory extends BaseEntity<Long> {

    @Id
    @Column(name = "ID_HIST")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "SCHED_NAME")
    private String jobName;

    @Temporal(TemporalType.DATE)
    @Column(name = "OPERDAY")
    private Date operday;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "SCHRSLT")
    private DwhUnloadStatus result;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "STARTTIME")
    private Date starttime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ENDTIME")
    private Date endtime;

    public JobHistory() {
    }

    public JobHistory(String jobName, Date operday) {
        this.jobName = jobName;
        this.operday = operday;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Date getOperday() {
        return operday;
    }

    public void setOperday(Date operday) {
        this.operday = operday;
    }

    public DwhUnloadStatus getResult() {
        return result;
    }

    public void setResult(DwhUnloadStatus result) {
        this.result = result;
    }

    public Date getStarttime() {
        return starttime;
    }

    public void setStarttime(Date starttime) {
        this.starttime = starttime;
    }

    public Date getEndtime() {
        return endtime;
    }

    public void setEndtime(Date endtime) {
        this.endtime = endtime;
    }

    public boolean isRunning() {
        return DwhUnloadStatus.STARTED == this.result;
    }

    public TimerJobHistoryWrapper toWrapper() {
        return new TimerJobHistoryWrapper(id);
    }
}
