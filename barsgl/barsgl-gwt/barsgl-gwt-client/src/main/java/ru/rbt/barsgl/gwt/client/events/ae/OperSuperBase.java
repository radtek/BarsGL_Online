package ru.rbt.barsgl.gwt.client.events.ae;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.client.operation.OperationHandsViewDlg;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.shared.ClientDateUtils;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.barsgl.shared.user.AppUserWrapper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.EnumSet;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;
import static ru.rbt.barsgl.gwt.core.utils.WhereClauseBuilder.getWhereInClause;
import static ru.rbt.barsgl.shared.enums.BatchPostStatus.*;

/**
 * Created by ER18837 on 15.07.16.
 */
abstract public class OperSuperBase extends GridForm {
    protected Boolean _ownMessages;
    protected StepChoiceDlg.MessageType _type = StepChoiceDlg.MessageType.ALL;

    public OperSuperBase(String title) {
        super(title);
    }

    public OperSuperBase(String title, boolean delayLoad) {
        super(title, delayLoad);
    }

    protected ManualOperationWrapper rowToWrapper(){
        ManualOperationWrapper wrapper = new ManualOperationWrapper();

        wrapper.setId((Long) getValue("ID"));

        wrapper.setPostDateStr(ClientDateUtils.Date2String((Date) getValue("POSTDATE")));
        wrapper.setValueDateStr(ClientDateUtils.Date2String((Date) getValue("VDATE")));

        wrapper.setDealSrc((String) getValue("SRC_PST"));
        wrapper.setDealId((String) getValue("DEAL_ID"));
        wrapper.setSubdealId((String) getValue("SUBDEALID"));
        wrapper.setPaymentRefernce((String) getValue("PMT_REF"));

        wrapper.setCurrencyDebit((String) getValue("CCY_DR"));
        wrapper.setFilialDebit((String) getValue("CBCC_DR"));
        wrapper.setAccountDebit((String) getValue("AC_DR"));
        wrapper.setAmountDebit((BigDecimal) getValue("AMT_DR"));

        wrapper.setCurrencyCredit((String) getValue("CCY_CR"));
        wrapper.setFilialCredit((String) getValue("CBCC_CR"));
        wrapper.setAccountCredit((String) getValue("AC_CR"));
        wrapper.setAmountCredit((BigDecimal) getValue("AMT_CR"));

        wrapper.setAmountRu((BigDecimal) getValue("AMTRU"));
        wrapper.setCorrection("Y".equals(((String) getValue("FCHNG")))); //TODO некошерно

        wrapper.setNarrative((String) getValue("NRT"));
        wrapper.setRusNarrativeLong((String) getValue("RNRTL"));

        wrapper.setDeptId((String) getValue("DEPT_ID"));
        wrapper.setProfitCenter((String) getValue("PRFCNTR"));

        wrapper.setReasonOfDeny((String) getValue("DESCRDENY"));
        String errorMessage = (String) getValue("EMSG");
        if (!isEmpty(errorMessage))
            wrapper.getErrorList().addErrorDescription(null, null, errorMessage, null);
        return wrapper;
    }

    protected GridAction createPreview(){
        return new GridAction(grid, null, "Просмотр", new Image(ImageConstants.INSTANCE.preview()), 10, true) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                OperationHandsViewDlg dlgOperation = new OperationHandsViewDlg("Просмотр запроса на операцию GL",
                        FormAction.PREVIEW, grid.getTable().getColumns(), BatchPostStep.NOHAND);
                dlgOperation.setDlgEvents(this);
                dlgOperation.show(rowToWrapper());
            }
        };
    }

    protected String getOwnMessagesClause(Boolean ownMessages, BatchPostStep step, String tbl){
        if (!ownMessages) return "";

        String tblPref = isEmpty(tbl) ? "" : tbl + ".";

        String field;
        switch (step){
            case HAND1:
                field = "USER_NAME";
                break;
            case HAND2:
                field = "USER_AU2";
                break;
            case HAND3:
                field = "USER_AU3";
                break;
            default:
                field = "";
                break;
        }

        AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        if (wrapper == null) return "";

        return  field.isEmpty() ? " and " + "'" + wrapper.getUserName() + "' in (" + tblPref + "USER_NAME, USER_AU2, USER_AU3, USER_CHNG)"
                : " and " + tblPref + field + "=" + "'" + wrapper.getUserName() + "'";
    }

    protected String getWhereMessageTypePart(String tbl){
        String predicate = "";
        String tblPref = isEmpty(tbl) ? "" : tbl + ".";

        switch (_type){
            case COMPLETED:
                predicate = " and " + tblPref + "STATE='" + BatchPostStatus.COMPLETED.name() + "'";
                break;
            case WORKING:
                EnumSet<BatchPostStatus> statuses = EnumSet.of(SIGNEDVIEW, SIGNED, SIGNEDDATE, WAITSRV, OKSRV, WORKING);
                predicate = " and " + getWhereInClause(statuses, tblPref + "STATE");
                break;
            case NOTCOMPLETED:
                predicate = " and " + tblPref + "STATE<>'" + BatchPostStatus.COMPLETED.name() + "'";
                break;
            default:
                predicate = "";
                break;
        }
        return predicate;
    }


}
