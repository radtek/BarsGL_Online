package ru.rbt.barsgl.gwt.client.operday;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.gwt.core.ui.GradientProgressBar;
import ru.rbt.barsgl.gwt.core.ui.ProgressBar;
import ru.rbt.barsgl.shared.Utils;

/**
 * Created by akichigi on 09.03.17.
 */
public class COBMonitoringDlg extends DlgFrame {
    private final int barsCount = 20;
    private final String completeMessage = "Завершено {0}%";

    private Label phase1Name;
    private Label phase2Name;
    private Label phase3Name;
    private Label phase4Name;
    private Label phase5Name;
    private Label phase6Name;

    private ProgressBar bar1;
    private ProgressBar bar2;
    private ProgressBar bar3;
    private ProgressBar bar4;
    private ProgressBar bar5;
    private ProgressBar bar6;
    private GradientProgressBar barTotal;

    private Label phase1Status;
    private Label phase2Status;
    private Label phase3Status;
    private Label phase4Status;
    private Label phase5Status;
    private Label phase6Status;
    private Label phaseTotalStatus;

    private AreaBox phase1Msg;
    private AreaBox phase2Msg;
    private AreaBox phase3Msg;
    private AreaBox phase4Msg;
    private AreaBox phase5Msg;
    private AreaBox phase6Msg;
    private AreaBox phaseTotalMsg;

    public COBMonitoringDlg(){
        super();
        setCaption("Мониторинг COB BARSGL");
        ok.setText("Пересчитать");
    }

