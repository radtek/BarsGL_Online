package ru.rbt.tasks.gwt.client.tasks;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Field;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.ICellValueEvent;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.enums.JobSchedulingType;
import ru.rbt.barsgl.shared.enums.JobStartupType;
import ru.rbt.barsgl.shared.jobs.TimerJobWrapper;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.shared.enums.SecurityActionCode;
import ru.rbt.tasks.gwt.client.TimerEntryPoint;

import java.util.ArrayList;
import java.util.List;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by Ivan Sevastyanov
 */
public class TasksFormNew extends GridForm {
    public final static String FORM_NAME = "Задания";

    public TasksFormNew() {
        super(FORM_NAME);
        reconfigure();
        grid.setCellValueEventHandler(new ICellValueEvent() {
            @Override
            public String getDisplayValue(String name, Field field, String defValue) {
                if (name.equalsIgnoreCase("STR_TYPE")) {
                    return JobStartupType.valueOf(field.getValue().toString()).getLabel();
                } else
                    return defValue;
            }
        });

    }

    private void reconfigure() {
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addSecureAction(createEditAction(), SecurityActionCode.TasksChng);
        abw.addSecureAction(createRunAction(), SecurityActionCode.TasksRun);
        abw.addSecureAction(createStopAction(), SecurityActionCode.TasksRun);

        abw.addSecureAction(createRunAllAction(), SecurityActionCode.TasksRun);
        abw.addSecureAction(createStopAllAction(), SecurityActionCode.TasksRun);

        abw.addSecureAction(createRunOnceAction(), SecurityActionCode.TasksRun);
        abw.addAction(createFlushCache());
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();
        Column col;
        result.addColumn(new Column("ID_TASK", Column.Type.LONG, "ID", 150, false, true ));
        result.addColumn(new Column("TSKNM", Column.Type.STRING, "Наименование", 250));
        result.addColumn(col = new Column("PROPS", Column.Type.STRING, "Свойства", 300));
        col.setSortable(false);
        result.addColumn(col = new Column("DESCR", Column.Type.STRING, "Описание", 350));
        col.setSortable(false);
        result.addColumn(new Column("STATE", Column.Type.STRING, "Статус", 120));
        result.addColumn(new Column("STR_TYPE", Column.Type.STRING, "Тип запуска", 200));
        result.addColumn(new Column("SCH_TYPE", Column.Type.STRING, "Тип задания", 120));
        result.addColumn(col = new Column("SCH_EXPR", Column.Type.STRING, "Расписание", 250));
        col.setSortable(false);

        return result;
    }

