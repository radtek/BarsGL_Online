package ru.rbt.barsgl.gwt.core.widgets;

import ru.rbt.barsgl.gwt.core.datafields.Column;

import java.io.Serializable;

/**
 * Created by akichigi on 21.04.15.
 */
public class SortItem implements Serializable {
    private static final long serialVersionUID = -222222225561414413L;

    private String name;
    private Column.Sort type;

    public SortItem(){}

    public SortItem(String name, Column.Sort type){
        this.name = name;
        this.type = type;
    }

    public String getName(){
        return name;
    }

    public Column.Sort getType(){
        return type;
    }
}
