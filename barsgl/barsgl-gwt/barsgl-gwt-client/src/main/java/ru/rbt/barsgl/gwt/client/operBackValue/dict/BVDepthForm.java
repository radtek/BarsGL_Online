package ru.rbt.barsgl.gwt.client.operBackValue.dict;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MenuItem;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.comp.PopupMenuBuilder;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.statusbar.StatusBarManager;
import ru.rbt.barsgl.gwt.core.widgets.IGridRowChanged;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.ClientDateUtils;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.BVSourceDealWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.security.gwt.client.CommonEntryPoint;
import ru.rbt.shared.enums.SecurityActionCode;


import java.util.ArrayList;
import java.util.Date;

import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.*;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by er17503 on 15.08.2017.
 */
public class BVDepthForm extends GridForm{
    public static final String FORM_NAME = "Глубина backvalue";
    private final String currentTitle = "Текущие записи";
    private final String allTitle = "Все записи";
    private GridAction _deleteAction;
    private BVDepthDlg dlg;
    private String _sql = "select * from GL_BVPARM ";
    private String _where = "where DTB<='{0}' and (DTE is null or DTE >='{0}')";
    private boolean isCurrent;

    public BVDepthForm(){
        super(FORM_NAME);
        reconfigure();
        setRowChangeEventHandler();
        isCurrent = true;
        changeCurrentMode();
    }

    private void reconfigure(){
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addSecureAction(editAction(), SecurityActionCode.ReferBackValue);
        abw.addSecureAction(createAction(), SecurityActionCode.ReferBackValue);
        abw.addSecureAction(_deleteAction = deleteAction(), SecurityActionCode.ReferBackValue);
        abw.addAction(currentAction());
    }

    private GridAction currentAction(){

        final PopupMenuBuilder builder = new PopupMenuBuilder(abw, "Режим отображения глубины backvalue",  new Image(ImageConstants.INSTANCE.oper_go()));

        MenuItem currentItem = new MenuItem(currentTitle, new Command() {

            @Override
            public void execute() {
                builder.hidePopup();
                isCurrent = true;
                changeCurrentMode();
            }
        });

        MenuItem allItem = new MenuItem(allTitle, new Command() {

            @Override
            public void execute() {
                builder.hidePopup();
                isCurrent = false;
                changeCurrentMode();
            }
        });

        builder.addItem(currentItem);
        builder.addSeparator();
        builder.addItem(allItem);
        return  builder.toAction(grid);
    }

    private void changeCurrentMode(){
        changeQuery();
        clearFilter();

        String pattern = "Режим отображения : [{0}]";
        StatusBarManager.ChangeStatusBarText(Utils.Fmt(pattern, isCurrent ? currentTitle : allTitle), StatusBarManager.MessageReason.MSG);
    }

    private void clearFilter(){
        filterAction.clearFilterCriteria(true);
        grid.setInitialFilterCriteria(null);
        refreshAction.setFilterCriteria(null);
        refreshAction.execute();
    }

    private GridAction createAction() {
        return new GridAction(grid, null, "Создать глубину backvalue", new Image(ImageConstants.INSTANCE.new24()), 10) {
            @Override
            public void execute() {
               dlg = dlg == null ? new BVDepthDlg() : dlg;
                dlg.setAction(FormAction.CREATE);
                dlg.setCaption(this.getHint());
                dlg.setDlgEvents(this);
                dlg.show(null);
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception{
                methodCaller(dlg, "Создание глубины backvalue не удалось", createWrapper(prms), FormAction.CREATE);
            }
        };
    }

    private GridAction editAction() {
        return new GridAction(grid, null, "Изменить глубину backvalue", new Image(ImageConstants.INSTANCE.edit24()), 10, true) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                dlg = dlg == null ? new  BVDepthDlg() : dlg;
                dlg.setAction(FormAction.UPDATE);
                dlg.setCaption(this.getHint());
                dlg.setDlgEvents(this);
                Object[] val = new Object[] { grid.getFieldValue("ID_SRC"), grid.getFieldValue("BV_SHIFT"),
                                              grid.getFieldValue("DTB"), grid.getFieldValue("DTE")};
                dlg.show(val);
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception{
                methodCaller(dlg, "Изменение глубины backvalue не удалось", createWrapper(prms), FormAction.UPDATE);
            }
        };
    }

