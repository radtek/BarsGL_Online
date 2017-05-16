package ru.rbt.ejbcore.mapping;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;
import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * Created by Ivan Sevastyanov
 */
@MappedSuperclass
public abstract class BaseEntity<T extends Serializable> implements Serializable {

    public abstract T getId();
    public void setId(T id) {
        throw new UnsupportedOperationException("Not supported");
    }

    public String toString() {
        return this.getClass().getName() + ": " + getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseEntity that = (BaseEntity) o;

        return !(getId() != null ? !getId().equals(that.getId()) : that.getId() != null);

    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }

    public String getTableName() {
        String tableName = "";
        try {
            Table annotation = this.getClass().getDeclaredAnnotation(Table.class);
            if (null == annotation)
                annotation = this.getClass().getSuperclass().getDeclaredAnnotation(Table.class);

            if (null != annotation)
                return annotation.name();

        } catch (Exception e) {
            return "";
        }
        return "";
    }

    public String getColumnName(String fieldName) {
        try {
            String tableName = getTableName();
            try {
                Field field = this.getClass().getDeclaredField(fieldName);
                String columnName = "";
                try {
                    columnName = field.getDeclaredAnnotation(Column.class).name();
                } catch (Exception e) {
                    columnName = field.getDeclaredAnnotation(JoinColumn.class).name();
                }
                return tableName + "." + columnName;
            } catch (NoSuchFieldException e) {
                return tableName + ".<" + fieldName + ">";
            }
        } catch (Exception e) {
            return "<" + fieldName + ">";
        }
    }

}
