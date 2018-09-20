package ru.rbt.barsgl.shared.operation;

import ru.rbt.barsgl.shared.enums.ProcessingType;

import java.util.Date;

/**
 * Created by er18837 on 06.08.2018.
 */
public class Rep47422Wrapper {

    private String contract;
    private String currency;
    private String filial;
    private ProcessingType procType;
    private Date dateFrom;
    private Date dateTo;
    private boolean isRegister;

    public String getContract() {
        return contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getFilial() {
        return filial;
    }

    public void setFilial(String filial) {
        this.filial = filial;
    }

    public ProcessingType getProcType() {
        return procType;
    }

    public void setProcType(ProcessingType procType) {
        this.procType = procType;
    }

    public Date getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(Date dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Date getDateTo() {
        return dateTo;
    }

    public void setDateTo(Date dateTo) {
        this.dateTo = dateTo;
    }

    public boolean isRegister() {
        return isRegister;
    }

    public void setRegister(boolean register) {
        isRegister = register;
    }
}
