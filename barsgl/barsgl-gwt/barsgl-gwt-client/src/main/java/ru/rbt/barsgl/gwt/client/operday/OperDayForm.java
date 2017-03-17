package ru.rbt.barsgl.gwt.client.operday;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.core.actions.Action;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.forms.BaseForm;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.ActionBarWidget;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.cob.CobWrapper;
import ru.rbt.barsgl.shared.enums.OperDayButtons;
import ru.rbt.barsgl.shared.enums.SecurityActionCode;
import ru.rbt.barsgl.shared.jobs.TimerJobHistoryWrapper;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by akichigi on 20.03.15.
 */

public class OperDayForm extends BaseForm {
    public final static String FORM_NAME = "Операционный день";

    private Action refreshAction;
    private Action open_OD;
    private Action close_Balance_Previous_OD;
    private Action change_Phase_To_PRE_COB;
    private Action switchPdMode;

    private Label currentOD;
    private Label phaseCurrentOD;
    private Label previousOD;
    private Label previousODBalanceStatus;
    private Label pdMode;

    public OperDayForm(){
        super();

        title.setText(FORM_NAME);
    }

    @Override
    public Widget createContent() {
        return getContent();
    }

    public Widget getContent(){
        Label label;
        Grid grid = new Grid(5, 2);

        grid.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
        grid.getElement().getStyle().setMarginTop(10, Style.Unit.PX);

        grid.setWidget(0, 0, label = new Label("Текущий ОД:"));
        label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);

