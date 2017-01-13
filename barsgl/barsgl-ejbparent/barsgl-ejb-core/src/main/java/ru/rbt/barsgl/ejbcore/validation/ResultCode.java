package ru.rbt.barsgl.ejbcore.validation;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ER22317 on 05.10.2016.
 */
public enum ResultCode {
     SUCCESS(0)
    ,ACCOUNT_NOT_FOUND(1)
    ,ACCOUNT_IS_OPEN_LATER(2)
    ,ACCOUNT_IS_CLOSED_BEFOR(3);

    private int value;
    private ResultCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
    private static Map<Integer, ResultCode> map = new HashMap<Integer, ResultCode>();

    static {
        for (ResultCode rc : ResultCode.values()) {
            map.put(rc.value, rc);
        }
    }
    public static ResultCode valueOf(int rc) {
        return map.get(rc);
    }
}
