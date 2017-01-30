package ru.rbt.barsgl.gwt.client.events.ae;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.operation.OperationHandsDlg;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterCancelEvent;
import ru.rbt.barsgl.gwt.core.dialogs.IDlgEvents;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.BatchPostAction;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.barsgl.shared.user.AppUserWrapper;

import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getEnumLabelsList;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getYesNoList;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by ER18837 on 03.03.16.
 */
public class BatchPostingForm extends OperBase {
    public static final String FORM_NAME = "Ввод и авторизация пакетов";

    private GridAction _preview;
    private GridAction _delete;
    private GridAction _forward;
    private GridAction _backward;
    private GridAction _sign;
    private GridAction _confirmDate;
    private GridAction _load;
    private GridAction _loadCard;

    protected Column colPackage;
    protected Column colRow;
    protected Column colError;

    public BatchPostingForm() {
        super(FORM_NAME);
    }

    @Override
    protected void reconfigure() {
        super.reconfigure();
        abw.addAction(_load = createLoad(new LoadFileDlg(), ImageConstants.INSTANCE.load()));
        abw.addAction(_loadCard = createLoad(new LoadCardDlg(), ImageConstants.INSTANCE.load_blue()));
        abw.addAction(_delete = createDelete());
        abw.addAction(_forward = createForward());
        abw.addAction(_backward = createBackward());
        abw.addAction(_sign = createSign());
        abw.addAction(_confirmDate = createConfirmDate());
    }

    @Override
    protected void doActionVisibility() {
        _load.setVisible(_step.isInputStep());
        _loadCard.setVisible(_step.isInputStep());
        _delete.setVisible(_step.isInputStep() || _step.isControlStep());
        _forward.setVisible(_step.isInputStep());
        _backward.setVisible(_step.isConfirmStep());
        _sign.setVisible(_step.isControlStep());
        _confirmDate.setVisible(_step.isConfirmStep());
        _packageStatAction.setVisible(true);
    }

