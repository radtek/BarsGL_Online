package ru.rbt.barsgl.gwt.core.datafields;

import java.io.Serializable;

public class Field<T extends Serializable> implements Serializable {
	private static final long serialVersionUID = -7294306385561414413L;

	private T initValue;
	private T curValue;
	private boolean changed;

	@SuppressWarnings("unused")
	private Field() {}
	
	public Field(T value) {
		initValue = value;
		curValue = null;
		changed = false;
	}

	public T getValue() {
		return isChanged() ? curValue : initValue;
	}
	
	public T getInitValue() {
		return initValue;
	}

	public void setValue(T value) {
		curValue = value;
		changed = true;
	}

	public boolean isChanged() {
		return changed;
	}

	public void apply() {
		if (isChanged()) {
			initValue = curValue;
			changed = false;
			curValue = null;
		}
	}

	public void cancel() {
		if (isChanged()) {
			changed = false;
			curValue = null;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Field field = (Field) o;
		if (changed != field.changed) return false;
		if (curValue != null ? !curValue.equals(field.curValue) : field.curValue != null) return false;
		if (initValue != null ? !initValue.equals(field.initValue) : field.initValue != null) return false;
		return true;
	}

	@Override
	public int hashCode() {
		int result = initValue != null ? initValue.hashCode() : 0;
		result = 31 * result + (curValue != null ? curValue.hashCode() : 0);
		result = 31 * result + (changed ? 1 : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Field [getValue()=" + getValue() + "]";
	}
}