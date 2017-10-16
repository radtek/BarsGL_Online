package ru.rbt.barsgl.gwt.core.ui;

import java.io.Serializable;

public interface IBoxValue<T extends Serializable> {
   void setValue(T value);
   T getValue();
   String getText();
   boolean hasValue();
   boolean validate();
   void setReadOnly(boolean readOnly);
   void setEnabled(boolean enabled);
   boolean isEnabled();
   void clear();
   void setWidth(String width);
}
