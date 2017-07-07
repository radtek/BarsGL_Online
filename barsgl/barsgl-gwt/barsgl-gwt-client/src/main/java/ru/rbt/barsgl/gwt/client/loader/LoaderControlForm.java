package ru.rbt.barsgl.gwt.client.loader;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.grid.gwt.client.GridEntryPoint;
import ru.rbt.barsgl.gwt.client.dict.EditableDictionary;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.*;
import ru.rbt.barsgl.gwt.core.dialogs.*;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.GridDataProvider;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.shared.enums.Repository;
import ru.rbt.shared.enums.SecurityActionCode;
import ru.rbt.barsgl.shared.loader.LoadStepWrapper;

import java.util.*;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import ru.rbt.security.gwt.client.CommonEntryPoint;

/**
 * Форма для управления загрузчиком BARSGL/BARSREP
 *
 * Created by SotnikovAV on 20.10.2016.
 */
public abstract class LoaderControlForm extends EditableDictionary<LoadStepWrapper> {

    public final static String FORM_NAME = "Мониторинг и управление загрузкой";

    public static final String COLUMN_ID = "ID";

    public static final String COLUMN_ORDID = "ORDID";

    public static final String COLUMN_DAT = "DAT";

    public static final String COLUMN_CODE = "CODE";

    public static final String COLUMN_NAME = "NAME";

    public static final String COLUMN_ACTION = "ACTION";

    public static final String COLUMN_ACTION_NAME = "ACTION_NAME";

    public static final String COLUMN_ORD = "ORD";

    public static final String COLUMN_STATUS = "STATUS";

    public static final String COLUMN_STATUS_NAME = "STATUS_NAME";

    public static final String COLUMN_STARTTIME = "STARTTIME";

    public static final String COLUMN_ENDTIME = "ENDTIME";

    public static final String COLUMN_EXEC_TIME = "EXEC_TIME";

    public static final String COLUMN_RESULT = "RESULT";

    public static final String COLUMN_RESULT_NAME = "RESULT_NAME";

    public static final String COLUMN_COUNT = "COUNT";

    public static final String COLUMN_MSG = "MSG";

    private Repository repository;

    public LoaderControlForm(Repository repository) {
        super(FORM_NAME);
        this.repository = repository;
        reconfigure();
    }

