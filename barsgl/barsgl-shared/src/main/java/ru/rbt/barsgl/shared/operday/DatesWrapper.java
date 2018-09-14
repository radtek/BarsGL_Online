package ru.rbt.barsgl.shared.operday;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by er18837 on 15.08.2018.
 */
public class DatesWrapper implements Serializable{
    public final String dateFormat = "dd.MM.yyyy";
    private String dateFromStr;
    private String dateToStr;
    private Date dateFrom;
    private Date dateTo;

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

}
