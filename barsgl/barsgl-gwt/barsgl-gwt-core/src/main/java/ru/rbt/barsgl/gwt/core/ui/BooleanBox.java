package ru.rbt.barsgl.gwt.core.ui;

/**
 * Created by ER18837 on 19.01.16.
 */
public class BooleanBox extends ValuesBox {

    public BooleanBox(String valEmpty, String valTrue, String valFalse) {
        super();
        if (null != valEmpty) addItem(null, valEmpty);
        addItem(true, valTrue);
        addItem(false, valFalse);
    }

    public BooleanBox(String valTrue, String valFalse) {
        this(null, valTrue, valFalse);
    }

    public BooleanBox() {
        this("", "Истина", "Ложь");
    }
}