    private void reconfigure() {
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addSecureAction(editAction(
                new LoadStepControlDlg(repository, LoadStepControlDlg.EDIT, FormAction.UPDATE, grid.getTable().getColumns()),
                "Действие не сохранено.\n Ошибка: ",
                "Ошибка при изменении действия: \n",
                "Действие изменено успешно: "),
                SecurityActionCode.LoaderStepActionChg);
        abw.addSecureAction(createAssignAction(), SecurityActionCode.LoaderStepActionAssign);
        abw.addSecureAction(createDeleteAction(), SecurityActionCode.LoaderStepActionAssign);
        abw.addSecureAction(createApproveAction(), SecurityActionCode.LoaderStepActionApprove);
        abw.addSecureAction(createExecuteAction(), SecurityActionCode.LoaderStepActionExecute);
        abw.addSecureAction(createCancelAction(), SecurityActionCode.LoaderStepActionCancel);
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();
        try {
            Column id = ColumnBuilder.newInstance().name(COLUMN_ID).type(Column.Type.LONG).caption("Идентификатор").width(80).visible(false).readonly(true).filterable(true).build();
            result.addColumn(id);
            Column ordid = ColumnBuilder.newInstance().name(COLUMN_ORDID).type(Column.Type.LONG).caption("Идентификатор набора действий").width(80).visible(false).readonly(true).filterable(true).build();
            result.addColumn(ordid);
            Column action = ColumnBuilder.newInstance().name(COLUMN_ACTION).type(Column.Type.STRING).caption("Код действия")
                    .width(80).visible(false).readonly(false).editable(true).filterable(true)
                    .build();
            result.addColumn(action);
            Column actionName = ColumnBuilder.newInstance().name(COLUMN_ACTION_NAME).type(Column.Type.STRING).caption("Действие")
                    .width(80).visible(true).readonly(false).editable(true).filterable(true)
                    .build();
            result.addColumn(actionName);
            Column status = ColumnBuilder.newInstance().name(COLUMN_STATUS).type(Column.Type.STRING).caption("Код статуса")
                    .width(80).visible(false).readonly(false).editable(true).filterable(true)
                    .build();
            result.addColumn(status);
            Column statusName = ColumnBuilder.newInstance().name(COLUMN_STATUS_NAME).type(Column.Type.STRING).caption("Статус")
                    .width(80).visible(true).readonly(false).editable(true).filterable(true)
                    .build();
            result.addColumn(statusName);
            Column exectionOrder = ColumnBuilder.newInstance().name(COLUMN_ORD).type(Column.Type.STRING).caption("Порядок выполнения").width(80).visible(true).editable(true).readonly(false).filterable(true).build();
            result.addColumn(exectionOrder);
            Column dat = ColumnBuilder.newInstance().name(COLUMN_DAT).type(Column.Type.DATE).caption("Дата").width(80).visible(true).readonly(true).filterable(true).build();
            result.addColumn(dat);
            Column code = ColumnBuilder.newInstance().name(COLUMN_CODE).type(Column.Type.STRING).caption("Шаг").width(80).visible(true).editable(true).filterable(true)
                    .readonly(true).build();
            result.addColumn(code);
            Column name = ColumnBuilder.newInstance().name(COLUMN_NAME).type(Column.Type.STRING).caption("Наименование").width(150).visible(true).editable(true).filterable(true)
                    .readonly(true).build();
            result.addColumn(name);
            Column startTime = ColumnBuilder.newInstance().name(COLUMN_STARTTIME).type(Column.Type.DATETIME).caption("Начало").width(150).visible(true).editable(true).readonly(true).filterable(true).build();
            result.addColumn(startTime);
            Column endTime = ColumnBuilder.newInstance().name(COLUMN_ENDTIME).type(Column.Type.DATETIME).caption("Окончание").width(150).visible(true).editable(true).readonly(true).filterable(true).build();
            result.addColumn(endTime);
            Column execTime = ColumnBuilder.newInstance().name(COLUMN_EXEC_TIME).type(Column.Type.STRING).caption("Время выполнения").width(80).visible(true).editable(true).readonly(true).filterable(true).build();
            result.addColumn(execTime);
            Column res = ColumnBuilder.newInstance().name(COLUMN_RESULT).type(Column.Type.STRING).caption("Флаг результата").width(50).visible(false).editable(true).readonly(true).sortable(false).filterable(true).build();
            result.addColumn(res);
            Column resName = ColumnBuilder.newInstance().name(COLUMN_RESULT_NAME).type(Column.Type.STRING).caption("Результат").width(100).visible(true).editable(true).readonly(true).sortable(false).filterable(true).build();
            result.addColumn(resName);
            Column count = ColumnBuilder.newInstance().name(COLUMN_COUNT).type(Column.Type.INTEGER).caption("Кол-во запусков").width(50).visible(true).editable(true).readonly(true).sortable(false).filterable(true).build();
            result.addColumn(count);
            Column msg = ColumnBuilder.newInstance().name(COLUMN_MSG).type(Column.Type.STRING).caption("Сообщение").width(300).visible(true).editable(true).readonly(true).multiLine(true).sortable(false).filterable(true).build();
            result.addColumn(msg);
        } catch(Exception ex) {
            throw new RuntimeException("Ошибка конфигурирования таблицы", ex);
        }
        return result;
    }

    @Override
    protected String prepareSql() {
        return "SELECT * FROM VW_LOAD_MANAGEMENT";
    }

