package ru.rbt.barsgl.gwt.client.account;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.client.quickFilter.DateHistoryQuickFilterParams;
import ru.rbt.barsgl.gwt.client.quickFilter.DateOwnHistQuickFilterAction;
import ru.rbt.barsgl.gwt.client.quickFilter.IQuickFilterParams;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.*;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.AccountBatchPackageState;
import ru.rbt.barsgl.shared.enums.InvisibleType;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.barsgl.shared.operation.AccountBatchWrapper;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.shared.user.AppUserWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static ru.rbt.barsgl.gwt.client.account.AccountBatchErrorForm.ViewType.V_ERROR;
import static ru.rbt.barsgl.gwt.client.account.AccountBatchErrorForm.ViewType.V_FULL;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getEnumLabelsList;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getYesNoList;
import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndFilialPart;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showConfirm;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;
import static ru.rbt.barsgl.shared.operation.AccountBatchWrapper.AccountBatchAction.DELETE;
import static ru.rbt.barsgl.shared.operation.AccountBatchWrapper.AccountBatchAction.OPEN;
import static ru.rbt.shared.enums.SecurityActionCode.AccPkgFileDel;
import static ru.rbt.shared.enums.SecurityActionCode.AccPkgFileLoad;
import static ru.rbt.shared.enums.SecurityActionCode.AccPkgFileOpen;

/**
 * Created by er18837 on 15.10.2018.
 */
public class AccountBatchPackageForm extends GridForm {
    public static final String FORM_NAME = "Пакеты для загрузки счетов";

    private Boolean _ownMessages = null;
    private int _indIdPackage;

    protected Column _colProcDate;
    protected Column _colInvisible;
    private String _select = "";
    private String _where_ownMessages = "";

    protected GridAction quickFilterAction;
    private GridAction _loadPackage;
    private GridAction _delPackage;
    private GridAction _openAccounts;
    private GridAction _viewFull;
    private GridAction _viewError;
    private GridAction _gotoAccounts;

    public AccountBatchPackageForm() {
        super(FORM_NAME, true);
        _select = getSelectClause();
        reconfigure();

        quickFilterAction.execute();
    }

    public AccountBatchPackageForm(Boolean ownMessages) {
        super(FORM_NAME, false);
        _select = getSelectClause();
        _ownMessages = ownMessages;
        _where_ownMessages = getOwnMessagesClause(_ownMessages);
        setSql(sql());
        reconfigure();
    }

    private void reconfigure() {
        quickFilterAction = new AccountBatchPackageQFAction(grid, _colProcDate, _colInvisible);
        abw.addAction(quickFilterAction);
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addSecureAction(_loadPackage = createLoad(ImageConstants.INSTANCE.load()), AccPkgFileLoad);
        abw.addSecureAction(_openAccounts = createCommand(ImageConstants.INSTANCE.add_list(), OPEN, "Открыть счета пакета", "открытие счетов пакета"), AccPkgFileLoad, AccPkgFileOpen);
        abw.addSecureAction(_delPackage = createCommand(ImageConstants.INSTANCE.del_list(), DELETE, "Удалить пакет", "удаление пакета"), AccPkgFileLoad, AccPkgFileDel);
        abw.addAction(_viewError = createView(ImageConstants.INSTANCE.err_list(), V_ERROR, "Ошибки пакета"));
        abw.addAction(_viewFull = createView(ImageConstants.INSTANCE.preview(), V_FULL, "Просмотр пакета"));
        abw.addAction(createGotoAccounts(ImageConstants.INSTANCE.oper_go(), "Переход на страницу счетов"));
//        abw.addAction(new PackageStatisticsAction(grid));
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();
        Column col;

        result.addColumn(new Column("ID_PKG", Column.Type.LONG, "ID пакета", 60));
        _indIdPackage = 0;
        result.addColumn(col = new Column("STATE", Column.Type.STRING, "Статус пакета", 120));
        col.setList(getEnumLabelsList(AccountBatchPackageState.values()));

        result.addColumn(_colProcDate = new Column("OD_LOAD", Column.Type.DATE, "Дата загрузки", 75));

        result.addColumn(new Column("CNT_REQ", Column.Type.INTEGER, "Всего запросов", 70));
        result.addColumn(new Column("CNT_OPEN", Column.Type.INTEGER, "Открыто счетов", 70));        // TODO
        result.addColumn(new Column("CNT_FOUND", Column.Type.INTEGER, "Найдено счетов", 70));
        result.addColumn(new Column("CNT_ERR", Column.Type.INTEGER, "Всего c ошибками", 70));

        result.addColumn(col = new Column("TS_LOAD", Column.Type.DATETIME, "Время загрузки", 135, false, false));
        col.setFilterable(false);
        result.addColumn(col = new Column("TS_STARTV", Column.Type.DATETIME, "Начало обработки", 135));
        col.setFilterable(false);
        result.addColumn(col = new Column("TS_ENDV", Column.Type.DATETIME, "Конец валидации", 135, false, false));
        col.setFilterable(false);
//        result.addColumn(col = new Column("TS_STARTP", Column.Type.DATETIME, "Начало открытия счетов", 135, false, false));
//        col.setFilterable(false);
        result.addColumn(col = new Column("TS_ENDP", Column.Type.DATETIME, "Конец обработки", 135));
        col.setFilterable(false);

        result.addColumn(new Column("FILIAL", Column.Type.STRING, "Филиал загрузившего пакет", 100));
        result.addColumn(new Column("USER_LOAD", Column.Type.STRING, "Логин загрузившего пакет", 110, false, false));
        result.addColumn(col = new Column("FIO_LOAD", Column.Type.STRING, "ФИО загрузившего пакет", 250));
        result.addColumn(new Column("USER_PROC", Column.Type.STRING, "Логин обработавшего пакет", 110, false, false));
        result.addColumn(col = new Column("FIO_PROC", Column.Type.STRING, "ФИО обработавшего пакет", 250, false, false));

        result.addColumn(new Column("FILE_NAME", Column.Type.STRING, "Имя файла", 100, false, false));
        result.addColumn(_colInvisible = new Column("INVISIBLE", Column.Type.STRING, "Удален", 60));
        _colInvisible.setList(getYesNoList());

        result.addColumn(new Column("ERROR_MSG", Column.Type.STRING, "Описание ошибки", 800, false, false));

        return result;
    }

