package ru.rbt.barsgl.gwt.client.account;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.quickFilter.DateHistoryQuickFilterParams;
import ru.rbt.barsgl.gwt.client.quickFilter.DateOwnHistQuickFilterAction;
import ru.rbt.barsgl.gwt.client.quickFilter.IQuickFilterParams;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.events.GridEvents;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.enums.AccountBatchPackageState;
import ru.rbt.barsgl.shared.enums.AccountBatchState;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.shared.user.AppUserWrapper;

import java.util.ArrayList;
import java.util.List;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getEnumLabelsList;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getYesNoList;
import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndFilialPart;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;

/**
 * Created by er18837 on 19.10.2018.
 */
public class AccountBatchForm extends GridForm {
    public static final String FORM_NAME = "Счета пакетной загрузки";

    private Boolean _ownMessages;
    private int _indIdPackage;

    protected GridAction quickFilterAction;

    protected Column _colIdPackage;
    protected Column _colProcDate;
    protected Column _colInvisible;

    private String _select = "";
    private String _where_ownMessages = "";
    private GridAction _loadFile;

    public AccountBatchForm() {
        super(FORM_NAME, true);
        _select = getSelectClause();
        reconfigure();

        quickFilterAction.execute();
    }

    public AccountBatchForm(Boolean ownMessages) {
        super(FORM_NAME, false);
        _select = getSelectClause();
        _ownMessages = ownMessages;
        _where_ownMessages = getOwnMessagesClause(_ownMessages);
        setSql(sql());
        reconfigure();
    }

