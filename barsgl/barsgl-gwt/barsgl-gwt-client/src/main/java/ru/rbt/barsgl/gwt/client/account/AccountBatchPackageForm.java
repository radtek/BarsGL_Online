package ru.rbt.barsgl.gwt.client.account;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.quickFilter.DateHistoryQuickFilterParams;
import ru.rbt.barsgl.gwt.client.quickFilter.DateOwnHistQuickFilterAction;
import ru.rbt.barsgl.gwt.client.quickFilter.IQuickFilterParams;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.*;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.gwt.server.upload.UploadFileType;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.AccountBatchPackageState;
import ru.rbt.barsgl.shared.enums.InvisibleType;
import ru.rbt.barsgl.shared.operation.AccountBatchWrapper;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.shared.user.AppUserWrapper;

import java.util.ArrayList;
import java.util.List;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getEnumLabelsList;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getYesNoList;
import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndFilialPart;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;
import static ru.rbt.barsgl.shared.operation.AccountBatchWrapper.AccountBatchAction.OPEN;

/**
 * Created by er18837 on 15.10.2018.
 */
public class AccountBatchPackageForm extends GridForm {
    public static final String FORM_NAME = "Пакеты для загрузки счетов";

    private Boolean _ownMessages;

    private Column _colProcDate;
    private Column _colInvisible;
    private String _select = "";
    private String _where_ownMessages = "";
    private GridAction _loadFile;

    public AccountBatchPackageForm() {
        super(FORM_NAME, true);
        _select = getSelectClause();
        reconfigure();
    }

