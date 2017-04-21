package ru.rbt.barsgl.gwt.client.backvalue;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.access.PrmValueWrapper;
import ru.rbt.shared.enums.PrmValueEnum;

import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getFilialPart;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by akichigi on 11.04.16.
 */
public class BackValueForm extends GridForm {
    public final static String FORM_NAME = "Доступ в архив";

    public BackValueForm() {
        super(FORM_NAME);
        reconfigure();
    }

    private void reconfigure(){
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addAction(createBackValue());
    }

    private GridAction createBackValue(){
        return new GridAction(grid, null, "Доступ в архив", new Image(ImageConstants.INSTANCE.back_value()), 10, true) {
            BackValueDlg dlg = new BackValueDlg();

            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                dlg.setCaption("Доступ в архив");
                dlg.setDlgEvents(this);

                getBackValue(row, dlg);
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception{
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                BarsGLEntryPoint.accessService.setBackValue((PrmValueWrapper) prms, new AuthCheckAsyncCallback<RpcRes_Base<PrmValueWrapper>>() {

                    @Override
                    public void onSuccess(RpcRes_Base<PrmValueWrapper> res) {
                        if (res.isError()){
                            DialogManager.error("Ошибка", "Операция сохранения не удалась.\nОшибка: " + res.getMessage());
                        } else {
                            dlg.hide();
                            refreshAction.execute();
                            //showInfo(res.getMessage());
                        }
                        WaitingManager.hide();
                    }
                });
            }
        };
    }

    private void getBackValue(final Row row, final BackValueDlg dlg){
        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

        BarsGLEntryPoint.accessService.getBackValue((Integer) row.getField(1).getValue(), PrmValueEnum.BackValue, new AuthCheckAsyncCallback<RpcRes_Base<PrmValueWrapper>>() {

            @Override
            public void onSuccess(RpcRes_Base<PrmValueWrapper> res) {
                if (res.isError()) {
                    DialogManager.error("Ошибка", "Операция изменения доступа в архив не удалась.\nОшибка: " + res.getMessage());
                } else {
                    PrmValueWrapper wrapper = res.getResult();
                    dlg.show(new Object[]{wrapper, row.getField(2).getValue(), row.getField(3).getValue(),
                            row.getField(4).getValue(), row.getField(5).getValue()});
                }
                WaitingManager.hide();
            }
        });
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();

        Column col;
        result.addColumn(col = new Column("ID_PRM", Column.Type.LONG, "ID_PRM", 80, false, false));
        col.setEditable(false);
        col.setFilterable(false);

        result.addColumn(new Column("ID_USER", Column.Type.INTEGER, "ID", 70, true, false));
        result.addColumn(new Column("USER_NAME", Column.Type.STRING, "Логин", 170));
        result.addColumn(new Column("SURNAME", Column.Type.STRING, "Фамилия", 200));
        result.addColumn(new Column("FIRSTNAME", Column.Type.STRING, "Имя", 160));
        result.addColumn(new Column("PATRONYMIC", Column.Type.STRING, "Отчество", 180));

        result.addColumn(col = new Column("PRM_CODE", Column.Type.STRING, "Параметр", 90, false, false));
        col.setEditable(false);
        col.setFilterable(false);
        result.addColumn(new Column("PRMVAL", Column.Type.STRING, "Кол-во дней назад", 105));

        result.addColumn(col = new Column("DT_BEGIN", Column.Type.DATE, "Дата начала действия", 105));
        col.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("DT_END", Column.Type.DATE, "Дата окончания действия", 95));
        col.setFormat("dd.MM.yyyy");

        result.addColumn(new Column("FILIAL", Column.Type.STRING, "Филиал", 70));
        result.addColumn(new Column("DEPID", Column.Type.STRING, "Подразделение", 115));

        return result;
    }

    @Override
    protected String prepareSql() {
        return "select id_prm, id_user, user_name, surname, firstname, patronymic, prm_code, " +
                "prmval, dt_begin, dt_end, filial, depid from V_GL_BACKVALUE " +
                "where end_dt is null and locked='0' " + getFilialPart("and", "filial");
    }

    @Override
    protected ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("id_user", Column.Sort.ASC));
        return list;
    }
}
