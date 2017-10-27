package ru.rbt.barsgl.gwt.client.events.ae;

import ru.rbt.shared.enums.SecurityActionCode;
import com.google.gwt.user.client.Window;
import ru.rbt.barsgl.gwt.client.quickFilter.DateHistoryQuickFilterParams;
import ru.rbt.barsgl.gwt.client.quickFilter.DateIntervalQuickFilterAction;
import ru.rbt.barsgl.gwt.client.quickFilter.IQuickFilterParams;
import ru.rbt.barsgl.gwt.core.SecurityChecker;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.gwt.core.widgets.IGridRowChanged;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.enums.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getEnumLabelsList;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getYesNoList;
import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndFilialPart;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;
import ru.rbt.shared.enums.SecurityActionCode;

/**
 * Created by akichigi on 03.06.16.
 */
public class OperEventHistoryForm  extends OperSuperBase {
    public static final String FORM_NAME = "История создания операций";

    private Column colProcDate;
    private Column colInvisible;

    private String _select = "";
    private String _where_ownMessages = "";
    private String _where_message_type = "";

    private GridAction statistics;

    public OperEventHistoryForm(){
        super(FORM_NAME, true);
        _select = getSelectClause();
        reconfigure();
        setRowChangeEventHandler();
    }

    private void reconfigure() {
        GridAction quickFilterAction;
        abw.addAction(quickFilterAction = new DateHistoryQuickFilterAction(grid, colProcDate, colInvisible));;
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addAction(createPreview());
        abw.addAction(statistics = new PackageStatisticsAction(grid));
        quickFilterAction.execute();
    }