    private GridAction deleteAction() {
        return new GridAction(grid, null, "Удалить глубину backvalue", new Image(ImageConstants.INSTANCE.stop()), 10, true) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                dlg = dlg == null ? new  BVDepthDlg() : dlg;
                dlg.setAction(FormAction.DELETE);
                dlg.setCaption(this.getHint());
                dlg.setDlgEvents(this);
                Object[] val = new Object[] { grid.getFieldValue("ID_SRC"), grid.getFieldValue("BV_SHIFT"),
                                              grid.getFieldValue("DTB"), grid.getFieldValue("DTE")};
                dlg.show(val);
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception{
                final Object _prms = prms;
                DialogManager.confirm("Удалить","Удалить глубину backvalue?", "Да", "Нет", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        methodCaller(dlg, "Удаление глубины backvalueа не удалось", createWrapper(_prms), FormAction.DELETE);
                    }
                }, null);
            }
        };
    }

    private void methodCaller(final DlgFrame dlg, final String message, final BVSourceDealWrapper wrapper, final FormAction action){
        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

        BarsGLEntryPoint.dictionaryService.saveBVSourceDeal(wrapper, action, new AuthCheckAsyncCallback<RpcRes_Base<BVSourceDealWrapper>>() {

            @Override
            public void onSuccess(RpcRes_Base<BVSourceDealWrapper>  res) {
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

    private BVSourceDealWrapper createWrapper(final Object prms){
        Object[] objs = (Object[]) prms;
        BVSourceDealWrapper wrapper = new BVSourceDealWrapper();
        wrapper.setSourceDeal((String)objs[0]);
        wrapper.setDepth((Integer)objs[1]);
        wrapper.setStartDateStr(ClientDateUtils.Date2String((Date)objs[2]));
        wrapper.setEndDateStr(ClientDateUtils.Date2String((Date)objs[3]));
        return wrapper;
    }

    @Override
    protected Table prepareTable() {
        Table table = new Table();
        Column col;
        table.addColumn(new Column("ID_SRC", STRING, "Система источник", 150));
        table.addColumn(new Column("BV_SHIFT", INTEGER, "Глубина backvalue", 80));
        table.addColumn(col = new Column("DTB", DATE, "Дата начала", 80));
        col.setFormat("dd.MM.yyyy");
        table.addColumn(col = new Column("DTE", DATE, "Дата окончания", 80));
        col.setFormat("dd.MM.yyyy");
        table.addColumn( new Column("USER_NAME", STRING, "Пользователь", 100, false, false));

        table.addColumn(col = new Column("OTS", DATETIME, "Дата создания", 80, false, false));
        col.setFormat("dd.MM.yyyy hh:mm:ss");
        col.setFilterable(false);

        return table;
    }

    @Override
    protected String prepareSql() {
        return _sql +  Utils.Fmt(_where, ClientDateUtils.Date2String(CommonEntryPoint.CURRENT_OPER_DAY));
    }

    private void changeQuery(){
        String sql = _sql + (isCurrent ? Utils.Fmt(_where, ClientDateUtils.Date2String(CommonEntryPoint.CURRENT_OPER_DAY)) : "");
        setSql(sql);
        setExcelSql(sql);
    }

    @Override
    protected ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("DTB", Column.Sort.DESC));
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
                    flag = ((Date)getFieldByName("DTB").getValue()).compareTo(CommonEntryPoint.CURRENT_OPER_DAY) == -1;
                }

                _deleteAction.setEnable(!flag);
            }
        });
    }
}
