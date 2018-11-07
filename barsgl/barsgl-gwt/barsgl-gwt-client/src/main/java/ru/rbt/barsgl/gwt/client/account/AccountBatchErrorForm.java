package ru.rbt.barsgl.gwt.client.account;

import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.grid.gwt.client.gridForm.GridForm;

import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.client.account.AccountBatchErrorForm.ViewType.V_ERROR;
import static ru.rbt.barsgl.gwt.client.account.AccountBatchErrorForm.ViewType.V_FULL;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getYesNoList;
import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndFilialPart;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;

/**
 * Created by er18837 on 24.10.2018.
 */
public class AccountBatchErrorForm extends GridForm {

    public enum ViewType {V_FULL, V_LOAD, V_ERROR};

    boolean _isError;
    protected Column _colIdPackage;
    protected Column _colState;
    protected Column _colRow;
    protected Column _colWasAcc;
    protected Column _colBsaacid;
    protected Column _colError1;
    protected Column _colError2;

    public AccountBatchErrorForm(String title) {
        super(title);
        reconfigure();
    }

    public ViewType getViewType(){return V_FULL;}

    public void setViewType(ViewType viewType){
        this._isError = (viewType == V_ERROR);
        _colError1.setVisible(_isError);
        _colError2.setVisible(!_isError);
        _colBsaacid.setVisible(!_isError);
        _colWasAcc.setVisible(!_isError);
    };

    private void reconfigure() {
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();
        Column col;

        result.addColumn(new Column("ID_REQ", Column.Type.LONG, "ID запроса", 60));
        result.addColumn(_colIdPackage = new Column("ID_PKG", Column.Type.LONG, "ID пакета", 60));
        result.addColumn(_colRow = new Column("RECNO", Column.Type.INTEGER, "Номер строки", 70));

        result.addColumn(_colState = new Column("STATE", Column.Type.STRING, "Статус запроса", 100));

        result.addColumn(_colError1 = new Column("ERROR_MSG1", Column.Type.STRING, "Описание ошибки", 800));

        result.addColumn(_colWasAcc = new Column("WAS_ACC", Column.Type.STRING, "Открыт ранее", 60, false, false));
        _colWasAcc.setList(getYesNoList());
        result.addColumn(_colBsaacid = new Column("BSAACID", Column.Type.STRING, "Счет ЦБ", 160));
        result.addColumn(new Column("ACID", Column.Type.STRING, "Счет Midas", 160, false, false));
        result.addColumn(new Column("BRANCH_IN", Column.Type.STRING, "Отделение", 60));
        result.addColumn(new Column("CBCC_BR", Column.Type.STRING, "Филиал", 60, false, false));
        result.addColumn(new Column("CCY_IN", Column.Type.STRING, "Код валюты", 60));
        result.addColumn(new Column("CUSTNO_IN", Column.Type.STRING, "Номер клиента", 80));

        result.addColumn(new Column("ACCTYPE_IN", Column.Type.STRING, "Accounting Type", 80));
        result.addColumn(new Column("CTYPE_IN", Column.Type.STRING, "Тип собств", 65));
        result.addColumn(new Column("CTYPE_PARM", Column.Type.STRING, "Тип собств (actparm)", 65, false, false));
        result.addColumn(new Column("CTYPE_ACC", Column.Type.STRING, "*Тип собств", 65, false, false));
        result.addColumn(new Column("TERM_IN", Column.Type.STRING, "Код срока", 65));
        result.addColumn(new Column("TERM_PARM", Column.Type.STRING, "*Код срока", 65, false, false));
        result.addColumn(new Column("ACC2_IN", Column.Type.STRING, "Баланс. счет 2", 70));
        result.addColumn(new Column("ACC2_PARM", Column.Type.STRING, "*Баланс. счет 2", 70, false, false));
        result.addColumn(new Column("PLCODE_PARM", Column.Type.STRING, "Символ ОФР", 70, false, false));
        result.addColumn(new Column("ACOD_PARM", Column.Type.STRING, "ACOD", 65, false, false));
        result.addColumn(new Column("ACSQ_PARM", Column.Type.STRING, "SQ", 65, false, false));

        result.addColumn(new Column("DEALSRC_IN", Column.Type.STRING, "Источник сделки", 75));
        result.addColumn(new Column("DEALID_IN", Column.Type.STRING, "ИД сделки", 160));
        result.addColumn(new Column("SUBDEALID_IN", Column.Type.STRING, "ИД субсделки", 160));

        result.addColumn(new Column("OPENDATE_IN", Column.Type.DATE, "Дата открытия счета", 75));
        result.addColumn(new Column("OPENDATE", Column.Type.DATE, "*Дата открытия счета", 75, false, false));

        result.addColumn(_colError2 = new Column("ERROR_MSG2", Column.Type.STRING, "Описание ошибки", 800));

        result.addColumn(col = new Column("TS_VALID", Column.Type.DATETIME, "Валидирован", 135, false, false));
        col.setFilterable(false);
        result.addColumn(col = new Column("TS_OPEN", Column.Type.DATETIME, "Зарегистрирован", 135, false, false));
        col.setFilterable(false);

        setViewType(getViewType());
        return result;
    }

    @Override
    protected String prepareSql() {
        String where = getSourceAndFilialPart("where", "", "FILIAL");
        if (isEmpty(where))
            where = " where 1=1";
        return " select ID_REQ, ID_PKG, OD_LOAD, RECNO, PKG_STATE, STATE, WAS_ACC, BSAACID, ACID,\n" +
                " BRANCH_IN, CBCC_BR, CCY_IN, CUSTNO_IN, ACCTYPE_IN, ACOD_PARM, ACSQ_PARM,\n" +
                " CTYPE_IN, CTYPE_PARM, CTYPE_ACC, TERM_IN, TERM_PARM, ACC2_IN, ACC2_PARM, PLCODE_PARM,\n" +
                " DEALSRC_IN, DEALID_IN, SUBDEALID_IN, OPENDATE_IN, OPENDATE, ACCNAME,\n" +
                " ERROR_MSG ERROR_MSG1, ERROR_MSG ERROR_MSG2, TS_VALID, TS_OPEN, TS_ENDP, USER_LOAD, USER_PROC, INVISIBLE\n" +
                " FROM V_GL_ACBATREQ"
                + where;
    }

    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem(_colIdPackage.getName(), Column.Sort.DESC));
        list.add(new SortItem(_colRow.getName(), Column.Sort.ASC));

        return list;
    }

}
