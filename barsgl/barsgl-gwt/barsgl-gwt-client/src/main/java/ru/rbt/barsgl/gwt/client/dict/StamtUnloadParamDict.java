package ru.rbt.barsgl.gwt.client.dict;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.dict.dlg.StamtUnloadParamDlg;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.dict.StamtUnloadParamWrapper;
import ru.rbt.shared.enums.SecurityActionCode;

import java.util.ArrayList;

import static ru.rbt.barsgl.shared.enums.StamtUnloadParamType.A;
import static ru.rbt.barsgl.shared.enums.StamtUnloadParamType.B;
import static ru.rbt.barsgl.shared.enums.StamtUnloadParamTypeCheck.EXCLUDE;
import static ru.rbt.barsgl.shared.enums.StamtUnloadParamTypeCheck.INCLUDE;

/**
 * Created by ER21006 on 19.01.2016.
 */
public class StamtUnloadParamDict extends EditableDictionary<StamtUnloadParamWrapper> {

    public final static String FORM_NAME = "Настройки выгрузки в STAMT";
    public final static String FIELD_ACC = "БС2/Счет";
    public final static String FIELD_ACCTYPE = "Тип настройки";
    public final static String FIELD_INCLUDE = "Проводки Вкл/Искл";
    public final static String FIELD_INCLUDE_BLN = "Остатки Вкл/Искл";


    public StamtUnloadParamDict() {
        super(FORM_NAME);
        reconfigure();
    }

    @Override
    protected void save(StamtUnloadParamWrapper cnw, FormAction action
            , AsyncCallback<RpcRes_Base<StamtUnloadParamWrapper>> asyncCallbackImpl) throws Exception {
        BarsGLEntryPoint.dictionaryService.saveStamtUnloadParam(cnw, action, asyncCallbackImpl);
    }

    @Override
    protected Table prepareTable() {
        Table table = new Table();
        table.addColumn(new Column("ACCOUNT", Column.Type.STRING, FIELD_ACC, 100, true, false));
        table.addColumn(new Column("ACCTYPE", Column.Type.STRING, FIELD_ACCTYPE, 20, true, false));
        table.addColumn(new Column("INCLUDE", Column.Type.STRING, FIELD_INCLUDE, 20, true, false));
        table.addColumn(new Column("INCLUDEBLN", Column.Type.STRING, FIELD_INCLUDE_BLN, 20, true, false));
        return table;
    }

    @Override
    protected String prepareSql() {
        return "select * from (select account, \n" +
                "       case\n" +
                "          when acctype = 'A' then trim('" + A.getLabel() + "')\n" +
                "          when acctype = 'B' then trim('" + B.getLabel() + "')\n" +
                "       end acctype,\n" +
                "       case\n" +
                "          when include = '1' then trim('" + INCLUDE.getLabel() + "')\n" +
                "          when include = '0' then trim('" + EXCLUDE.getLabel() + "')\n" +
                "       end include,\n" +
                "       case\n" +
                "          when includebln = '1' then trim('" + INCLUDE.getLabel() + "')\n" +
                "          when includebln = '0' then trim('" + EXCLUDE.getLabel() + "')\n" +
                "       end includebln\n" +
                " from GL_STMPARM) v";
    }

    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
        return null;
    }

    private void reconfigure() {
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addSecureAction(createAction(new StamtUnloadParamDlg(StamtUnloadParamDlg.CREATE, grid.getTable().getColumns(), FormAction.CREATE),
                "Настройка не создана.\n Ошибка: ",
                "Ошибка создания настройки: \n",
                "Настройка создана успешно: "), SecurityActionCode.ReferAccSTAMT);
        abw.addSecureAction(editAction(new StamtUnloadParamDlg(StamtUnloadParamDlg.EDIT, grid.getTable().getColumns(), FormAction.UPDATE),
                "Настройка не сохранена.\n Ошибка: ",
                "Ошибка при изменении настройки: \n",
                "Настройка изменена успешно: "), SecurityActionCode.ReferAccSTAMT);
        abw.addSecureAction(deleteAction(new StamtUnloadParamDlg(StamtUnloadParamDlg.DELETE, grid.getTable().getColumns(), FormAction.DELETE),
                "Настройка не удалена.\n Ошибка: ",
                "Ошибка удаления настройки: \n",
                "Настройка удалена успешно: "), SecurityActionCode.ReferAccSTAMT);
    }
}