    @Override
    protected List<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams) {
        List<FilterItem> initFilterList = new ArrayList<>(1);
        initFilterList.add(new FilterItem(getTable().getColumn(COLUMN_DAT), FilterCriteria.EQ, CommonEntryPoint.CURRENT_WORKDAY));
        return initFilterList;
    }

    @Override
    protected List<SortItem> getInitialSortCriteria() {
        List<SortItem> initSortList = new ArrayList<>(2);
        initSortList.add(new SortItem(COLUMN_STARTTIME, Column.Sort.ASC));
        initSortList.add(new SortItem(COLUMN_ENDTIME, Column.Sort.ASC));
        return initSortList;
    }

    @Override
    protected void save(LoadStepWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<LoadStepWrapper>> asyncCallbackImpl) throws Exception {
        BarsGLEntryPoint.loaderService.saveLoadStepAction(cnw, action, asyncCallbackImpl);
    }

    private GridAction createAssignAction(){
        return new GridAction(grid, null, "Назначить", new Image(ImageConstants.INSTANCE.function()), 35) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (null == row) return;

                final Long ordId = (Long)row.getField(1).getValue();
                if(null == ordId || 0L == ordId) {
                    return;
                }

                DialogManager.confirm("Назначение действий на шаги загрузки", "Подтверждаете назначение действий на шаги загрузки?", "Да", "Нет", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                        BarsGLEntryPoint.loaderService.assignActions(repository, ordId, new AuthCheckAsyncCallback<List<LoadStepWrapper>>() {
                            @Override
                            public void onFailureOthers(Throwable throwable) {
                                WaitingManager.hide();
                                Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());
                            }

                            @Override
                            public void onSuccess(List<LoadStepWrapper> loadStepWrappers) {
                                refreshAction.execute();
                                WaitingManager.hide();
                            }
                        });
                    }
                }, null);
            }
        };
    }

    private GridAction createApproveAction(){
        return new GridAction(grid, null, "Согласовать", new Image(ImageConstants.INSTANCE.sign()), 35) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (null == row) return;

                final Long ordId = (Long)row.getField(1).getValue();
                if(null == ordId || 0L == ordId) {
                    return;
                }

                DialogManager.confirm("Согласование действий, назначенных на шаги загрузки", "Согласуете действия, назначенные на шаги загрузки?", "Да", "Нет", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                        BarsGLEntryPoint.loaderService.approveActions(repository, ordId, new AuthCheckAsyncCallback<List<LoadStepWrapper>>() {
                            @Override
                            public void onFailureOthers(Throwable throwable) {
                                WaitingManager.hide();
                                Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());
                            }

                            @Override
                            public void onSuccess(List<LoadStepWrapper> loadStepWrappers) {
                                refreshAction.execute();
                                WaitingManager.hide();
                            }
                        });
                    }
                }, null);
            }
        };
    }

    private GridAction createExecuteAction(){
        return new GridAction(grid, null, "Выполнить", new Image(ImageConstants.INSTANCE.run()), 35) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (null == row) return;

                final Long ordId = (Long)row.getField(1).getValue();
                if(null == ordId || 0L == ordId) {
                    return;
                }

                DialogManager.confirm("Выполнение действий, назначенных на шаги загрузки", "Выполнить действия, назначенные на шаги загрузки?", "Да", "Нет", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                        BarsGLEntryPoint.loaderService.executeActions(repository, ordId, new AuthCheckAsyncCallback<List<LoadStepWrapper>>() {
                            @Override
                            public void onFailureOthers(Throwable throwable) {
                                WaitingManager.hide();
                                Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());
                            }

                            @Override
                            public void onSuccess(List<LoadStepWrapper> loadStepWrappers) {
                                refreshAction.execute();
                                WaitingManager.hide();
                            }
                        });
                    }
                }, null);
            }
        };
    }

    private GridAction createCancelAction(){
        return new GridAction(grid, null, "Отменить", new Image(ImageConstants.INSTANCE.stop_all()), 35) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (null == row) return;

                final Long ordId = (Long)row.getField(1).getValue();
                if(null == ordId || 0L == ordId) {
                    return;
                }

                DialogManager.confirm("Отмена действий, назначенных на шаги загрузки", "Отменить действия, назначенные на шаги загрузки?", "Да", "Нет", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                        BarsGLEntryPoint.loaderService.cancelActions(repository, ordId, new AuthCheckAsyncCallback<List<LoadStepWrapper>>() {
                            @Override
                            public void onFailureOthers(Throwable throwable) {
                                WaitingManager.hide();
                                Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());
                            }

                            @Override
                            public void onSuccess(List<LoadStepWrapper> loadStepWrappers) {
                                refreshAction.execute();
                                WaitingManager.hide();
                            }
                        });
                    }
                }, null);
            }
        };
    }

    private GridAction createDeleteAction(){
        return new GridAction(grid, null, "Удалить все действия", new Image(ImageConstants.INSTANCE.stop()), 35) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (null == row) return;

                final Long ordId = (Long)row.getField(1).getValue();
                if(null == ordId || 0L == ordId) {
                    return;
                }

                DialogManager.confirm("Удалить действий, назначенных на шаги загрузки", "Удалить действия, назначенные на шаги загрузки?", "Да", "Нет", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                        BarsGLEntryPoint.loaderService.deleteActions(repository, ordId, new AuthCheckAsyncCallback<List<LoadStepWrapper>>() {
                            @Override
                            public void onFailureOthers(Throwable throwable) {
                                WaitingManager.hide();
                                Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());
                            }

                            @Override
                            public void onSuccess(List<LoadStepWrapper> loadStepWrappers) {
                                refreshAction.execute();
                                WaitingManager.hide();
                            }
                        });
                    }
                }, null);
            }
        };
    }

    @Override
    protected GridWidget createGrid(final Object[] initialFilterParams, final boolean delayLoad) {
        return new GridWidget(table, new GridDataProvider(delayLoad) {
            @Override
            protected void getServerCount(AsyncCallback<Integer> callback) {
                GridEntryPoint.asyncGridService.getAsyncCount(repository, sql_select, getFilterCriteria(initialFilterParams), callback);
            }

            @Override
            protected void getServerData(int start, int pageSize, AsyncCallback<List<Row>> callback) {
                List<SortItem> sortItems = getSortCriteria();
                List<FilterItem> filterItems = getFilterCriteria(initialFilterParams);
                refreshGridParams(filterItems, sortItems);
                GridEntryPoint.asyncGridService.getAsyncRows(repository, sql_select, table.getColumns(), start, pageSize,
                        filterItems, sortItems, callback);
            };
        }, 30);
    }
}
