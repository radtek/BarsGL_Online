package ru.rbt.barsgl.gwt.client.operationTemplate;

import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.client.operation.NewOperationAction;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import java.util.HashMap;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.trimStr;

/**
 * Created by ER18837 on 21.03.16.
 */
public class OperationTemplateFormDlg extends GridFormDlgBase {

    OperationTemplateForm operationTemplateForm;
    GridWidget gridOperation;

    public OperationTemplateFormDlg(String title, GridWidget gridWidget) {
        super(title);
        gridOperation = gridWidget;
        ok.setText("Выбрать");
        cancel.setText("Закрыть");
    }

    @Override
    protected GridForm getGridForm() {
        return operationTemplateForm = new OperationTemplateForm(false);
    }

    protected String getGridWidth() {
        return "800px";
    }

    @Override
    protected boolean setResultList(final HashMap<String, Object> result) {

        boolean isExtended = "E".equals((String)result.get("TMPL_TYPE"));
        NewOperationAction newTemplateOperationAction = new NewOperationAction(
        		gridOperation, ImageConstants.INSTANCE.oper_go(), isExtended) {

            @Override
            protected Object getParams() {
                if (result == null) return null;

                ManualOperationWrapper wrapper = new ManualOperationWrapper();
                wrapper.setDealSrc((String)result.get("SRC_PST"));
                wrapper.setAccountDebit(trimStr((String)result.get("AC_DR")));
                wrapper.setCurrencyDebit((String)result.get("CCY_DR"));
                wrapper.setFilialDebit((String)result.get("CBCC_DR"));
                wrapper.setAccountCredit(trimStr((String)result.get("AC_CR")));
                wrapper.setCurrencyCredit((String)result.get("CCY_CR"));
                wrapper.setFilialCredit((String)result.get("CBCC_CR"));

                wrapper.setNarrative((String)result.get("NRT"));
                wrapper.setRusNarrativeLong((String)result.get("RNRTL"));

                wrapper.setDeptId((String)result.get("DEPT_ID"));
                wrapper.setProfitCenter((String)result.get("PRFCNTR"));

                return wrapper;            }
        };

        newTemplateOperationAction.execute();
        return true;
    }

    @Override
    protected Object[] getInitialFilterParams() {
        return new Object[0];
    }

}
