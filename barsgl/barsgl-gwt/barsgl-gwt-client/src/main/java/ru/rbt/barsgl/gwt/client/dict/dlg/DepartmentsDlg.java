/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.dict.Departments;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.shared.dict.CodeNameWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;

/**
 *
 * @author Andrew Samsonov
 */
public class DepartmentsDlg extends EditableDialog<CodeNameWrapper> {

  public final static String EDIT = "Редактирование подразделения";
  public final static String CREATE = "Ввод нового подразделения";
  public final static String DELETE = "Удаление подразделения";
    
  private TextBox txtName;

  public DepartmentsDlg(String caption, FormAction action, Columns columns) {
    super(columns, action);
    setCaption(caption);
  }

  @Override
  public Widget createContent() {

    Grid grid = new Grid(3, 2);
    grid.setWidget(0, 0, new HTML("<b>" + new Label("Подразделение:") + "</b>"));

    grid.setWidget(1, 0, new Label(Departments.FIELD_CODE));

    grid.setWidget(1, 1, txtCode = createTextBox(3));
    
    grid.setWidget(2, 0, new Label(Departments.FIELD_NAME));
    grid.setWidget(2, 1, txtName = createTextBox(30));

    return grid;
  }

  @Override
  protected void fillContent() {
    row = (Row) params;
    int ind = columns.getColumnIndexByCaption(Departments.FIELD_CODE);
    if (ind >= 0) {
      txtCode.setText((String) row.getField(ind).getValue());
    }
    ind = columns.getColumnIndexByCaption(Departments.FIELD_NAME);
    if (ind >= 0) {
      txtName.setText((String) row.getField(ind).getValue());
    }
    if (action == FormAction.UPDATE || action == FormAction.DELETE) {
      txtCode.setReadOnly(true);
    }
    if (action == FormAction.DELETE) {
      txtName.setReadOnly(true);
    }
  }
  
  @Override
  protected void setFields(CodeNameWrapper cnw) {
    cnw.setCode(checkRequeredString(txtCode.getText(), Departments.FIELD_CODE));
    cnw.setName(checkRequeredString(txtName.getText(), Departments.FIELD_NAME));
  }

  @Override
  protected CodeNameWrapper createWrapper() {
    return new CodeNameWrapper();
  }

}
