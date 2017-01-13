/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.gwt.client.dict;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.dict.dlg.TypesOfTermsDlg;
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
public class TypesOfTerms extends EditableDictionary<CodeNameWrapper> {
    public final static String FORM_NAME = "Коды сроков";

    public final static String FIELD_CODE = "Код";
    public final static String FIELD_NAME = "Наименование";

    public TypesOfTerms(boolean isLookup) {
        super(FORM_NAME);
        reconfigure(isLookup);
    }


    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("TERM", Column.Sort.ASC));
        return list;
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();

        result.addColumn(new Column("TERM", Column.Type.STRING, FIELD_CODE, 10, true, false, Column.Sort.ASC, ""));
        result.addColumn(new Column("TERMNAME", Column.Type.STRING, FIELD_NAME, 80));
        return result;
    }

    @Override
    protected String prepareSql() {
        return "select TERM, TERMNAME from GL_DICTERM";
    }

    private void reconfigure(boolean isLookup) {
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        if (isLookup) return;
        abw.addSecureAction(editAction(new TypesOfTermsDlg(TypesOfTermsDlg.EDIT, FormAction.UPDATE, grid.getTable().getColumns()),
                "Код срока не сохранен.\n Ошибка: ",
                "Ошибка при изменении кода срока: \n",
                "Код срока изменен успешно: "), SecurityActionCode.ReferChng);
        abw.addSecureAction(createAction(new TypesOfTermsDlg(TypesOfTermsDlg.CREATE, FormAction.CREATE, grid.getTable().getColumns()),
                "Код срока не создан.\n Ошибка: ",
                "Ошибка создания кода срока: \n",
                "Код срока создан успешно: "), SecurityActionCode.ReferChng);
        abw.addSecureAction(deleteAction(new TypesOfTermsDlg(TypesOfTermsDlg.DELETE, FormAction.DELETE, grid.getTable().getColumns()),
                "Код срока не удален.\n Ошибка: ",
                "Ошибка удаления кода срока: \n",
                "Код срока удален успешно: "), SecurityActionCode.ReferDel);
    }

    @Override
    protected void save(CodeNameWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<CodeNameWrapper>> asyncCallbackImpl) throws Exception {
        BarsGLEntryPoint.dictionaryService.saveTypesOfTerms(cnw, action, asyncCallbackImpl);
    }
}
