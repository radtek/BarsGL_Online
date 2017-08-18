package ru.rbt.barsgl.shared.operation;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by er18837 on 18.08.2017.
 */
public class CardReportWrapper implements Serializable, IsSerializable {
    public final String dateFormat = "dd.MM.yyyy";

    private String filial;
    private String postDateStr;

    public String getDateFormat() {
        return dateFormat;
    }

    public String getFilial() {
        return filial;
    }

    public void setFilial(String filial) {
        this.filial = filial;
    }

    public String getPostDateStr() {
        return postDateStr;
    }

    public void setPostDateStr(String postDateStr) {
        this.postDateStr = postDateStr;
    }
}