    protected void reconfigure() {
        quickFilterAction = new AccountBatchQFAction(grid, _colProcDate, _colInvisible);
        abw.addAction(quickFilterAction);
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addAction(createGotoPackage(ImageConstants.INSTANCE.oper_go(), "Переход на страницу пакетов"));
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();
        Column col;

        result.addColumn(new Column("ID_REQ", Column.Type.LONG, "ID запроса", 60));
        result.addColumn(_colIdPackage = new Column("ID_PKG", Column.Type.LONG, "ID пакета", 60));
        _indIdPackage = 1;
        result.addColumn(_colProcDate = new Column("OD_LOAD", Column.Type.DATE, "Дата загрузки", 75));
        result.addColumn(new Column("RECNO", Column.Type.INTEGER, "Номер строки", 70));

        result.addColumn(col = new Column("PKG_STATE", Column.Type.STRING, "Статус пакета", 100, false, false));
        col.setList(getEnumLabelsList(AccountBatchPackageState.values()));
        result.addColumn(col = new Column("STATE", Column.Type.STRING, "Статус запроса", 100));
        col.setList(getEnumLabelsList(AccountBatchState.values()));
        result.addColumn(col = new Column("WAS_ACC", Column.Type.STRING, "Открыт ранее", 60));
        col.setList(getYesNoList());

        result.addColumn(new Column("BSAACID", Column.Type.STRING, "Счет ЦБ", 160));
        result.addColumn(new Column("ACID", Column.Type.STRING, "Счет Midas", 160));
        result.addColumn(new Column("BRANCH_IN", Column.Type.STRING, "Отделение", 60));
        result.addColumn(new Column("CBCC_BR", Column.Type.STRING, "Филиал", 60));
        result.addColumn(new Column("CCY_IN", Column.Type.STRING, "Код валюты", 60));
        result.addColumn(new Column("CUSTNO_IN", Column.Type.STRING, "Номер клиента", 80));

        result.addColumn(new Column("ACCTYPE_IN", Column.Type.STRING, "Accounting Type", 80));
        result.addColumn(new Column("CTYPE_IN", Column.Type.STRING, "Тип собств", 65, false, false));
        result.addColumn(new Column("CTYPE_PARM", Column.Type.STRING, "Тип собств (actparm)", 65, false, false));
        result.addColumn(new Column("CTYPE_ACC", Column.Type.STRING, "*Тип собств", 65));
        result.addColumn(new Column("TERM_IN", Column.Type.STRING, "Код срока", 65, false, false));
        result.addColumn(new Column("TERM_PARM", Column.Type.STRING, "*Код срока", 65));
        result.addColumn(new Column("ACC2_IN", Column.Type.STRING, "Баланс. счет 2", 70, false, false));
        result.addColumn(new Column("ACC2_PARM", Column.Type.STRING, "*Баланс. счет 2", 70));
        result.addColumn(new Column("PLCODE_PARM", Column.Type.STRING, "Символ ОФР", 70));
        result.addColumn(new Column("ACOD_PARM", Column.Type.STRING, "ACOD", 65, false, false));
        result.addColumn(new Column("ACSQ_PARM", Column.Type.STRING, "SQ", 65, false, false));

        result.addColumn(new Column("DEALSRC_IN", Column.Type.STRING, "Источник сделки", 75));
        result.addColumn(new Column("DEALID_IN", Column.Type.STRING, "ИД сделки", 160));
        result.addColumn(new Column("SUBDEALID_IN", Column.Type.STRING, "ИД субсделки", 160));

        result.addColumn(new Column("OPENDATE_IN", Column.Type.DATE, "Дата открытия счета", 75, false, false));
        result.addColumn(new Column("OPENDATE", Column.Type.DATE, "*Дата открытия счета", 75));
        result.addColumn(new Column("ACCNAME", Column.Type.STRING, "Название счета", 260, false, false));

        result.addColumn(new Column("ERROR_MSG", Column.Type.STRING, "Описание ошибки", 800));

        result.addColumn(col = new Column("TS_VALID", Column.Type.DATETIME, "Валидирован", 135, false, false));
        col.setFilterable(false);
        result.addColumn(col = new Column("TS_OPEN", Column.Type.DATETIME, "Зарегистрирован", 135, false, false));
        col.setFilterable(false);
        result.addColumn(col = new Column("TS_ENDP", Column.Type.DATETIME, "Пакет обработан", 135));
        col.setFilterable(false);

        result.addColumn(new Column("USER_LOAD", Column.Type.STRING, "Исполнитель загрузки", 110, false, false));
        result.addColumn(new Column("USER_PROC", Column.Type.STRING, "Исполнитель", 110));

        result.addColumn(_colInvisible = new Column("INVISIBLE", Column.Type.STRING, "Удален", 60));
        _colInvisible.setList(getYesNoList());

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
        return " select ID_REQ, ID_PKG, OD_LOAD, RECNO, PKG_STATE, STATE, WAS_ACC, BSAACID, ACID,\n" +
                " BRANCH_IN, CBCC_BR, CCY_IN, CUSTNO_IN, ACCTYPE_IN, ACOD_PARM, ACSQ_PARM,\n" +
                " CTYPE_IN, CTYPE_PARM, CTYPE_ACC, TERM_IN, TERM_PARM, ACC2_IN, ACC2_PARM, PLCODE_PARM,\n" +
                " DEALSRC_IN, DEALID_IN, SUBDEALID_IN, OPENDATE_IN, OPENDATE, ACCNAME,\n" +
                " ERROR_MSG, TS_VALID, TS_OPEN, TS_ENDP, USER_LOAD, USER_PROC, INVISIBLE\n" +
                " FROM V_GL_ACBATREQ"
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

    protected String getOwnMessagesClause(Boolean ownMessages){
        if (null == ownMessages || !ownMessages) return "";

        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        if (wrapper == null) return "";

        return  " and " + "'" + wrapper.getUserName() + "' in (USER_LOAD, USER_PROC)";
    }


    @Override
    protected List<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("ID_PKG", Column.Sort.DESC));
        list.add(new SortItem("RECNO", Column.Sort.ASC));
        return list;
    }

    class AccountBatchQFAction extends DateOwnHistQuickFilterAction {

        public AccountBatchQFAction(GridWidget grid, Column dateColumn, Column histColumn) {
            super(grid, dateColumn, histColumn);
        }

        @Override
        public void beforeFireFilterEvent(IQuickFilterParams filterParams) {
            DateHistoryQuickFilterParams params = (DateHistoryQuickFilterParams)filterParams;
            _ownMessages = params.getOwnMessages();
            _where_ownMessages = getOwnMessagesClause(_ownMessages);

            setSql(sql());
        }
    }

    private Long getIdPackage(){
        if(grid.getRowCount() == 0) return null;
        Row row = grid.getCurrentRow();
        return row == null ? null : (Long) row.getField(_indIdPackage).getValue();
    }

    private GridAction createGotoPackage(ImageResource img, final String hint) {
        return new GridAction(grid, null, hint, new Image(img), 10, true) {

            @Override
            public void execute() {
                final Long idPackage = getIdPackage();
                if (idPackage == null) return ;

                BarsGLEntryPoint.menuBuilder.formLoad(new AccountBatchPackageForm(_ownMessages){
                    @Override
                    protected List<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams) {
                        ArrayList<FilterItem> list = new ArrayList<>();
                        List<FilterItem> listPkg =  AccountBatchForm.this.grid.getFilterCriteria();
                        if (listPkg == null)
                            listPkg = new ArrayList<>();
                        for (FilterItem item : listPkg) {
                            if (item.getName().equals(_colProcDate.getName()))
                                list.add(item);
                        }
                        list.add(new FilterItem(_colIdPackage, FilterCriteria.EQ, idPackage));

                        return list;
                    }
                });
            }
        };
    }

}
