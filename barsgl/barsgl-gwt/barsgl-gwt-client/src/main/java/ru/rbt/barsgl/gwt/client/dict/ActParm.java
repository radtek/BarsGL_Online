package ru.rbt.barsgl.gwt.client.dict;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.comp.GLComponents;
import ru.rbt.barsgl.gwt.client.dict.dlg.ActParmDlg;
import ru.rbt.security.gwt.client.formmanager.FormManagerUI;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.ActParmWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.shared.enums.SecurityActionCode;

import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;
import static ru.rbt.barsgl.shared.enums.YesNoType.No;
import static ru.rbt.barsgl.shared.enums.YesNoType.Yes;

/**
 * Created by akichigi on 24.08.16.
 */
public class ActParm extends GridForm {
    public final static String FORM_NAME = "Параметры счета AccType {0}";
    public final static String FORM_NAME2 = "План счетов по Accounting Type";
    public final static String FIELD_ACCTYPE = "AccTypе";
    public final static String FIELD_CUSTYPE = "Тип собств.";
    public final static String FIELD_TERM = "Код срока";
    public final static String FIELD_ACC2 = "Б/счет 2-го порядка";
    public final static String FIELD_PLCODE = "Символ доходов/ расходов";
    public final static String FIELD_ACOD = "ACOD Midas";
    public final static String FIELD_AC_SQ = "SQ Midas";
    public final static String FIELD_DTB = "Дата начала";
    public final static String FIELD_DTE = "Дата конца";
    public final static String FIELD_ACCNAME = "Наименование AccType";
    public final static String FIELD_FL_CTRL = "Контролируемый";

    private String  initSection = null;
    private String  initProduct = null;
    private String  initSubProduct = null;
    private String  initModifier = null;
    private String  initAccType = null;
    private Column  accTypeColumn;
    private ActParmDlg dlg = null;

    public ActParm(String initSection, String initProduct, String initSubProduct, String initModifier){
        super("");
        this.initSection = initSection;
        this.initProduct = initProduct;
        this.initSubProduct = initSubProduct;
        this.initModifier = initModifier;
        initAccType = initSection + initProduct + initSubProduct + initModifier;
        title.setText(Utils.Fmt(FORM_NAME, initAccType));
        exportToExcel.setFormTitle(title.getText());
        reconfigure();
    }

    public ActParm() {
        super(Utils.Fmt(FORM_NAME2, ""));
        reconfigure();
    }

    private void reconfigure(){
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addSecureAction(editAction(), SecurityActionCode.ReferAccTypeChng);
        abw.addSecureAction(createAction(), SecurityActionCode.ReferAccTypeChng);
        abw.addSecureAction(deleteAction(), SecurityActionCode.ReferAccTypeDel);

        if (initAccType != null)
        abw.addAction(gotoAccTypeAction());
    }

    private void save(ActParmWrapper wrapper, FormAction action, final String failureMessage, final String errorMessage, final String successMessage) throws Exception {
        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
        BarsGLEntryPoint.dictionaryService.saveActParm(wrapper, action, new AuthCheckAsyncCallback<RpcRes_Base<ActParmWrapper>>() {
            @Override
            public void onFailureOthers(Throwable caught) {
                WaitingManager.hide();
                showInfo(failureMessage, caught.getLocalizedMessage());
            }

            @Override
            public void onSuccess(RpcRes_Base<ActParmWrapper> res) {
                if (res.isError()) {
                    showInfo(errorMessage, res.getMessage());
                } else {
                    showInfo(successMessage);
                    dlg.hide();
                    refreshAction.execute();
                }

                WaitingManager.hide();
            }
        });
    }


    private GridAction createAction(){
        return new GridAction(grid, null, ActParmDlg.CREATE, new Image(ImageConstants.INSTANCE.new24()), 10) {

            @Override
            public void execute() {
                if (dlg == null) dlg = new ActParmDlg();
                dlg.setFormAction(FormAction.CREATE);
                dlg.setDlgEvents(this);
                dlg.show(initAccType);
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception{
                save((ActParmWrapper) prms, FormAction.CREATE,
                        "Параметры счета не созданы",
                        "Ошибка создания параметров счета",
                        "Параметры счета созданы успешно");
            }
        };
    }

    private GridAction editAction(){
        return new GridAction(grid, null, ActParmDlg.EDIT, new Image(ImageConstants.INSTANCE.edit24()), 10, true) {

            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                if (dlg == null) dlg = new ActParmDlg();
                dlg.setFormAction(FormAction.UPDATE);
                dlg.setDlgEvents(this);
                dlg.show(row);
            }

            @Override
               public void onDlgOkClick(Object prms) throws Exception{
                save((ActParmWrapper) prms, FormAction.UPDATE,
                        "Параметры счета не сохранены",
                        "Ошибка изменения параметров счета",
                        "Параметры счета изменены успешно");
            }
        };
    }

