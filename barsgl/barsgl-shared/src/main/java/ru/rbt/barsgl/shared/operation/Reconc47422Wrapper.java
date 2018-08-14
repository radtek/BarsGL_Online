package ru.rbt.barsgl.shared.operation;

import ru.rbt.barsgl.shared.enums.ProcessingType;

/**
 * Created by er18837 on 06.08.2018.
 */
public class Reconc47422Wrapper {

    private String contract;
    private String currency;
    private String filial;
    private ProcessingType procType;
    private Boolean boolNow;
    private String dateFromStr;
    private String dateToStr;

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

    public Boolean getBoolNow() {
        return boolNow;
    }

    public void setBoolNow(Boolean boolNow) {
        this.boolNow = boolNow;
    }

    public String getDateFromStr() {
        return dateFromStr;
    }

    public void setDateFromStr(String dateFromStr) {
        this.dateFromStr = dateFromStr;
    }

    public String getDateToStr() {
        return dateToStr;
    }

    public void setDateToStr(String dateToStr) {
        this.dateToStr = dateToStr;
    }
}
