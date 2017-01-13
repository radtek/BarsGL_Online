package ru.rbt.barsgl.gwt.client.comp.enm;

import ru.rbt.barsgl.gwt.client.comp.DataListBox;
import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by Ivan Sevastyanov on 20.01.2016.
 */
public class EnumListBox<T extends Enum & HasLabel> extends DataListBox {

    public EnumListBox(T[] list) {
        super(new EnumListBoxDataProvider<T>(list));
    }

    @Override
    public T getValue() {
        return (T) super.getValue();
    }

    public void setValue(T value) {
        super.setValue(value);
    }
}
