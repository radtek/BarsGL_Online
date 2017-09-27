package ru.rbt.barsgl.gwt.client.checkCardsRem;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.statusbar.StatusBarManager;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.operation.CardReportWrapper;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;


import java.util.ArrayList;
import java.util.Date;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by akichigi on 15.12.16.
 */
public class CheckCardRemForm extends GridForm {
    public final static String FORM_NAME = "Проверка остатков по картотеке";

    private CheckCardRemFilterDlg dlg = null;
    private GridAction preFilterAction;

    public CheckCardRemForm() {
        super(FORM_NAME, true);
        reconfigure();
        doActionEnable(false);
        preFilterAction.execute();
    }


    private void reconfigure() {
        abw.addAction(preFilterAction = createPreFilter());
    }

    private GridAction createPreFilter() {
        return new GridAction(grid, null, "Выбор параметров проверки остатков", new Image(ImageConstants.INSTANCE.quickfilter()), 10){

            @Override
            public void execute() {
               if (dlg == null) dlg = new CheckCardRemFilterDlg();
               dlg.setDlgEvents(this);
               dlg.show(null);
            }

            public void onDlgOkClick(Object prms){
                dlg.hide();
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                CardReportWrapper wrapper = new CardReportWrapper();
                wrapper.setPostDateStr(DateTimeFormat.getFormat(wrapper.getDateFormat()).format((Date)((Object[]) prms)[0]));
                wrapper.setFilial((String)((Object[]) prms)[1]);

                BarsGLEntryPoint.operationService.getCardReport(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<CardReportWrapper>>() {
                    @Override
                    public void onSuccess(RpcRes_Base<CardReportWrapper> res) {
                        if (res.isError()){
                            DialogManager.error("Ошибка", res.getMessage());
                        } else {
                            CardReportWrapper _wrapper =  res.getResult() ;
                            StatusBarManager.ChangeStatusBarText(_wrapper.getComment(), StatusBarManager.MessageReason.MSG);
                            setSql(_wrapper.getReportSql());
                            doActionEnable(true);
                            refreshAction.execute();
                        }
                        WaitingManager.hide();
                    }
                });
            }
        };
    }

    private void doActionEnable(boolean enable){
        refreshAction.setEnable(enable);
        filterAction.setEnable(enable);
        exportToExcel.setEnable(enable);
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();

        result.addColumn(new Column("branch", Column.Type.STRING, "Код отделения", 100));
        result.addColumn(new Column("sum", Column.Type.DECIMAL, "Сумма остатка на счетах", 100));
        result.addColumn(new Column("ccy", Column.Type.STRING, "Валюта", 70));
        result.addColumn(new Column("subdealid", Column.Type.STRING, "Тип картотеки", 100));

        return  result;
    }

    @Override
    protected String prepareSql() {
        return null;
    }

    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("subdealid", Column.Sort.ASC));
        return list;
    }

    public void setSql(String text){
        sql_select = text;
        setExcelSql(sql_select);
    }
}
