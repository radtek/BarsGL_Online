package ru.rbt.barsgl.gwt.client.operday;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.core.SecurityChecker;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.comp.DataListBox;
import ru.rbt.barsgl.gwt.client.comp.ListBoxSqlDataProvider;
import ru.rbt.barsgl.gwt.client.comp.StringRowConverter;
import ru.rbt.barsgl.gwt.core.actions.RefreshAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.ColumnsBuilder;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.*;
import ru.rbt.barsgl.gwt.core.forms.BaseForm;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.jobs.TimerJobHistoryWrapper;
import ru.rbt.shared.enums.SecurityActionCode;

import java.util.ArrayList;
import java.util.List;

import static com.google.gwt.user.client.ui.HasVerticalAlignment.ALIGN_MIDDLE;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by Ivan Sevastyanov on 25.10.2016.
 */
public class BufferSyncForm extends BaseForm {

    public static final String FORM_NAME = "Сброс буфера";

    private EmbeddableGridWidget grid;
    private DataListBox historiesBox;
    private RefreshAction refreshAction;
    private ListBoxSqlDataProvider listBoxDataProvider;
    private PushButton refreshComboButton;
    private PushButton replicateButton;

    public BufferSyncForm() {
        super();
        title.setText(FORM_NAME);
    }

    @Override
    public Widget createContent() {

        DockLayoutPanel rootPanel = new DockLayoutPanel(Style.Unit.MM);

        grid = createGrid();

        HorizontalPanel hp = new HorizontalPanel();
        hp.setVerticalAlignment(ALIGN_MIDDLE);
        hp.setSpacing(5);
        hp.add(createRefreshComboButton());
        hp.add(historiesBox = createSyncRowsListBox());
        hp.add(createRefreshTabButton());
        hp.add(createExecButton());
        if (SecurityChecker.checkAction(SecurityActionCode.Replication)){
           // hp.add(createReplicateButton());
        }

        rootPanel.addNorth(hp, 12);

        rootPanel.add(grid);

        return rootPanel;
    }

    private DataListBox createSyncRowsListBox() {
        Columns columns = new ColumnsBuilder().addColumn("ID_HIST", Column.Type.LONG).addColumn("ts", Column.Type.STRING).build();
        listBoxDataProvider = new ListBoxSqlDataProvider(true, "",
                "select * from V_GL_LNGTSKINF order by id_hist desc fetch first 10 rows only", columns, null, null, new StringRowConverter(0, 1));
        DataListBox listBox = new DataListBox(listBoxDataProvider);
        listBox.setWidth("600px");
        return listBox;
    }

    private EmbeddableGridWidget createGrid() {
        final Table table = new Table();
        table.addColumn(new Column("ID_STEP", Column.Type.LONG, "ID шага", 100));
        table.addColumn(new Column("STEPNAME", Column.Type.STRING, "Название шага", 150));
        table.addColumn(new Column("RESULT", Column.Type.STRING, "Результат", 100));
        table.addColumn(new Column("DTM_START", Column.Type.DATETIME, "Начало", 120));
        table.addColumn(new Column("DTM_END", Column.Type.DATETIME, "Окончание", 120));
        table.addColumn(new Column("ID_HIST", Column.Type.STRING, "", 0, false, false));
        EmbeddableGridWidget grid = new EmbeddableGridWidget(table, getSqlString());
        return grid;
    }

