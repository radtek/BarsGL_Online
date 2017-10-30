package ru.rbt.barsgl.gwt.core.dialogs;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Field;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.ui.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;

import static ru.rbt.barsgl.gwt.core.datafields.Column.Type;
import static ru.rbt.barsgl.shared.ClientDateUtils.TZ_CLIENT;

public class EditDlg extends DlgFrame{
	private static String valueWidth = "350px";
	private DlgMode mode;
	private Row row;
    private Columns columns;
    private HashMap<String, Widget> controls;
	
    private Grid grid;
    
	public EditDlg(Columns columns, DlgMode mode, Row row){
		super();
        setCaption(mode.getValue());  

		ok.setVisible(mode != DlgMode.BROWSE);

        this.mode = mode; 
        this.columns = columns;
        this.row = row;
        controls = new HashMap<String, Widget>();
        
        setContent(createDlgContent());        
	}
		
	
	private Widget createDlgContent() {		
		ScrollPanel clientPanel = new ScrollPanel();
		clientPanel.setHeight("350px");		
		clientPanel.setWidth("100%");
		
		clientPanel.add(grid = populateGrid());					
		clientPanel.setStyleName("FrameDecorLine");
		
		return clientPanel;
	}
	
	
	private Grid populateGrid(){
		grid = new Grid(columns.getColumnCount(), 2);
		int c = 0;
		for (int i = 0; i < columns.getColumnCount(); i++) {
			Column col = columns.getColumnByIndex(i);
			if (col.isEditable()){
				grid.setText(c, 0, col.isNullable() ? col.getCaption() : col.getCaption() + "*");
				Widget widget = getControl(col, row.getField(i).getValue());
				grid.setWidget(c, 1, widget);
				controls.put(col.getName(), widget);

				c++;
			}
		}
		
		CellFormatter fmt = grid.getCellFormatter(); 
        fmt.setWidth(0, 0, "150px");
        fmt.setWidth(0, 1, "330px");
		
		return grid;
	}
	
	private Widget getControl(Column column, Serializable value){
		IBoxValue box = makeValuesBoxWidget(column);
		if (box != null) return (Widget)box;

		Type type = column.getType();
        boolean readOnly = (mode == DlgMode.BROWSE) || (column.isReadonly());  
		int maxLen = column.getMaxLength();
		if (null != value) 
			maxLen = Math.max(maxLen, value.toString().length());

		box = getControl(type, maxLen, column.isMultiLine());

		box.setReadOnly(readOnly);
		box.setWidth(valueWidth);

		return (Widget)box;
	}

	private IBoxValue getControl(Type type, int maxLen, boolean isMultiLine){
		switch(type){
			case DATE:
				return new DatePickerBox(null, TZ_CLIENT);

			case DATETIME:
				return new TxtBox(null);

			case BOOLEAN:
				return new BooleanBox();

			case DECIMAL:
				DecBox dec = new DecBox(null);
				if (maxLen > 0) dec.setMaxLength(maxLen);
				return dec;

			case INTEGER:
				IntBox ib = new IntBox(null);
				if (maxLen > 0)ib.setMaxLength(maxLen);
				return ib;

			case LONG:
				LongBox lb = new LongBox(null);
				if (maxLen > 0) lb.setMaxLength(maxLen);
				return lb;

			default:
				if (isMultiLine || maxLen > 50){
					AreaBox ab = new AreaBox(null);
					int height = maxLen / 2;
					ab.setHeight(height + "px");
					ab.setTextWrap(true);
					return ab;
				} else{
					TxtBox tb = new TxtBox(null);
					if (maxLen > 0) tb.setMaxLength(maxLen);
					return tb;
				}
		}
	}

	private ValuesBox makeValuesBoxWidget(Column column){
		HashMap<Serializable, String> list = column.getList();
		if (list == null) return null;

		ValuesBox valBox = new ValuesBox(list);
		boolean readOnly = (mode == DlgMode.BROWSE) || (column.isReadonly()); 
		valBox.setEnabled(!list.isEmpty() && !readOnly);

		valBox.setWidth(valueWidth);

		return valBox;
	}
	
	@Override
	protected void fillContent(){	    	 
		row = (Row) params;
		populateRows();		
	}

	private void populateRows(){
		for (int i = 0; i < columns.getColumnCount(); i++) {
			Column col = columns.getColumnByIndex(i);
			if (col.isEditable()){
				IBoxValue<Serializable> widget = (IBoxValue<Serializable>)controls.get(col.getName());
			    Serializable value = mode == DlgMode.NEW 
			    		? getDefValue(col)
			            : row.getField(i).getValue();
			    try {
				    if (null == value) {
				    	widget.setValue(null);
				    }
				    else {
						switch (col.getType()) {
							case CHAR:
							case STRING:
							case DATETIME:	// строка
								if (col.getFormat() != null && !col.getFormat().isEmpty()){
									widget.setValue(DateTimeFormat.getFormat(col.getFormat()).format((Date) value));
								}else
								    widget.setValue(value.toString());
								break;
							case DATE:
								Date valueDate = DatePickerBox.DATE_FORMAT.parse(DatePickerBox.DATE_FORMAT.format((Date)value, TZ_CLIENT));
								widget.setValue(valueDate);
								break;
              case LONG:
                if(value instanceof BigDecimal){
    							widget.setValue(((BigDecimal)value).longValue());                                  
                }else
    							widget.setValue(value);                  
                break;
              case INTEGER:
                if(value instanceof BigDecimal){
    							widget.setValue(((BigDecimal)value).intValue());                                                    
                }else
  								widget.setValue(value);                  
                break;
							default:		// значение
								widget.setValue(value);
								break;
						}
					}
			    } catch (Exception e){
			    	Window.alert("Неверный тип столбца '" + col.getName() + "':\n" + e.getMessage());
			    }
			}
		}
	}

	private void  format(){

	}

	private Serializable getDefValue(Column col){
	    Field<Serializable> field = col.getDefValue();
	
	    if (field == null) return null;
	    return field.getValue();	 	   
	}
	
	@Override
	public boolean onClickOK() throws Exception {
		 params = controls;
		return true;
	}
}
