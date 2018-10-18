package ru.rbt.barsgl.gwt.client.account;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.events.ae.LoadOperDlg;
import ru.rbt.barsgl.gwt.client.events.ae.LoadOperDlgBase;
import ru.rbt.barsgl.gwt.client.loadFile.LoadFileFactory;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterCancelEvent;
import ru.rbt.barsgl.gwt.core.dialogs.IDlgEvents;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.gwt.server.upload.UploadFileType;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.AccountBatchPackageState;
import ru.rbt.barsgl.shared.enums.BatchPostAction;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.shared.user.AppUserWrapper;

import java.util.ArrayList;
import java.util.List;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getEnumLabelsList;
import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndFilialPart;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by er18837 on 15.10.2018.
 */
public class AccountBatchPkgForm extends GridForm {
    public static final String FORM_NAME = "Пакеты для загрузки счетов";

    private GridAction _loadFile;

    public AccountBatchPkgForm(String title) {
        super(title);
    }

    public AccountBatchPkgForm(String title, boolean delayLoad) {
        super(title, delayLoad);
    }

    public AccountBatchPkgForm() {
        super(FORM_NAME, false);    // true
//        _select = getSelectClause();
        reconfigure();
    }

    private void reconfigure() {
        GridAction quickFilterAction;
//        abw.addAction(quickFilterAction = new BatchPackageForm.DateOwnQuickFilterAction(grid, colProcDate));
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addAction(_loadFile = createLoad(UploadFileType.Account, ImageConstants.INSTANCE.load()));

//        abw.addAction(new PackageStatisticsAction(grid));

//        quickFilterAction.execute();
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();
        Column col;

        result.addColumn(new Column("ID_PKG", Column.Type.LONG, "ID пакета", 60));
        result.addColumn(col = new Column("STATE", Column.Type.STRING, "Статус пакета", 120));
        col.setList(getEnumLabelsList(AccountBatchPackageState.values()));

        result.addColumn(new Column("OD_LOAD", Column.Type.DATE, "Дата опердня загрузки", 75));

        result.addColumn(new Column("CNT_REQ", Column.Type.INTEGER, "Всего запросов", 70));
        result.addColumn(new Column("CNT_OPEN", Column.Type.INTEGER, "Открыто счетов", 70));        // TODO
        result.addColumn(new Column("CNT_FOUND", Column.Type.INTEGER, "Найдено счетов", 70));
        result.addColumn(new Column("CNT_ERR", Column.Type.INTEGER, "Всего ошибок", 70));

        result.addColumn(col = new Column("TS_LOAD", Column.Type.DATETIME, "Время загрузки", 130, false, false));
        col.setFilterable(false);
        result.addColumn(col = new Column("TS_STARTV", Column.Type.DATETIME, "Начало обработки", 130));
        col.setFilterable(false);
        result.addColumn(col = new Column("TS_ENDV", Column.Type.DATETIME, "Окончание валидации", 130, false, false));
        col.setFilterable(false);
        result.addColumn(col = new Column("TS_STARTP", Column.Type.DATETIME, "Начало открытия счетов", 130, false, false));
        col.setFilterable(false);
        result.addColumn(col = new Column("TS_ENDP", Column.Type.DATETIME, "Окончание обработки", 130));
        col.setFilterable(false);

        result.addColumn(new Column("FILIAL", Column.Type.STRING, "Филиал", 100));
        result.addColumn(col = new Column("FIO", Column.Type.STRING, "ФИО", 250));
        col.setFilterable(false);

        result.addColumn(new Column("USER_LOAD", Column.Type.STRING, "Логин 1 руки", 100, false, false));
        result.addColumn(new Column("USER_PROC", Column.Type.STRING, "Логин 2 руки", 100, false, false));
        result.addColumn(new Column("FILE_NAME", Column.Type.STRING, "Имя файла", 100, false, false));
        result.addColumn(new Column("INVISIBLE", Column.Type.STRING, "Удален", 80));

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
                " TS_LOAD, TS_STARTV, TS_ENDV, TS_STARTP, TS_ENDP, STATE, USER_LOAD, USER_PROC, FILE_NAME, INVISIBLE,\n" +
                " TRIM(REPLACE(U.SURNAME || ' '  || U.FIRSTNAME || ' ' || U.PATRONYMIC, '  ', ' ')) AS FIO, FILIAL\n" +
                " FROM GL_ACBATPKG PKG LEFT JOIN GL_USER U ON U.USER_NAME = PKG.USER_LOAD "
                + where;
    }

    @Override
    protected List<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("ID_PKG", Column.Sort.DESC));
        return list;
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
                        ManualOperationWrapper wrapper = new ManualOperationWrapper();
                        wrapper.setPkgId((Long) prms);
                        wrapper.setAction(BatchPostAction.CONTROL);

                        AppUserWrapper appUserWrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
                        wrapper.setUserId(appUserWrapper.getId());

                        BarsGLEntryPoint.operationService.processPackageRq(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<ManualOperationWrapper>>() {
                            @Override
                            public void onSuccess(RpcRes_Base<ManualOperationWrapper> wrapper) {
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

}
