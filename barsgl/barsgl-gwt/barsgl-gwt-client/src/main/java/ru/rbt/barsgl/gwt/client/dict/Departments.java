/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.gwt.client.dict;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.dict.dlg.DepartmentsDlg;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.CodeNameWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.SecurityActionCode;

import java.util.ArrayList;

/**
 *
 * @author Andrew Samsonov
 */
public class Departments extends EditableDictionary<CodeNameWrapper> {

  public final static String FORM_NAME = "Подразделения";

  public final static String FIELD_CODE = "Код";
  public final static String FIELD_NAME = "Наименование";

  public Departments() {
    super(FORM_NAME);
    reconfigure();
  }

    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("DEPID", Column.Sort.ASC));
        return list;
    }

    @Override
  protected Table prepareTable() {
    Table result = new Table();
    Column col;

    result.addColumn(new Column("DEPID", Column.Type.STRING, FIELD_CODE, 10, true, false, Column.Sort.ASC, ""));
    result.addColumn(new Column("DEPNAME", Column.Type.STRING, FIELD_NAME, 80));
    return result;
  }

  @Override
  protected String prepareSql() {
    return "select DEPID, DEPNAME from GL_DEPT";
  }

  private void reconfigure() {
    abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
    abw.addSecureAction(editAction(new DepartmentsDlg(DepartmentsDlg.EDIT, FormAction.UPDATE, grid.getTable().getColumns()),
            "Подразделение не сохранено.\n Ошибка: ",
            "Ошибка при изменении подразделения: \n",
            "Подразделение изменено успешно: "), SecurityActionCode.ReferChng);
    abw.addSecureAction(createAction(new DepartmentsDlg(DepartmentsDlg.CREATE, FormAction.CREATE, grid.getTable().getColumns()),
            "Подразделение не создано.\n Ошибка: ",
            "Ошибка создания подразделения: \n",
            "Подразделение создано успешно: "), SecurityActionCode.ReferChng);
    abw.addSecureAction(deleteAction(new DepartmentsDlg(DepartmentsDlg.DELETE, FormAction.DELETE, grid.getTable().getColumns()),
            "Подразделение не удалено.\n Ошибка: ",
            "Ошибка удаления подразделения: \n",
            "Подразделение удалено успешно: "), SecurityActionCode.ReferDel);
  }

  @Override
  protected void save(CodeNameWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<CodeNameWrapper>> asyncCallbackImpl) throws Exception {
    BarsGLEntryPoint.dictionaryService.saveDepartment(cnw, action, asyncCallbackImpl);
  }

}
