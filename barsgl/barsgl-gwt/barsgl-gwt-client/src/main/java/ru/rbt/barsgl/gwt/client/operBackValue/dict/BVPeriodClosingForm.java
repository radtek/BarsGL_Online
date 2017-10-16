package ru.rbt.barsgl.gwt.client.operBackValue.dict;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.IGridRowChanged;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.ClientDateUtils;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.ClosedReportPeriodWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.security.gwt.client.CommonEntryPoint;
import ru.rbt.shared.enums.SecurityActionCode;

import java.util.ArrayList;
import java.util.Date;

import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.DATE;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.DATETIME;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.STRING;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by er17503 on 15.08.2017.
 */
public class BVPeriodClosingForm extends GridForm {
    public static final String FORM_NAME = "Закрытие отчетных периодов";
    private GridAction _editAction;
    private GridAction _deleteAction;
    private BVPeriodClosingDlg dlg;

    public BVPeriodClosingForm(){
        super(FORM_NAME);
        reconfigure();
        setRowChangeEventHandler();
    }

    private void reconfigure(){
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addSecureAction(_editAction = editAction(), SecurityActionCode.ReferBackValue);
        abw.addSecureAction(createAction(), SecurityActionCode.ReferBackValue);
        abw.addSecureAction(_deleteAction = deleteAction(), SecurityActionCode.ReferBackValue);
    }


    private GridAction createAction() {
        return new GridAction(grid, null, "Создать отчетный период", new Image(ImageConstants.INSTANCE.new24()), 10) {
            @Override
            public void execute() {
                dlg = dlg == null ? new BVPeriodClosingDlg() : dlg;
                dlg.setAction(FormAction.CREATE);
                dlg.setCaption(this.getHint());
                dlg.setDlgEvents(this);
                dlg.show(null);
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception{
                methodCaller(dlg, "Создание отчетного периода не удалось", createWrapper(prms), FormAction.CREATE);
            }
        };
    }

    private GridAction editAction() {
        return new GridAction(grid, null, "Изменить отчетный период", new Image(ImageConstants.INSTANCE.edit24()), 10, true) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                dlg = dlg == null ? new BVPeriodClosingDlg() : dlg;
                dlg.setAction(FormAction.UPDATE);
                dlg.setCaption(this.getHint());
                dlg.setDlgEvents(this);
                Object[] val = new Object[] {(Date)grid.getFieldValue("PRD_LDATE"), (Date)grid.getFieldValue("PRD_CUTDATE")};
                dlg.show(val);
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception{
                methodCaller(dlg, "Изменение отчетного периода не удалось", createWrapper(prms), FormAction.UPDATE);
            }
        };
    }

    private GridAction deleteAction() {
        return new GridAction(grid, null, "Удалить отчетный период", new Image(ImageConstants.INSTANCE.stop()), 10, true) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                dlg = dlg == null ? new BVPeriodClosingDlg() : dlg;
                dlg.setAction(FormAction.DELETE);
                dlg.setCaption(this.getHint());
                dlg.setDlgEvents(this);
                Object[] val = new Object[] {(Date)grid.getFieldValue("PRD_LDATE"), (Date)grid.getFieldValue("PRD_CUTDATE")};
                dlg.show(val);
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception{
                final Object _prms = prms;
                DialogManager.confirm("Удалить","Удалить отчетный период?", "Да", "Нет", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        methodCaller(dlg, "Удаление отчетного периода не удалось", createWrapper(_prms), FormAction.DELETE);
                    }
                }, null);
            }
        };
    }

    private ClosedReportPeriodWrapper createWrapper(final Object prms){
        Object[] objs = (Object[]) prms;
        ClosedReportPeriodWrapper wrapper = new ClosedReportPeriodWrapper();
        wrapper.setLastDateStr(ClientDateUtils.Date2String((Date)objs[0]));
        wrapper.setCutDateStr(ClientDateUtils.Date2String((Date)objs[1]));
        return wrapper;
    }

    private void methodCaller(final DlgFrame dlg, final String message, final ClosedReportPeriodWrapper wrapper, final FormAction action){
        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

        BarsGLEntryPoint.dictionaryService.saveClosedReportPeriod(wrapper, action, new AuthCheckAsyncCallback<RpcRes_Base<ClosedReportPeriodWrapper>>() {

          @Override
            public void onSuccess(RpcRes_Base<ClosedReportPeriodWrapper>  res) {
                if (res.isError()){
                    DialogManager.error("Ошибка", message + "\nОшибка: " + res.getMessage());
                } else {
                    if (dlg != null) dlg.hide();
                    refreshAction.execute();
                    showInfo(res.getMessage());
                }
                WaitingManager.hide();
            }
        });
    }

    @Override
    protected Table prepareTable() {
        Table table = new Table();
        Column col;

        table.addColumn(col = new Column("PRD_LDATE", DATE, "Дата завершения отчетного периода", 80));
        col.setFormat("dd.MM.yyyy");
        table.addColumn(col = new Column("PRD_CUTDATE", DATE, "Дата закрытия отчетного периода", 80));
        col.setFormat("dd.MM.yyyy");
        table.addColumn( new Column("USER_NAME", STRING, "Пользователь", 100, false, false));
        table.addColumn(col = new Column("OTS", DATETIME, "Дата создания", 80, false, false));
        col.setFormat("dd.MM.yyyy hh:mm:ss");
        col.setFilterable(false);

        return table;
    }

    @Override
    protected String prepareSql() {
        return "select * from GL_CRPRD ";
    }

    @Override
    protected ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("PRD_LDATE", Column.Sort.DESC));
        return list;
    }

    private void setRowChangeEventHandler() {
        grid.setRowChangedEvent(new IGridRowChanged() {
            @Override
            public void onRowChanged(Row row) {
                boolean flag;
                if (grid.getCurrentRow() == null) return;
                if (grid.getRowCount() == 0){
                   flag = true;
                }else{
                   flag = ((Date)getFieldByName("PRD_CUTDATE").getValue()).compareTo(CommonEntryPoint.CURRENT_OPER_DAY) == -1;
                }

                _editAction.setEnable(!flag);
                _deleteAction.setEnable(!flag);
            }
        });
    }
}
