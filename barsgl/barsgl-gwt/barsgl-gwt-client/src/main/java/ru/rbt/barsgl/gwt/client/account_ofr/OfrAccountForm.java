package ru.rbt.barsgl.gwt.client.account_ofr;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;

import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndCodeFilialPart;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.DATE;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.STRING;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by ER18837 on 10.11.15.
 */
public class OfrAccountForm extends GridForm {
    public static final String FORM_NAME = "Счета ОФР по Midas";
    /*don't used IMBCBHBPN*/
    //private OfrAccountDlg dlg = null;
    private boolean isOfrAccountDlgCreated;  //Флаг, что форма уже создана
    public OfrAccountForm(){
        super(FORM_NAME);
        reconfigure();
    }

    private void reconfigure() {
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
//        abw.addSecureAction(createNewAccount(), SecurityActionCode.AccOFRInp);

    }

    /*don't used IMBCBHBPN*/
    /*
    private GridAction createNewAccount(){
        return new GridAction(grid, null, "Открыть счет ОФР", new Image(ImageConstants.INSTANCE.new24()), 10)  {

            @Override
            public void execute() {
                if (dlg == null){
                    dlg = new OfrAccountDlg();
                    dlg.setDlgEvents(this);
                }

                dlg.show(isOfrAccountDlgCreated);
                isOfrAccountDlgCreated = dlg != null;
            }

            @Override
            public void onDlgOkClick(Object prms){
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                ManualAccountWrapper wrapper = (ManualAccountWrapper) prms;
                BarsGLEntryPoint.operationService.saveOfrAccount(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<ManualAccountWrapper>>() {
                    @Override
                    public void onFailureOthers(Throwable throwable) {
                        WaitingManager.hide();
                        showInfo(" Ошибка: Счет не создан", throwable.getLocalizedMessage());
                    }

                    @Override
                    public void onSuccess(RpcRes_Base<ManualAccountWrapper> accWrapper) {
                        if (accWrapper.isError()) {
                            showInfo("Ошибка создания счета", accWrapper.getMessage());
                        } else {
                        	showInfo("Счет создан успешно",
                                    "<pre>Счет ЦБ:    " + accWrapper.getResult().getBsaAcid() + "</pre>"
                                            + "<pre>Счет Midas: " + accWrapper.getResult().getAcid() + "</pre>");
                            dlg.hide();
                            refreshAction.execute();
                        }
                        WaitingManager.hide();
                    }
                });
            }
        };
    }
    */
    
    @Override
    protected String prepareSql() {
        return "SELECT BSAACID,  CTYPE, PLCODE, CCODE, CCPCD, CNUM, GLACOD, " +
                "SUBSTR(ACID, 16, 2) ACSQ, SUBSTR(ACID, 18, 3) BRCA,  DRLNO, DRLNC " +
                "FROM ACCRLN rl LEFT JOIN IMBCBCMP ic on rl.CCODE = ic.CCBBR " +
                "WHERE rl. ACID <> '' AND rl.BSAACID BETWEEN '70600' AND '70699' " +
                "AND CBCCY = '810' AND CAST(GLACOD as integer) <> 0 AND rl.RLNTYPE=2 AND rl.DRLNC >= '2029-01-01' " +
                getSourceAndCodeFilialPart("and", "", "CCPCD", "");
    }


    protected Table prepareTable() {
        Table result = new Table();
        Column col;

        result.addColumn(new Column("BSAACID", STRING, "Счет ЦБ", 60));
        result.addColumn(new Column("CTYPE", STRING, "Тип собственности", 30));
        result.addColumn(new Column("PLCODE", STRING, "Символ ОФР", 30));
        result.addColumn(new Column("CCODE", STRING, "Код филиала", 30, false, false));
        result.addColumn(new Column("CCPCD", STRING, "Филиал", 30));
        result.addColumn(new Column("BRCA", STRING, "Отделение", 30));
        result.addColumn(new Column("CNUM", STRING, "Клиент", 40));
        result.addColumn(new Column("GLACOD", STRING, "ACOD", 30));
        result.addColumn(new Column("ACSQ", STRING, "SQ", 30));
        result.addColumn(col = new Column("DRLNO", DATE, "Дата открытия", 30));
        col.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("DRLNC", DATE, "Дата закрытия", 30));
        col.setFormat("dd.MM.yyyy");

        return result;
    }

    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("BSAACID", Column.Sort.ASC));
        return list;
    }

}