    private void reconfigure() {
        GridAction quickFilterAction = new AccountBatchPackageQFAction(grid, _colProcDate, _colInvisible);
        abw.addAction(quickFilterAction);
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addAction(_loadFile = createLoad(UploadFileType.Account, ImageConstants.INSTANCE.load_acc()));
//        abw.addAction(createGotoAccounts(ImageConstants.INSTANCE.oper_go()));

//        abw.addAction(new PackageStatisticsAction(grid));

        quickFilterAction.execute();
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();
        Column col;

        result.addColumn(new Column("ID_PKG", Column.Type.LONG, "ID пакета", 60));
        result.addColumn(col = new Column("STATE", Column.Type.STRING, "Статус пакета", 120));
        col.setList(getEnumLabelsList(AccountBatchPackageState.values()));

        result.addColumn(_colProcDate = new Column("OD_LOAD", Column.Type.DATE, "Дата загрузки", 75));

        result.addColumn(new Column("CNT_REQ", Column.Type.INTEGER, "Всего запросов", 70));
        result.addColumn(new Column("CNT_OPEN", Column.Type.INTEGER, "Открыто счетов", 70));        // TODO
        result.addColumn(new Column("CNT_FOUND", Column.Type.INTEGER, "Найдено счетов", 70));
        result.addColumn(new Column("CNT_ERR", Column.Type.INTEGER, "Всего c ошибками", 70));

        result.addColumn(col = new Column("TS_LOAD", Column.Type.DATETIME, "Время загрузки", 135, false, false));
        col.setFilterable(false);
        result.addColumn(col = new Column("TS_STARTV", Column.Type.DATETIME, "Время начала обработки", 135));
        col.setFilterable(false);
        result.addColumn(col = new Column("TS_ENDV", Column.Type.DATETIME, "Время завершения валидации", 135, false, false));
        col.setFilterable(false);
//        result.addColumn(col = new Column("TS_STARTP", Column.Type.DATETIME, "Начало открытия счетов", 135, false, false));
//        col.setFilterable(false);
        result.addColumn(col = new Column("TS_ENDP", Column.Type.DATETIME, "Время окончания обработки", 135));
        col.setFilterable(false);

        result.addColumn(new Column("FILIAL", Column.Type.STRING, "Филиал загрузившего пакет", 100));
        result.addColumn(new Column("USER_LOAD", Column.Type.STRING, "Логин загрузившего пакет", 110, false, false));
        result.addColumn(new Column("USER_PROC", Column.Type.STRING, "Логин обработавшего пакет", 110));
        result.addColumn(col = new Column("FIO", Column.Type.STRING, "ФИО обработавшего пакет", 250));

        result.addColumn(new Column("FILE_NAME", Column.Type.STRING, "Имя файла", 100, false, false));
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
        return " SELECT ID_PKG, OD_LOAD, CNT_REQ, CNT_ERR, CNT_FOUND, CNT_REQ - CNT_ERR - CNT_FOUND CNT_OPEN,\n" +
                " TS_LOAD, TS_STARTV, TS_ENDV, TS_STARTP, TS_ENDP, STATE, USER_LOAD, USER_PROC, FILE_NAME," +
                " case when INVISIBLE = '" + InvisibleType.N.name() + "' then ' ' else INVISIBLE end as INVISIBLE, " +
                " U1.FILIAL, TRIM(REPLACE(U2.SURNAME || ' '  || U2.FIRSTNAME || ' ' || U2.PATRONYMIC, '  ', ' ')) AS FIO\n" +
                " FROM GL_ACBATPKG PKG" +
                " LEFT JOIN GL_USER U1 ON U1.USER_NAME = PKG.USER_LOAD" +
                " LEFT JOIN GL_USER U2 ON U2.USER_NAME = PKG.USER_PROC"
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
    protected List<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("ID_PKG", Column.Sort.DESC));
        return list;
    }

    protected String getOwnMessagesClause(Boolean ownMessages){
        if (!ownMessages) return "";

        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        if (wrapper == null) return "";

        return  " and " + "'" + wrapper.getUserName() + "' in (USER_LOAD, USER_PROC)";
    }

    private GridAction createLoad(final UploadFileType loadType, ImageResource img) {

        return new GridAction(grid, null, LoadAccountDlg.TITLE, new Image(img), 10) {
            LoadAccountDlg dlg = null;

            @Override
            public void execute() {
                WaitingManager.show("Загрузка из файла...");
                if (dlg == null) dlg = new LoadAccountDlg(); // LoadFileFactory.create(loadType);

                dlg.setAfterCancelEvent(new IAfterCancelEvent() {
                    @Override
                    public void afterCancel() {
                        WaitingManager.hide();
                        dlg.hide();
                        grid.refresh();
                    }
                });

                dlg.setDlgEvents(new IDlgEvents() {
                    @Override
                    public void onDlgOkClick(Object prms) {
                        WaitingManager.hide();
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                        AccountBatchWrapper wrapper = new AccountBatchWrapper();
                        wrapper.setPackageId((Long) prms);
                        wrapper.setAction(OPEN);

                        AppUserWrapper appUserWrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
                        wrapper.setUserId(appUserWrapper.getId());

                        BarsGLEntryPoint.accountService.processAccountBatchRq(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<AccountBatchWrapper>>() {
                            @Override
                            public void onSuccess(RpcRes_Base<AccountBatchWrapper> wrapper) {
                                if (wrapper.isError()) {
                                    showInfo("Ошибка", wrapper.getMessage());
                                } else {
                                    dlg.hide();
                                    showInfo("Информация", wrapper.getMessage());

                                    grid.refresh();
                                }
                                WaitingManager.hide();
                            }
                        });
                    }
                });

                dlg.show(null);
            }
        };
    }
/*

    private GridAction createGotoAccounts(ImageResource img) {
        return new GridAction(grid, null, LoadAccountDlg.TITLE, new Image(img), 10) {

            @Override
            public void execute() {
                Row row = grid.getCurrentRow();
                if (row == null) return ;

                Long idPackage = (Long)row.getField(grid.getTable().getColumns().getColumnIndexByName("ID_PKG")).getValue();
                BarsGLEntryPoint.menuBuilder.formLoad(new AccountBatchForm(new PackageFilterParams()));
            }
        };
    }
*/

    class AccountBatchPackageQFAction extends DateOwnHistQuickFilterAction {

        public AccountBatchPackageQFAction(GridWidget grid, Column dateColumn, Column histColumn) {
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
}