    private PushButton createRefreshTabButton() {
        refreshAction = createRefreshAction(grid);
        PushButton refresh = new PushButton(new Image(ImageConstants.INSTANCE.refresh_tab()), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                refreshAction.execute();
            }
        });
        refresh.setEnabled(true);
        refresh.setWidth("24px");
        refresh.setHeight("24px");
        refresh.setTitle("Обновить результат");
        refresh.getElement().getStyle().setMarginLeft(10, Style.Unit.PX);
        return refresh;
    }

    private PushButton createRefreshComboButton() {
        refreshComboButton = new PushButton(new Image(ImageConstants.INSTANCE.refresh_simple()), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                historiesBox.setSelectValue(getSelectedHistory().toString());
                listBoxDataProvider.provide(historiesBox);
            }
        });
        refreshComboButton.setEnabled(true);
        refreshComboButton.setWidth("24px");
        refreshComboButton.setHeight("24px");
        refreshComboButton.setTitle("Обновить историю");
        refreshComboButton.getElement().getStyle().setMarginRight(5, Style.Unit.PX);
        return refreshComboButton;
    }

    private PushButton createReplicateButton() {
       replicateButton = new PushButton(new Image(ImageConstants.INSTANCE.copy()), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                DialogManager.confirm("Подтверждение", "Запустить задачу репликации?", "Запустить", new ClickHandler() {
                            @Override
                            public void onClick(ClickEvent clickEvent) {

                                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                                BarsGLEntryPoint.replService.Test(new AuthCheckAsyncCallback<RpcRes_Base<TimerJobHistoryWrapper>>() {
                                    @Override
                                    public void onFailureOthers(Throwable throwable) {
                                        WaitingManager.hide();

                                        Window.alert("Операция не удалась.\nОшибка: " + throwable.getLocalizedMessage());
                                    }

                                    @Override
                                    public void onSuccess(RpcRes_Base<TimerJobHistoryWrapper> res) {
                                        WaitingManager.hide();

                                        if (res.isError()) {
                                            DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + res.getMessage());
                                        } else {
                                            DialogManager.message("Инфо", res.getMessage());
                                        }
                                    }
                                });
                            }
                        });
            }
        });
        replicateButton.setEnabled(true);
        replicateButton.setWidth("24px");
        replicateButton.setHeight("24px");
        replicateButton.setTitle("Репликация");
        replicateButton.getElement().getStyle().setMarginRight(5, Style.Unit.PX);
        return replicateButton;
    }

    private Long getSelectedHistory() {
        if (historiesBox.getSelectedIndex() > 0) {
            return Long.parseLong((String) historiesBox.getValue());
        } else {
            return Long.valueOf(0);
        }
    }

    private String getSqlString() {
        return "select * from V_GL_LNGTSKSTPINF";
    }

    private RefreshAction createRefreshAction(final EmbeddableGridWidget grid) {
        return new RefreshAction(grid) {
            @Override
            public void onRefresh(List<FilterItem> filterCriteria, List<SortItem> sortCriteria,
                                  List<FilterItem> linkMDFilterCriteria) {
                ArrayList<FilterItem> idHist = (ArrayList<FilterItem>) ClientFilterBuilder.create()
                        .addFilterItem(new FilterItem(grid.getTable().getColumn("ID_HIST"), FilterCriteria.EQ, getSelectedHistory())).build();
                grid.setFilter(idHist);
                refreshAction.setFilterCriteria(idHist);
                grid.refresh();
            }
        };
    }

    private PushButton createExecButton () {
        PushButton pushButton = new PushButton(new Image(ImageConstants.INSTANCE.run()), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                DialogManager.confirm("Подтверждение", "Запустить задачу синхронизации полупроводок?", "Запустить"
                        , new ClickHandler() {
                            @Override
                            public void onClick(ClickEvent event) {
                                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                                BarsGLEntryPoint.pdSyncService.execPdSync(new AuthCheckAsyncCallback<RpcRes_Base<TimerJobHistoryWrapper>>() {

                                    @Override
                                    public void onFailureOthers(Throwable throwable) {
                                        WaitingManager.hide();
                                        showInfo("Ошибка при запуске задачи. Проверьте результат выполнения", throwable.getLocalizedMessage());
                                    }

                                    @Override
                                    public void onSuccess(RpcRes_Base<TimerJobHistoryWrapper> result) {
                                        if (!result.isError()) {
                                            historiesBox.setSelectValue(result.getResult().getIdHistory().toString());
                                            listBoxDataProvider.provide(historiesBox);
                                        } else {
                                            DialogManager.error("Ошибка", "Задача не запущена.\nОшибка: " + result.getMessage());
                                        }
                                        WaitingManager.hide();
                                    }
                                });
                            }
                        });
            }
        });
        pushButton.setWidth("24px");
        pushButton.setHeight("24px");
        pushButton.setTitle("Запуск");
        return pushButton;
    }
}
