/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.dict.TypesOfTerms;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.shared.dict.CodeNameWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;

/**
 *
 * @author Andrew Samsonov
 */
public class TypesOfTermsDlg extends EditableDialog<CodeNameWrapper> {

  public final static String EDIT = "Редактирование кода срока";
  public final static String CREATE = "Ввод нового кода срока";
  public final static String DELETE = "Удаление кода срока";

  private TextArea txtLongName;

  public TypesOfTermsDlg(String caption, FormAction action, Columns columns) {
    super(columns, action);
    setCaption(caption);
  }

  @Override
  public Widget createContent() {

    Grid grid = new Grid(3, 2);
    grid.setWidget(0, 0, new HTML("<b>" + new Label("Код срока:") + "</b>"));

    grid.setWidget(1, 0, new Label(TypesOfTerms.FIELD_CODE));

    grid.setWidget(1, 1, txtCode = createTextBox(3));

    grid.setWidget(2, 0, new Label(TypesOfTerms.FIELD_NAME));
    grid.setWidget(2, 1, txtLongName = createTextBoxArea(128));

    return grid; //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  protected void fillContent() {
    row = (Row) params;
    int ind = columns.getColumnIndexByCaption(TypesOfTerms.FIELD_CODE);
    if (ind >= 0) {
      txtCode.setText((String) row.getField(ind).getValue());
    }
    ind = columns.getColumnIndexByCaption(TypesOfTerms.FIELD_NAME);
    if (ind >= 0) {
      txtLongName.setText((String) row.getField(ind).getValue());
    }
    if (action == FormAction.UPDATE || action == FormAction.DELETE) {
      txtCode.setReadOnly(true);
    }
    if (action == FormAction.DELETE) {
      txtLongName.setReadOnly(true);
    }
  }

  @Override
  protected void setFields(CodeNameWrapper cnw) {
    cnw.setCode(checkRequeredString(txtCode.getText(), TypesOfTerms.FIELD_CODE));
    cnw.setName(checkRequeredString(txtLongName.getText(), TypesOfTerms.FIELD_NAME));
  }

  @Override
  protected CodeNameWrapper createWrapper() {
    return new CodeNameWrapper();
  }
}