    @Override
    public Widget createContent(){
        Grid grid = new Grid(7, 4);
        //Labels
        grid.setWidget(0, 0, phase1Name = new Label("Фаза 1 Остановка обработки проводок"));
        grid.setWidget(1, 0, phase2Name = new Label("Фаза 2 Автоматический сброс буфера"));
        grid.setWidget(2, 0, phase3Name = new Label("Фаза 3 Обработка необработанных запросов на операцию"));
        grid.setWidget(3, 0, phase4Name = new Label("Фаза 4 Обработка сторно текущего дня"));
        grid.setWidget(4, 0, phase5Name = new Label("Фаза 5 Обработка вееров"));
        grid.setWidget(5, 0, phase6Name = new Label("Фаза 6 Пересчеты и локализация"));
        grid.setWidget(6, 0, new Label("Общее по COB"));

        grid.getCellFormatter().getElement(0, 0).getStyle().setWidth(150, Style.Unit.PX);
        grid.getCellFormatter().getElement(6, 0).getStyle().setFontWeight(Style.FontWeight.BOLD);

        //ProgressBars
        grid.setWidget(0, 1, bar1 = new ProgressBar(barsCount, ProgressBar.SHOW_TEXT));
        grid.setWidget(1, 1, bar2 = new ProgressBar(barsCount, ProgressBar.SHOW_TEXT));
        grid.setWidget(2, 1, bar3 = new ProgressBar(barsCount, ProgressBar.SHOW_TEXT));
        grid.setWidget(3, 1, bar4 = new ProgressBar(barsCount, ProgressBar.SHOW_TEXT));
        grid.setWidget(4, 1, bar5 = new ProgressBar(barsCount, ProgressBar.SHOW_TEXT));
        grid.setWidget(5, 1, bar6 = new ProgressBar(barsCount, ProgressBar.SHOW_TEXT));
        grid.setWidget(6, 1, barTotal = new GradientProgressBar(barsCount, ProgressBar.SHOW_TEXT));

        bar1.setText(Utils.Fmt(completeMessage, 0));
        bar2.setText(Utils.Fmt(completeMessage, 0));
        bar3.setText(Utils.Fmt(completeMessage, 0));
        bar4.setText(Utils.Fmt(completeMessage, 0));
        bar5.setText(Utils.Fmt(completeMessage, 0));
        bar6.setText(Utils.Fmt(completeMessage, 0));
        barTotal.setText(Utils.Fmt(completeMessage, 0));
        barTotal.setShowGradientColor(true);

        //Statuses
        grid.setWidget(0, 2, phase1Status = new Label("Статус 1 Остановка обработки проводок"));
        grid.setWidget(1, 2, phase2Status = new Label("Статус 2 Автоматический сброс буфера"));
        grid.setWidget(2, 2, phase3Status = new Label("Статус 3 Обработка необработанных запросов на операцию"));
        grid.setWidget(3, 2, phase4Status = new Label("Статус 4 Обработка сторно текущего дня"));
        grid.setWidget(4, 2, phase5Status = new Label("Статус 5 Обработка вееров"));
        grid.setWidget(5, 2, phase6Status = new Label("Статус 6 Пересчеты и локализация"));
        grid.setWidget(6, 2, phaseTotalStatus = new Label("Статус 123456 Общее по СОВ Статус 123456 Общее по СОВ 12345!"));

        grid.getCellFormatter().getElement(0, 2).getStyle().setWidth(200, Style.Unit.PX);

        //Messages
        grid.setWidget(0, 3, phase1Msg = new AreaBox());
        grid.setWidget(1, 3, phase2Msg = new AreaBox("Статус 2 Автоматический сброс буфера"));
        grid.setWidget(2, 3, phase3Msg = new AreaBox("Статус 3 Обработка необработанных запросов на операцию"));
        grid.setWidget(3, 3, phase4Msg = new AreaBox("Статус 4 Обработка сторно текущего дня"));
        grid.setWidget(4, 3, phase5Msg = new AreaBox("Статус 5 Обработка вееров"));
        grid.setWidget(5, 3, phase6Msg = new AreaBox("Статус 6 Пересчеты и локализация Статус 6 Пересчеты и локализация Статус 6 Пересчеты и локализацияСтатус 6 Пересчеты и локализация"));

        phase1Msg.setReadOnly(true);
        phase2Msg.setReadOnly(true);
        phase3Msg.setReadOnly(true);
        phase4Msg.setReadOnly(true);
        phase5Msg.setReadOnly(true);
        phase6Msg.setReadOnly(true);

        phase1Msg.setHeight("50px");
        phase1Msg.setWidth("300px");

        phase2Msg.setHeight("50px");
        phase2Msg.setWidth("300px");

        phase3Msg.setHeight("50px");
        phase3Msg.setWidth("300px");

        phase4Msg.setHeight("50px");
        phase4Msg.setWidth("300px");

        phase5Msg.setHeight("50px");
        phase5Msg.setWidth("300px");

        phase6Msg.setHeight("50px");
        phase6Msg.setWidth("300px");

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

        bar4.setProgress(25);
        bar5.setProgress(100);
        bar6.setProgress(50);
        barTotal.setProgress(80);

        return vp ;
    }


    @Override
    protected void fillContent(){

    }

    private void reset(){
        bar1.setText(Utils.Fmt(completeMessage, 0));
        bar2.setText(Utils.Fmt(completeMessage, 0));
        bar3.setText(Utils.Fmt(completeMessage, 0));
        bar4.setText(Utils.Fmt(completeMessage, 0));
        bar5.setText(Utils.Fmt(completeMessage, 0));
        bar6.setText(Utils.Fmt(completeMessage, 0));
        barTotal.setText(Utils.Fmt(completeMessage, 0));

        bar1.setProgress(0);
        bar2.setProgress(0);
        bar3.setProgress(0);
        bar4.setProgress(0);
        bar5.setProgress(0);
        bar6.setProgress(0);
        barTotal.setProgress(0);

        phase1Status.setText("");
        phase2Status.setText("");
        phase3Status.setText("");
        phase4Status.setText("");
        phase5Status.setText("");
        phase6Status.setText("");
        phaseTotalStatus.setText("");

        phase1Msg.clear();
        phase2Msg.clear();
        phase3Msg.clear();
        phase4Msg.clear();
        phase5Msg.clear();
        phase6Msg.clear();
        phaseTotalMsg.clear();
    }


    @Override
    protected boolean onClickOK() throws Exception {
        DialogManager.confirm("Подтверждение", "Запустить задачу синхронизации полупроводок?", "Запустить", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                reset();
            }
        });

        return false;
    }
}
