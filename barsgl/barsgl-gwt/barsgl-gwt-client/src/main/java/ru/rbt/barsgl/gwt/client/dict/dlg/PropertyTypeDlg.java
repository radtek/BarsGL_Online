/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.client.dict.PropertyType;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.ui.ValuesBox;
import ru.rbt.barsgl.gwt.core.utils.AppFunction;
import ru.rbt.barsgl.gwt.core.utils.AppPredicate;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.dict.GroupType;
import ru.rbt.barsgl.shared.dict.PropertyTypeWrapper;
import ru.rbt.barsgl.shared.dict.ResidentType;

import java.io.Serializable;
import java.util.HashMap;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.check;

/**
 *
 * @author Andrew Samsonov
 */
public class PropertyTypeDlg extends EditableDialog<PropertyTypeWrapper> {

  public final static String EDIT = "Редактирование типа собственности";
  public final static String CREATE = "Ввод нового типа собственности";
  public final static String DELETE = "Удаление типа собственности";

  private TextBox txtName;

  private ValuesBox txtGroup;
  private ValuesBox txtResidentType;

  public PropertyTypeDlg(String caption, FormAction action, Columns columns) {
    super(columns, action);
    setCaption(caption);
  }

  @Override
  public Widget createContent() {
    Grid grid = new Grid(5, 2);
    grid.setWidget(0, 0, new HTML("<b>" + new Label("Тип собственности:") + "</b>"));

    grid.setWidget(1, 0, new Label(PropertyType.FIELD_CODE));
    grid.setWidget(1, 1, txtCode = createTextBox(5));
    txtCode.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(KeyPressEvent event) {
        char charCode = event.getCharCode();
        if (!Character.isDigit(charCode)) {
          ((TextBox) event.getSource()).cancelKey();
        }
      }
    });

    grid.setWidget(2, 0, new Label(PropertyType.FIELD_NAME));
    grid.setWidget(2, 1, txtName = createTextBox(30));

    grid.setWidget(3, 0, new Label(PropertyType.FIELD_GROUP));
    grid.setWidget(3, 1, txtGroup = new ValuesBox());
    for (GroupType grp : GroupType.values()) {
      txtGroup.addItem(grp.name(), grp.name());
    }

    grid.setWidget(4, 0, new Label(PropertyType.FIELD_RESIDENT_TYPE));
    grid.setWidget(4, 1, txtResidentType = new ValuesBox());
    for (ResidentType rsdt : ResidentType.values()) {
      txtResidentType.addItem(rsdt.name(), rsdt.name());
    }

    return grid;
  }

  protected HashMap<Serializable, String> getListForValuesBox(String caption) {
    Column column = columns.getColumnByCaption(caption);
    if (column != null && column.getList() != null) {
      return column.getList();    
    }else
      return null;
  }
  
  @Override
  protected void fillContent() {
    row = (Row) params;
    int ind;
    ind = columns.getColumnIndexByCaption(PropertyType.FIELD_CODE);
    if (ind >= 0) {
      txtCode.setValue(((Integer) row.getField(ind).getValue()).toString());
    }
    ind = columns.getColumnIndexByCaption(PropertyType.FIELD_NAME);
    if (ind >= 0) {
      txtName.setText((String) row.getField(ind).getValue());
    }
    ind = columns.getColumnIndexByCaption(PropertyType.FIELD_GROUP);
    if (ind >= 0) {
      txtGroup.setText((String) row.getField(ind).getValue());
    }
    ind = columns.getColumnIndexByCaption(PropertyType.FIELD_RESIDENT_TYPE);
    if (ind >= 0) {
      txtResidentType.setText((String) row.getField(ind).getValue());
    }

    if (action == FormAction.UPDATE || action == FormAction.DELETE) {
      txtCode.setReadOnly(true);
    }
    if (action == FormAction.DELETE) {
      txtName.setReadOnly(true);
      txtGroup.setEnabled(false);
      txtResidentType.setEnabled(false);
    }
  }

  @Override
  protected PropertyTypeWrapper createWrapper() {
    return new PropertyTypeWrapper();
  }

  @Override
  protected void setFields(PropertyTypeWrapper ptw) {
    ptw.setCode(check(txtCode.getValue(), PropertyType.FIELD_CODE, REQUIRED,
            new AppPredicate<String>() {
      @Override
      public boolean check(String target) {
        try {
          if (null != target && !target.trim().isEmpty()) {
            final Short interm = Short.decode(target);
            return true;
          } else {
            return false;
          }
        } catch (NumberFormatException e) {
          return false;
        }
      }
    },
            new AppFunction<String, Short>() {
      @Override
      public Short apply(String from) {
        return Short.decode(from);
      }
    }
    ));
    ptw.setName(checkRequeredString(txtName.getText(), PropertyType.FIELD_NAME));
    ptw.setGroup(checkRequeredString(txtGroup.getText(), PropertyType.FIELD_GROUP));
    ptw.setResidentType(checkRequeredString(txtResidentType.getText(), PropertyType.FIELD_RESIDENT_TYPE));
  }

}