    private GridAction createLoad(final LoadFileDlgBase loadFileDlg, ImageResource img) {
        return new GridAction(grid, null, loadFileDlg.getCaption(), new Image(img), 10) {
            LoadFileDlgBase dlg;

            @Override
            public void execute() {
               WaitingManager.show("Загрузка из файла...");
               dlg = loadFileDlg;
               dlg.setAfterCancelEvent(new IAfterCancelEvent() {
                   @Override
                   public void afterCancel() {
                       WaitingManager.hide();
                       dlg.hide();
                       grid.refresh();
                   }
               });

               dlg.setDlgEvents(new IDlgEvents() {
                   @Override
                   public void onDlgOkClick(Object prms) throws Exception {
                       WaitingManager.hide();
                       WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                       ManualOperationWrapper wrapper = new ManualOperationWrapper();
                       wrapper.setPkgId((Long) prms);
                       wrapper.setAction(BatchPostAction.CONTROL);

                       AppUserWrapper appUserWrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
                       wrapper.setUserId(appUserWrapper.getId());

                       BarsGLEntryPoint.operationService.processPackageRq(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<ManualOperationWrapper>>() {
                           @Override
                           public void onSuccess(RpcRes_Base<ManualOperationWrapper> wrapper) {
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
               });

               dlg.show();
            }
        };
    }

    public GridAction commonAction(String hint, ImageResource image, final String title, final FormAction formAction) {
        return new GridAction(grid, null, hint, new Image(image), 10, true) {
            OperationPkgDlg dlg;

            private BatchPostAction calcAction(final OperationHandsDlg.ButtonOperAction operAction){
                BatchPostAction action;
                if (formAction == FormAction.DELETE){
                    //удалить
                    action = BatchPostAction.DELETE;
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
                    action = operAction == OperationHandsDlg.ButtonOperAction.OK
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

                dlg = new OperationPkgDlg(title, formAction);
                dlg.setDlgEvents(this);
                Object[] params = new Object[]{getValue("ID_PKG"), getValue("FILE_NAME"), getValue("POSTDATE")};
                dlg.show(params);
            }

            @Override
            public void onDlgOkClick(Object prms){
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                ManualOperationWrapper wrapper = new ManualOperationWrapper();
                wrapper.setPkgId((Long) getValue("ID_PKG"));
                wrapper.setAction(calcAction(dlg.getOperationAction()));
                wrapper.setReasonOfDeny((String) prms);

                AppUserWrapper appUserWrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
                wrapper.setUserId(appUserWrapper.getId());

                //TODO for debug
                /* if (1==1){
                     System.out.println(wrapper.getPkgId());
                    System.out.println("Action => " + wrapper.getAction().name());
                    System.out.println("Prms => " + wrapper.getReasonOfDeny());
                    System.out.println("User=" + wrapper.getUserId());
                    dlg.hide();
                    WaitingManager.hide();
                    return;
                }
               */

                BarsGLEntryPoint.operationService.processPackageRq(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<ManualOperationWrapper>>() {
                    @Override
                    public void onSuccess(RpcRes_Base<ManualOperationWrapper> wrapper) {
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

    private GridAction createDelete(){
        return commonAction("Удалить",
                ImageConstants.INSTANCE.stop(),
                "Удаление пакета",
                FormAction.DELETE);
    }

    private GridAction createForward(){
        return commonAction("Передать на подпись",
                ImageConstants.INSTANCE.increase(),
                "Передача пакета на подпись",
                FormAction.SEND);
    }

    private GridAction createBackward(){
        return commonAction("Вернуть на доработку",
                ImageConstants.INSTANCE.decrease(),
                "Возврат пакета на доработку",
                FormAction.RETURN);
    }

    private GridAction createSign(){
        return commonAction("Подписать",
                ImageConstants.INSTANCE.sign(),
                "Подпись пакета",
                FormAction.SIGN);
    }

    private GridAction createConfirmDate(){
        return commonAction("Подтвердить дату",
                ImageConstants.INSTANCE.date_edit(),
                "Подтверждение даты пакета",
                FormAction.CONFIRM);
    }

    @Override
    protected InputMethod getInputMethod() {
        return InputMethod.F;
    }

    @Override
    protected String getSelectClause() {
        return  "select PST.ID, PST.GLOID_REF, PST.STATE, PST.ECODE, PST.ID_PKG, PST.NROW, PKG.FILE_NAME, PKG.STATE PKG_STATE, " +
                "PST.INVISIBLE, PST.INP_METHOD, PST.ID_PAR, PST.ID_PREV, PST.SRV_REF, PST.SEND_SRV, PST.OTS_SRV, PST.SRC_PST, " +
                "PST.DEAL_ID, PST.SUBDEALID, PST.PMT_REF, PST.PROCDATE, PST.VDATE, PST.POSTDATE, " +
                "PST.AC_DR, PST.CCY_DR, PST.AMT_DR, PST.CBCC_DR, PST.AC_CR, PST.CCY_CR, PST.AMT_CR, PST.CBCC_CR, " +
                "PST.AMTRU, PST.NRT, PST.RNRTL, PST.RNRTS, PST.DEPT_ID, PST.PRFCNTR, PST.FCHNG, PST.EMSG, " +
                "PST.USER_NAME, PST.OTS, PST.HEADBRANCH, PST.USER_AU2, PST.OTS_AU2, PST.USER_AU3, PST.OTS_AU3, " +
                "PST.USER_CHNG, PST.OTS_CHNG, PST.DESCRDENY, " +
                "U.SURNAME || ' '  || VALUE(U.FIRSTNAME, '') || ' ' || VALUE(U.PATRONYMIC, '') as FIO, PKG.MVMNT_OFF " +
                "from GL_BATPKG PKG join GL_BATPST PST on PST.ID_PKG = PKG.ID_PKG " +
                "left join GL_USER U on U.USER_NAME = PST.USER_NAME ";
    }


    @Override
    protected Table prepareTable() {
        Table result = new Table();
        Column col;

        result.addColumn(new Column("ID", Column.Type.LONG, "ID запроса", 70));
        result.addColumn(new Column("GLOID_REF", Column.Type.LONG, "ID операции", 70));
        result.addColumn(col = new Column("STATE", Column.Type.STRING, "Статус", 90));
        col.setList(getEnumLabelsList(BatchPostStatus.values()));
        result.addColumn(colError = new Column("ECODE", Column.Type.INTEGER, "Код ошибки", 60));

        result.addColumn(colPackage = new Column("ID_PKG", Column.Type.LONG, "ID пакета", 60));
        result.addColumn(colRow = new Column("NROW", Column.Type.INTEGER, "Строка в файле", 60));
        result.addColumn(new Column("FILE_NAME", Column.Type.STRING, "Имя файла", 100, false, false));
        result.addColumn(new Column("PKG_STATE", Column.Type.STRING, "Статус пакета", 90, false, false));

        result.addColumn(new Column("SRC_PST", Column.Type.STRING, "Источник сделки", 80));

        result.addColumn(new Column("DEAL_ID", Column.Type.STRING, "ИД сделки", 70));
        result.addColumn(new Column("SUBDEALID", Column.Type.STRING, "ИД субсделки", 80, false, false));
        result.addColumn(new Column("PMT_REF", Column.Type.STRING, "ИД платежа", 100));

        result.addColumn(col = new Column("PROCDATE", Column.Type.DATE, "Дата опердня", 75));
        col.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("VDATE", Column.Type.DATE, "Дата валютирования", 75));
        col.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("POSTDATE", Column.Type.DATE, "Дата проводки", 75));
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

        result.addColumn(new Column("USER_NAME", Column.Type.STRING, "Логин 1 руки", 100, false, false));
        result.addColumn(col = new Column("OTS", Column.Type.DATETIME, "Дата создания", 130, false, false));
        result.addColumn(new Column("HEADBRANCH", Column.Type.STRING, "Филиал 1 руки", 100, false, false));
        result.addColumn(new Column("USER_AU2", Column.Type.STRING, "Логин 2 руки", 100, false, false));
        result.addColumn(col = new Column("OTS_AU2", Column.Type.DATETIME, "Дата подписи", 130, false, false));
        result.addColumn(new Column("USER_AU3", Column.Type.STRING, "Логин 3 руки", 100, false, false));
        result.addColumn(col = new Column("OTS_AU3", Column.Type.DATETIME, "Дата подтверж.", 130, false, false));
        result.addColumn(new Column("USER_CHNG", Column.Type.STRING, "Логин изменения", 100, false, false));
        result.addColumn(col = new Column("OTS_CHNG", Column.Type.DATETIME, "Дата изменения", 130, false, false));
        result.addColumn(new Column("MVMNT_OFF", Column.Type.STRING, "Искл.запроса к АБС", 150, false, false));
        result.addColumn(new Column("SRV_REF", Column.Type.STRING, "ID запроса в АБС", 150));
        result.addColumn(col = new Column("SEND_SRV", Column.Type.DATETIME, "Запрос в АБС", 120, false, false));
        result.addColumn(col = new Column("OTS_SRV", Column.Type.DATETIME, "Ответ от АБС", 120));

        result.addColumn(new Column("DESCRDENY", Column.Type.STRING, "Причина возврата", 300, false, false));

        result.addColumn(col = new Column("FIO", Column.Type.STRING, "ФИО создателя проводки", 250));
        col.setFilterable(false);

        return result;
    }

    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
       ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("ID_PKG", Column.Sort.DESC));
        list.add(new SortItem("ID", Column.Sort.DESC));
        return list;
    }
}


