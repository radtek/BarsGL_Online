package ru.rbt.ejbcore.datarec;

/**
 * Created by Ivan Sevastyanov
 */

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * Запись реляционных данных (строка выборки)
 */
public final class DataRecord implements Serializable {

    /**
     * Если эта колонка есть в объекте, сравнение объектов будет проходить по значению этого поля
     */
    public static final String IDENTIFIER = "ID";

    /**
     * Колонки в данной записи
     */
    private final List _cols = new ArrayList();

    /**
     * Конструктор по умолчанию (необходимо для сериализации)
     */
    public DataRecord() {
    }

    /**
     * Создает новый экземпляр {@link DataRecord}, инициализируя его значениями из реляционной выборки
     *
     * @param rs      Реляционная выборка
     * @param adapter Адаптер, предоставляющий функции считывания данных из реляционной выборки
     * @throws java.sql.SQLException В случае ошибки считывания данных
     */
    public DataRecord(ResultSet rs, JdbcAdapter adapter) {
        try {
            ResultSetMetaData meta = rs.getMetaData();
            int col_count = meta.getColumnCount();
            for (int i = 1; i <= col_count; i++) {
                String name = meta.getColumnLabel(i);
                Object value = adapter.getResultValue(rs, i);
                _cols.add(new DataColumn(name, value));
            }
        } catch (SQLException e) {
            throw new DataRecordException(e.getMessage(), e);
        }
    }

    public int getColumnCount() {
        return _cols.size();
    }

    public String getColumnName(int index) {
        return getColumn(index).getName();
    }

    public int getColumnIndex(String name) {
        for (int i = 0; i < _cols.size(); i++) {
            DataColumn col = (DataColumn) _cols.get(i);
            if (col.getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        throw new DataRecordException("Invalid column name: " + name);
    }

    private DataColumn getColumn(int index) {
        if ((index < 0) || (index >= _cols.size())) {
            throw new DataRecordException("Invalid column index: " + index);
        }
        return (DataColumn)_cols.get(index);
    }

    private DataColumn getColumn(String name) {
        for (int i=0; i < _cols.size(); i++) {
            DataColumn column = (DataColumn) _cols.get(i);
            if (column.getName().equalsIgnoreCase(name)) {
                return column;
            }
        }
        throw new DataRecordException("Invalid column name: " + name);
    }

    public Boolean getBoolean(String name) {
        DataColumn col = getColumn(name);
        Object obj = col.getData();
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return new Boolean(((Number) obj).longValue() != 0);
        }
        if (obj instanceof String) {
            return new Boolean("y".equalsIgnoreCase("" + obj) || "yes".equalsIgnoreCase("" + obj) || "true".equalsIgnoreCase("" + obj));
        }
        throw new DataRecordException("Error converting value of type " + obj.getClass().getName() + " to Boolean");
    }

    public Boolean getBoolean(int index) {
        return getBoolean(getColumnName(index));
    }

    public Integer getInteger(String name) {
        DataColumn col = getColumn(name);
        Object obj = col.getData();
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return new Integer(((Number) obj).intValue());
        }
        if (obj instanceof String) {
            try {
                return new Integer(Integer.parseInt((String) obj));
            } catch (Throwable ignore) {
                throw new DataRecordException("String to Integer conversion error");
            }
        }
        throw new DataRecordException("Error converting value of type " + obj.getClass().getName() + " to Integer");
    }

    public Integer getInteger(int index) {
        return getInteger(getColumnName(index));
    }

