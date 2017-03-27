package ru.rbt.barsgl.gwt.client.operday;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.gwt.core.ui.GradientProgressBar;
import ru.rbt.barsgl.gwt.core.ui.ProgressBar;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.cob.CobStepItem;
import ru.rbt.barsgl.shared.cob.CobWrapper;
import ru.rbt.barsgl.shared.enums.CobStepStatus;

import java.util.HashMap;
import java.util.List;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by akichigi on 09.03.17.
 */
public class COBMonitoringDlg extends DlgFrame {
    private final int steps = 7;
    private final int barsCount = 20;
    private final String completeMessage = "Завершено {0}%";
    private final String phaseNameTmpl = "Фаза {0} {1}";
    private GradientProgressBar barTotal;
    private Label phaseTotalStatus;
    private AreaBox phaseTotalMsg;

    private Long idCOB = null;
    CobStepStatus totalStatus = null;
    private boolean isNeedStartTimer = false;
    private HashMap<Integer, Label> phaseNames;
    private HashMap<Integer, Label> phaseStatuses;
    private HashMap<Integer, ProgressBar> bars;
    private HashMap<Integer, AreaBox> phaseMsgs;

    private Timer timer;
    private boolean isAllowPing;
    private final int tick = 1000;

    public COBMonitoringDlg(){
        super();
        setCaption("Мониторинг COB BARSGL");
        ok.setText("Пересчитать");
    }

    private void prepareElements(int steps){
        phaseNames = new HashMap<>();
        phaseStatuses = new HashMap<>();
        bars = new HashMap<>();
        phaseMsgs = new HashMap<>();

        for (int i = 1; i <= steps; i++){
            phaseNames.put(i, createPhaseName());
            phaseStatuses.put(i, createPhaseStatus());
            bars.put(i, createBar());
            phaseMsgs.put(i, createPhaseMsg());
        }
    }

    private Label createPhaseName(){
        return new Label();
    }

    private Label createPhaseStatus(){
        return new Label();
    }

    private ProgressBar createBar(){
        ProgressBar bar = new ProgressBar(barsCount, ProgressBar.SHOW_TEXT);
        bar.setText(Utils.Fmt(completeMessage, 0));
        return bar;
    }

    private AreaBox createPhaseMsg(){
        AreaBox box = new AreaBox();
        box.setReadOnly(true);
        box.setHeight("50px");
        box.setWidth("300px");
        return box;
    }

    @Override
    public Widget createContent(){
        prepareElements(steps);
        Grid grid = new Grid(steps + 1, 4);
        for(int i = 0; i < steps; i++){
            grid.setWidget(i, 0, phaseNames.get(i + 1));
            grid.setWidget(i, 1, bars.get(i + 1));
            grid.setWidget(i, 2, phaseStatuses.get(i + 1));
            grid.setWidget(i, 3, phaseMsgs.get(i + 1));
        }

        grid.setWidget(steps, 0, new Label("Общее по COB"));

        grid.getCellFormatter().getElement(0, 0).getStyle().setWidth(150, Style.Unit.PX);
        grid.getCellFormatter().getElement(steps, 0).getStyle().setFontWeight(Style.FontWeight.BOLD);

        grid.setWidget(steps, 1, barTotal = new GradientProgressBar(barsCount, ProgressBar.SHOW_TEXT));
        barTotal.setText(Utils.Fmt(completeMessage, 0));
        barTotal.setShowGradientColor(true);

        grid.setWidget(steps, 2, phaseTotalStatus = new Label());
        grid.getCellFormatter().getElement(0, 2).getStyle().setWidth(200, Style.Unit.PX);

        phaseTotalMsg = new AreaBox();
        phaseTotalMsg.setReadOnly(true);
        phaseTotalMsg.setHeight("50px");
        phaseTotalMsg.setWidth("835px");
        phaseTotalMsg.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);

        VerticalPanel vp = new VerticalPanel();
        vp.add(grid);
        vp.add(phaseTotalMsg);

