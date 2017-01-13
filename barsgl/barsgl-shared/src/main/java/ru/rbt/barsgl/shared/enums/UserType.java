package ru.rbt.barsgl.shared.enums;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by ER18837 on 10.09.15.
 */
public enum UserType implements Serializable, IsSerializable{
        TY_ADMIN, TY_USER;

        UserType() {};
}
