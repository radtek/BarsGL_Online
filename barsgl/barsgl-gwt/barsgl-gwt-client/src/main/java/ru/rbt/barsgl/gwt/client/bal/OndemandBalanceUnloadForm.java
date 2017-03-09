package ru.rbt.barsgl.gwt.client.bal;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.forms.BaseForm;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.jobs.TimerJobWrapper;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.rbt.tasks.gwt.client.TimerEntryPoint;
import ru.rbt.security.gwt.client.CommonEntryPoint;

/**
 * Created by Ivan Sevastyanov on 25.01.2016.
 */
public class OndemandBalanceUnloadForm extends BaseForm {
    public static final String FORM_NAME = "Выгрузка остатков за дату";
    private static final String UNLOAD_JOB_NAME = "UniAccountBalanceUnloadTask";
    private static DateTimeFormat format = DateTimeFormat.getFormat("dd.MM.yyyy");

    private DatePickerBox dateUnloadPicker;
    private Button button;
    private Label resultLabel;
    private Date lastWorkingDate;
    private Date workingDate;

    public OndemandBalanceUnloadForm() {
        super();
        title.setText(FORM_NAME);
    }

    @Override
    public Widget createContent() {
        return createFormContent();
    }

    private Widget createFormContent() {
        Grid grid = new Grid(2, 2);
        grid.setWidget(0, 0, new Label("Введите дату выгрузки:"));
        grid.setWidget(0, 1, dateUnloadPicker = new DatePickerBox());
        grid.setWidget(1, 0, resultLabel = new Label());
        grid.setWidget(1, 1, button = new Button("Выгрузить"));

        init();

        return grid;
    }

    private void init() {

        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (checkDate()) {
                    resultLabel.setText("");
                    WaitingManager.show();
                    Map<String,String> map = new HashMap<>();
                    map.put("operday", format.format(dateUnloadPicker.getValue()));
                    TimerEntryPoint.timerJobService.executeJob(UNLOAD_JOB_NAME, map, new AuthCheckAsyncCallback<List<TimerJobWrapper>>() {
                        @Override
                        public void onSuccess(List<TimerJobWrapper> result) {
                            WaitingManager.hide();
                            resultLabel.setText("Выгрузка прошла успешно");

                        }
                    });
                }
            };
        });

        CommonEntryPoint.operDayService.getOperDay(new AuthCheckAsyncCallback<RpcRes_Base<OperDayWrapper>>() {
            @Override
            public void onSuccess(RpcRes_Base<OperDayWrapper> result) {
                lastWorkingDate = format.parse(result.getResult().getPreviousOD());
                workingDate = format.parse(result.getResult().getCurrentOD());
                dateUnloadPicker.setValue(lastWorkingDate);
            }
        });
    }

    private boolean checkDate() {
        if (workingDate.before(dateUnloadPicker.getValue())) {
            Window.alert("Введена дата больше текущего ОД: " + format.format(workingDate));
            return false;
        }
        return true;
    }


}