    @Override
    protected String prepareSql() {
        return "SELECT * FROM GL_SCHED";
    }


    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
        return null;
    }

    private GridAction createEditAction(){
        return new GridAction(grid, null, "Править", new Image(ImageConstants.INSTANCE.edit24()), 2)  {

            private TaskFormDlg dlg;

            @Override
            public void execute() {
                Row row = grid.getCurrentRow();
                if (row == null) return;

                TimerJobWrapper timerJobs = new TimerJobWrapper();

                timerJobs.setId((Long) row.getField(0).getValue());
//                timerJobs.setId(((BigDecimal) row.getField(0).getValue()).longValueExact());
                timerJobs.setProperties((String)row.getField(2).getValue());

                timerJobs.setStartupType(JobStartupType.valueOf(row.getField(5).getValue().toString()));
                timerJobs.setJobType(JobSchedulingType.valueOf(row.getField(6).getValue().toString()));
                timerJobs.setScheduleExpression((String)row.getField(7).getValue());

                dlg = new TaskFormDlg();
                dlg.setDlgEvents(this);
                dlg.show(timerJobs);
            }

            @Override
            public void onDlgOkClick(Object prms){
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                TimerJobWrapper timerJobWrapper = (TimerJobWrapper) prms;
                TimerEntryPoint.timerJobService.save(timerJobWrapper, new AuthCheckAsyncCallback<List<TimerJobWrapper>>() {
                    @Override
                    public void onFailureOthers(Throwable throwable) {
                        WaitingManager.hide();

                        Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());
                    }

                    @Override
                    public void onSuccess(List<TimerJobWrapper> timerJobWrappers) {
                        dlg.hide();

                        refreshAction.execute();

                        WaitingManager.hide();
                    }
                });
            }
        };
    }

    private GridAction createRunAction(){
        return new GridAction(grid, null, "Запустить", new Image(ImageConstants.INSTANCE.run()), 35) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                DialogManager.confirm("Задачи", Utils.Fmt("Подтверждаете запуск задачи {0} ?", (String) row.getField(1).getValue()), "OK", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

//                        TimerEntryPoint.timerJobService.startupJob(((BigDecimal) row.getField(0).getValue()).longValueExact(), new AuthCheckAsyncCallback<List<TimerJobWrapper>>() {
                        TimerEntryPoint.timerJobService.startupJob((Long) row.getField(0).getValue(), new AuthCheckAsyncCallback<List<TimerJobWrapper>>() {
                            @Override
                            public void onFailureOthers(Throwable throwable) {
                                WaitingManager.hide();

                                Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());
                            }

                            @Override
                            public void onSuccess(List<TimerJobWrapper> timerJobWrappers) {
                                refreshAction.execute();

                                WaitingManager.hide();
                            }
                        });
                    }
                });
            }
        };
    }

    private GridAction createStopAction(){
        return new GridAction(grid, null, "Остановить", new Image(ImageConstants.INSTANCE.stop()), 2) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                DialogManager.confirm("Задачи", Utils.Fmt("Подтверждаете остановку задачи {0} ?", (String) row.getField(1).getValue()), "OK", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                        TimerEntryPoint.timerJobService.shutdownJob((Long) row.getField(0).getValue(), new AuthCheckAsyncCallback<List<TimerJobWrapper>>() {
                            @Override
                            public void onFailureOthers(Throwable throwable) {
                                WaitingManager.hide();

                                Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());
                            }

                            @Override
                            public void onSuccess(List<TimerJobWrapper> timerJobWrappers) {
                                refreshAction.execute();

                                WaitingManager.hide();
                            }
                        });
                    }
                });
            }
        };
    }

    private GridAction createRunAllAction(){
        return new GridAction(grid, null, "Запустить все", new Image(ImageConstants.INSTANCE.run_all()), 5) {
            @Override
            public void execute() {
                Row row = grid.getCurrentRow();
                if (row == null) return;

                DialogManager.confirm("Задачи", "Подтверждаете запуск всех задач ?", "OK", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                        TimerEntryPoint.timerJobService.startupAll(new AuthCheckAsyncCallback<List<TimerJobWrapper>>() {
                            @Override
                            public void onFailureOthers(Throwable throwable) {
                                WaitingManager.hide();

                                Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());
                            }

                            @Override
                            public void onSuccess(List<TimerJobWrapper> timerJobWrappers) {
                                refreshAction.execute();

                                WaitingManager.hide();
                            }
                        });
                    }
                });
            }
        };
    }

    private GridAction createStopAllAction(){
        return new GridAction(grid, null, "Остановить все", new Image(ImageConstants.INSTANCE.stop_all()), 2) {
            @Override
            public void execute() {
                Row row = grid.getCurrentRow();
                if (row == null) return;

                DialogManager.confirm("Задачи", "Подтверждаете остановку всех задач ?", "OK", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                        TimerEntryPoint.timerJobService.shutdownAll(new AuthCheckAsyncCallback<List<TimerJobWrapper>>() {
                            @Override
                            public void onFailureOthers(Throwable throwable) {
                                WaitingManager.hide();

                                Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());
                            }

                            @Override
                            public void onSuccess(List<TimerJobWrapper> timerJobWrappers) {
                                refreshAction.execute();

                                WaitingManager.hide();
                            }
                        });
                    }
                });
            }
        };
    }

    private GridAction createRunOnceAction(){
        return new GridAction(grid, null, "Запустить принудительно", new Image(ImageConstants.INSTANCE.run_once()), 10) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                DialogManager.confirm("Задачи", Utils.Fmt("Подтверждаете принудительный запуск задачи {0} ?", (String) row.getField(1).getValue()), "OK", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                        TimerEntryPoint.timerJobService.executeJob((Long) row.getField(0).getValue(), new AuthCheckAsyncCallback<List<TimerJobWrapper>>() {
                            @Override
                            public void onFailureOthers(Throwable throwable) {
                                WaitingManager.hide();

                                Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());
                            }

                            @Override
                            public void onSuccess(List<TimerJobWrapper> timerJobWrappers) {
                                refreshAction.execute();

                                WaitingManager.hide();
                            }
                        });
                    }
                });
            }
        };
    }
    private GridAction createFlushCache() {
        return new GridAction(grid, null, "Очистить кэш", new Image(ImageConstants.INSTANCE.clean_cache()), 10) {

            @Override
            public void execute() {
                DialogManager.confirm("Кэш", "Очистить кэш?", "OK", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                        TimerEntryPoint.timerJobService.flushCache(new AuthCheckAsyncCallback() {
                            @Override
                            public void onSuccess(Object result) {
                                WaitingManager.hide();
                                DialogManager.message("Информация", "Сброс кэша прошел успешно");
                            }

                        });
                    }
                });

            }
        };
    }
}
