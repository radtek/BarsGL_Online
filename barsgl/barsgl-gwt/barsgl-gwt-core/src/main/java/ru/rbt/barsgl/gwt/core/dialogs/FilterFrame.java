package ru.rbt.barsgl.gwt.core.dialogs;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.ui.*;
import ru.rbt.barsgl.gwt.core.ui.LongBox;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;


/**
 * Created by akichigi on 01.04.15.
 */
public class FilterFrame  extends Composite {
    private int rows = 0;
    private int id = 0;

    private Columns columns;
    private Grid grid;
    private ValuesBox columnsList;
    private ArrayList<String> frozenColumns;

    public FilterFrame (){
        grid = new Grid(rows, 4);
        columnsList = new ValuesBox();
        frozenColumns = new ArrayList<>();

        initWidget(getFilterFrame());
    }

    private Widget getFilterFrame() {
        columnsList.setWidth("530px");

        HorizontalPanel actionPanel = new HorizontalPanel();
        actionPanel.add(columnsList);
        actionPanel.add(createPlusButton());

        HorizontalPanel topPanel  = new HorizontalPanel();
        topPanel.setWidth("100%");
        topPanel.setStyleName("FrameDecorLine");
        topPanel.add(actionPanel);

        ScrollPanel clientPanel = new ScrollPanel();
        clientPanel.setHeight("250px");
        clientPanel.setWidth("100%");
        clientPanel.add(grid);
        clientPanel.setStyleName("FrameDecorLine");

        VerticalPanel filterFrame = new VerticalPanel();
        filterFrame.add(topPanel);
        filterFrame.add(clientPanel);

        return filterFrame;
    }

    private void fillColumnsList(Columns columns) {
        columnsList.clear();
        for (int i = 0; i < columns.getColumnCount(); i++) {
            Column col = columns.getColumnByIndex(i);
            if (!col.isFilterable() || (frozenColumns.contains(col.getName()))) {
                continue;
            }
            columnsList.addItem(col.getName(), col.getCaption());
        }
    }

    public void fillContent(Columns columns, List<FilterItem> filterConditions) {
        this.columns = columns;
        clearAll();
        frozenColumns.clear();

        if (null != filterConditions) {
            for(FilterItem filterItem: filterConditions) {
                addCondition(filterItem.getName(), filterItem.getCriteria(), filterItem.getValue(), filterItem.isPined(), filterItem.isReadOnly());
                if (filterItem.isReadOnly()) frozenColumns.add(filterItem.getName());
            }
        }

        fillColumnsList(columns);
    };

    private void addCondition(String fieldName, FilterCriteria criteria, Serializable value, boolean pined, boolean isReadOnly) {
        Column column = columns.getColumnByName(fieldName);
        rows++;

        grid.resizeRows(rows);
        grid.setWidget(rows - 1, 0, createMinusButton(pined || isReadOnly));
        grid.setWidget(rows - 1, 1, new Label(column.getCaption()));
        grid.getCellFormatter().setWidth(rows - 1, 1, "230px");
        grid.setWidget(rows - 1, 2, createOperList(column.getType(), criteria, isReadOnly));
        grid.setWidget(rows - 1, 3, createValue(column, value, isReadOnly));
    }

