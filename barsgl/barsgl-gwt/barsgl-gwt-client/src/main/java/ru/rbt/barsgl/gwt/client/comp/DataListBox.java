package ru.rbt.barsgl.gwt.client.comp;

import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.events.DataListBoxEvent;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.ui.ValuesBox;
import ru.rbt.barsgl.gwt.core.utils.UUID;

import java.io.Serializable;

/**
 * Created by Ivan Sevastyanov
 */
public class DataListBox extends ValuesBox implements IDataListBox {
	
    protected final ListBoxDataProvider provider;
    private String id;
    
    public DataListBox(ListBoxDataProvider provider) {
        super();
        id = UUID.randomUUID();
        init();
        this.provider = provider;
        provider.provide(this);
    }

    protected void init() {};

    public void setSelectValue(String value) {
        provider.setSelectValue(value);       
        setValue(value);
    }

    @Override
    public void addItem(Serializable key, String value, Row row) {
        super.addItem(key, value);
    }

    public String getId() {
		return id;
	}

	public void sendCompleteMessage(){
        LocalEventBus.fireEvent(new DataListBoxEvent(id));
    }

}
