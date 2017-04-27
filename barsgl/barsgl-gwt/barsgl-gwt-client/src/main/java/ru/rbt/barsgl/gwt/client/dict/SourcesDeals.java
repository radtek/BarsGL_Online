/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.gwt.client.dict;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.dict.dlg.SourcesDealsDlg;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.ExtCodeNameWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.shared.enums.SecurityActionCode;

import java.util.ArrayList;

/**
 *
 * @author Andrew Samsonov
 */
public class SourcesDeals extends EditableDictionary<ExtCodeNameWrapper> {

  public final static String FORM_NAME = "Источники сделок";

  public final static String FIELD_CODE = "Код";
  public final static String FIELD_SHORT_NAME = "Краткий код";
  public final static String FIELD_NAME = "Наименование";

  public SourcesDeals() {
    super(FORM_NAME);
    reconfigure();
  }

    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("ID_SRC", Column.Sort.ASC));
        return list;
    }

    private void reconfigure() {
    abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
    abw.addSecureAction(editAction(new SourcesDealsDlg(SourcesDealsDlg.EDIT, FormAction.UPDATE, grid.getTable().getColumns()),
            "Источник сделок не сохранен.\n Ошибка: ",
            "Ошибка при изменении источника сделок: \n",
            "Источник сделок изменен успешно: "), SecurityActionCode.ReferChng);
    abw.addSecureAction(createAction(new SourcesDealsDlg(SourcesDealsDlg.CREATE, FormAction.CREATE, grid.getTable().getColumns()),
            "Источник сделок не создан.\n Ошибка: ",
            "Ошибка создания источника сделок: \n",
            "Источник сделок создан успешно: "), SecurityActionCode.ReferChng);
    abw.addSecureAction(deleteAction(new SourcesDealsDlg(SourcesDealsDlg.DELETE, FormAction.DELETE, grid.getTable().getColumns()),
            "Источник сделок не удален.\n Ошибка: ",
            "Ошибка удаления источника сделок: \n",
            "Источник сделок удален успешно: "), SecurityActionCode.ReferDel);
  }

  @Override
  protected Table prepareTable() {
    Table result = new Table();

    result.addColumn(new Column("ID_SRC", Column.Type.STRING, FIELD_CODE, 40, true, false, Column.Sort.ASC, ""));
    result.addColumn(new Column("SHNM", Column.Type.STRING, FIELD_SHORT_NAME, 10));
    result.addColumn(new Column("LGNM", Column.Type.STRING, FIELD_NAME, 80));
    return result;
  }

  @Override
  protected String prepareSql() {
    return "select ID_SRC, SHNM, LGNM from GL_SRCPST";
  }

  @Override
  protected void save(ExtCodeNameWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<ExtCodeNameWrapper>> asyncCallbackImpl) throws Exception {
    BarsGLEntryPoint.dictionaryService.saveSourcesDeals(cnw, action, asyncCallbackImpl);
  }

}