    public Long getLong(String name) {
        DataColumn col = getColumn(name);
        Object obj = col.getData();
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return new Long(((Number) obj).longValue());
        }
        if (obj instanceof String) {
            try {
                return new Long(Long.parseLong((String) obj));
            } catch (Throwable ignore) {
                throw new DataRecordException("String to Long conversion error");
            }
        }
        throw new DataRecordException("Error converting value of type " + obj.getClass().getName() + " to Long");
    }

    public Long getLong(int index) {
        return getLong(getColumnName(index));
    }

    public Double getDouble(String name) {
        DataColumn col = getColumn(name);
        Object obj = col.getData();
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return new Double(((Number) obj).doubleValue());
        }
        if (obj instanceof String) {
            try {
                return new Double(Double.parseDouble((String) obj));
            } catch (Throwable ignore) {
                throw new DataRecordException("String to Double conversion error");
            }
        }
        throw new DataRecordException("Error converting value of type " + obj.getClass().getName() + " to Double");
    }

    public Double getDouble(int index) {
        return getDouble(getColumnName(index));
    }

    public Float getFloat(String name) {
        DataColumn col = getColumn(name);
        Object obj = col.getData();
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return new Float(((Number) obj).floatValue());
        }
        if (obj instanceof String) {
            try {
                return new Float(Float.parseFloat((String) obj));
            } catch (Throwable ignore) {
                throw new DataRecordException("String to Double conversion error");
            }
        }
        throw new DataRecordException("Error converting value of type " + obj.getClass().getName() + " to Double");
    }

    public Float getFloat(int index) {
        return getFloat(getColumnName(index));
    }

    public Short getShort(String name) {
        DataColumn col = getColumn(name);
        Object obj = col.getData();
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return new Short(((Number) obj).shortValue());
        }
        if (obj instanceof String) {
            try {
                return new Short(Short.parseShort((String) obj));
            } catch (Throwable e) {
                throw new DataRecordException("String to Double conversion error", e);
            }
        }
        throw new DataRecordException("Error converting value of type " + obj.getClass().getName() + " to Double");
    }

    public Short getShort(int index) {
        return getShort(getColumnName(index));
    }

    public Byte getByte(String name) {
        DataColumn col = getColumn(name);
        Object obj = col.getData();
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return new Byte(((Number) obj).byteValue());
        }
        if (obj instanceof String) {
            try {
                return new Byte(Byte.parseByte((String) obj));
            } catch (Throwable ignore) {
                throw new DataRecordException("String to Double conversion error");
            }
        }
        throw new DataRecordException("Error converting value of type " + obj.getClass().getName() + " to Double");
    }

    public Byte getByte(int index) {
        return getByte(getColumnName(index));
    }

    public BigInteger getBigInteger(String name) {
        DataColumn col = getColumn(name);
        Object obj = col.getData();
        if (obj == null) {
            return null;
        }
        if (obj instanceof BigInteger) {
            return (BigInteger) obj;
        }
        if (obj instanceof BigDecimal) {
            try {
                return ((BigDecimal) obj).toBigInteger();
            } catch (Throwable th) {
                throw new DataRecordException("Error converting BigDecimal to BigInteger");
            }
        }

        if ((obj instanceof Number) || (obj instanceof String)) {
            try {
                return new BigInteger(obj.toString());
            } catch (Throwable th) {
                throw new DataRecordException("Error converting value of type " + obj.getClass().getName() + " to BigInteger");
            }
        }
        throw new DataRecordException("Error converting value of type " + obj.getClass().getName() + " to BigInteger");
    }

    public BigInteger getBigInteger(int index) {
        return getBigInteger(getColumnName(index));
    }

    public BigDecimal getBigDecimal(String name) {
        DataColumn col = getColumn(name);
        Object obj = col.getData();
        if (obj == null) {
            return null;
        }
        if (obj instanceof BigDecimal) {
            return (BigDecimal) obj;
        }
        if ((obj instanceof Number) || (obj instanceof String)) {
            try {
                return new BigDecimal(obj.toString());
            } catch (Throwable th) {
                throw new DataRecordException("Error converting value of type " + obj.getClass().getName() + " to BigDecimal");
            }
        }
        throw new DataRecordException("Error converting value of type " + obj.getClass().getName() + " to BigDecimal");
    }

    public BigDecimal getBigDecimal(int index) {
        return getBigDecimal(getColumnName(index));
    }

    public Date getDate(int index) {
        return getDate(getColumnName(index));
    }

    public Date getDate(String name) {
        return (Date) getObject(name);
    }

    /**
     * @param name
     * @return
     */
    public java.sql.Date getSqlDate(String name) {
        return getObject(name) != null ? new java.sql.Date(((Date) getObject(name)).getTime()) : null;
    }

    public String getString(String name) {
        Object obj = getObject(name);
        return obj == null ? null : "" + obj;
    }

    public String getString(int index) {
        return getString(getColumnName(index));
    }

    public byte[] getBinary(String name) {
        return (byte[]) getObject(name);
    }

    public byte[] getBinary(int index) {
        return getBinary(getColumnName(index));
    }

    public Object getObject(String name) {
        return getColumn(name).getData();
    }

    public Object getObject(int index) {
        return getObject(getColumnName(index));
    }

    public boolean isNull(String name) {
        return (getObject(name) == null);
    }

    public boolean isNull(int index) {
        return (getObject(index) == null);
    }

    public boolean getSimpleBoolean(int index) {
        Boolean b = getBoolean(index);
        return (b != null && b.booleanValue());
    }

    public boolean getSimpleBoolean(String name) {
        Boolean b = getBoolean(name);
        return (b != null && b.booleanValue());
    }

    public int getSimpleInteger(int index) {
        Integer obj = getInteger(index);
        return (obj == null ? 0 : obj.intValue());
    }

    public int getSimpleInteger(String name) {
        Integer obj = getInteger(name);
        return (obj == null ? 0 : obj.intValue());
    }

    public long getSimpleLong(int index) {
        Long obj = getLong(index);
        return (obj == null ? 0 : obj.longValue());
    }

    public long getSimpleLong(String name) {
        Long obj = getLong(name);
        return (obj == null ? 0 : obj.longValue());
    }

    public double getSimpleDouble(int index) {
        Double obj = getDouble(index);
        return (obj == null ? 0 : obj.doubleValue());
    }

    public double getSimpleDouble(String name) {
        Double obj = getDouble(name);
        return (obj == null ? 0 : obj.doubleValue());
    }

    public Object getObject(int index, Class type) {
        Object obj = getObject(index);
        if ((obj == null) || (type == null) || type.isAssignableFrom(obj.getClass())) {
            return obj;
        }
        if (String.class.equals(type)) {
            return getString(index);
        } else if (Boolean.class.equals(type) || Boolean.TYPE.equals(type)) {
            return getBoolean(index);
        } else if (Byte.class.equals(type) || Byte.TYPE.equals(type)) {
            return getByte(index);
        } else if (Short.class.equals(type) || Short.TYPE.equals(type)) {
            return getShort(index);
        } else if (Integer.class.equals(type) || Integer.TYPE.equals(type)) {
            return getInteger(index);
        } else if (Long.class.equals(type) || Long.TYPE.equals(type)) {
            return getLong(index);
        } else if (Float.class.equals(type) || Float.TYPE.equals(type)) {
            return getFloat(index);
        } else if (Double.class.equals(type) || Double.TYPE.equals(type)) {
            return getDouble(index);
        } else if (Date.class.equals(type)) {
            return getDate(index);
        } else if (BigInteger.class.equals(type)) {
            return getBigInteger(index);
        } else if (BigDecimal.class.equals(type)) {
            return getBigDecimal(index);
        }
        // непонятно, как преобразовывать
        return obj;
    }

    public Object getObject(String name, Class type) {
        int index = getColumnIndex(name);
        return getObject(index, type);
    }

    public boolean containsColumn(String name) {
        return null != getColumn(name);
    }

