package ru.rbt.barsgl.gwt.client.operday;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.core.SecurityChecker;
import ru.rbt.barsgl.gwt.core.dialogs.IDlgEvents;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.shared.enums.AccessMode;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;
import ru.rbt.barsgl.shared.operday.LwdBalanceCutWrapper;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.grid.gwt.client.export.Export2Excel;
import ru.rbt.grid.gwt.client.export.ExportActionCallback;
import ru.rbt.barsgl.gwt.core.actions.Action;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterCancelEvent;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.forms.BaseForm;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.utils.UUID;
import ru.rbt.barsgl.gwt.core.widgets.ActionBarWidget;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.cob.CobWrapper;
import ru.rbt.barsgl.shared.enums.OperDayButtons;
import ru.rbt.security.gwt.client.monitoring.HPanel;
import ru.rbt.shared.enums.SecurityActionCode;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showConfirm;

import ru.rbt.barsgl.shared.jobs.TimerJobHistoryWrapper;
import ru.rbt.barsgl.shared.operday.COB_OKWrapper;
import ru.rbt.security.gwt.client.CommonEntryPoint;

import java.util.logging.Logger;

/**
 * Created by akichigi on 20.03.15.
 */

public class OperDayForm extends BaseForm {
    public final static String FORM_NAME = "Операционный день";
    public final static String REST_RADIO = "restGroup";

    private Action refreshAction;
    private Action open_OD;
    private Action change_Phase_To_PRE_COB;
    private Action switchPdMode;
    private Action autoCloseAction;

    private Label currentOD;
    private Label phaseCurrentOD;
    private Label previousOD;
    private Label previousODBalanceStatus;
    private Label pdMode;
    private Label acm;

    private Timer timer;
    private Label lbProcessStatus;
    private Button btChangeProcessStatus;
    private ProcessingStatus currentChangeProcessStatus = ProcessingStatus.STARTED;
    private final int tick = 2000;
    private boolean isStatusSet = false;
    private HPanel panelProcessStatus, panelRefreshRest;

    private Label reason;
    private Grid vip_errors;
    private Label vip;
    private Label not_vip;

    private Label dateCB;
    private Label dateOD;
    private Label timeClose;
    private LwdBalanceCutWrapper lwdBalanceCutWrapper;
    private AccessMode accessMode;

    static Logger log = Logger.getLogger("OperDayForm");

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
        Grid grid = new Grid(6, 2);

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

        grid.setWidget(5, 0, label = new Label("Режим доступа:"));
        label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);

        grid.setWidget(0, 1, currentOD = new Label(""));
        grid.setWidget(1, 1, phaseCurrentOD = new Label(""));
        grid.setWidget(2, 1, previousOD = new Label(""));
        grid.setWidget(3, 1, previousODBalanceStatus = new Label(""));
        grid.setWidget(4, 1, pdMode = new Label(""));
        grid.setWidget(5, 1, acm = new Label(""));

        grid.getCellFormatter().setWidth(0, 0, "285px");

        ActionBarWidget abw = new ActionBarWidget();
        abw.addAction(createRefreshAction());
        abw.addAction(export2ExcelActtion());

        abw.addSecureAction(createOpenODAction(), SecurityActionCode.TskOdOpenRun);
        abw.addSecureAction(createChangePhaseToPRE_COBAction(), SecurityActionCode.TskOdPreCobRun);
        abw.addSecureAction(createSwitchPdMode(), SecurityActionCode.TskOdSwitchModeRun);
        abw.addSecureAction(createMonitoring(), SecurityActionCode.TskOdPreCobRun);
        abw.addSecureAction(autoCloseAction = createAutoCloseODAction(), SecurityActionCode.TskOdBalCloseRun);
        abw.addSecureAction(createSwitchAccessModeAction(), SecurityActionCode.TskOdAccessModeSwitch);

        refreshAction.execute();

        DockLayoutPanel panel = new DockLayoutPanel(Style.Unit.MM);

        panel.addNorth(abw, 10);
        HorizontalPanel hp = new HorizontalPanel();
        hp.setSpacing(10);
        VerticalPanel column1 = new VerticalPanel();
