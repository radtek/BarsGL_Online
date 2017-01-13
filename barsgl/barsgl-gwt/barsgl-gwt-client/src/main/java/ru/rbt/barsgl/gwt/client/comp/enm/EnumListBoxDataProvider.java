package ru.rbt.barsgl.gwt.client.comp.enm;

import ru.rbt.barsgl.gwt.client.comp.DataListBox;
import ru.rbt.barsgl.gwt.client.comp.ListBoxDataProvider;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by Ivan Sevastyanov on 20.01.2016.
 */
public class EnumListBoxDataProvider<T extends Enum & HasLabel> implements ListBoxDataProvider {

    private final T[] items;

    public EnumListBoxDataProvider(T[] items) {
        this.items = items;
    }

    @Override
    public void provide(DataListBox listBox) {
        if (null != items) {
            for (T item : items) {
                listBox.addItem(item, item.getLabel());
            }
        }
    }

	@Override
	public void setSelectValue(String selectValue) {
		// TODO Auto-generated method stub

	}

    @Override
    public Columns getColumns() {
        return null;
    }
}