    @Override
    protected String prepareSql() {
        return getSelectClause();
    }

    protected String getSelectClause() {
        String where = getSourceAndFilialPart("where", "", "U1.FILIAL");
        if (isEmpty(where))
            where = " where 1=1";
        return " SELECT ID_PKG, OD_LOAD, CNT_REQ, CNT_ERR, CNT_FOUND, CNT_REQ - CNT_ERR - CNT_FOUND CNT_OPEN,\n" +
                " TS_LOAD, TS_STARTV, TS_ENDV, TS_STARTP, TS_ENDP, STATE, USER_LOAD, USER_PROC, FILE_NAME, ERROR_MSG, " +
                " case when INVISIBLE = '" + InvisibleType.N.name() + "' then ' ' else INVISIBLE end as INVISIBLE, " +
                " U1.FILIAL, TRIM(REPLACE(U1.SURNAME || ' '  || U1.FIRSTNAME || ' ' || U1.PATRONYMIC, '  ', ' ')) AS FIO_LOAD,\n" +
                " TRIM(REPLACE(U2.SURNAME || ' '  || U2.FIRSTNAME || ' ' || U2.PATRONYMIC, '  ', ' ')) AS FIO_PROC\n" +
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
        if (ownMessages == null || !ownMessages) return "";

        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        if (wrapper == null) return "";

        return  " and " + "'" + wrapper.getUserName() + "' in (USER_LOAD, USER_PROC)";
    }

    private Long getIdPackage(){
        if(grid.getRowCount() == 0) return null;
        Row row = grid.getCurrentRow();
        return row == null ? null : (Long) row.getField(_indIdPackage).getValue();
    }

    private GridAction createLoad(ImageResource img) {

        return new GridAction(grid, null, "Загрузить пакет", new Image(img), 10) {  // LoadAccountDlg.TITLE
            LoadAccountDlg dlg = null;

            @Override
            public void execute() {
                WaitingManager.show("Загрузка из файла...");
                if (dlg == null) dlg = new LoadAccountDlg();

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

    private GridAction createCommand(ImageResource img, final AccountBatchWrapper.AccountBatchAction batchAction, final String hint, final String confirm) {
        return new GridAction(grid, null, hint, new Image(img), 10, true) {
            @Override
            public void execute() {
                final Long idPackage = getIdPackage();
                if (idPackage == null)
                    return;
                showConfirm("Подтверждение", "Подтвердите " + confirm + " c ID = " + idPackage,
                        new IDlgEvents() {
                            @Override
                            public void onDlgOkClick(Object p) throws Exception {
                                executeCommand(batchAction, idPackage);
                            }
                        }
                        , null);
            }
        };
    }

    private void executeCommand(final AccountBatchWrapper.AccountBatchAction batchAction, long idPackage) {
        AccountBatchWrapper wrapper = new AccountBatchWrapper();
        wrapper.setPackageId(idPackage);
        wrapper.setAction(batchAction);
        wrapper.setUserId(((AppUserWrapper) LocalDataStorage.getParam("current_user")).getId());

        BarsGLEntryPoint.accountService.processAccountBatchRq(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<AccountBatchWrapper>>() {
            @Override
            public void onSuccess(RpcRes_Base<AccountBatchWrapper> wrapper) {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                if (wrapper.isError()) {
                    showInfo("Ошибка", wrapper.getMessage());
                    WaitingManager.hide();
                } else {
                    showInfo("Информация", wrapper.getMessage());
                    grid.refresh();
                    WaitingManager.hide();
                }
            }
        });
    }

    private GridAction createView(ImageResource img, final AccountBatchErrorForm.ViewType viewType, final String hint) {
        return new GridAction(grid, null, hint, new Image(img), 10, true) {
            @Override
            public void execute() {
                try {
                    final Long idPackage = getIdPackage();
                    if (idPackage == null)
                        return;
                    GridFormDlgBase dlg = new AccountBatchFormDlg(viewType) {
                        @Override
                        public AccountBatchErrorForm.ViewType getViewType() {
                            return viewType;
                        }

                        @Override
                        protected boolean setResultList(HashMap<String, Object> result) {
                            return true;
                        }

                        @Override
                        protected Object[] getInitialFilterParams() {
                            return new Object[] {idPackage};
                        }
                    };
                    dlg.setModal(true);
                    dlg.show();
                } catch (Exception e) {
                    Window.alert(e.getMessage());
                }
            }
        };
    }

    private GridAction createGotoAccounts(ImageResource img, final String hint) {
        return new GridAction(grid, null, hint, new Image(img), 10, true) {

            @Override
            public void execute() {
                final Long idPackage = getIdPackage();
                if (idPackage == null) return ;

                Boolean ownMessage = _ownMessages == null ? false : _ownMessages;
                BarsGLEntryPoint.menuBuilder.formLoad(new AccountBatchForm(ownMessage){
                    @Override
                    protected List<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams) {
                        ArrayList<FilterItem> list = new ArrayList<>();
                        List<FilterItem> listPkg =  AccountBatchPackageForm.this.grid.getFilterCriteria();
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
