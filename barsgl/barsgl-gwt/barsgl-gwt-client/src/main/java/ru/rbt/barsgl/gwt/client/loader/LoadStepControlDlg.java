package ru.rbt.barsgl.gwt.client.loader;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.comp.enm.EnumListBox;
import ru.rbt.barsgl.gwt.client.dict.dlg.EditableDialog;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.gwt.core.utils.DialogUtils;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.LoadManagementAction;
import ru.rbt.shared.enums.Repository;
import ru.rbt.barsgl.shared.loader.LoadStepWrapper;

import java.util.Date;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import ru.rbt.barsgl.gwt.core.comp.Components;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by SotnikovAV on 27.10.2016.
 */
public class LoadStepControlDlg extends EditableDialog<LoadStepWrapper> {

    public final static String EDIT = "Назначить действие";

    private final String BUTTON_WIDTH = "120px";
    private final String LABEL_WIDTH = "130px";
    private final String FIELD_WIDTH = "120px";
    private final String LABEL_WIDTH2 = "125px";
    private final String FIELD_WIDTH2 = "135px";
    private final String TEXT_WIDTH = "80px";
    private final String LONG_WIDTH = "390px";

    private TxtBox stepCodeTxtBox;

//    private TxtBox actionTxtBox;

    private TxtBox orderTxtBox;

    private EnumListBox<LoadManagementAction> actionListBox;

    private Repository repository;
    private Long id;
    private Long ordid;
    private Date dat;

    public LoadStepControlDlg(Repository repository, String caption, FormAction action, Columns columns) {
        super(columns, action);
        this.repository = repository;
        ok.setText(TEXT_CONSTANTS.formInput_save());
        cancel.setText(TEXT_CONSTANTS.formInput_cancel());
        setCaption(caption);
    }

    @Override
    public Widget createContent() {
        VerticalPanel mainVP = new VerticalPanel();

        Grid stepCodeGrid = new Grid(1, 2);
        Label stepCodeLabel = Components.createLabel("Код шага загрузки", LABEL_WIDTH);
        stepCodeGrid.setWidget(0, 0, stepCodeLabel);
        stepCodeTxtBox = Components.createTxtBox(200, LONG_WIDTH);
        stepCodeGrid.setWidget(0, 1, stepCodeTxtBox);
        stepCodeTxtBox.setEnabled(false);
        mainVP.add(stepCodeGrid);

        Grid actionGrid = new Grid(1, 2);
        Label actionLabel = Components.createLabel("Действие", LABEL_WIDTH);
        actionGrid.setWidget(0, 0, actionLabel);
        actionListBox = new EnumListBox<>(LoadManagementAction.values());
        actionGrid.setWidget(0, 1, actionListBox);
        mainVP.add(actionGrid);

        Grid orderGrid = new Grid(1, 2);
        Label orderLabel = Components.createLabel("Порядок выполнения", LABEL_WIDTH);
        orderGrid.setWidget(0, 0, orderLabel);
        orderTxtBox = Components.createTxtIntBox(9, TEXT_WIDTH);
        orderGrid.setWidget(0, 1, orderTxtBox);
        mainVP.add(orderGrid);

        return mainVP;
    }

    @Override
    protected LoadStepWrapper createWrapper() {
        return new LoadStepWrapper();
    }

    @Override
    protected void setFields(LoadStepWrapper cnw) {
        cnw.setRepository(repository);
        cnw.setId(null == id? 0L:id);
        cnw.setOrdid(null == ordid? 0L:id);
        cnw.setDat(dat);
        cnw.setCode(stepCodeTxtBox.getValue());
        cnw.setAction(actionListBox.getValue().ordinal());
        cnw.setOrder(Integer.parseInt(orderTxtBox.getValue()));
    }

    @Override
    public void clearContent() {
        id = 0L;
        ordid = 0L;
        dat = null;
        actionListBox.setValue(LoadManagementAction.None);
        orderTxtBox.setValue(null);
    }

    @Override
    protected void fillContent() {
        clearContent();

        if (action != FormAction.UPDATE)
            return;

        row = (Row) params;
        id = getFieldValue(LoaderControlForm.COLUMN_ID);
        ordid = getFieldValue(LoaderControlForm.COLUMN_ORDID);
        dat = getFieldValue(LoaderControlForm.COLUMN_DAT);

        String stepCode = getFieldValue(LoaderControlForm.COLUMN_CODE);
        stepCodeTxtBox.setValue(stepCode);

        Object actionObj = getFieldValue(LoaderControlForm.COLUMN_ACTION);
        int actionCode = null == actionObj ? 0 : (Integer)actionObj;
        actionListBox.setValue(LoadManagementAction.values()[actionCode]);

        Object orderObj = getFieldValue(LoaderControlForm.COLUMN_ORD);
        orderTxtBox.setValue(null == orderObj ? "" : String.valueOf(orderObj));
    }
}
