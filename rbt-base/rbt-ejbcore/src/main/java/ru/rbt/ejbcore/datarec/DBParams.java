package ru.rbt.ejbcore.datarec;

import ru.rbt.ejbcore.datarec.DBParam.DBParamDirectionType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DBParams implements Serializable {

    private static final int serialVersionUID = -1;

    private List<DBParam> store = new ArrayList<>();

    public DBParams addParam(int type, DBParamDirectionType direction, Object value) {
        store.add(new DBParam(type, direction, value));
        return this;
    }

    public DBParams addParam(DBParam.DbParamType type, DBParamDirectionType direction, Object value) {
        store.add(new DBParam(type, direction, value));
        return this;
    }

    public DBParams addParam(int type, Object value) {
        store.add(new DBParam(type, DBParamDirectionType.IN, value));
        return this;
    }

    public DBParams addParam(DBParam param) {
        store.add(param);
        return this;
    }


    public List<DBParam> getParams() {
        return Collections.unmodifiableList(store);
    }

    public static final DBParams createParams(DBParam ... params) {
        DBParams res = new DBParams();
        if (null != params) {
            Arrays.stream(params).forEach(res::addParam);
        }
        return res;
    }
}
