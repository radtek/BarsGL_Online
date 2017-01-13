package ru.rbt.barsgl.gwt.core.widgets;

import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;

import java.util.ArrayList;

/**
 * Created by akichigi on 28.05.15.
 */
public interface ILinkFilterCriteria {
    public ArrayList<FilterItem> createLinkFilterCriteria(Row row);
}
