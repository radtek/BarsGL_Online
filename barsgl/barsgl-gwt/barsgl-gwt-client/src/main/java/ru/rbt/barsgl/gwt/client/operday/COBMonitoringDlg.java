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
    private final int steps = 6;
    private final int barsCount = 20;
    private final String completeMessage = "Завершено {0}%";
    private final String phaseNameTmpl = "Фаза {0} {1}";
    private GradientProgressBar barTotal;
    private Label phaseTotalStatus;
    private AreaBox phaseTotalMsg;

    private Long idCOB = null;
    private boolean isNeedStartTimer = false;
    private HashMap<Integer, Label> phaseNames;
    private HashMap<Integer, Label> phaseStatuses;
    private HashMap<Integer, ProgressBar> bars;
    private HashMap<Integer, AreaBox> phaseMsgs;

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

        grid.setWidget(6, 0, new Label("Общее по COB"));

        grid.getCellFormatter().getElement(0, 0).getStyle().setWidth(150, Style.Unit.PX);
        grid.getCellFormatter().getElement(6, 0).getStyle().setFontWeight(Style.FontWeight.BOLD);

        grid.setWidget(6, 1, barTotal = new GradientProgressBar(barsCount, ProgressBar.SHOW_TEXT));
        barTotal.setText(Utils.Fmt(completeMessage, 0));
        barTotal.setShowGradientColor(true);

        grid.setWidget(6, 2, phaseTotalStatus = new Label());
        grid.getCellFormatter().getElement(0, 2).getStyle().setWidth(200, Style.Unit.PX);

        phaseTotalMsg = new AreaBox();
        phaseTotalMsg.setReadOnly(true);
        phaseTotalMsg.setHeight("50px");
        phaseTotalMsg.setWidth("835px");
        phaseTotalMsg.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);

        VerticalPanel vp = new VerticalPanel();
        vp.add(grid);
        vp.add(phaseTotalMsg);

       /* final ProgressBar progressBar = new ProgressBar(20
                ,ProgressBar.SHOW_TIME_REMAINING
                +ProgressBar.SHOW_TEXT);
        progressBar.setText("Doing something...");


        final GradientProgressBar progressBar2 = new GradientProgressBar(20, ProgressBar.SHOW_TEXT);
        progressBar2.setShowGradientColor(true);
        progressBar2.setText(Utils.Fmt(completeMessage, 0));


        Timer t = new Timer() {
            public void run() {
                int progress = progressBar.getProgress()+4;
                if (progress>100) cancel();
                progressBar.setProgress(progress);


                int progress2 = progressBar2.getProgress()+4;
                if (progress2>100) cancel();
                progressBar2.setProgress(progress2);
                progressBar2.setText(Utils.Fmt(completeMessage,  progress2 + 1 > 100 ? 100 : progress2 + 1));
            }
        };
        t.scheduleRepeating(1000);*/
        return vp ;
    }

    @Override
    protected void fillContent(){
        //reset();
        CobWrapper  wrapper = (CobWrapper) params;
        setPhaseNames(wrapper);
        setMonitoringInfo(wrapper);
        Window.alert(wrapper.getStartTimer().toString());
        //TODO watcher
    }
    //TODO rewrite
    private void getMonitoringInfo(){
        BarsGLEntryPoint.operDayService.getCobInfo(idCOB, new AuthCheckAsyncCallback<RpcRes_Base<CobWrapper>>() {
            @Override
            public void onSuccess(RpcRes_Base<CobWrapper> result) {
                if (result.isError()) {
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
            phaseMsgs.get(item.getPhaseNo()).setValue(item.getMessage());
            bars.get(item.getPhaseNo()).setText(Utils.Fmt(completeMessage, item.getIntPercent()));
            bars.get(item.getPhaseNo()).setProgress(item.getIntPercent());
            phaseStatuses.get(item.getPhaseNo()).setText(getPhaseMessage(item));
        }

        idCOB = wrapper.getIdCob();
        isNeedStartTimer = wrapper.getStartTimer();

        phaseTotalMsg.setValue(wrapper.getErrorMessage());
        CobStepItem item = wrapper.getTotal();
        barTotal.setProgress(item.getIntPercent());
        barTotal.setText(Utils.Fmt(completeMessage, item.getIntPercent()));
        phaseTotalStatus.setText(getPhaseMessage(item));
    }

    private String getPhaseMessage(CobStepItem item){
       String res = "";
        switch (item.getStatus()){
            case Step_NotStart:
                res = Utils.Fmt("{0}. Завершится за {1}", item.getStatus().getLabel(), Value2TimeStr(item.getIntEstimation()));
                break;
            case Step_Running:
                res = Utils.Fmt("{0}. Выполняется {1}. Завершится за {2}", item.getStatus().getLabel(), Value2TimeStr(item.getIntDuration()),
                        Value2TimeStr(item.getIntEstimation()));
                break;
            case Step_Success:
                res = Utils.Fmt("{0} за {1}", item.getStatus().getLabel(), Value2TimeStr(item.getIntDuration()));
                break;
            case Step_Error:
                res = Utils.Fmt("{0} через {1}", item.getStatus().getLabel(), Value2TimeStr(item.getIntDuration()));
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

   /* private void reset(){
        for(int i = 1; i <= steps; i++){
            bars.get(i).setProgress(0);
            bars.get(i).setText(Utils.Fmt(completeMessage, 0));
            phaseStatuses.get(i).setText("");
            phaseMsgs.get(i).clear();
        }
        barTotal.setProgress(0);
        barTotal.setText(Utils.Fmt(completeMessage, 0));
        phaseTotalStatus.setText("");
        phaseTotalMsg.clear();
    }*/

    @Override
    protected boolean onClickOK() throws Exception {
        DialogManager.confirm("Подтверждение", "Пересчитать статистику по СОВ?", "Пересчитать", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
               // reset();
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
}