    @Override
    protected Table prepareTable() {
        Table result = new Table();
        Column col;

        result.addColumn(new Column("ID", Column.Type.LONG, "ID запроса", 70));
        result.addColumn(new Column("GLOID_REF", Column.Type.LONG, "ID операции", 70));
        result.addColumn(col = new Column("STATE", Column.Type.STRING, "Статус", 90));
        col.setList(getEnumLabelsList(BatchPostStatus.values()));
        result.addColumn(new Column("ECODE", Column.Type.INTEGER, "Код ошибки", 60));

        result.addColumn(new Column("ID_PKG", Column.Type.LONG, "ID пакета", 60));
        result.addColumn(new Column("NROW", Column.Type.INTEGER, "Строка в файле", 60, false, false));
        result.addColumn(new Column("FILE_NAME", Column.Type.STRING, "Имя файла", 100, false, false));

        result.addColumn(colInvisible = new Column("INVISIBLE", Column.Type.STRING, "Удален", 60));
        colInvisible.setList(getInvisibleList());
        result.addColumn(col = new Column("INP_METHOD", Column.Type.STRING, "Способ ввода", 80));
        col.setList(getEnumLabelsList(InputMethod.values()));

        result.addColumn(new Column("ID_PAR", Column.Type.LONG, "ID род. запроса", 80));
        result.addColumn(new Column("ID_PREV", Column.Type.LONG, "ID пред. запроса", 80));
        result.addColumn(new Column("SRC_PST", Column.Type.STRING, "Источник сделки", 80));

        result.addColumn(new Column("DEAL_ID", Column.Type.STRING, "ИД сделки", 70));
        result.addColumn(new Column("SUBDEALID", Column.Type.STRING, "ИД субсделки", 80, false, false));
        result.addColumn(new Column("PMT_REF", Column.Type.STRING, "ИД платежа", 100));

        result.addColumn(colProcDate = new Column("PROCDATE", Column.Type.DATE, "Дата опердня", 72));
        colProcDate.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("VDATE", Column.Type.DATE, "Дата валютирования", 80));
        col.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("POSTDATE", Column.Type.DATE, "Дата проводки", 72));
        col.setFormat("dd.MM.yyyy");

        result.addColumn(new Column("AC_DR", Column.Type.STRING, "Счет ДБ", 160));
        result.addColumn(new Column("CCY_DR", Column.Type.STRING, "Валюта ДБ", 60, false, false));
        result.addColumn(new Column("AMT_DR", Column.Type.DECIMAL, "Сумма ДБ", 100));
        result.addColumn(new Column("CBCC_DR", Column.Type.STRING, "Филиал ДБ", 60, false, false));

        result.addColumn(new Column("AC_CR", Column.Type.STRING, "Счет КР", 160));
        result.addColumn(new Column("CCY_CR", Column.Type.STRING, "Валюта КР", 60, false, false));
        result.addColumn(new Column("AMT_CR", Column.Type.DECIMAL, "Сумма КР", 100));
        result.addColumn(new Column("CBCC_CR", Column.Type.STRING, "Филиал КР", 60, false, false));

        result.addColumn(new Column("AMTRU", Column.Type.DECIMAL, "Сумма в рублях", 100, false, false));

        result.addColumn(new Column("NRT", Column.Type.STRING, "Основание ENG", 250, false, false));
        result.addColumn(new Column("RNRTL", Column.Type.STRING, "Основание RUS", 250, false, false));
        result.addColumn(new Column("RNRTS", Column.Type.STRING, "Основание короткое", 200, false, false));
        result.addColumn(new Column("DEPT_ID", Column.Type.STRING, "Подразделение", 90, false, false));
        result.addColumn(new Column("PRFCNTR", Column.Type.STRING, "Профит центр", 100, false, false));
        result.addColumn(col = new Column("FCHNG", Column.Type.STRING, "Исправительная", 100, false, false));
        col.setList(getYesNoList());
        result.addColumn(new Column("EMSG", Column.Type.STRING, "Описание ошибки", 800));

        result.addColumn(new Column("USER_NAME", Column.Type.STRING, "Логин 1 руки", 100));
        result.addColumn(new Column("OTS", Column.Type.DATETIME, "Дата создания", 130));
        result.addColumn(new Column("HEADBRANCH", Column.Type.STRING, "Филиал 1 руки", 100, false, false));
        result.addColumn(new Column("USER_AU2", Column.Type.STRING, "Логин 2 руки", 100));
        result.addColumn(new Column("OTS_AU2", Column.Type.DATETIME, "Дата подписи", 130));
        result.addColumn(new Column("USER_AU3", Column.Type.STRING, "Логин 3 руки", 100));
        result.addColumn(new Column("OTS_AU3", Column.Type.DATETIME, "Дата подтверж.", 130));
        result.addColumn(new Column("USER_CHNG", Column.Type.STRING, "Логин изменения", 100));
        result.addColumn(new Column("OTS_CHNG", Column.Type.DATETIME, "Дата изменения", 130));
        result.addColumn(new Column("MVMNT_OFF", Column.Type.STRING, "Искл.запроса к АБС", 150, false, false));
        result.addColumn(new Column("SRV_REF", Column.Type.STRING, "ID запроса в АБС", 150));
        result.addColumn(new Column("SEND_SRV", Column.Type.DATETIME, "Запрос в АБС", 120, false, false));
        result.addColumn(new Column("OTS_SRV", Column.Type.DATETIME, "Ответ от АБС", 120));
        result.addColumn(new Column("DESCRDENY", Column.Type.STRING, "Причина возврата", 300));

        return result;
    }

    private HashMap getInvisibleList() {
        HashMap invisibleMap = getEnumLabelsList(InvisibleType.values());
       // String visName = (String) invisibleMap.remove(InvisibleType.N.name());
       // invisibleMap.put("", visName);
        return invisibleMap;
    }

    protected String getSelectClause() {
        String whereSFpart = "";
        String where = "";
        if ( !SecurityChecker.checkAction(SecurityActionCode.OperHistory)) {
                where = (isEmpty(whereSFpart = getSourceAndFilialPart("where", "PST.SRC_PST", "PST.CBCC_CR", "PST.CBCC_DR")) ? " where" : whereSFpart + " and") +
                    " INVISIBLE <> '" + InvisibleType.H.name() + "' ";
        }
        if(isEmpty(where)) {
            where = " where 1=1";
        }

        return
                "select PST.ID, PST.GLOID_REF, PST.STATE, PST.ECODE, PST.ID_PKG, PST.NROW, PKG.FILE_NAME, " +
                "PST.INVISIBLE, " +
                // "case when PST.INVISIBLE = '" + InvisibleType.N.name() + "' then '' else PST.INVISIBLE end as INVISIBLE, " +
                "PST.INP_METHOD, PST.ID_PAR, PST.ID_PREV, PST.SRV_REF, PST.SEND_SRV, PST.OTS_SRV, PST.SRC_PST, " +
                "PST.DEAL_ID, PST.SUBDEALID, PST.PMT_REF, PST.PROCDATE, PST.VDATE, PST.POSTDATE, " +
                "PST.AC_DR, PST.CCY_DR, PST.AMT_DR, PST.CBCC_DR, PST.AC_CR, PST.CCY_CR, PST.AMT_CR, PST.CBCC_CR, " +
                "PST.AMTRU, PST.NRT, PST.RNRTL, PST.RNRTS, PST.DEPT_ID, PST.PRFCNTR, PST.FCHNG, PST.EMSG, " +
                "PST.USER_NAME, PST.OTS, PST.HEADBRANCH, PST.USER_AU2, PST.OTS_AU2, PST.USER_AU3, PST.OTS_AU3, " +
                "PST.USER_CHNG, PST.OTS_CHNG, PST.DESCRDENY, PKG.MVMNT_OFF " +
                "from GL_BATPST PST left join GL_BATPKG PKG on PST.ID_PKG = PKG.ID_PKG " + where;
    }

    private void changeWhereOwnMessagesPart(){
        _where_ownMessages = getOwnMessagesClause(_ownMessages, BatchPostStep.NOHAND, "PST");
    }

    private void changeWhereMessageTypePart(){
        _where_message_type = getWhereMessageTypePart("PST");
    }

    @Override
    protected String prepareSql() {
        return getSelectClause();
    }

    public void setSql(String text){
        sql_select = text;
        setExcelSql(sql_select);
    }

    private String sql(){
        return new StringBuilder()
                .append(_select )
                .append(_where_message_type)
                .append(_where_ownMessages).toString();
    }

    @Override
    protected ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("ID", Column.Sort.DESC));
        return list;
    }

    class DateHistoryQuickFilterAction extends DateIntervalQuickFilterAction {
        private Column histColumn;

        public DateHistoryQuickFilterAction(GridWidget grid, Column dateColumn, Column histColumn) {
            super(grid, dateColumn);
            this.histColumn = histColumn;
        }

        @Override
        public DlgFrame getFilterDialog() {
            return new OperHistoryDlg();
        }

        @Override
        public IQuickFilterParams getFilterParams() {
            return new DateHistoryQuickFilterParams(dateColumn, histColumn);
        }

        @Override
        public Object[] getInitialFilterParams(Date operday, Date prevday) { return new Object[]{operday, operday, ""}; }

        @Override
        public void beforeFireFilterEvent(IQuickFilterParams filterParams) {
            DateHistoryQuickFilterParams params = (DateHistoryQuickFilterParams)filterParams;
            _ownMessages = params.getOwnMessages();
            _type = params.getMsgType();

            changeWhereMessageTypePart();
            changeWhereOwnMessagesPart();

            setSql(sql());
        }
    }

    private void setRowChangeEventHandler() {
        grid.setRowChangedEvent(new IGridRowChanged() {
            @Override
            public void onRowChanged(Row row) {
                if (grid.getCurrentRow() == null) return;
                boolean flag;
                if (grid.getRowCount() == 0){
                    flag = false;
                }else{
                    Long idPkg = (Long)grid.getCurrentRow().getField(grid.getTable().getColumns().getColumnIndexByName("ID_PKG")).getValue();
                    flag = (null != idPkg && 0 != idPkg);
                }
                statistics.setEnable(flag);
            }
        });
    }
}
