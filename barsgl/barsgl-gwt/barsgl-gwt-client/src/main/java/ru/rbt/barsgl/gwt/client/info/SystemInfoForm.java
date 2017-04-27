package ru.rbt.barsgl.gwt.client.info;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.security.gwt.client.CommonEntryPoint;
import ru.rbt.grid.gwt.client.GridEntryPoint;
import ru.rbt.barsgl.gwt.core.actions.Action;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.forms.BaseForm;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.ActionBarWidget;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by SotnikovAV on 10.11.2016.
 */
public class SystemInfoForm extends BaseForm {

    public final static String FORM_NAME = "Информация о системе";

    private Action refreshAction;

    private Label currentOD;
    private Label phaseCurrentOD;
    private Label previousOD;
    private Label previousODBalanceStatus;
    private Label pdMode;

    private Label barsJrnUnProcessed;
    private Label barsJrnProcessedOk;
    private Label barsJrnProcessedErr;
    private Label barsJrnReplicationSpeed;
    private Label barsJrnReplicationRemainingTime;

    private long barsJrnPreviosRefreshCnt;
    private long barsJrnPreviosRefreshTime;

    public SystemInfoForm(){
        super();
        title.setText(FORM_NAME);
    }

    @Override
    public Widget createContent() {
        return getContent();
    }

    public Widget getContent(){
        Label label;
        Grid grid = new Grid(11, 2);

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

        grid.setWidget(5, 0, label = new Label("РЕПЛИКАЦИЯ"));
        label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);

