package ru.rbt.barsgl.gwt.client.events.ae;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.SecurityChecker;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.ui.ValuesBox;
import ru.rbt.barsgl.shared.HasLabel;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.shared.enums.SecurityActionCode;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;


/**
 * Created by akichigi on 07.06.16.
 */
public class StepChoiceDlg extends DlgFrame {
    public enum MessageType implements HasLabel {
        ALL("Все"), COMPLETED("Обработанные"), WORKING("В обработке"), NOTCOMPLETED ("Необработанные");
        private String label;

        MessageType(String label) {
            this.label = label;
        }
        @Override
        public String getLabel() {
            return label;
        }

    }

    public enum ChoiseType
    {
        SIMPLE,
        TECH
    }

    private ValuesBox _steps;
    private CheckBox _ownMessages;
    private ValuesBox _types;
    private InputMethod _inpMethod;
    private ChoiseType _formType = ChoiseType.SIMPLE;

    public StepChoiceDlg(InputMethod inpMethod){
        super();
        _inpMethod = inpMethod;

        setCaption("Выбор шага обработки");
        ok.setText(TEXT_CONSTANTS.btn_select());
    }

    public StepChoiceDlg(InputMethod inpMethod,ChoiseType type)
    {
        this(inpMethod);
        _formType = type;
    }

    @Override
    public Widget createContent() {
        VerticalPanel panel = new VerticalPanel();
        Grid grid = new Grid(2, 2);

        grid.setText(0, 0, "Шаг обработки");
        grid.setWidget(0, 1, _steps = new ValuesBox());
        _steps.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                _types.setValue(MessageType.ALL);
                _types.setEnabled(_steps.getValue() == BatchPostStep.NOHAND);
            }
        });

        grid.setText(1, 0, "Состояние");
        grid.setWidget(1, 1, _types = new ValuesBox());
        initMessageType();
        grid.getElement().getStyle().setMarginBottom(8, Style.Unit.PX);

        panel.add(grid);
        panel.add(_ownMessages = new CheckBox("Только свои"));
        _ownMessages.setValue(true);

        return panel;
    }

    private void initSteps(){
        _steps.addItem(BatchPostStep.NOHAND, "");
        if (_inpMethod == InputMethod.M ){
            if (SecurityChecker.checkAction(SecurityActionCode.OperInp) ||
                SecurityChecker.checkAction(SecurityActionCode.OperInpTmpl) ||
                SecurityChecker.checkAction(SecurityActionCode.AccOperInp)) _steps.addItem(BatchPostStep.HAND1,
                    "Ввод и передача на подпись");
        }else {
            if (SecurityChecker.checkAction(SecurityActionCode.OperFileLoad)) _steps.addItem(BatchPostStep.HAND1,
                   "Загрузка и передача на подпись");
        }

        if (SecurityChecker.checkAction(SecurityActionCode.OperHand2)) _steps.addItem(BatchPostStep.HAND2, "Подпись (авторизация)");
        if (_formType!=ChoiseType.TECH) {
            if (SecurityChecker.checkAction(SecurityActionCode.OperHand3))
                _steps.addItem(BatchPostStep.HAND3, "Подтверждение даты");
        }
    }

    private void initMessageType(){
        for (MessageType value: MessageType.values()){
            _types.addItem(value, value.getLabel());
        }
    }

    @Override
    protected void fillContent() {
        initSteps();
        BatchPostStep step = (BatchPostStep)((Object[])params)[0];
        Boolean ownMsg = (Boolean)((Object[])params)[1];
        MessageType type = (MessageType)((Object[])params)[2];

        _steps.setValue(step);
        _ownMessages.setValue(ownMsg);
        _types.setValue(type);
        _types.setEnabled(_steps.getValue() == BatchPostStep.NOHAND);

    }

    @Override
    protected boolean onClickOK() throws Exception {
        params = new Object[] {_steps.getValue(), _ownMessages.getValue(), _types.getValue()};
        return true;
    }
}
