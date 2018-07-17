package ru.rbt.barsgl.gwt.client.about;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.template.Version;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;

/**
 * Created by akichigi on 07.04.15.
 */
public class AboutForm extends DlgFrame {

    private Label databaseVersionLabel;

    public AboutForm() {
        super();
        setCaption("О программе");
        ok.setVisible(false);
    }

    @Override
    public Widget createContent() {
        Grid grid = new Grid(5, 2);
        grid.setText(0, 0, "Продукт");
        grid.setText(0, 1, "BARS GL");
        grid.setText(1, 0, "Версия");
        grid.setText(1, 1, Version.VERSION);
        grid.setText(2, 0, "Ревизия");
        grid.setText(2, 1, Version.REVISION);
        grid.setText(3, 0, "Бранч");
        grid.setText(3, 1, Version.BUILDSCMBRANCH);
        grid.setText(4, 0, "Версия БД");
        grid.setWidget(4, 1, databaseVersionLabel = new Label());

        grid.getCellFormatter().setWidth(0, 0, "80px");
        grid.getCellFormatter().getElement(0, 0).getStyle().setFontWeight(Style.FontWeight.BOLD);
        grid.getCellFormatter().getElement(1, 0).getStyle().setFontWeight(Style.FontWeight.BOLD);
        grid.getCellFormatter().getElement(2, 0).getStyle().setFontWeight(Style.FontWeight.BOLD);
        grid.getCellFormatter().getElement(3, 0).getStyle().setFontWeight(Style.FontWeight.BOLD);
        grid.getCellFormatter().getElement(4, 0).getStyle().setFontWeight(Style.FontWeight.BOLD);

        SimplePanel panel = new SimplePanel();
        panel.add(grid);
        panel.addStyleName("FrameDecorLine");

        return panel;
    }

    @Override
    protected void fillContent() {
        databaseVersionLabel.setText((String) params);
    }

    @Override
    protected boolean onClickOK() throws Exception {
        return true;
    }
}
