/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.dict.SourcesDeals;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.shared.dict.ExtCodeNameWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;

/**
 *
 * @author Andrew Samsonov
 */
public class SourcesDealsDlg extends EditableDialog<ExtCodeNameWrapper> {

  public final static String EDIT = "Редактирование источника сделки";
  public final static String CREATE = "Ввод нового источника сделки";
  public final static String DELETE = "Удаление источника сделки";

  private TextBox txtShortName;
  private TextArea txtLongName;

  public SourcesDealsDlg(String caption, FormAction action, Columns columns) {
    super(columns, action);
    setCaption(caption);
  }

  @Override
  public Widget createContent() {

    Grid grid = new Grid(4, 2);
    grid.setWidget(0, 0, new HTML("<b>" + new Label("Источник сделки:") + "</b>"));

    grid.setWidget(1, 0, new Label(SourcesDeals.FIELD_CODE));
    grid.setWidget(1, 1, txtCode = createTextBox(7));

    grid.setWidget(2, 0, new Label(SourcesDeals.FIELD_SHORT_NAME));
    grid.setWidget(2, 1, txtShortName = createTextBox(3));

    grid.setWidget(3, 0, new Label(SourcesDeals.FIELD_NAME));
    grid.setWidget(3, 1, txtLongName = createTextBoxArea(255));

    return grid;
  }

  @Override
  protected void fillContent() {
    row = (Row) params;
    int ind = columns.getColumnIndexByCaption(SourcesDeals.FIELD_CODE);
    if (ind >= 0) {
      txtCode.setText((String) row.getField(ind).getValue());
    }
    ind = columns.getColumnIndexByCaption(SourcesDeals.FIELD_SHORT_NAME);
    if (ind >= 0) {
      txtShortName.setText((String) row.getField(ind).getValue());
    }
    ind = columns.getColumnIndexByCaption(SourcesDeals.FIELD_NAME);
    if (ind >= 0) {
      txtLongName.setText((String) row.getField(ind).getValue());
    }
    if (action == FormAction.UPDATE || action == FormAction.DELETE) {
      txtCode.setReadOnly(true);
    }
    if (action == FormAction.DELETE) {
      txtLongName.setReadOnly(true);
      txtShortName.setReadOnly(true);
    }
  }

  @Override
  protected void setFields(ExtCodeNameWrapper cnw) {
    cnw.setCode(checkRequeredString(txtCode.getText(), SourcesDeals.FIELD_CODE));
    cnw.setShortName(checkRequeredString(txtShortName.getText(), SourcesDeals.FIELD_SHORT_NAME));
    cnw.setName(checkRequeredString(txtLongName.getText(), SourcesDeals.FIELD_NAME));
  }

  @Override
  protected ExtCodeNameWrapper createWrapper() {
    return new ExtCodeNameWrapper();
  }

}
