package ru.rbt.barsgl.gwt.client.dict.dlg;

import java.util.Date;

/**
 * Created by ER18837 on 18.01.16.
 */
public class OfrQuickFilterParams {
    public enum SymbolState {All, Active, Inactive};

    private Date visibleDate;
    private Date reportDate;
    private SymbolState reportState;

    public OfrQuickFilterParams(Date visibleDate, Date reportDate, SymbolState reportState) {
        this.visibleDate = visibleDate;
        this.reportDate = reportDate;
        this.reportState = reportState;
    }

    public Date getVisibleDate() {
        return visibleDate;
    }

    public Date getReportDate() {
        return reportDate;
    }

    public SymbolState getReportState() {
        return reportState;
    }
}
