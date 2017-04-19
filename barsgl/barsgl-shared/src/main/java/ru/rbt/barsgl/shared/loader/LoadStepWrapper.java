package ru.rbt.barsgl.shared.loader;

import com.google.gwt.user.client.rpc.IsSerializable;
import ru.rbt.barsgl.shared.ErrorList;
import ru.rbt.shared.enums.Repository;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by SotnikovAV on 28.10.2016.
 */
public class LoadStepWrapper implements Serializable, IsSerializable {

    private Repository repository;

    private long id;

    private long ordid;

    private Date dat;

    private int action;

    private String code;

    private String name;

    private Date startTime;

    private Date endTime;

    private String execTime;

    private String result;

    private int count;

    private String msg;

    private String procedure;

    private int order;

    private int status;

    private String expert;

    private Date expertModified;

    private String manager;

    private Date managerModified;

    private String operator;

    private Date operatorModified;

    private Date actionStartTime;

    private Date actionFinishTime;
    /**
     * список ошибок
     */
    private ErrorList errorList = new ErrorList();

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getExecTime() {
        return execTime;
    }

    public void setExecTime(String execTime) {
        this.execTime = execTime;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getProcedure() {
        return procedure;
    }

    public void setProcedure(String procedure) {
        this.procedure = procedure;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
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

    public Date getActionStartTime() {
        return actionStartTime;
    }

    public void setActionStartTime(Date actionStartTime) {
        this.actionStartTime = actionStartTime;
    }

    public Date getActionFinishTime() {
        return actionFinishTime;
    }

    public void setActionFinishTime(Date actionFinishTime) {
        this.actionFinishTime = actionFinishTime;
    }

    public ErrorList getErrorList() {
        return errorList;
    }

    public void setErrorList(ErrorList errorList) {
        this.errorList = errorList;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

}
