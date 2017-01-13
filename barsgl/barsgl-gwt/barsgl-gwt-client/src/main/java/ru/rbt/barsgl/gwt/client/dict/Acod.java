package ru.rbt.barsgl.gwt.client.dict;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.dict.dlg.AcodDlg;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.AcodWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.SecurityActionCode;

import java.util.ArrayList;

/**
 * Created by akichigi on 21.10.16.
 */
public class Acod extends EditableDictionary<AcodWrapper> {
    public final static String FORM_NAME = "ACOD Midas";
    public final static String FIELD_ACOD = "ACOD";
    public final static String FIELD_ACC2DSCR = "Б/Сч 2 (описание)";
    public final static String FIELD_TYPE = "Тип";
    public final static String FIELD_SQDSCR = "SQ (описание)";
    public final static String FIELD_ENAME = "Наименование (ENG)";
    public final static String FIELD_RNAME = "Наименование (RUS)";

    public Acod() {
        super(FORM_NAME);
        reconfigure();
    }

    protected void reconfigure() {
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));

        abw.addSecureAction(commonLazyAction("AcodDlg",
                AcodDlg.EDIT,
                FormAction.UPDATE,
                table.getColumns(),
                "Acod не сохранен",
                "Ошибка изменения Acod",
                "Acod изменен успешно"
        ), SecurityActionCode.ReferAccTypeChng);

        abw.addSecureAction(commonLazyAction("AcodDlg",
                AcodDlg.CREATE,
                FormAction.CREATE,
                table.getColumns(),
                "Acod не создан",
                "Ошибка создания Acod",
                "Acod создан успешно"
        ), SecurityActionCode.ReferAccTypeChng);
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();
        Column col;
        result.addColumn(col = new Column("ID", Column.Type.LONG, "ID", 80, false, false));
        col.setEditable(false);
        col.setFilterable(false);

        result.addColumn(new Column("ACOD", Column.Type.STRING, FIELD_ACOD, 30));
        result.addColumn(new Column("ACC2DSCR", Column.Type.STRING, FIELD_ACC2DSCR, 80));
        result.addColumn(new Column("TYPE", Column.Type.STRING, FIELD_TYPE, 20));
        result.addColumn(new Column("SQDSCR", Column.Type.STRING, FIELD_SQDSCR, 80));
        result.addColumn(new Column("ENAME", Column.Type.STRING, FIELD_ENAME, 80));
        result.addColumn(new Column("RNAME", Column.Type.STRING, FIELD_RNAME, 250));

        return result;
    }

    @Override
    protected String prepareSql() {
        return "select ID, ACOD, ACC2DSCR, TYPE, SQDSCR, ENAME, RNAME from GL_ACOD " +
               "where cast(acod as integer) > 999 or ename <> 'not used'";

    }

    @Override
    protected ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("ACOD", Column.Sort.ASC));
        return list;
    }

    @Override
    protected void save(AcodWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<AcodWrapper>> asyncCallbackImpl) throws Exception {
        BarsGLEntryPoint.dictionaryService.saveAcod(cnw, action, asyncCallbackImpl);
    }
}
