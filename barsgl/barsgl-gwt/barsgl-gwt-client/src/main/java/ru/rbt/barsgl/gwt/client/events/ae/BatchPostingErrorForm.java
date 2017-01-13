package ru.rbt.barsgl.gwt.client.events.ae;

import ru.rbt.barsgl.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;

/**
 * Created by akichigi on 22.06.16.
 */
public class BatchPostingErrorForm extends GridForm {
    public static final String FORM_NAME = "Строки пакета";

    protected Column colPackage;
    protected Column colRow;
    protected Column colError;

    public BatchPostingErrorForm() {
        super(FORM_NAME);
        reconfigure();
    }

    private void reconfigure() {
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();
        Column col;

        result.addColumn(new Column("ID", Column.Type.LONG, "ID запроса", 70, false, false));
        result.addColumn(new Column("GLOID_REF", Column.Type.LONG, "ID операции", 70));
        result.addColumn(new Column("STATE", Column.Type.STRING, "Статус", 80, false, false));
        result.addColumn(colError = new Column("ECODE", Column.Type.INTEGER, "Код ошибки", 60));

        result.addColumn(colPackage = new Column("ID_PKG", Column.Type.LONG, "ID пакета", 60));
        result.addColumn(colRow = new Column("NROW", Column.Type.INTEGER, "Строка в файле", 60));
        result.addColumn(new Column("EMSG", Column.Type.STRING, "Описание ошибки", 800));
        result.addColumn(new Column("FILE_NAME", Column.Type.STRING, "Имя файла", 100));

        result.addColumn(new Column("SRC_PST", Column.Type.STRING, "Источник сделки", 80));

        result.addColumn(new Column("DEAL_ID", Column.Type.STRING, "ИД сделки", 70));
        result.addColumn(new Column("SUBDEALID", Column.Type.STRING, "ИД субсделки", 80, false, false));
        result.addColumn(new Column("PMT_REF", Column.Type.STRING, "ИД платежа", 100));

        result.addColumn(col = new Column("PROCDATE", Column.Type.DATE, "Дата ОД рег.", 80));
        col.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("VDATE", Column.Type.DATE, "Дата валютир.", 80));
        col.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("POSTDATE", Column.Type.DATE, "Дата проводки", 80));
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
        result.addColumn(new Column("FCHNG", Column.Type.STRING, "Исправительная", 100, false, false));


        result.addColumn(new Column("USER_NAME", Column.Type.STRING, "Логин 1 руки", 100, false, false));
        result.addColumn(col = new Column("OTS", Column.Type.DATETIME, "Дата создания", 130, false, false));
        col.setFormat("dd.MM.yyyy HH:mm:ss");
        result.addColumn(new Column("HEADBRANCH", Column.Type.STRING, "Филиал 1 руки", 100, false, false));
        result.addColumn(new Column("USER_AU2", Column.Type.STRING, "Логин 2 руки", 100, false, false));
        result.addColumn(col = new Column("OTS_AU2", Column.Type.DATETIME, "Дата подписи", 130, false, false));
        col.setFormat("dd.MM.yyyy HH:mm:ss");
        result.addColumn(new Column("USER_AU3", Column.Type.STRING, "Логин 3 руки", 100, false, false));
        result.addColumn(col = new Column("OTS_AU3", Column.Type.DATETIME, "Дата подтверж.", 130, false, false));
        col.setFormat("dd.MM.yyyy HH:mm:ss");
        result.addColumn(new Column("USER_CHNG", Column.Type.STRING, "Логин изменения", 100, false, false));
        result.addColumn(col = new Column("OTS_CHNG", Column.Type.DATETIME, "Дата изменения", 130, false, false));
        col.setFormat("dd.MM.yyyy HH:mm:ss");
        result.addColumn(new Column("DESCRDENY", Column.Type.STRING, "Причина возврата", 300, false, false));

        return result;
    }

    @Override
    protected String prepareSql() {

        return  "select * from ("+
                "select PST.ID, PST.GLOID_REF, PST.STATE, PST.ECODE, PST.ID_PKG, PST.NROW, PKG.FILE_NAME, " +
                "PST.INVISIBLE, PST.INP_METHOD, PST.ID_PAR, PST.ID_PREV, PST.SRV_REF, PST.OTS_SRV, PST.SRC_PST, " +
                "PST.DEAL_ID, PST.SUBDEALID, PST.PMT_REF, PST.PROCDATE, PST.VDATE, PST.POSTDATE, " +
                "PST.AC_DR, PST.CCY_DR, PST.AMT_DR, PST.CBCC_DR, PST.AC_CR, PST.CCY_CR, PST.AMT_CR, PST.CBCC_CR, " +
                "PST.AMTRU, PST.NRT, PST.RNRTL, PST.RNRTS, PST.DEPT_ID, PST.PRFCNTR, PST.FCHNG, PST.EMSG, " +
                "PST.USER_NAME, PST.OTS, PST.HEADBRANCH, PST.USER_AU2, PST.OTS_AU2, PST.USER_AU3, PST.OTS_AU3, " +
                "PST.USER_CHNG, PST.OTS_CHNG, PST.DESCRDENY " +
                "from GL_BATPKG PKG join GL_BATPST PST on PST.ID_PKG = PKG.ID_PKG " +
                ") v";
    }
}
