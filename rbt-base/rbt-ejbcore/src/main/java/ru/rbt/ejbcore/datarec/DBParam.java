package ru.rbt.ejbcore.datarec;

import java.io.Serializable;
import java.sql.Types;
import java.util.Date;

public class DBParam implements Serializable {

    private static final int serialVersionUID = -1;

    public enum DbParamType {

        VARCHAR(Types.VARCHAR), DATE(Types.DATE), LONG(Types.BIGINT), INTEGER(Types.INTEGER);

        private final int paramType;

        DbParamType(int paramType) {
            this.paramType = paramType;
        }

        public int getParamType() {
            return paramType;
        }
    }

    public enum DBParamDirectionType {
        IN, OUT, IN_OUT
    }

    private int paramType;
    private DBParamDirectionType directionType = DBParamDirectionType.IN;
    private Object value;

    public DBParam(int type, DBParamDirectionType direction, Object value) {
        this.paramType = type;
        this.directionType = direction;
        this.value = value;
    }

    public DBParam(int type, DBParamDirectionType direction) {
        this.paramType = type;
        this.directionType = direction;
        this.value = null;
    }

    public DBParam(int type, Object value) {
        this.paramType = type;
        this.value = value;
    }

    public DBParam(DbParamType type, DBParamDirectionType direction, Object value) {
        this(type.getParamType(), direction, value);
    }

    public DBParam(DbParamType type, DBParamDirectionType direction) {
        this(type.getParamType(), direction);
    }

    public int getParamType() {
        return paramType;
    }

    public void setParamType(int paramType) {
        this.paramType = paramType;
    }

    public DBParamDirectionType getDirectionType() {
        return directionType;
    }

    public void setDirectionType(DBParamDirectionType directionType) {
        this.directionType = directionType;
    }

    public Object getValue() {
        return value;
    }

    public Date getDate() {
        return (Date) value;
    }

    public String getString() {
        return (String) value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isIn() {
        return this.directionType == DBParamDirectionType.IN;
    }

}
