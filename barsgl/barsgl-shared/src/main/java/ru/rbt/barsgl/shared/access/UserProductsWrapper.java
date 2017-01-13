package ru.rbt.barsgl.shared.access;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by akichigi on 20.04.16.
 */
public class UserProductsWrapper implements Serializable, IsSerializable {
    private String code;
    private String descr;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }
}