        grid.setWidget(6, 0, label = new Label("На репликации, записей:"));
        label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);

        grid.setWidget(7, 0, label = new Label("Реплицировано успешно, записей:"));
        label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);

        grid.setWidget(8, 0, label = new Label("Реплицировано с ошибкой, записей:"));
        label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);

        grid.setWidget(9, 0, label = new Label("Скорость репликации, записей/сек:"));
        label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);

        grid.setWidget(10, 0, label = new Label("Время до окончания репликации:"));
        label.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);

        grid.setWidget(0, 1, currentOD = new Label(""));
        grid.setWidget(1, 1, phaseCurrentOD = new Label(""));
        grid.setWidget(2, 1, previousOD = new Label(""));
        grid.setWidget(3, 1, previousODBalanceStatus = new Label(""));
        grid.setWidget(4, 1, pdMode = new Label(""));
        grid.setWidget(5, 1, new Label(""));

        grid.setWidget(6, 1, barsJrnUnProcessed = new Label(""));
        grid.setWidget(7, 1, barsJrnProcessedOk = new Label(""));
        grid.setWidget(8, 1, barsJrnProcessedErr = new Label(""));
        grid.setWidget(9, 1, barsJrnReplicationSpeed = new Label(""));
        grid.setWidget(10, 1, barsJrnReplicationRemainingTime = new Label(""));

        grid.getCellFormatter().setWidth(0, 0, "285px");

        ActionBarWidget abw = new ActionBarWidget();
        abw.addAction(createRefreshAction());

        refreshAction.execute();

        DockLayoutPanel panel = new DockLayoutPanel(Style.Unit.MM);

        panel.addNorth(abw, 10);
        panel.add(grid);

        return panel;
    }

    private Action createRefreshAction(){
        return refreshAction = new Action(null, "Обновить", new Image(ImageConstants.INSTANCE.refresh24()), 5) {
            @Override
            public void execute() {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                CommonEntryPoint.operDayService.getOperDay(new AuthCheckAsyncCallback<RpcRes_Base<OperDayWrapper>>() {
                    @Override
                    public void onFailureOthers(Throwable throwable) {
                        WaitingManager.hide();

                        Window.alert("Операция не удалась.\nОшибка: " + throwable.getLocalizedMessage());
                    }

                    @Override
                    public void onSuccess(RpcRes_Base<OperDayWrapper> res) {
                        if (res.isError()){
                            DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + res.getMessage());
                        } else {
                            operDateRefresh(res.getResult());
                        }
                        WaitingManager.hide();
                    }
                });

                GridEntryPoint.asyncGridService.selectOne("select count(1) as cnt from bars_jrn where is_processed=0", null, new AsyncCallback<Row>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        Window.alert("Операция не удалась.\nОшибка: " + throwable.getLocalizedMessage());
                    }

                    @Override
                    public void onSuccess(Row row) {
                        Integer cnt = (Integer)row.getField(0).getValue();
                        if(null == cnt) {
                            barsJrnUnProcessed.setText("нет информации");
                        } else {
                            long currentTime = System.currentTimeMillis();
                            barsJrnUnProcessed.setText(cnt.toString());
                        }
                    }
                });

                GridEntryPoint.asyncGridService.selectOne("select count(1) as cnt from bars_jrn where is_processed=1", null, new AsyncCallback<Row>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        Window.alert("Операция не удалась.\nОшибка: " + throwable.getLocalizedMessage());
                    }

                    @Override
                    public void onSuccess(Row row) {
                        Integer cnt = (Integer)row.getField(0).getValue();
                        if(null == cnt) {
                            barsJrnProcessedOk.setText("нет информации");
                        } else {
                            long currentTime = System.currentTimeMillis();
                            int speed = (int)((cnt - barsJrnPreviosRefreshCnt) * 1000/ (currentTime - barsJrnPreviosRefreshTime)) ;

                            long remainingTime = ("".equals(barsJrnUnProcessed.getText()) ? 0 : Integer.parseInt(barsJrnUnProcessed.getText())) / (0 == speed ? 1 : speed);
                            barsJrnPreviosRefreshTime = currentTime;
                            barsJrnPreviosRefreshCnt =  cnt;

                            barsJrnProcessedOk.setText(cnt.toString());
                            barsJrnReplicationSpeed.setText(Integer.toString(speed));
                            barsJrnReplicationRemainingTime.setText(milisToHHMISSSSStr(remainingTime));
                        }
                    }
                });

                GridEntryPoint.asyncGridService.selectOne("select count(1) as cnt from bars_jrn where is_processed=-1", null, new AsyncCallback<Row>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        Window.alert("Операция не удалась.\nОшибка: " + throwable.getLocalizedMessage());
                    }

                    @Override
                    public void onSuccess(Row row) {
                        Integer cnt = (Integer)row.getField(0).getValue();
                        if(null == cnt) {
                            barsJrnProcessedErr.setText("нет информации");
                        } else {
                            barsJrnProcessedErr.setText(cnt.toString());
                        }
                    }
                });
            }
        };
    }

    private void operDateRefresh(OperDayWrapper operDayWrapper){
        currentOD.setText(operDayWrapper.getCurrentOD());
        phaseCurrentOD.setText(operDayWrapper.getPhaseCurrentOD());
        previousOD.setText(operDayWrapper.getPreviousOD());
        previousODBalanceStatus.setText(operDayWrapper.getPreviousODBalanceStatus());
        pdMode.setText(operDayWrapper.getPdMode());
    }

    /**
     * Преобразовать время в милисекундах в строку вида hh:mi:ss.sss
     *
     * @param milis
     *            - время в милисекундах
     * @return строка вида hh:mi:ss.sss
     */
    private String milisToHHMISSSSStr(long milis) {
        int hours = (int) (milis / (60 * 60 * 1000));
        int minutes = (int) ((milis - (hours * 60 * 60 * 1000)) / (60 * 1000));
        int seconds = (int) ((milis - (hours * 60 * 60 * 1000) - (minutes * 60 * 1000)) / 1000);
        int miliseconds = (int) (milis - (hours * 60 * 60 * 1000) - (minutes * 60 * 1000) - (seconds * 1000));
        return Integer.toString(hours) + ':'
                + Integer.toString(minutes) + ':'
                + Integer.toString(seconds) + '.'
                + Integer.toString(miliseconds);
    }

}
