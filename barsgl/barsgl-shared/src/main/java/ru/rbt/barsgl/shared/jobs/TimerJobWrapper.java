package ru.rbt.barsgl.shared.jobs;

import ru.rbt.barsgl.shared.enums.JobSchedulingType;
import ru.rbt.barsgl.shared.enums.JobStartupType;

import java.io.Serializable;

/**
 * Created by akichigi on 11.03.15.
 */
public class TimerJobWrapper implements Serializable {

    private Long id;
    private String name;
    private JobStartupType startupType;
    private String state;
    private JobSchedulingType jobType;
    private String description;
    private Long interval;
    private String scheduleExpression;
    private String properties;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JobStartupType getStartupType() {
        return startupType;
    }

    public void setStartupType(JobStartupType startupType) {
        this.startupType = startupType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public JobSchedulingType getJobType() {
        return jobType;
    }

    public void setJobType(JobSchedulingType jobType) {
        this.jobType = jobType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public String getScheduleExpression() {
        return scheduleExpression;
    }

    public void setScheduleExpression(String scheduleExpression) {
        this.scheduleExpression = scheduleExpression;
    }
}