//        vp.setBorderWidth(1);
        column1.getElement().getStyle().setProperty("border", "1px solid #003366");
        column1.add(grid);
        column1.add(createCOB_OKInfo());
        column1.add(vip_errors = createVipErrorInfo());
        column1.add(createAutoClosePreviousODInfo());
        VerticalPanel column2 = new VerticalPanel();
        column2.setSpacing(5);
        column2.add(createPanelProcessStatus());
        column2.add(createPanelRefreshRest());
        hp.add(column1);
        hp.add(column2);
        panel.add(hp);

        return panel;
    }

    private Widget createPanelRefreshRest(){
        panelRefreshRest = new HPanel(350, 70){
            public void handlerBody(ClickEvent event){
                panelRefreshRest.setButVisible(false);
                isStatusSet = false;
                startWatching();
            }
        };
        panelRefreshRest.setButTitle("Обновить");
        panelRefreshRest.addHeader("Управления режимом обновления остатков");
        HorizontalPanel panelContent = new HorizontalPanel();
        panelContent.setSpacing(10);
        HorizontalPanel panelRadio = new HorizontalPanel();
        RadioButton radioButton1 = new RadioButton(REST_RADIO, "color1");
        RadioButton radioButton2 = new RadioButton(REST_RADIO, "color2");
        RadioButton radioButton3 = new RadioButton(REST_RADIO, "color3");
        panelRadio.add(radioButton1);
        panelRadio.add(radioButton2);
        panelRadio.add(radioButton3);
        panelContent.add(panelRadio);
        panelRefreshRest.setBody(panelContent);
//        getRefreshRestStatus();
        return panelRefreshRest;
    }

    private Widget createPanelProcessStatus(){
        panelProcessStatus = new HPanel(350, 70){
            public void handlerBody(ClickEvent event){
                panelProcessStatus.setButVisible(false);
                isStatusSet = false;
                startWatching();
            }
        };
        panelProcessStatus.setButTitle("Обновить");
        panelProcessStatus.addHeader("Статус обработки");
        HorizontalPanel hp = new HorizontalPanel();
        hp.setSpacing(10);

        lbProcessStatus = new Label("?");
        lbProcessStatus.setTitle("Если в течение минуты действие не выполнено, убедитесь, что запущена задачыа 'Мониторинг входящих сообщений АЕ'");
        lbProcessStatus.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);
        hp.add(lbProcessStatus);
        hp.setCellWidth(lbProcessStatus, "240px");
        hp.setCellHorizontalAlignment(lbProcessStatus, HasHorizontalAlignment.ALIGN_LEFT);

        btChangeProcessStatus = new Button("?");
        if (SecurityChecker.checkActions(SecurityActionCode.TskStopStart)) {
            btChangeProcessStatus.setWidth("90px");
            btChangeProcessStatus.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    showConfirm("Подтверждение", "Подтвердите " + getStatusText() + " обработки проводок",
                            new IDlgEvents() {
                                @Override
                                public void onDlgOkClick(Object p) throws Exception {
                                    log.info("onClick(ClickEvent event)");
                                    if (getNextStatus() != null) {
                                        setProcessStatus(getNextStatus());
                                        setBtChangeProcessStatusEnable(false);
                                    }
                                }
                            }
                            , new IAfterCancelEvent() {
                                @Override
                                public void afterCancel() {
                                    btChangeProcessStatus.setFocus(true);
                                }
                            }, null);
                }
            });
            hp.add(btChangeProcessStatus);
            hp.setCellWidth(btChangeProcessStatus, "90px");
            hp.setCellHorizontalAlignment(btChangeProcessStatus, HasHorizontalAlignment.ALIGN_RIGHT);
        }else {
            btChangeProcessStatus.setVisible(false);
        }

        panelProcessStatus.setBody(hp);
        getProcessStatus();
        return panelProcessStatus;
    }

    private String getStatusText(){
       if (currentChangeProcessStatus.equals(ProcessingStatus.STARTED)) return "остановку";
       if (currentChangeProcessStatus.equals(ProcessingStatus.STOPPED)) return "запуск";
       return "XXX";
    }
