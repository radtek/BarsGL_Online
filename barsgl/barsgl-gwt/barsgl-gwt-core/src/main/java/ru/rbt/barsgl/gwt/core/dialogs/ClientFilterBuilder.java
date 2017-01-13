package ru.rbt.barsgl.gwt.core.dialogs;

import ru.rbt.barsgl.shared.Builder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ivan Sevastyanov on 27.10.2016.
 */
public class ClientFilterBuilder implements Builder<List<FilterItem>>{

    private List<FilterItem> filter = new ArrayList<>();

    public static ClientFilterBuilder create() {
        return new ClientFilterBuilder();
    }

    public ClientFilterBuilder addFilterItem(FilterItem filterItem) {
        filter.add(filterItem);
        return this;
    }

    @Override
    public List<FilterItem> build() {
        return filter;
    }
}