        grid.setWidget(1, 0, label = new Label("Фаза текущего ОД:"));
        label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);

        grid.setWidget(2, 0, label = new Label("Предыдущий ОД:"));
        label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);

        grid.setWidget(3, 0, label = new Label("Статус баланса предыдущего ОД:"));
        label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);

        grid.setWidget(4, 0, label = new Label("Режим обработки проводок:"));
        label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);

        grid.setWidget(0, 1, currentOD = new Label(""));
        grid.setWidget(1, 1, phaseCurrentOD = new Label(""));
        grid.setWidget(2, 1, previousOD = new Label(""));
        grid.setWidget(3, 1, previousODBalanceStatus = new Label(""));
        grid.setWidget(4, 1, pdMode = new Label(""));

        grid.getCellFormatter().setWidth(0, 0, "285px");

        ActionBarWidget abw = new ActionBarWidget();
        abw.addAction(createRefreshAction());

        abw.addSecureAction(createOpenODAction(), SecurityActionCode.TskOdOpenRun);
        abw.addSecureAction(createCloseBalancePreviousODAction(), SecurityActionCode.TskOdBalCloseRun);
        abw.addSecureAction(createChangePhaseToPRE_COBAction(), SecurityActionCode.TskOdPreCobRun);
        abw.addSecureAction(createSwitchPdMode(), SecurityActionCode.TskOdSwitchModeRun);
        abw.addSecureAction(createMonitoring(), SecurityActionCode.TskOdPreCobRun);

        abw.addSecureAction(createFakeCOB(), SecurityActionCode.TskOdPreCobRun);

        refreshAction.execute();

        DockLayoutPanel panel = new DockLayoutPanel(Style.Unit.MM);

        panel.addNorth(abw, 10);
        panel.add(grid);

        return panel;
    }

    private void operDateRefresh(OperDayWrapper operDayWrapper){
        currentOD.setText(operDayWrapper.getCurrentOD());
        phaseCurrentOD.setText(operDayWrapper.getPhaseCurrentOD());
        previousOD.setText(operDayWrapper.getPreviousOD());
        previousODBalanceStatus.setText(operDayWrapper.getPreviousODBalanceStatus());
        pdMode.setText(operDayWrapper.getPdMode());

        setButtonsEnabled(operDayWrapper.getEnabledButton());
    }

    private void setButtonsEnabled(OperDayButtons button){
        open_OD.setEnable(button == OperDayButtons.OPEN_OD);
        close_Balance_Previous_OD.setEnable(button == OperDayButtons.CLOSE_BALANCE_PREVIOUS_OD);
        change_Phase_To_PRE_COB.setEnable(button == OperDayButtons.CHANGE_PHASE_TO_PRE_COB);
        switchPdMode.setEnable(true);
    }

    private Action createRefreshAction(){
        return refreshAction = new Action(null, "Обновить", new Image(ImageConstants.INSTANCE.refresh24()), 5) {
            @Override
            public void execute() {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                BarsGLEntryPoint.operDayService.getOperDay(new AuthCheckAsyncCallback<RpcRes_Base<OperDayWrapper>>() {
                    @Override
                    public void onFailureOthers(Throwable throwable) {
                        WaitingManager.hide();

                        Window.alert("Операция не удалась.\nОшибка: " + throwable.getLocalizedMessage());
                    }

                    @Override
                    public void onSuccess(RpcRes_Base<OperDayWrapper> res) {
                        if (res.isError()) {
                            DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + res.getMessage());
                        } else {
                            operDateRefresh(res.getResult());
                        }
                        WaitingManager.hide();
                    }
                });
            }
        };
    }

    private Action createOpenODAction(){
        return open_OD = new Action("Открытие ОД", "", null, 5) {
            @Override
            public void execute() {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                BarsGLEntryPoint.operDayService.runOpenOperdayTask(new AuthCheckAsyncCallback<RpcRes_Base<Boolean>>() {
                    @Override
                    public void onFailureOthers(Throwable throwable) {
                        WaitingManager.hide();

                        Window.alert("Операция не удалась.\nОшибка: " + throwable.getLocalizedMessage());
                    }

                    @Override
                    public void onSuccess(RpcRes_Base<Boolean> res) {
                        WaitingManager.hide();

                        if (res.isError()) {
                            DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + res.getMessage());
                        } else {
                            open_OD.setEnable(false);
                            DialogManager.message("Инфо", "Задание 'Открытие ОД' выполнено.\n" +
                                    "Для обновления информации нажмите 'Обновить'.");
                        }
                    }
                });
            }
        };
    }

    private Action createCloseBalancePreviousODAction(){
        return close_Balance_Previous_OD = new Action("Закрытие баланса предыдущего ОД", "", null, 5) {
            @Override
            public void execute() {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                BarsGLEntryPoint.operDayService.runCloseLastWorkdayBalanceTask(new AuthCheckAsyncCallback<RpcRes_Base<Boolean>>() {
                    @Override
                    public void onFailureOthers(Throwable throwable) {
                        WaitingManager.hide();

                        Window.alert("Операция не удалась.\nОшибка: " + throwable.getLocalizedMessage());
                    }

                    @Override
                    public void onSuccess(RpcRes_Base<Boolean> res) {
                        WaitingManager.hide();

                        if (res.isError()) {
                            DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + res.getMessage());
                        } else {
                            close_Balance_Previous_OD.setEnable(false);
                            DialogManager.message("Инфо", "Задание 'Закрытие баланса предыдущего ОД' выполнено.\n" +
                                    "Для обновления информации нажмите 'Обновить'. ");
                        }
                    }
                });
            }
        };
    }

    private Action createChangePhaseToPRE_COBAction(){
        return change_Phase_To_PRE_COB = new Action("Перевод фазы в PRE_COB", "", null, 5) {
            @Override
            public void execute() {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                BarsGLEntryPoint.operDayService.runExecutePreCOBTask(new AuthCheckAsyncCallback<RpcRes_Base<Boolean>>() {
                    @Override
                    public void onFailureOthers(Throwable throwable) {
                        WaitingManager.hide();

                        Window.alert("Операция не удалась.\nОшибка: " + throwable.getLocalizedMessage());
                    }

                    @Override
                    public void onSuccess(RpcRes_Base<Boolean> res) {
                        WaitingManager.hide();

                        if (res.isError()) {
                            DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + res.getMessage());
                        } else {
                            change_Phase_To_PRE_COB.setEnable(false);
                            DialogManager.message("Инфо", "Задание 'Перевод фазы в PRE_COB' выполнено.\n" +
                                    "Для обновления информации нажмите 'Обновить'.");
                        }
                    }
                });
            }
        };
    }

    private Action createSwitchPdMode() {
        return switchPdMode = new Action("Переключение режима загрузки", "", null, 5) {
            @Override
            public void execute() {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                BarsGLEntryPoint.operDayService.swithPdMode(new AuthCheckAsyncCallback<RpcRes_Base<OperDayWrapper>>() {

                    @Override
                    public void onSuccess(RpcRes_Base<OperDayWrapper> res) {
                        WaitingManager.hide();

                        if (res.isError()) {
                            DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + res.getMessage());
                        } else {
                            pdMode.setText(res.getResult().getPdMode());
                            DialogManager.message("Инфо", "Режим обработки проводок изменен.\n" +
                                    "Для обновления информации нажмите 'Обновить'.");
                        }
                    }
                });
            }
        };
    }

   private Action createMonitoring(){
       return new Action(null, "Мониторинг COB", new Image(ImageConstants.INSTANCE.display()), 5){
           COBMonitoringDlg dlg = null;
           @Override
           public void execute() {
               WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

               BarsGLEntryPoint.operDayService.getCobInfo(null, new AuthCheckAsyncCallback<RpcRes_Base<CobWrapper>>() {
                   @Override
                   public void onSuccess(RpcRes_Base<CobWrapper> result) {
                       if (result.isError()) {
                           DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + result.getMessage());
                       } else {
                           (dlg = dlg == null ? new COBMonitoringDlg() : dlg).show(result.getResult());
                       }
                       WaitingManager.hide();
                   }
               });
           }
       };
   }

    private Action createFakeCOB(){
        return new Action("Fake COB", "", null, 5){
            COBMonitoringDlg dlg = null;
            @Override
            public void execute() {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                BarsGLEntryPoint.operDayService.runExecuteFakeCOBTask(new AuthCheckAsyncCallback<RpcRes_Base<TimerJobHistoryWrapper>>() {
                    @Override
                    public void onSuccess(RpcRes_Base<TimerJobHistoryWrapper> result) {
                        if (result.isError()) {
                            DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + result.getMessage());
                        } else {
                            Window.alert(result.getResult().toString());
                        }
                        WaitingManager.hide();
                    }
                });
            }
        };
    }
}
