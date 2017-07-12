package ru.rbt.barsgl.gwt.client.events.ae;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.operation.NewTechOperationAction;
import ru.rbt.barsgl.gwt.client.operation.OperationTechHandsDlg2;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterCancelEvent;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.BatchPostAction;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.operation.ManualTechOperationWrapper;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.shared.enums.SecurityActionCode;

import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getEnumLabelsList;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getYesNoList;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;


/**
 * Created by akichigi on 06.06.16.
 */
public class OperTechInpConfirmForm extends OperTechBase{

    public static final String FORM_NAME = "Ввод и авторизация операций - техсчета";
    private GridAction _modify;
    private GridAction _create;
    private GridAction _delete;
    private GridAction _forward;
    private GridAction _backward;
    private GridAction _sign;
    private GridAction _confirmDate;

    public OperTechInpConfirmForm() {
        super(FORM_NAME);
    }

    @Override
    protected void reconfigure() {
        super.reconfigure();

        abw.addSecureAction(_modify = createModify(), SecurityActionCode.TechOperInp);
        abw.addSecureAction(_create = createNewOperation(),SecurityActionCode.TechOperInp);
        abw.addSecureAction(_delete = createDelete(),SecurityActionCode.TechOperHand2);
        abw.addSecureAction(_forward = createForward(),SecurityActionCode.TechOperHand2);
        abw.addSecureAction(_backward = createBackward(),SecurityActionCode.TechOperHand2);
        abw.addSecureAction(_sign = createSign(),SecurityActionCode.TechOperHand2);
        abw.addSecureAction(_confirmDate = createConfirmDate(),SecurityActionCode.TechOperHand3);
    }

    @Override
    protected InputMethod getInputMethod() {
        return InputMethod.M;
    }

    @Override
    protected String getSelectClause() {
        return "select PST.ID, PST.GLOID_REF, PST.STATE, PST.ECODE, PST.ID_PKG, PST.NROW, " +
                "PST.INVISIBLE, PST.INP_METHOD, PST.ID_PAR, PST.ID_PREV, PST.SRV_REF, PST.SEND_SRV, PST.OTS_SRV, PST.SRC_PST, " +
                "PST.DEAL_ID, PST.SUBDEALID, PST.PMT_REF, PST.PROCDATE, PST.VDATE, PST.POSTDATE, PST.ACCTYPE_DR, " +
                "PST.AC_DR, PST.CCY_DR, PST.AMT_DR, PST.CBCC_DR, PST.AC_CR, PST.ACCTYPE_CR, PST.CCY_CR, PST.AMT_CR, PST.CBCC_CR, " +
                "PST.AMTRU, PST.NRT, PST.RNRTL, PST.RNRTS, PST.DEPT_ID, PST.PRFCNTR, PST.FCHNG, PST.EMSG, " +
                "PST.USER_NAME, PST.OTS, PST.HEADBRANCH, PST.USER_AU2, PST.OTS_AU2, PST.USER_AU3, PST.OTS_AU3, " +
                "PST.USER_CHNG, PST.OTS_CHNG, PST.DESCRDENY, PST.FIO " +
                "from V_GL_BATPST_TH PST ";
    }

    @Override
    protected void doActionVisibility(){
        _modify.setVisible(_step.isInputStep()/* || _step.isControlStep()*/);
        _create.setVisible(_step.isInputStep());
        //_createFromTemplate.setVisible(_step.isInputStep());
        _delete.setVisible(_step.isInputStep());
        _forward.setVisible(_step.isInputStep());
        _backward.setVisible(_step.isControlStep() || _step.isConfirmStep());
        _sign.setVisible(_step.isControlStep());
        _confirmDate.setVisible(_step.isConfirmStep());
        _packageStatAction.setVisible(false);
    }

    private GridAction createNewOperation() {
        return new NewTechOperationAction(grid, ImageConstants.INSTANCE.new24());
    }

    private GridAction createForward(){
        return commonAction("Передать на подпись",
                ImageConstants.INSTANCE.increase(),
                "Передача бухгалтерской операции GL на подпись",
                FormAction.SEND);
    }

    private GridAction createBackward(){
        return commonAction("Вернуть на доработку",
                ImageConstants.INSTANCE.decrease(),
                "Возврат бухгалтерской операции GL на доработку",
                FormAction.RETURN);
    }

    private GridAction createModify(){
        return commonAction("Редактировать",
                ImageConstants.INSTANCE.edit24(),
                "Редактирование бухгалтерской операции по техсчетам",
                FormAction.UPDATE);
    }

    private GridAction createDelete(){
        return commonAction("Удалить",
                ImageConstants.INSTANCE.stop(),
                "Удаление бухгалтерской операции GL",
                FormAction.DELETE);
    }

    private GridAction createSign(){
        return commonAction("Подписать",
                ImageConstants.INSTANCE.sign(),
                "Подпись бухгалтерской операции GL",
                FormAction.SIGN);
    }

    private GridAction createConfirmDate(){
        return commonAction("Подтвердить дату",
                ImageConstants.INSTANCE.date_edit(),
                "Подтверждение даты бухгалтерской операции GL",
                FormAction.CONFIRM);
    }

