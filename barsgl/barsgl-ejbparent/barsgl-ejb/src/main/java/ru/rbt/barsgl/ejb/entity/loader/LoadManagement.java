package ru.rbt.barsgl.ejb.entity.loader;

import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.shared.enums.LoadManagementAction;
import ru.rbt.barsgl.shared.enums.LoadManagementStatus;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by SotnikovAV on 28.10.2016.
 */
@Entity
@Table(name = "LOAD_MANAGEMENT")
public class LoadManagement extends BaseEntity<Long> {

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ORDID", nullable = false)
    private long ordid;

    @Column(name = "DAT", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date dat;

    @Column(name = "CODE", nullable = false)
    private String code;

    @Column(name = "ACTION")
    @Enumerated(EnumType.ORDINAL)
    private LoadManagementAction action;

    @Column(name = "ORD")
    private int order;

    @Column(name = "STATUS")
    @Enumerated(EnumType.ORDINAL)
    private LoadManagementStatus status;

    @Column(name = "EXPERT")
    private String expert;

    @Column(name = "EXPERT_MODIFIED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expertModified;

    @Column(name = "MANAGER")
    private String manager;

    @Column(name = "MANAGER_MODIFIED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date managerModified;

    @Column(name = "OPERATOR")
    private String operator;

    @Column(name = "OPERATOR_MODIFIED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date operatorModified;

    @Column(name = "START_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;

    @Column(name = "FINISH_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date finishTime;

    public LoadManagement() {}

    public LoadManagement(
            long id,
            long ordid,
            Date dat,
            String code,
            LoadManagementAction action,
            int order,
            LoadManagementStatus status,
            String expert,
            Date expertModified,
            String manager,
            Date managerModified,
            String operator,
            Date operatorModified,
            Date startTime,
            Date finishTime
    ) {
        this.id = id;
        this.ordid = ordid;
        this.dat = dat;
        this.code = code;
        this.action = action;
        this.order = order;
        this.status = status;
        this.expert = expert;
        this.expertModified = expertModified;
        this.manager = manager;
        this.managerModified = managerModified;
        this.operator = operator;
        this.operatorModified = operatorModified;
        this.startTime = startTime;
        this.finishTime = finishTime;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getOrdid() {
        return ordid;
    }

    public void setOrdid(long ordid) {
        this.ordid = ordid;
    }

    public Date getDat() {
        return dat;
    }

    public void setDat(Date dat) {
        this.dat = dat;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LoadManagementAction getAction() {
        return action;
    }

    public void setAction(LoadManagementAction action) {
        this.action = action;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public LoadManagementStatus getStatus() {
        return status;
    }

    public void setStatus(LoadManagementStatus status) {
        this.status = status;
    }

    public String getExpert() {
        return expert;
    }

    public void setExpert(String expert) {
        this.expert = expert;
    }

    public Date getExpertModified() {
        return expertModified;
    }

    public void setExpertModified(Date expertModified) {
        this.expertModified = expertModified;
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public Date getManagerModified() {
        return managerModified;
    }

    public void setManagerModified(Date managerModified) {
        this.managerModified = managerModified;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Date getOperatorModified() {
        return operatorModified;
    }

    public void setOperatorModified(Date operatorModified) {
        this.operatorModified = operatorModified;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }
}