// Если в течение минуты действие не выполнено, убедитесь, что запущена задачыа "Мониторинг входящих сообщений АЕ"
    private void startWatching(){
        log.info("startWatching()");
        timer = new Timer(){
            int MAX_COUNT = 30;
            int count;
            @Override
            public void run() {
                if (isStatusSet || count++ == MAX_COUNT){
                    timerCancel();
                    return;
                }
                getProcessStatus();
            }
        };

        timer.scheduleRepeating(tick);
    }

    void timerCancel(){
        timer.cancel();
        panelProcessStatus.setButVisible(true);
        log.info("timer.cancel()");

    }

    ProcessingStatus getNextStatus(){
        if (currentChangeProcessStatus.equals(ProcessingStatus.STARTED)) return ProcessingStatus.REQUIRED;
        if (currentChangeProcessStatus.equals(ProcessingStatus.STOPPED)) return ProcessingStatus.ALLOWED;
        return null;
    }

    private void setProcessStatus(ProcessingStatus processingStatus){
        BarsGLEntryPoint.operDayService.setProcessingStatus(processingStatus, new AuthCheckAsyncCallback<RpcRes_Base<String>>() {
            String errText = "Операция установки статуса не удалась.\nОшибка: ";
            @Override
            public void onFailureOthers(Throwable throwable) {
                getProcessStatus();
                Window.alert(errText + throwable.getLocalizedMessage());
            }

            @Override
            public void onSuccess(RpcRes_Base<String> res) {
                if (res.isError()) {
                    getProcessStatus();
                    DialogManager.error("Ошибка", errText + res.getMessage());
                }
            }
        });
    }

    private void getProcessStatus(){
        BarsGLEntryPoint.operDayService.getProcessingStatus(new AuthCheckAsyncCallback<RpcRes_Base<ProcessingStatus>>() {
            String errText = "Операция получения статуса не удалась.\nОшибка: ";
            @Override
            public void onFailureOthers(Throwable throwable) {
                Window.alert(errText + throwable.getLocalizedMessage());
                isStatusSet = true;
                showProcessingStatus();
            }

            @Override
            public void onSuccess(RpcRes_Base<ProcessingStatus> res) {
                if (res.isError()) {
                    DialogManager.error("Ошибка", errText + res.getMessage());
                } else {
                    currentChangeProcessStatus = res.getResult();
                    log.info("getProcessStatus() = "+currentChangeProcessStatus.toString());
                    isStatusSet = currentChangeProcessStatus.equals(ProcessingStatus.STARTED) || currentChangeProcessStatus.equals(ProcessingStatus.STOPPED);
                    if (isStatusSet && timer != null) timerCancel();;
                }
                showProcessingStatus();
            }
        });
    }

    private void showProcessingStatus(){
        lbProcessStatus.setText(currentChangeProcessStatus.getLabel()+" ("+currentChangeProcessStatus.toString()+")");
        if (currentChangeProcessStatus.equals(ProcessingStatus.STARTED)){
            btChangeProcessStatus.setText("Остановить");
            setBtChangeProcessStatusEnable(true);
        }else if (currentChangeProcessStatus.equals(ProcessingStatus.STOPPED)){
            btChangeProcessStatus.setText("Запустить");
            setBtChangeProcessStatusEnable(true);
        }else{
            btChangeProcessStatus.setText(currentChangeProcessStatus.toString());
            setBtChangeProcessStatusEnable(false);
        }
    }

    private void setBtChangeProcessStatusEnable(boolean isEnable){
        btChangeProcessStatus.setEnabled(isEnable);
        if (isEnable){
            btChangeProcessStatus.getElement().getStyle().setColor("black");
        }else{
            btChangeProcessStatus.getElement().getStyle().setColor("white");
        }
    }

    private Widget createAutoClosePreviousODInfo(){
        VerticalPanel vp = new VerticalPanel();
        vp.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
        vp.getElement().getStyle().setMarginTop(50, Style.Unit.PX);

        HTMLPanel html = new HTMLPanel("<h4>Автоматическое закрытие баланса предыдущего дня</h4>");
        html.getElement().getStyle().setBackgroundColor("#FFFF00");
        html.getElement().getStyle().setHeight(20, Style.Unit.PX);
        html.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);

        Label label;
        Grid grid = new Grid(3, 2);


        grid.setWidget(0, 0, label = new Label("Закрывается баланс за дату"));
        label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);
        grid.setWidget(0, 1, dateCB = new Label());

        grid.setWidget(1, 0, label = new Label("Дата закрытия"));
        label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);
        grid.setWidget(1, 1, dateOD = new Label());

        grid.setWidget(2, 0, label = new Label("Время закрытия"));
        label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);
        grid.setWidget(2, 1, timeClose = new  Label());

        grid.getCellFormatter().setWidth(0, 0, "285px");
        grid.getColumnFormatter().getElement(0).getStyle().setFontWeight(Style.FontWeight.BOLD);

        vp.add(html);
        vp.add(grid);
        return vp;
    }

    private Grid createVipErrorInfo(){
        Grid grid = new Grid(2,2);
        Label label;
        grid.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);

        grid.setWidget(0, 0, label = new Label("Ошибки обработки по VIP-клиентам:"));
        label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);
        grid.setWidget(1, 0, label = new Label("Ошибки обработки по не VIP-клиентам:"));
        label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);

        grid.setWidget(0, 1, vip = new Label(""));
        grid.setWidget(1, 1, not_vip = new Label(""));
        grid.getCellFormatter().setWidth(0, 0, "285px");

        return grid;
    }

    private Grid createCOB_OKInfo(){
        Grid grid = new Grid(1,2);
        Label label;
        grid.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
        grid.getElement().getStyle().setMarginTop(10, Style.Unit.PX);

        grid.setWidget(0, 0, label = new Label("Состояние GL Online"));label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);

        grid.setWidget(0, 1, reason = new Label(""));
        grid.getCellFormatter().setWidth(0, 0, "285px");

        return grid;
    }

    private void setCOB_OKInfo(COB_OKWrapper wrapper){
        reason.setText("");
        vip.setText("");
        not_vip.setText("");

        if (wrapper == null) return;

        reason.setText(wrapper.getReason() == null ? "" : wrapper.getReason().toString());
        vip.setText(wrapper.getVipCount() == null || wrapper.getVipCount() == 0 ? "0 (OK)" : wrapper.getVipCount().toString());

        not_vip.setText(wrapper.getNotVipCount() == null ? "0 (OK)" :
                (wrapper.getNotVipCount() <= 10 ? Utils.Fmt("{0} (OK)", wrapper.getNotVipCount()) : wrapper.getNotVipCount().toString()));

    }

    private void operDateRefresh(OperDayWrapper operDayWrapper){
        currentOD.setText(operDayWrapper.getCurrentOD());
        phaseCurrentOD.setText(operDayWrapper.getPhaseCurrentOD());
        previousOD.setText(operDayWrapper.getPreviousOD());
        previousODBalanceStatus.setText(operDayWrapper.getPreviousODBalanceStatus());
        pdMode.setText(operDayWrapper.getPdMode());
        accessMode = operDayWrapper.getAccessMode();
        acm.getElement().getStyle().setColor(accessMode == AccessMode.FULL ? "#000000" : "#FF0000");
        acm.getElement().getStyle().setFontWeight(accessMode == AccessMode.FULL ? Style.FontWeight.NORMAL :  Style.FontWeight.BOLD);
        acm.setText(accessMode.getLabel());

        setButtonsEnabled(operDayWrapper.getEnabledButton());
    }

    private void setButtonsEnabled(OperDayButtons button){
        open_OD.setEnable(button == OperDayButtons.OPEN_OD);
        change_Phase_To_PRE_COB.setEnable(button == OperDayButtons.CHANGE_PHASE_TO_PRE_COB);
        switchPdMode.setEnable(true);
    }

    private Action createRefreshAction(){
        return refreshAction = new Action(null, "Обновить", new Image(ImageConstants.INSTANCE.refresh24()), 5) {
            String errText = "Операция createRefreshAction() не удалась.\nОшибка: ";
            @Override
            public void execute() {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                autoCloseAction.setEnable(false);

                CommonEntryPoint.operDayService.getOperDay(new AuthCheckAsyncCallback<RpcRes_Base<OperDayWrapper>>() {
                    @Override
                    public void onFailureOthers(Throwable throwable) {
                        WaitingManager.hide();

                        Window.alert(errText + throwable.getLocalizedMessage());
                    }

                    @Override
                    public void onSuccess(RpcRes_Base<OperDayWrapper> res) {
                        if (res.isError()) {
                            DialogManager.error("Ошибка", errText + res.getMessage());
                        } else {
                            operDateRefresh(res.getResult());
                        }
                        WaitingManager.hide();
                    }
                });

                BarsGLEntryPoint.operDayService.getCOB_OK(new AuthCheckAsyncCallback<RpcRes_Base<COB_OKWrapper>>() {
                    @Override
                    public void onFailureOthers(Throwable throwable) {
                        Window.alert("Операция не удалась.\nОшибка: " + throwable.getLocalizedMessage());
                    }

                    @Override
                    public void onSuccess(RpcRes_Base<COB_OKWrapper > res) {
                        if (res.isError()) {
                            DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + res.getMessage());
                        } else {
                            setCOB_OKInfo(res.getResult());
                        }
                    }
                });

                getAutoClosePreviousOD();
            }
        };
    }

    private void getAutoClosePreviousOD(){
        BarsGLEntryPoint.operDayService.getLwdBalanceCut(new AuthCheckAsyncCallback<RpcRes_Base<LwdBalanceCutWrapper>>() {

            @Override
            public void onFailureOthers(Throwable throwable) {
                autoCloseAction.setEnable(true);
                Window.alert("Операция не удалась.\nОшибка: " + throwable.getLocalizedMessage());
            }

            @Override
            public void onSuccess(RpcRes_Base<LwdBalanceCutWrapper> res) {
                autoCloseAction.setEnable(true);
                if (res.isError()) {
                    DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + res.getMessage());
                } else {
                    lwdBalanceCutWrapper = res.getResult();
                    dateCB.setText(lwdBalanceCutWrapper.getCloseDateStr());
                    dateOD.setText(lwdBalanceCutWrapper.getRunDateStr());
                    timeClose.setText(lwdBalanceCutWrapper.getCutTimeStr());
                }
            }
        });
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
                            refreshAction.execute();
                            open_OD.setEnable(false);

                            DialogManager.message("Инфо", "Задание 'Открытие ОД' выполнено.\n" +
                                    "Для обновления информации нажмите 'Обновить'.");
                        }
                    }
                });
            }
        };
    }

    private Action createChangePhaseToPRE_COBAction(){
        return change_Phase_To_PRE_COB = new Action("Закрытие баланса предыдущего ОД и перевод фазы в PRE_COB", "", null, 5) {
            @Override
            public void execute() {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                BarsGLEntryPoint.operDayService.runExecutePreCOBTask(new AuthCheckAsyncCallback<RpcRes_Base<TimerJobHistoryWrapper>>() {
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
                            refreshAction.execute();
                            change_Phase_To_PRE_COB.setEnable(false);
                            DialogManager.message("Инфо", res.getMessage());
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
                            refreshAction.execute();
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
                            dlg.setAfterCancelEvent(new IAfterCancelEvent() {
                               @Override
                               public void afterCancel() {
                                   refreshAction.execute();
                               }
                           });
                       }
                       WaitingManager.hide();
                   }
               });
           }
       };
   }

    private Action createAutoCloseODAction(){
        return new Action(null, "Закрытие баланса предыдущего дня", new Image(ImageConstants.INSTANCE.back_value()), 5){
            AutoCloseODDlg dlg = null;
            @Override
            public void execute() {
                dlg =  new AutoCloseODDlg();
                dlg.setCaption("Настройка закрытия баланса предыдущего дня");
                dlg.setDlgEvents(new IDlgEvents() {
                    @Override
                    public void onDlgOkClick(Object prms) throws Exception {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                        BarsGLEntryPoint.operDayService.setLwdBalanceCut((LwdBalanceCutWrapper)prms, new AuthCheckAsyncCallback<RpcRes_Base<LwdBalanceCutWrapper>>() {

                            @Override
                            public void onSuccess(RpcRes_Base<LwdBalanceCutWrapper> res) {
                                if (res.isError()) {
                                    DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + res.getMessage());
                                } else {
                                    dlg.hide();
                                    getAutoClosePreviousOD();
                                }
                                WaitingManager.hide();
                            }
                        });
                    }
                });

                dlg.show(lwdBalanceCutWrapper);
            }
        };
    }


   private Action export2ExcelActtion(){
       return new Action(null, "Ночной отчет об ошибках", new Image(ImageConstants.INSTANCE.report()), 5) {
           @Override
           public void execute() {
               setEnable(false);
               Export2Excel e2e = new Export2Excel(new NigthErrRepExportData(), null,
                                                   new ExportActionCallback(this, UUID.randomUUID().replace("-", "")));
               e2e.export();
           }
       };
   }

    private Action createFakeCOB(){
        return new Action("Fake COB", "", null, 5){
            @Override
            public void execute() {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                BarsGLEntryPoint.operDayService.runExecuteFakeCOBTask(new AuthCheckAsyncCallback<RpcRes_Base<TimerJobHistoryWrapper>>() {
                    @Override
                    public void onSuccess(RpcRes_Base<TimerJobHistoryWrapper> result) {
                        if (result.isError()) {
                            DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + result.getMessage());
                        } else {
                            Window.alert(result.getMessage());
                        }
                        WaitingManager.hide();
                    }
                });
            }
        };
    }

    private Action createSwitchAccessModeAction(){
        return switchPdMode = new Action("Переключение режима доступа", "", null, 5) {
            @Override
            public void execute() {
                DialogManager.confirm("Переключение режима доступа", Utils.Fmt("Подтверждаете установку режима {0} доступа?",
                        accessMode == AccessMode.FULL ? "ограниченного" : "полного"), "Да", "Нет", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                        OperDayWrapper wrapper = new OperDayWrapper();
                        wrapper.setAccessMode(accessMode);
                        BarsGLEntryPoint.operDayService.switchAccessMode(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<Boolean>>() {

                            @Override
                            public void onSuccess(RpcRes_Base<Boolean> res) {
                                WaitingManager.hide();

                                if (res.isError()) {
                                    DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + res.getMessage());
                                } else {
                                    refreshAction.execute();
                                    if (res.getResult() == false) {
                                        DialogManager.error("Ошибка", "Операция изменения режима доступа не удалась");
                                    } else {
                                        DialogManager.message("Инфо", Utils.Fmt("Режим доступа изменен на {0}",
                                                accessMode == AccessMode.FULL ? "ограниченный" : "полный"));
                                    }
                                }
                            }
                        });
                    }
                }, null);


            }
        };
    }
}