    public GridAction commonAction(String hint, ImageResource image, final String title, final FormAction formAction){
        return new GridAction (grid, null, hint, new Image(image), 10, true)  {
            OperationTechHandsDlg2 dlg;

            private BatchPostAction calcAction(final OperationTechHandsDlg2.ButtonOperAction operAction){
                BatchPostAction action;
                if (formAction == FormAction.DELETE){
                    //удалить
                    action = BatchPostAction.DELETE;
                }
                else if (formAction == FormAction.UPDATE) {
                    //править
                    if (_step == BatchPostStep.HAND1){
                        action = operAction == OperationTechHandsDlg2.ButtonOperAction.OK
                                ? BatchPostAction.UPDATE
                                : BatchPostAction.UPDATE_CONTROL;
                    }else{
                        //BatchPostStep.HAND2
                        action = operAction == OperationTechHandsDlg2.ButtonOperAction.OK
                                ? BatchPostAction.UPDATE_CONTROL
                                : BatchPostAction.UPDATE_SIGN;
                    }
                }
                else if (formAction == FormAction.SEND){
                    //на подпись
                    action = BatchPostAction.CONTROL;
                }
                else if (formAction == FormAction.SIGN){
                    //подписать
                    action = BatchPostAction.SIGN;
                }
                else if (formAction == FormAction.CONFIRM){
                    //подтвердить дату
                    action = operAction == OperationTechHandsDlg2.ButtonOperAction.OK
                            ? BatchPostAction.CONFIRM_NOW
                            : BatchPostAction.CONFIRM;
                }else{
                    //отказать
                    action = BatchPostAction.REFUSE;
                }
                return action;
            }

            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                //dlg = new OperationHandsDlg(title, FormAction.CREATE, grid.getTable().getColumns(), BatchPostStep.HAND1);

                dlg = new OperationTechHandsDlg2(title, formAction, grid.getTable().getColumns(), _step);
                dlg.setDlgEvents(this);
                dlg.setAfterCancelEvent(new IAfterCancelEvent() {
                    @Override
                    public void afterCancel() {
                        grid.refresh();
                    }
                });
                ManualTechOperationWrapper wrapper = rowToWrapper();

                dlg.show(wrapper);
            }

            @Override
            public void onDlgOkClick(Object prms){
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                ManualTechOperationWrapper wrapper = (ManualTechOperationWrapper) prms;

                BatchPostStatus status = BatchPostStatus.valueOf((String) getValue("STATE"));
                wrapper.setStatus(status);
                wrapper.setAction(calcAction(dlg.getOperationAction()));

                BarsGLEntryPoint.operationService.processTechOperationRq(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<ManualTechOperationWrapper>>() {
                    @Override
                    public void onSuccess(RpcRes_Base<ManualTechOperationWrapper> wrapper) {
                        if (wrapper.isError()) {
                            showInfo("Ошибка", wrapper.getMessage());
                        } else {
                            dlg.hide();
                            showInfo("Информация", wrapper.getMessage());
                            grid.refresh();
                        }
                        WaitingManager.hide();
                    }
                });
            }
        };
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
        result.addColumn(col = new Column("INP_METHOD", Column.Type.STRING, "Способ ввода", 80));
        col.setList(getEnumLabelsList(InputMethod.values()));
        result.addColumn(new Column("SRC_PST", Column.Type.STRING, "Источник сделки", 80));

        result.addColumn(new Column("DEAL_ID", Column.Type.STRING, "ИД сделки", 70));
        result.addColumn(new Column("SUBDEALID", Column.Type.STRING, "ИД субсделки", 80, false, false));
        result.addColumn(new Column("PMT_REF", Column.Type.STRING, "ИД платежа", 100, false, false));

        result.addColumn(col = new Column("PROCDATE", Column.Type.DATE, "Дата опердня", 72));
        col.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("VDATE", Column.Type.DATE, "Дата валютирования", 80));
        col.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("POSTDATE", Column.Type.DATE, "Дата проводки", 72));
        col.setFormat("dd.MM.yyyy");

        result.addColumn(new Column("ACCTYPE_DR", Column.Type.STRING, "AccountType ДБ", 120));
        result.addColumn(new Column("AC_DR", Column.Type.STRING, "Счет ДБ", 160));
        result.addColumn(new Column("CCY_DR", Column.Type.STRING, "Валюта ДБ", 60, false, false));
        result.addColumn(new Column("AMT_DR", Column.Type.DECIMAL, "Сумма ДБ", 100));
        result.addColumn(new Column("CBCC_DR", Column.Type.STRING, "Филиал ДБ", 60, false, false));

        result.addColumn(new Column("ACCTYPE_CR", Column.Type.STRING, "AccountType КР", 120));
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

        result.addColumn(new Column("USER_NAME", Column.Type.STRING, "Логин 1 руки", 100, false, false));
        result.addColumn(new Column("OTS", Column.Type.DATETIME, "Дата создания", 130, false, false));
        result.addColumn(new Column("HEADBRANCH", Column.Type.STRING, "Филиал 1 руки", 100, false, false));
        result.addColumn(new Column("USER_AU2", Column.Type.STRING, "Логин 2 руки", 100, false, false));
        result.addColumn(new Column("OTS_AU2", Column.Type.DATETIME, "Дата подписи", 130, false, false));
        result.addColumn(new Column("USER_AU3", Column.Type.STRING, "Логин 3 руки", 100, false, false));
        result.addColumn(new Column("OTS_AU3", Column.Type.DATETIME, "Дата подтверж.", 130, false, false));
        result.addColumn(new Column("USER_CHNG", Column.Type.STRING, "Логин изменения", 100, false, false));
        result.addColumn(new Column("OTS_CHNG", Column.Type.DATETIME, "Дата изменения", 130, false, false));

        result.addColumn(new Column("DESCRDENY", Column.Type.STRING, "Причина возврата", 300, false, false));

        result.addColumn(col = new Column("FIO", Column.Type.STRING, "ФИО создателя проводки", 250));
        col.setFilterable(false);

        return result;
    }


    @Override
    protected ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("ID", Column.Sort.DESC));
        return list;
    }
}
