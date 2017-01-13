package ru.rbt.barsgl.shared.column;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by ER18837 on 16.02.16.
 */
public enum XlsType implements Serializable, IsSerializable {
    STRING, BOOLEAN, INTEGER, DECIMAL, DATE, DATETIME, LONG;

    public static XlsType getType(String typeStr) {
        for (XlsType type : XlsType.values()) {
            if(type.toString().equals(typeStr))
                return type;
        }
        return null;
    }
}
