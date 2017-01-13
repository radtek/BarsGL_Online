package ru.rbt.barsgl.gwt.core.ui;

import java.io.Serializable;

public interface IBoxValue<T extends Serializable> {
   public void setValue(T value);
   public T getValue();
   public String getText();
   public boolean hasValue();
   public boolean validate();
   public void setReadOnly(boolean readOnly);
   public void setEnabled(boolean enabled);
   public boolean isEnabled();
   public void clear();
   public void setWidth(String width);
}
