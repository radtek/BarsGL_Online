package ru.rbt.barsgl.gwt.core.datafields;

import java.io.Serializable;
import java.util.ArrayList;

public class Row implements Serializable {
	private static final long serialVersionUID = -8889847742275148689L;

	private ArrayList<Field> fields = new ArrayList<Field>();

    public Row(Field[] values) {
        for (Field f : values) {
            fields.add(f);
        }
    }

// TODO А оно вообще надо?
//    public Row(Serializable[] values) {
//        for (Serializable o : values) {
//            fields.add(new Field(o));
//        }
//    }

    public Row() {}

    public int getFieldsCount() {
        return fields.size();
    }

    public int addField(Field field) {
        return fields.add(field) ? fields.indexOf(field) : -1;
    }

    public boolean removeField(Field field) {
        return fields.remove(field);
    }

    public Field removeField(int index) {
        return ((index >= 0) && (index < getFieldsCount())) ? fields.remove(index) : null;
    }

    public Field getField(int index) {
        return ((index >= 0) && (index < getFieldsCount())) ? fields.get(index) : null;
    }

    public Field replaceField(Field field, int index) {
        return ((index >= 0) && (index < getFieldsCount())) ? fields.set(index, field) : null;
    }

    public void applyChanges () {
        for (Field f : fields) {
        	f.apply();
        }
    }

    public void cancelChanges () {
        for (Field f : fields) {
            f.cancel();
        }
    }
}
