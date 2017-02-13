package ru.rbt.barsgl.gwt.client.tasks;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Field;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.forms.BaseForm;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.ActionBarWidget;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.gwt.core.widgets.ICellValueEvent;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.enums.JobSchedulingType;
import ru.rbt.barsgl.shared.enums.JobStartupType;
import ru.rbt.barsgl.shared.jobs.TimerJobWrapper;

import java.util.List;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by akichigi on 10.03.15.
 * @deprecated форма не используется. Баг с постраничным выводом. Вылеживается...
 */
public class TasksForm extends BaseForm {

    private GridWidget grid;
    private Table table;

    private GridAction refreshAction;
    private GridAction editAction;
    private GridAction runOnceAction;
    private GridAction runAction;
    private GridAction runAllAction;
    private GridAction stopAction;
    private GridAction stopAllAction;

    public TasksForm() {
       super();
       //FormManagerUI.ChangeStatusBarText("", FormManagerUI.MessageReason.MSG);
       title.setText("Задания");
    }

    @Override
    public Widget createContent() {
        return makeContent();
    }

    private Table prepareTable() {
        Table result = new Table();
        Column col;
        result.addColumn(col = new Column("ID_TASK", Column.Type.LONG, "ID", 150, false, true ));
        col.setSortable(false);
        result.addColumn(col = new Column("TSKNM", Column.Type.STRING, "Наименование", 250));
        col.setSortable(false);
        result.addColumn(col = new Column("PROPS", Column.Type.STRING, "Свойства", 300));
        col.setSortable(false);
        result.addColumn(col = new Column("DESCR", Column.Type.STRING, "Описание", 350));
        col.setSortable(false);
        result.addColumn(col = new Column("STATE", Column.Type.STRING, "Статус", 120));
        col.setSortable(false);
        result.addColumn(col = new Column("STR_TYPE", Column.Type.STRING, "Тип запуска", 200));
        col.setSortable(false);
        result.addColumn(col = new Column("SCH_TYPE", Column.Type.STRING, "Тип задания", 120));
        col.setSortable(false);
        result.addColumn(col = new Column("SCH_EXPR", Column.Type.STRING, "Расписанпие", 250));
        col.setSortable(false);

        return result;
    }

    private void gridRefresh(List<TimerJobWrapper> timerJobWrappers)
    {
        table.removeAll();

        for (TimerJobWrapper wrapper : timerJobWrappers){
            Row row = new Row();
            row.addField(new Field(wrapper.getId()));
            row.addField(new Field(wrapper.getName()));
            row.addField(new Field(wrapper.getProperties()));
            row.addField(new Field(wrapper.getDescription()));
            row.addField(new Field(wrapper.getState()));
            row.addField(new Field(wrapper.getStartupType().toString()));
            row.addField(new Field(wrapper.getJobType().toString()));
            row.addField(new Field(wrapper.getScheduleExpression()));
            table.addRow(row);
        }
        grid.refresh(table);
    }