    private GridAction deleteAction(){
        return new GridAction(grid, null, ActParmDlg.DELETE, new Image(ImageConstants.INSTANCE.stop()), 10, true) {

            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                if (dlg == null) dlg = new ActParmDlg();
                dlg.setFormAction(FormAction.DELETE);
                dlg.setDlgEvents(this);
                dlg.show(row);
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception{
                save((ActParmWrapper) prms, FormAction.DELETE,
                        "Параметры счета не удалены",
                        "Ошибка удаления параметров счета",
                        "Параметры счета удалены успешно");
            }
        };
    }

    private GridAction gotoAccTypeAction() {
        return new GridAction(grid, "AccType", "Управление AccType", null, 10) {
            @Override
            public void execute() {
                FormManagerUI.show(new AccType(initSection, initProduct, initSubProduct, initModifier));
            }
        };
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();

        result.addColumn( accTypeColumn = new Column("ACCTYPE", Column.Type.STRING, FIELD_ACCTYPE, 24, true, false, Column.Sort.ASC, ""));
        result.addColumn(new Column("CUSTYPE", Column.Type.STRING, FIELD_CUSTYPE, 20, true, false, Column.Sort.ASC, ""));
        result.addColumn(new Column("TERM", Column.Type.STRING, FIELD_TERM, 20, true, false, Column.Sort.ASC, ""));
        result.addColumn(new Column("ACC2", Column.Type.STRING, FIELD_ACC2, 22, true, false, Column.Sort.ASC, ""));
        result.addColumn(new Column("PLCODE", Column.Type.STRING, FIELD_PLCODE, 25));
        result.addColumn(new Column("ACOD", Column.Type.STRING, FIELD_ACOD, 20));
        result.addColumn(new Column("AC_SQ", Column.Type.STRING, FIELD_AC_SQ, 20));
        result.addColumn(new Column("DTB", Column.Type.DATE, FIELD_DTB, 25));
        result.addColumn(new Column("DTE", Column.Type.DATE, FIELD_DTE, 25));
        result.addColumn(new Column("ACCNAME", Column.Type.STRING, FIELD_ACCNAME, 240));
        Column col;
        result.addColumn(col = new Column("FL_CTRL", Column.Type.STRING, FIELD_FL_CTRL, 20, false, false));
        col.setList(GLComponents.getArrayValuesList(new String[]{"Да", "Нет"}));
        return result;
    }

    @Override
    protected String prepareSql() {
        return "select * from ("+
                        "select PARM.ACCTYPE, PARM.CUSTYPE, PARM.TERM, PARM.ACC2, PARM.PLCODE, PARM.ACOD, PARM.AC_SQ, PARM.DTB, PARM.DTE, NM.ACCNAME, " +
                        "case when NM.FL_CTRL = 'N' then trim('" + No.getLabel() + "') " +
                        "else trim('" + Yes.getLabel() + "') " +
                        "end FL_CTRL " +
                        "from GL_ACTPARM PARM join GL_ACTNAME NM on NM.ACCTYPE = PARM.ACCTYPE" +
                        " )v";
    }

    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("ACCTYPE", Column.Sort.ASC));
        list.add(new SortItem("CUSTYPE", Column.Sort.ASC));
        list.add(new SortItem("TERM", Column.Sort.ASC));
        list.add(new SortItem("ACC2", Column.Sort.ASC));
        list.add(new SortItem("DTB", Column.Sort.ASC));
        return list;
    }

    @Override
    protected ArrayList<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams) {
        ArrayList<FilterItem> list = new ArrayList<FilterItem>();
        FilterItem item = new FilterItem(accTypeColumn, FilterCriteria.EQ, initAccType, true);
        item.setReadOnly(true);
        list.add(item);
        return list;
    }
}
