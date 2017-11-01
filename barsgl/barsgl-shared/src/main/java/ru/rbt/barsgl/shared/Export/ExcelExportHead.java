package ru.rbt.barsgl.shared.Export;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by akichigi on 06.02.17.
 */
public class ExcelExportHead implements Serializable, IsSerializable {
    private String formTitle;
    private String user;
    private String filter;

    public ExcelExportHead(){}

    public ExcelExportHead(String formTitle, String user, String filter) {
        this.formTitle = formTitle;
        this.user = user;
        this.filter = filter;
    }

    public String getFormTitle() {
        return formTitle;
    }

    public String getUser() {
        return user;
    }

    public String getFilter() {
        return filter;
    }

    public void setFormTitle(String formTitle) {
        this.formTitle = formTitle;
    }
}
