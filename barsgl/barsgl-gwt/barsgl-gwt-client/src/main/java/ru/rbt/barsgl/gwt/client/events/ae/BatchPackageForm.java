package ru.rbt.barsgl.gwt.client.events.ae;

import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.quickFilter.DateHistoryQuickFilterParams;
import ru.rbt.barsgl.gwt.client.quickFilter.DateIntervalQuickFilterAction;
import ru.rbt.barsgl.gwt.client.quickFilter.IQuickFilterParams;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.enums.BatchPackageState;
import ru.rbt.barsgl.shared.enums.BatchPostStep;

import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getEnumLabelsList;
import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndFilialPart;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;

/**
 * Created by ER18837 on 16.01.17.
 */
public class BatchPackageForm extends OperSuperBase {
    public static final String FORM_NAME = "Пакеты, загруженные из файла";

    private Column colProcDate;
    private String _select = "";
    private String _where_ownMessages = "";

    public BatchPackageForm() {
        super(FORM_NAME, true);
        _select = getSelectClause();
        reconfigure();
    }

    private void reconfigure() {
        GridAction quickFilterAction;
        abw.addAction(quickFilterAction = new DateOwnQuickFilterAction(grid, colProcDate));
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addAction(new PackageStatisticsAction(grid));
        quickFilterAction.execute();
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();
        Column col;

        result.addColumn(new Column("ID_PKG", Column.Type.LONG, "ID пакета", 60));
        result.addColumn(col = new Column("STATE", Column.Type.STRING, "Статус пакета", 120));
        col.setList(getEnumLabelsList(BatchPackageState.values()));

//        result.addColumn(new Column("SRC_PST", Column.Type.STRING, "Источник сделки", 80));

        result.addColumn(colProcDate = new Column("PROCDATE", Column.Type.DATE, "Дата опердня", 75, false, false));
        result.addColumn(new Column("POSTDATE", Column.Type.DATE, "Дата проводки", 75));

        result.addColumn(new Column("MVMNT_OFF", Column.Type.STRING, "Без АБС", 40));

        result.addColumn(new Column("PST_ALL", Column.Type.INTEGER, "Всего операций", 70));
        result.addColumn(new Column("SRV_ALL", Column.Type.INTEGER, "Запросов в АБС", 70));
        result.addColumn(new Column("SRV_OK", Column.Type.INTEGER, "Ответов от АБС", 70));
        result.addColumn(new Column("SRV_ERR", Column.Type.INTEGER, "Ошибок от АБС", 70));
        result.addColumn(new Column("PST_OK", Column.Type.INTEGER, "Обраб. успешно", 70));
        result.addColumn(new Column("PST_ERR", Column.Type.INTEGER, "Всего ошибок", 70));

        result.addColumn(new Column("DT_LOAD", Column.Type.DATETIME, "Дата загрузки", 130));
        result.addColumn(new Column("DT_PRC", Column.Type.DATETIME, "Дата обработки", 150));

        result.addColumn(new Column("FILE_NAME", Column.Type.STRING, "Имя файла", 100, false, false));
        result.addColumn(new Column("FILIAL", Column.Type.STRING, "Филиал", 60));
        result.addColumn(col = new Column("FIO", Column.Type.STRING, "ФИО", 250));
        col.setFilterable(false);

        result.addColumn(new Column("USER_NAME", Column.Type.STRING, "Логин 1 руки", 100, false, false));
        result.addColumn(new Column("USER_AU2", Column.Type.STRING, "Логин 2 руки", 100, false, false));
        result.addColumn(new Column("OTS_AU2", Column.Type.DATETIME, "Дата подписи", 130, false, false));
        result.addColumn(new Column("USER_AU3", Column.Type.STRING, "Логин 3 руки", 100, false, false));
        result.addColumn(new Column("OTS_AU3", Column.Type.DATETIME, "Дата подтверж.", 130, false, false));
        result.addColumn(new Column("USER_CHNG", Column.Type.STRING, "Логин изменения", 100, false, false));
        result.addColumn(new Column("OTS_CHNG", Column.Type.DATETIME, "Дата изменения", 130, false, false));
        result.addColumn(new Column("DESCRDENY", Column.Type.STRING, "Причина возврата", 300, false, false));

        return result;
    }

    @Override
    protected String prepareSql() {
        return getSelectClause();
    }

    protected String getSelectClause() {
    	String where = getSourceAndFilialPart("where", "", "FILIAL");
    	if (isEmpty(where))
    		where = " where 1=1";
        return "select ID_PKG, DT_LOAD, STATE, DT_PRC, PKG.USER_NAME, FILE_NAME, CNT_PST, MVMNT_OFF, POSTDATE, PROCDATE,\n" +
                "PST_ALL, PST_OK, PST_ERR, SRV_ALL, SRV_OK, SRV_ERR,\n" +
                "U.SURNAME || ' '  || VALUE(U.FIRSTNAME, '') || ' ' || VALUE(U.PATRONYMIC, '') as FIO, FILIAL,\n" +
                "USER_AU2, OTS_AU2, USER_AU3, OTS_AU3, USER_CHNG, OTS_CHNG, DESCRDENY\n" +
                "from V_GL_BATPKG_STAT PKG left join GL_USER U on U.USER_NAME = PKG.USER_NAME"
                + where;
    }

    public void setSql(String text){
        sql_select = text;
        setExcelSql(sql_select);
    }

    private String sql(){
        return new StringBuilder()
                .append(_select )
                .append(_where_ownMessages).toString();
    }


    @Override
    protected ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("ID_PKG", Column.Sort.DESC));
        return list;
    }

    class DateOwnQuickFilterAction extends DateIntervalQuickFilterAction {

    	public DateOwnQuickFilterAction(GridWidget grid, Column dateColumn) {
            super(grid, dateColumn);
        }

        @Override
        public DlgFrame getFilterDialog() {
            return new OperHistoryDlg() {
                @Override
                public Widget createContent() {
                	Widget panel = super.createContent();
                	_choicePanel.setVisible(false);
                    return panel;
                }
            	
            };
        }

        @Override
        public IQuickFilterParams getFilterParams() {
            return new DateHistoryQuickFilterParams(dateColumn, null);
        }

        @Override
        public void beforeFireFilterEvent(IQuickFilterParams filterParams) {
            DateHistoryQuickFilterParams params = (DateHistoryQuickFilterParams)filterParams;
            _ownMessages = params.getOwnMessages();
            _where_ownMessages = getOwnMessagesClause(_ownMessages, BatchPostStep.NOHAND, "PKG");

            setSql(sql());
        }
    }

}
