package ru.rbt.barsgl.gwt.client.accountPl;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.PopupPanel;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.dict.EditableDictionary;
import ru.rbt.barsgl.gwt.client.quickFilter.AccountQuickFilterParams;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.shared.enums.SecurityActionCode;

import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndFilialPart;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.*;
import static ru.rbt.barsgl.shared.dict.FormAction.CREATE;
import static ru.rbt.barsgl.shared.dict.FormAction.UPDATE;

/**
 * Created by ER18837 on 28.11.16.
 */
public class PlAccountForm extends EditableDictionary<ManualAccountWrapper> {
    public static final String FORM_NAME = "Счета ОФР по Accounting Type";
    PopupPanel sidePanel;

    private Column colFilial;
    private Column colAccount;
    private Column colAcc2;
    private Column colDateOpen;
    private Column colDateClose;

    AccountQuickFilterParams quickFilterParams;
    GridAction quickFilterAction;

    public PlAccountForm() {
        super(FORM_NAME, false);
        reconfigure();
    }

    private void reconfigure() {

        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addSecureAction(editAccount(), SecurityActionCode.AccOFRChng, SecurityActionCode.Acc707Chng);
        abw.addSecureAction(createAccount(), SecurityActionCode.AccOFRInp, SecurityActionCode.Acc707Inp);
    }

    @Override
    protected String prepareSql() {
        return "select ID, BSAACID, CBCC, CBCCN, BRANCH, CCY, CUSTNO, ACCTYPE, CBCUSTTYPE, TERM, GL_SEQ, " +
                "ACC2, PLCODE, ACOD, SQ, ACID, PSAV, DEALSRS, RLNTYPE, DESCRIPTION, " +
                "DTO, DTC, DTR, DTM, OPENTYPE, GLOID, GLO_DC " +
                "from GL_ACC where COALESCE(PLCODE, '-') <> '-' "
                + getSourceAndFilialPart("and", "", "CBCC");
    }

    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("ID", Column.Sort.DESC));
        return list;
    }

    protected Table prepareTable() {
        Table result = new Table();

        result.addColumn(colAccount = new Column("BSAACID", STRING, "Счет ЦБ", 160));
        result.addColumn(new Column("PSAV", STRING, "А/ П", 25, false, false));
        result.addColumn(new Column("ACID", STRING, "Счет Midas", 160, false, false));
        result.addColumn(new Column("DEALSRS", STRING, "Источник сделки", 70, false, false));

        result.addColumn(new Column("ACCTYPE", DECIMAL, "Accounting Type", 80, true, false, Column.Sort.NONE, "#"));// No Space
        result.addColumn(new Column("CBCUSTTYPE", DECIMAL, "Тип соб", 40, true, true, Column.Sort.NONE, "#,##"));
        result.addColumn(new Column("TERM", DECIMAL, "Код срока", 40, true, true, Column.Sort.NONE, "#,##"));
        result.addColumn(new Column("ACC2", STRING, "Счет 2-го порядка", 60, false, false));
        result.addColumn(new Column("PLCODE", STRING, "Символ ОФР", 60));
        result.addColumn(new Column("RLNTYPE", STRING, "Relation Type", 60, false, false));

        result.addColumn(new Column("CBCCN", STRING, "Код филиала", 60, false, false));
        result.addColumn(colFilial = new Column("CBCC", STRING, "Филиал", 60));
        result.addColumn(new Column("BRANCH", STRING, "Отделение", 60));
        result.addColumn(new Column("CUSTNO", STRING, "Номер клиента", 70, false, false));
        result.addColumn(colDateOpen = new Column("DTO", DATE, "Дата открытия", 80));
        result.addColumn(colDateClose = new Column("DTC", DATE, "Дата закрытия", 80, false, false));
        result.addColumn(new Column("DTR", DATE, "Дата регистрации", 80, false, false));
        result.addColumn(new Column("DTM", DATE, "Дата изменения", 80, false, false));
        result.addColumn(new Column("OPENTYPE", STRING, "Способ открытия", 70, false, false));

        result.addColumn(new Column("ACOD", DECIMAL, "Midas Acod", 50, false, false));
        result.addColumn(new Column("SQ", DECIMAL, "Midas SQ", 40, false, false));
        result.addColumn(new Column("DESCRIPTION", STRING, "Название счета", 560));
        result.addColumn(new Column("ID", LONG, "ИД счета", 60, false, false, Column.Sort.DESC, ""));
        return result;
    }

    private GridAction createAccount() {
        return (GridAction) commonLazyAction("PlAccountDlg", "Открытие счета ОФР", CREATE, table.getColumns(),
                "Счет не создан",
                "Ошибка создания счета",
                "Счет создан успешно");
    }
    private GridAction editAccount() {
        return (GridAction) commonLazyAction("PlAccountDlg", "Редактирование счета ОФР", UPDATE, table.getColumns(),
                "Счет не изменен",
                "Ошибка изменения счета",
                "Счет изменен успешно");
    }

    @Override
    protected void save(ManualAccountWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<ManualAccountWrapper>> asyncCallbackImpl) throws Exception {
        switch(action) {
            case CREATE:
                BarsGLEntryPoint.operationService.savePlAccount(cnw, asyncCallbackImpl);
                break;
            case UPDATE:
                BarsGLEntryPoint.operationService.updateAccount(cnw, asyncCallbackImpl);
                break;
        }
    }

    @Override
    protected String getSuccessMessage(ManualAccountWrapper wrapper, FormAction action) {
        switch(action) {
            case CREATE:
                return "<pre>Счет ЦБ:      " + wrapper.getBsaAcid() + "</pre>"
                        + "<pre>Счет Midas:   " + wrapper.getAcid() + "</pre>";
            default:
                return null;
        }
    }

    @Override
    public ArrayList<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams) {
        ArrayList<FilterItem> list = new ArrayList<FilterItem>();
        list.add(new FilterItem(colDateClose, FilterCriteria.IS_NULL, null));
        return list;
    }

}

