package ru.rbt.barsgl.gwt.client.operBackValue;

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
import ru.rbt.shared.enums.SecurityActionCode;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by er17503 on 25.07.2017.
 */
public class BVModeChoiceDlg extends DlgFrame {
    public enum ModeType implements HasLabel {
        NONE("Просмотр"), SINGLE("Одиночные операции"), LIST ("Операции списком");
        private String label;

        ModeType(String label) {
            this.label = label;
        }
        @Override
        public String getLabel() {
            return label;
        }
    }


    public enum StateType implements HasLabel {
        ALL("Все"), COMPLETED("Обработанные"), WORKING("В обработке"), NOTCOMPLETED ("Необработанные");
        private String label;

        StateType(String label) {
            this.label = label;
        }
        @Override
        public String getLabel() {
            return label;
        }
    }

    private ValuesBox _mode;
    private CheckBox _ownMessages;
    private ValuesBox _state;

    public BVModeChoiceDlg(){
        setCaption("Выбор способа обработки");
        ok.setText(TEXT_CONSTANTS.btn_select());
    }

    @Override
    public Widget createContent() {
        VerticalPanel panel = new VerticalPanel();
        Grid grid = new Grid(2, 2);

        grid.setText(0, 0, "Способ обработки");
        grid.setWidget(0, 1, _mode = new ValuesBox());

        _mode.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                _state.setValue(StateType.ALL);
                _state.setEnabled(_mode.getValue() == ModeType.NONE);
            }
        });

        grid.setText(1, 0, "Состояние");
        grid.setWidget(1, 1, _state = new ValuesBox());

        _mode.setWidth("150px");
        _state.setWidth("150px");

        initBoxes();
        grid.getElement().getStyle().setMarginBottom(8, Style.Unit.PX);

        panel.add(grid);
        panel.add(_ownMessages = new CheckBox("Только свои"));
        _ownMessages.setValue(true);

        return panel;
    }

    private void initBoxes(){
        for (BVModeChoiceDlg.ModeType value: BVModeChoiceDlg.ModeType.values()){

            if (value != ModeType.NONE && !SecurityChecker.checkActions(SecurityActionCode.OperHand3, SecurityActionCode.OperHand3Super)) continue;
            _mode.addItem(value, value.getLabel());
        }

        for (BVModeChoiceDlg.StateType value: BVModeChoiceDlg.StateType.values()){
            _state.addItem(value, value.getLabel());
        }
    }

    @Override
    protected void fillContent() {
        _mode.setValue((BVModeChoiceDlg.ModeType)((Object[])params)[0]);
        _ownMessages.setValue((Boolean)((Object[])params)[1]);
        _state.setValue((BVModeChoiceDlg.StateType)((Object[])params)[2]);
        _state.setEnabled(_mode.getValue() == ModeType.NONE);
    }

    @Override
    protected boolean onClickOK() throws Exception {
        params = new Object[] {_mode.getValue(), _ownMessages.getValue(), _state.getValue()};
        return true;
    }
}