    private GridAction createRefreshAction(){
        return refreshAction = new GridAction(grid, null, "Обновить", new Image(ImageConstants.INSTANCE.refresh24()), 5) {
            @Override
            public void execute() {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                BarsGLEntryPoint.timerJobService.getAllJobs(new AuthCheckAsyncCallback<List<TimerJobWrapper>>() {
                    @Override
                    public void onFailureOthers(Throwable throwable) {
                        WaitingManager.hide();
                        Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());
                    }

                    @Override
                    public void onSuccess(List<TimerJobWrapper> timerJobWrappers) {
                        gridRefresh(timerJobWrappers);
                        WaitingManager.hide();
                    }
                });
            }
        };
    }

    private GridAction createEditAction(){
        return editAction = new GridAction(grid, null, "Править", new Image(ImageConstants.INSTANCE.edit24()), 2)  {

            private TaskFormDlg dlg;

            @Override
            public void execute() {
                Row row = grid.getCurrentRow();
                if (row == null) return;

                TimerJobWrapper timerJobs = new TimerJobWrapper();

                timerJobs.setId((Long) row.getField(0).getValue());
                timerJobs.setProperties((String)row.getField(2).getValue());

                timerJobs.setStartupType(JobStartupType.valueOf(row.getField(5).getValue().toString()));
                timerJobs.setJobType(JobSchedulingType.valueOf(row.getField(6).getValue().toString()));
                timerJobs.setScheduleExpression((String)row.getField(7).getValue());

                dlg = new TaskFormDlg();
                dlg.setDlgEvents(this);
               /*
                    //for cache form
                    if (dlg == null) {
                    dlg = new TaskFormDlg2();
                    dlg.setDlgEvents(this);
                }*/

                dlg.show(timerJobs);
            }

            @Override
            public void onDlgOkClick(Object prms){
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                TimerJobWrapper timerJobWrapper = (TimerJobWrapper) prms;
                BarsGLEntryPoint.timerJobService.save(timerJobWrapper, new AuthCheckAsyncCallback<List<TimerJobWrapper>>() {
                    @Override
                    public void onFailureOthers(Throwable throwable) {
                        WaitingManager.hide();

                        Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());
                    }

                    @Override
                    public void onSuccess(List<TimerJobWrapper> timerJobWrappers) {
                        dlg.hide();

                        gridRefresh(timerJobWrappers);

                        WaitingManager.hide();
                    }
                });
            }
        };
    }

    private GridAction createRunAction(){
        return runAction = new GridAction(grid, null, "Запустить", new Image(ImageConstants.INSTANCE.run()), 35) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                DialogManager.confirm("Задачи", Utils.Fmt("Подтверждаете запуск задачи {0} ?", (String) row.getField(1).getValue()), "OK", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                        BarsGLEntryPoint.timerJobService.startupJob((Long) row.getField(0).getValue(), new AuthCheckAsyncCallback<List<TimerJobWrapper>>() {
                            @Override
                            public void onFailureOthers(Throwable throwable) {
                                WaitingManager.hide();

                                Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());
                            }

                            @Override
                            public void onSuccess(List<TimerJobWrapper> timerJobWrappers) {
                                gridRefresh(timerJobWrappers);

                                WaitingManager.hide();
                            }
                        });
                    }
                });
            }
        };
    }

    private GridAction createStopAction(){
        return stopAction = new GridAction(grid, null, "Остановить", new Image(ImageConstants.INSTANCE.stop()), 2) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                DialogManager.confirm("Задачи", Utils.Fmt("Подтверждаете остановку задачи {0} ?", (String) row.getField(1).getValue()), "OK", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                        BarsGLEntryPoint.timerJobService.shutdownJob((Long) row.getField(0).getValue(), new AuthCheckAsyncCallback<List<TimerJobWrapper>>() {
                            @Override
                            public void onFailureOthers(Throwable throwable) {
                                WaitingManager.hide();

                                Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());
                            }

                            @Override
                            public void onSuccess(List<TimerJobWrapper> timerJobWrappers) {
                                gridRefresh(timerJobWrappers);

                                WaitingManager.hide();
                            }
                        });
                    }
                });
            }
        };
    }

    private GridAction createRunAllAction(){
        return runAllAction = new GridAction(grid, null, "Запустить все", new Image(ImageConstants.INSTANCE.run_all()), 5) {
            @Override
            public void execute() {
                Row row = grid.getCurrentRow();
                if (row == null) return;

                DialogManager.confirm("Задачи", "Подтверждаете запуск всех задач ?", "OK", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                        BarsGLEntryPoint.timerJobService.startupAll(new AuthCheckAsyncCallback<List<TimerJobWrapper>>() {
                            @Override
                            public void onFailureOthers(Throwable throwable) {
                                WaitingManager.hide();

                                Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());
                            }

                            @Override
                            public void onSuccess(List<TimerJobWrapper> timerJobWrappers) {
                                gridRefresh(timerJobWrappers);

                                WaitingManager.hide();
                            }
                        });
                    }
                });
            }
        };
    }

    private GridAction createStopAllAction(){
        return stopAllAction = new GridAction(grid, null, "Остановить все", new Image(ImageConstants.INSTANCE.stop_all()), 2) {
            @Override
            public void execute() {
                Row row = grid.getCurrentRow();
                if (row == null) return;

                DialogManager.confirm("Задачи", "Подтверждаете остановку всех задач ?", "OK", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                        BarsGLEntryPoint.timerJobService.shutdownAll(new AuthCheckAsyncCallback<List<TimerJobWrapper>>() {
                            @Override
                            public void onFailureOthers(Throwable throwable) {
                                WaitingManager.hide();

                                Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());
                            }

                            @Override
                            public void onSuccess(List<TimerJobWrapper> timerJobWrappers) {
                                gridRefresh(timerJobWrappers);

                                WaitingManager.hide();
                            }
                        });
                    }
                });
            }
        };
    }

    private GridAction createRunOnceAction(){
        return runOnceAction = new GridAction(grid, null, "Запустить принудительно", new Image(ImageConstants.INSTANCE.run_once()), 10) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                DialogManager.confirm("Задачи", Utils.Fmt("Подтверждаете принудительный запуск задачи {0} ?", (String) row.getField(1).getValue()), "OK", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                        BarsGLEntryPoint.timerJobService.executeJob((Long) row.getField(0).getValue(), new AuthCheckAsyncCallback<List<TimerJobWrapper>>() {
                            @Override
                            public void onFailureOthers(Throwable throwable) {
                                WaitingManager.hide();

                                Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());
                            }

                            @Override
                            public void onSuccess(List<TimerJobWrapper> timerJobWrappers) {
                                gridRefresh(timerJobWrappers);

                                WaitingManager.hide();
                            }
                        });
                    }
                });
            }
        };
    }

    private Widget makeContent(){
        table = prepareTable();

        grid = new GridWidget(table);

        grid.setCellValueEventHandler(new ICellValueEvent() {
            @Override
            public String getDisplayValue(String name, Field field, String defValue) {
                if (name.equalsIgnoreCase("STR_TYPE")) {
                    return JobStartupType.valueOf(field.getValue().toString()).getLabel();
                } else
                    return defValue;
            }
        });

        ActionBarWidget abw = new ActionBarWidget();

        abw.addAction(createRefreshAction());
        refreshAction.execute();

        abw.addAction(createEditAction());

        abw.addAction(createRunAction());
        abw.addAction(createStopAction());

        abw.addAction(createRunAllAction());
        abw.addAction(createStopAllAction());

        abw.addAction(createRunOnceAction());

        DockLayoutPanel panel = new DockLayoutPanel(Style.Unit.MM);

        panel.addNorth(abw, 10);
        panel.add(grid);

        return panel;
    }
}
