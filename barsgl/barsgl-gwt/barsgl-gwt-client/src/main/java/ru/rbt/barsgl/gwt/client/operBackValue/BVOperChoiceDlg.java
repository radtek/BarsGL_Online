package ru.rbt.barsgl.gwt.client.operBackValue;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.ui.ValuesBox;
import ru.rbt.barsgl.shared.HasLabel;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by er17503 on 07.08.2017.
 */
public class BVOperChoiceDlg extends DlgFrame {
    public enum ModeType implements HasLabel {
        STANDARD("Стандарт"), NONSTANDARD("Нестандарт");
        private String label;

        ModeType(String label) {
            this.label = label;
        }
        @Override
        public String getLabel() {
            return label;
        }
    }


    public enum SpecType implements HasLabel {
        MANUAL("Вручную"), AUTO("Автомат"), K_Plus_TP("К+ТР"), FAN ("Веер");
        private String label;

        SpecType(String label) {
            this.label = label;
        }
        @Override
        public String getLabel() {
            return label;
        }
    }

    private ValuesBox _mode;
    private CheckBox _owner;
    private ValuesBox _spec;

    public BVOperChoiceDlg(){
        setCaption("Выбор операций Backvalue");
        ok.setText(TEXT_CONSTANTS.btn_select());
    }

    @Override
    public Widget createContent() {
        VerticalPanel panel = new VerticalPanel();
        Grid grid = new Grid(2, 2);

        grid.setText(0, 0, "Тип обработки Backvalue");
        grid.setWidget(0, 1, _mode = new ValuesBox());

        _mode.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                onModeTypeChange((BVOperChoiceDlg.ModeType)_mode.getValue());
                _owner.setEnabled(_mode.getValue() == ModeType.STANDARD && _spec.getValue() == SpecType.MANUAL);
            }
        });

        grid.setText(1, 0, "Специфика");
        grid.setWidget(1, 1, _spec = new ValuesBox());

        _spec.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                _owner.setEnabled(_mode.getValue() == ModeType.STANDARD && _spec.getValue() == SpecType.MANUAL);
            }
        });

        _mode.setWidth("150px");
        _spec.setWidth("150px");

        initBoxes();
        grid.getElement().getStyle().setMarginBottom(8, Style.Unit.PX);

        panel.add(grid);
        panel.add(_owner = new CheckBox("Только свои"));

        return panel;
    }

    private void initBoxes(){
        for (BVOperChoiceDlg.ModeType value: BVOperChoiceDlg.ModeType.values()){
            _mode.addItem(value, value.getLabel());
        }
        onModeTypeChange((BVOperChoiceDlg.ModeType)_mode.getValue());
    }

    private void onModeTypeChange(BVOperChoiceDlg.ModeType mode){
        _spec.clear();
        if (mode == ModeType.STANDARD){
            _spec.addItem(SpecType.MANUAL, SpecType.MANUAL.getLabel());
            _spec.addItem(SpecType.AUTO, SpecType.AUTO.getLabel());
        }
        else {
            _spec.addItem(SpecType.K_Plus_TP, SpecType.K_Plus_TP.getLabel());
            _spec.addItem(SpecType.FAN, SpecType.FAN.getLabel());
        }
    }

    @Override
    protected void fillContent() {
        _mode.setValue((BVOperChoiceDlg.ModeType)((Object[])params)[0]);
        _owner.setValue((Boolean)((Object[])params)[1]);
        onModeTypeChange((BVOperChoiceDlg.ModeType)_mode.getValue());
        _spec.setValue((BVOperChoiceDlg.SpecType)((Object[])params)[2]);
        _owner.setEnabled(_mode.getValue() == ModeType.STANDARD && _spec.getValue() == SpecType.MANUAL);
    }

    @Override
    protected boolean onClickOK() throws Exception {
        params = new Object[] {_mode.getValue(), _owner.getValue(), _spec.getValue()};
        return true;
    }
}