    private PushButton createPlusButton() {
        PushButton plusButton;

        plusButton = new PushButton(new Image(ImageConstants.INSTANCE.plus16()));
        plusButton.setWidth("16px");
        plusButton.setHeight("16px");
        plusButton.setTitle("Добавить критерий");

        plusButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (columnsList.getItemCount() == 0) return;

                FilterFrame.this.addCondition((String) columnsList.getValue(), null, null, false, false);
            }
        });

        return plusButton;
    }

    private PushButton createMinusButton(boolean isReadOnly){
        final PushButton minusButton;

        minusButton = new PushButton(new Image(ImageConstants.INSTANCE.minus16()));
        minusButton.setWidth("16px");
        minusButton.setHeight("16px");
        minusButton.setTitle("Удалить критерий");
        minusButton.getElement().setAttribute("num", ((Integer) id++).toString());
        minusButton.setEnabled(!isReadOnly);

        minusButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                String numAttr = minusButton.getElement().getAttribute("num");
                grid.removeRow(getGridCurrentRowId(numAttr));
                rows--;
            }
        });
       return minusButton;
    }

    private boolean isConditionPined(int row) {
        return !((PushButton)grid.getWidget(row, 0)).isEnabled();
    }

    private boolean isConditionReadOnly(int row) {
        return !((ValuesBox)grid.getWidget(row, 2)).isEnabled();
    }

    private ValuesBox createOperList(Column.Type type, FilterCriteria criteria, boolean isReadOnly){
        LinkedHashMap<Serializable, String> map = new LinkedHashMap<Serializable, String>();
        map.put(FilterCriteria.EQ, FilterCriteria.EQ.getValue());
        map.put(FilterCriteria.NE, FilterCriteria.NE.getValue());
        map.put(FilterCriteria.IS_NULL, FilterCriteria.IS_NULL.getValue());
        map.put(FilterCriteria.NOT_NULL, FilterCriteria.NOT_NULL.getValue());

        switch(type){
            case STRING:
                map.put(FilterCriteria.HAVE, FilterCriteria.HAVE.getValue());
                map.put(FilterCriteria.START_WITH, FilterCriteria.START_WITH.getValue());
                map.put(FilterCriteria.LIKE, FilterCriteria.LIKE.getValue());
                map.put(FilterCriteria.IS_EMPTY, FilterCriteria.IS_EMPTY.getValue());
                map.put(FilterCriteria.NOT_EMPTY, FilterCriteria.NOT_EMPTY.getValue());
                break;
            case BOOLEAN:
                break;
            default:
                map.put(FilterCriteria.GT, FilterCriteria.GT.getValue());
                map.put(FilterCriteria.GE, FilterCriteria.GE.getValue());
                map.put(FilterCriteria.LT, FilterCriteria.LT.getValue());
                map.put(FilterCriteria.LE, FilterCriteria.LE.getValue());
        }

        ValuesBox operList = new ValuesBox(map);
        if (null != criteria ) operList.setValue(criteria);
        operList.setWidth("120px");
        operList.setEnabled(!isReadOnly);
        return operList;
    }

    private Widget createValue(Column column, Serializable value, boolean isReadOnly){
        Widget box = makeListWidget(column, value);
        if (box != null) {
            ((IBoxValue)box).setEnabled(!isReadOnly);
            return box;
        }

        Column.Type type = column.getType();

        switch(type){
            case DATE:
            case DATETIME:
            	box = new DatePickerBox(null);
                break;
            case BOOLEAN:
                box = new BooleanBox();
                break;
            case DECIMAL:
                box = new DecBox((BigDecimal)value);
                break;
            case INTEGER:
            	 box = new IntBox((Integer)value);
                 break;
            case LONG:
                box = new LongBox((Long)value);
                break;
            default:
                box = new TxtBox((String)value);
        }
        box.setWidth("150px");
        if (null != value) ((IBoxValue)box).setValue(value);
        ((IBoxValue)box).setEnabled(!isReadOnly);
        return box;
    }

    private ValuesBox makeListWidget(Column column, Serializable value){
       HashMap<Serializable, String> list = column.getList();
        if (list == null || list.isEmpty()) return null;

        ValuesBox valBox = new ValuesBox(list);
        valBox.setWidth("150px");

        if (null != value) valBox.setValue(value);

        return valBox;
    }

    private int getGridCurrentRowId(String num){
        for (int i=0 ; i < grid.getRowCount(); i++){
            if (grid.getWidget(i, 0).getElement().getAttribute("num").equalsIgnoreCase(num))
                return i;
        }
        return -1;
    }

    private void clearAll(){
        while(grid.getRowCount()> 0)
            grid.removeRow(0);

        rows = 0;
    }

    public void clearAllFilterCriteria(){
        int i = 0;
        while ( i < grid.getRowCount() ) {
            if (!isConditionPined(i))
                grid.removeRow(i);
            else
                i++;
        }
        rows = i;
    }

    public ArrayList<FilterItem> getFilterConditions(){
        ArrayList<FilterItem> list = new ArrayList<FilterItem>();

        for (int i=0 ; i < grid.getRowCount(); i++){
            String text = ((Label)grid.getWidget(i, 1)).getText();
            Column col = columns.getColumnByCaption(text);
//            String colName = columns.getColumnByCaption(text).getName();

            FilterCriteria criteria = (FilterCriteria) ((IBoxValue<?>)grid.getWidget(i, 2)).getValue();

            Serializable value = (Serializable)((IBoxValue<?>)grid.getWidget(i, 3)).getValue();

            FilterItem item = new FilterItem(col, criteria, value, isConditionPined(i));
            item.setReadOnly(isConditionReadOnly(i));
            list.add(item);
        }
        return list.isEmpty() ? null : list;
    }
}