        return vp ;
    }

    @Override
    protected void fillContent(){
        CobWrapper  wrapper = (CobWrapper) params;
        setPhaseNames(wrapper);
        setMonitoringInfo(wrapper);

        startWatching();
    }

    private void startWatching(){
        if (! isNeedStartTimer) {
            if (timer != null) timer.cancel();
            return;
        }

        Window.alert("Watching");
        timer = new Timer(){

            @Override
            public void run() {
                if (isNeedStartTimer){
                    if (isAllowPing){
                        isAllowPing = false;
                        getMonitoringInfo();
                    }
                } else{
                    timer.cancel();
                    Window.alert("Stop Watching");
                }
            }
        };

        timer.scheduleRepeating(tick);
    }


    private void getMonitoringInfo(){
        BarsGLEntryPoint.operDayService.getCobInfo(totalStatus == CobStepStatus.Running ? idCOB : null,
                new AuthCheckAsyncCallback<RpcRes_Base<CobWrapper>>() {
            @Override
            public void onSuccess(RpcRes_Base<CobWrapper> result) {
                if (result.isError()) {
                    if (timer != null) timer.cancel();
                    DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + result.getMessage());
                } else {
                    setMonitoringInfo(result.getResult());
                }
            }
        });
    }

    private void setPhaseNames(CobWrapper wrapper){
        List<CobStepItem> stepItems = wrapper.getStepList();
        for(int i = 0; i < stepItems.size(); i++){
            CobStepItem item = stepItems.get(i);
            phaseNames.get(item.getPhaseNo()).setText(Utils.Fmt(phaseNameTmpl, item.getPhaseNo().toString(), item.getPhaseName()));
        }
    }

    private void setMonitoringInfo(CobWrapper wrapper){
        List<CobStepItem> stepItems = wrapper.getStepList();
        for(int i = 0; i < stepItems.size(); i++){
            CobStepItem item = stepItems.get(i);
            phaseMsgs.get(item.getPhaseNo()).setValue(item.getMessage() == null ? " " : item.getMessage()); //fix IE bug
            bars.get(item.getPhaseNo()).setText(Utils.Fmt(completeMessage, item.getIntPercent()));
            bars.get(item.getPhaseNo()).setProgress(item.getIntPercent());
            phaseStatuses.get(item.getPhaseNo()).setText(getPhaseMessage(item));
        }

        idCOB = wrapper.getIdCob();
        totalStatus = wrapper.getTotal().getStatus();
        isNeedStartTimer = wrapper.getStartTimer();

        phaseTotalMsg.setValue(wrapper.getErrorMessage() == null ? " " : wrapper.getErrorMessage()); //fix IE bug
        CobStepItem item = wrapper.getTotal();
        barTotal.setProgress(item.getIntPercent());
        barTotal.setText(Utils.Fmt(completeMessage, item.getIntPercent()));
        phaseTotalStatus.setText(getPhaseMessage(item));

        ok.setEnabled(!isNeedStartTimer);
        isAllowPing = true;
    }

    private String getPhaseMessage(CobStepItem item){
       String res = "";
        switch (item.getStatus()){
            case NotStart:
                res = Utils.Fmt("{0}. Завершится за {1}", item.getStatus().getLabel(), Value2TimeStr(item.getIntEstimation()));
                break;
            case Running:
                res = Utils.Fmt("{0}. Выполняется {1}. Завершится за {2}", item.getStatus().getLabel(), Value2TimeStr(item.getIntDuration()),
                        Value2TimeStr(item.getIntEstimation()));
                break;
            case Success:
                res = Utils.Fmt("{0} за {1}", item.getStatus().getLabel(), Value2TimeStr(item.getIntDuration()));
                break;
            case Error:
            case Halt:
                res = Utils.Fmt("{0} за {1}", item.getStatus().getLabel(), Value2TimeStr(item.getIntDuration()));
                break;
            default:res = item.getStatus().getLabel();
        }
        return res;
    }

    private String Value2TimeStr(int value){
        int min = value / 60;
        int sec = value % 60;
        return Utils.Fmt("{0} мин {1} сек", min, sec);
    }

    @Override
    protected boolean onClickOK() throws Exception {
        DialogManager.confirm("Подтверждение", "Пересчитать статистику по СОВ?", "Пересчитать", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                BarsGLEntryPoint.operDayService.calculateCob(new AuthCheckAsyncCallback<RpcRes_Base<CobWrapper>>() {
                    @Override
                    public void onSuccess(RpcRes_Base<CobWrapper> result) {
                        if (result.isError()) {
                            DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + result.getMessage());
                        } else {
                            setMonitoringInfo(result.getResult());
                        }
                        WaitingManager.hide();
                    }
                });
            }
        });

        return false;
    }

    protected void onCancelClick(){
       if (timer != null) timer.cancel();
        super.onCancelClick();
    }
}
