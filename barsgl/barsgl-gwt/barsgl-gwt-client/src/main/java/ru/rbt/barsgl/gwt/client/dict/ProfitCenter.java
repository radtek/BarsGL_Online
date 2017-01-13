package ru.rbt.barsgl.gwt.client.dict;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.dict.dlg.ProfitCenterDlg;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.dict.ProfitCenterWrapper;
import ru.rbt.barsgl.shared.enums.SecurityActionCode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getYesNoList;

/**
 * Created by akichigi on 03.08.16.
 */
public class ProfitCenter extends EditableDictionary<ProfitCenterWrapper>{
    public final static String FORM_NAME = "Профит центр";

    public final static String FIELD_CODE = "Код";
    public final static String FIELD_NAME = "Наименование";
    public final static String FIELD_CLOSED = "Закрыт";

    public ProfitCenter() {
        super(FORM_NAME);
        reconfigure();
    }

    private void reconfigure() {
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addSecureAction(editAction(new ProfitCenterDlg(ProfitCenterDlg.EDIT, FormAction.UPDATE, grid.getTable().getColumns()),
                "Профит центр не сохранен",
                "Ошибка при изменении профит центра",
                "Профит центр изменен успешно"), SecurityActionCode.ReferChng);
        abw.addSecureAction(createAction(new ProfitCenterDlg(ProfitCenterDlg.CREATE, FormAction.CREATE, grid.getTable().getColumns()),
                "Профит центр не создан",
                "Ошибка создания профит центра",
                "Профит центр создан успешно"), SecurityActionCode.ReferChng);
       /* abw.addSecureAction(deleteAction(new ProfitCenterDlg(ProfitCenterDlg.DELETE, FormAction.DELETE, grid.getTable().getColumns()),
                "Профит центр не удален",
                "Ошибка удаления профит центра",
                "Профит центр удален успешно"), SecurityActionCode.ReferDel);*/
    }

    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("PRFCODE", Column.Sort.ASC));
        return list;
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();
        HashMap<Serializable, String> yesNoList = getYesNoList();
        Column col;

        result.addColumn(new Column("PRFCODE", Column.Type.STRING, FIELD_CODE, 40, true, false, Column.Sort.ASC, ""));
        result.addColumn(new Column("PRFNAME", Column.Type.STRING, FIELD_NAME, 150));
        result.addColumn(col = new Column("CLOSED", Column.Type.STRING, FIELD_CLOSED, 60));
        col.setList(yesNoList);
        return result;
    }

    @Override
    protected String prepareSql() {
        return "select PRFCODE, PRFNAME, CLOSED from GL_PRFCNTR";
    }

    @Override
    protected void save(ProfitCenterWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<ProfitCenterWrapper>> asyncCallbackImpl) throws Exception {
        BarsGLEntryPoint.dictionaryService.saveProfitCenter(cnw, action, asyncCallbackImpl);
    }
}
