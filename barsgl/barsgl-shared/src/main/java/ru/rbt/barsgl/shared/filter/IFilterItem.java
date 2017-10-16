package ru.rbt.barsgl.shared.filter;

import java.io.Serializable;

/**
 * Created by er18837 on 07.07.2017.
 */
public interface IFilterItem {

    String getName();

    Serializable getValue();

    FilterCriteria getCriteria();

    boolean needValue();

    boolean isPined();

    Serializable getSqlValue();

    String getSqlName();

    boolean isReadOnly();

    void setReadOnly(boolean isReadOnly);

    String getCaption();

    void setCaption(String caption);

    String getStrValue();

    void setStrValue(String strValue);
}
