/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.gwt.client.dict;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.dict.GroupType;
import ru.rbt.barsgl.shared.dict.PropertyTypeWrapper;
import ru.rbt.barsgl.shared.dict.ResidentType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Andrew Samsonov
 */
public class PropertyType extends EditableDictionary<PropertyTypeWrapper> {

  public final static String FORM_NAME = "Типы собственности";

  public final static String FIELD_CODE = "Код";
  public final static String FIELD_NAME = "Наименование";
  public final static String FIELD_GROUP = "Принадлежность";
  public final static String FIELD_RESIDENT_TYPE = "Резидентность";

  public PropertyType() {
    super(FORM_NAME);
    reconfigure();
  }

  @Override
  public ArrayList<SortItem> getInitialSortCriteria() {
    ArrayList<SortItem> list = new ArrayList<SortItem>();
    list.add(new SortItem("CTYPE", Column.Sort.ASC));
    return list;
  }

  @Override
  protected Table prepareTable() {
    Table result = new Table();
    Column col;

    result.addColumn(new Column("CTYPE", Column.Type.INTEGER, FIELD_CODE, 10, true, false, Column.Sort.ASC, ""));
    result.addColumn(new Column("NAME", Column.Type.STRING, FIELD_NAME, 80));
    result.addColumn(col = new Column("PRCD", Column.Type.STRING, FIELD_GROUP, 10));
    col.setList(getGroupMap());
    result.addColumn(col = new Column("RECD", Column.Type.STRING, FIELD_RESIDENT_TYPE, 10));
    col.setList(getResidentTypeList());
    
    return result;
  }

  @Override
  protected String prepareSql() {
    return "select CTYPE, NAME, PRCD, RECD from CBCTP";
  }

  private void reconfigure() {
    abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
    /* disable create, update, delete 
    abw.addAction(editAction(new PropertyTypeDlg(PropertyTypeDlg.EDIT, FormAction.UPDATE, grid.getTable().getColumns()),
            "Тип собственности не сохранен.\n Ошибка: ",
            "Ошибка при изменении типа собственности: \n",
            "Тип собственности изменен успешно: "));    
    abw.addAction(createAction(new PropertyTypeDlg(PropertyTypeDlg.CREATE, FormAction.CREATE, grid.getTable().getColumns()),
            "Тип собственности не создан.\n Ошибка: ",
            "Ошибка создания типа собственности: \n",
            "Тип собственности создан успешно: "));
    abw.addAction(deleteAction(new PropertyTypeDlg(PropertyTypeDlg.DELETE, FormAction.DELETE, grid.getTable().getColumns()),
            "Тип собственности не удален.\n Ошибка: ",
            "Ошибка удаления типа собственности: \n",
            "Тип собственности удален успешно: "));
    */
  }

  @Override
  protected void save(PropertyTypeWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<PropertyTypeWrapper>> asyncCallbackImpl) throws Exception {
    BarsGLEntryPoint.dictionaryService.savePropertyType(cnw, action, asyncCallbackImpl);
  }
  
  private HashMap<Serializable, String> getGroupMap() {
    HashMap<Serializable, String> map = new HashMap<>();

    for (GroupType grp : GroupType.values()) {
      map.put(grp.name(), grp.name());
    }
    
    return map;
  }

  private HashMap<Serializable, String> getResidentTypeList() {
    HashMap<Serializable, String> map = new HashMap<>();

    for (ResidentType rsdt : ResidentType.values()) {
      map.put(rsdt.name(), rsdt.name());
    }
    
    return map;
  }
}