/*
    public <T extends Enum<T>> T getEnum(Class<T> enumClass, int index) {
        return getEnumValue(enumClass, getString(index));
    }
*/

/*
    public <T extends Enum<T>> T getEnum(Class<T> enumClass, String name) {
        return getEnumValue(enumClass, getString(name));
    }
*/

/*
    private <T extends Enum<T>> T getEnumValue(Class<T> enumClass, String value) {
        if (enumClass == null) {
            throw new IllegalArgumentException("Enum class not provided");
        }
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value);
        } catch (Exception ex) {
            throw new DataRecordException("Failed to obtain enum value: " + enumClass.getName() + "." + value, ex);
        }
    }
*/

    public DataRecord addColumn(String name, Object data) {
        _cols.add(new DataColumn(name, data));
        return this;
    }

    public List getColumns() {
        return Collections.unmodifiableList(_cols);
    }

    public class DataColumn implements Serializable {

        private String _name;
        private Object _data;

        private DataColumn(String name, Object data) {
            _name = name;
            _data = data;
        }

        public String getName() {
            return _name;
        }

        public Object getData() {
            return _data;
        }

    }

    
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof DataRecord)) return false;

        return record2Map(this).equals(record2Map((DataRecord) o));
    }

    
    public int hashCode() {
        return record2Map(this).hashCode();
    }

    private static Map record2Map(DataRecord rec) {
        HashMap res = new HashMap();
        if (rec.isColumnPresented(IDENTIFIER)) {
            res.put(IDENTIFIER, rec.getObject(IDENTIFIER));
        } else {
            for (int i = 0; i < rec.getColumnCount(); i++) {
                DataColumn col = rec.getColumn(i);
                if (!"rn".equalsIgnoreCase(col.getName())) {
                    res.put(col.getName(), col.getData());
                }
            }
        }
        return res;
    }

    private boolean isColumnPresented(String columnName) {
        DataColumn col = null;
        try {
            col = getColumn(columnName);
        } catch (Exception ignore) {}
        return null != col;
    }
}
