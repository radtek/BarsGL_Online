package ru.rbt.barsgl.gwt.core.actions;

import java.io.Serializable;
import java.util.HashMap;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.EditDlg;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.ui.IBoxValue;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;

/**
 * Created by akichigi on 08.05.15.
 */
public class SimpleDlgAction extends GridAction{
	protected EditDlg dlg;
	protected GridWidget grid;
	protected DlgMode mode;
	
	public SimpleDlgAction(GridWidget grid, DlgMode mode, double separator){
		 super(grid, null, mode.getValue(), mode == DlgMode.NEW ? new Image(ImageConstants.INSTANCE.new24()):
			  (mode == DlgMode.EDIT ? new Image(ImageConstants.INSTANCE.edit24()) 
			                        : new Image(ImageConstants.INSTANCE.properties())), separator,
		       mode != DlgMode.NEW);
		
		this.grid = grid;
		this.mode = mode;
	}
		
	@Override
	public void execute() {
		Row row = grid.getCurrentRow();
		if (row == null) return;
		//if (dlg == null) 
		dlg = new EditDlg(grid.getTable().getColumns(), mode, row);

		dlg.setDlgEvents(this);
		dlg.show(row);
	}
	
	 @Override
	    public void onDlgOkClick(Object prms){
		 Window.alert("Validating...");
		 
		 HashMap<String, Widget> controls = (HashMap<String, Widget>) prms;
		  for (Widget w : controls.values()){
			  Serializable value = ((IBoxValue<Serializable>)w).getValue();
			  System.out.println(value);
		  }
		  		 				 
	        dlg.hide();
	     // maybe fire event
	     //LocalEventBus.fireEvent(???));
	    }
}
