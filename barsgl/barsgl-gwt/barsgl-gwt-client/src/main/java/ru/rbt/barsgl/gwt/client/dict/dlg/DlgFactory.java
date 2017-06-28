package ru.rbt.barsgl.gwt.client.dict.dlg;

import ru.rbt.barsgl.gwt.client.account.AccountDlg;
import ru.rbt.barsgl.gwt.client.account.AccountTechDlg;
import ru.rbt.barsgl.gwt.client.accountPl.PlAccountDlg;
import ru.rbt.barsgl.gwt.client.operationTemplate.OperationTemplateDlg;
import ru.rbt.barsgl.gwt.client.pd.PostingTechDlg;

/**
 * Created by akichigi on 02.09.16.
 */
public class DlgFactory {
    public static EditableDialog create(String name){
        switch (name){
            case "AccTypeDlg": return new AccTypeDlg();
            case "AccTypeSectionDlg": return new AccTypeSectionDlg();
            case "AccTypeProductDlg": return new AccTypeProductDlg();
            case "AccTypeSubProductDlg": return new AccTypeSubProductDlg();
            case "AccTypeModifierDlg": return new AccTypeModifierDlg();
            case "OperationTemplateDlg": return new OperationTemplateDlg();
            case "AcodDlg": return new AcodDlg();
            case "AccountDlg": return new AccountDlg();
            case "PlAccountDlg": return new PlAccountDlg();
            case "AccountTechDlg": return new AccountTechDlg();
            default: return null;
        }
    }
}
