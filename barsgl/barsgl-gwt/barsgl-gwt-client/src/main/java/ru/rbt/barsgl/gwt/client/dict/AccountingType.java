/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.gwt.client.dict;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.dict.dlg.*;
import ru.rbt.barsgl.gwt.client.gridForm.MDForm;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.IAfterRefreshEvent;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.*;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.*;
import ru.rbt.barsgl.shared.enums.SecurityActionCode;

import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 *
 * @author Andrew Samsonov
 */
public class AccountingType extends MDForm implements IAfterRefreshEvent {
    public final static String FORM_NAME = "Счета Accounting Type";
    public final static String FIELD_SECTION = "Раздел";
    public final static String FIELD_PRODUCT = "Продукт";
    public final static String FIELD_SUBPRODUCT = "Подпродукт";
    public final static String FIELD_MODIFIER = "Модификатор";
    public final static String FIELD_ACCTYPE = "AccType";
    public final static String FIELD_ACCTYPENAME = "Наименование";
    public final static String FIELD_PL_ACT = "Счет ОФР - BARSGL";
    public final static String FIELD_FL_CTRL = "Контролир. счет";
    public final static String FIELD_TECH_ACT = "Техн. счёт";
    private Column acc_type;
    private ActParmDlg detail_dlg = null;
    private AccountingTypeDlg master_dlg = null;
    private LinkAccType2SourceDlg linkAccType2SourceDlg = null;
    private AccountingTypeCreateDlg createDlg = null;
    private GridAction _action;
    private GridAction createDetailAction;

    private Column sectionColumn;
    private Column productColumn;
    private Column subProductColumn;
    private Column modifierColumn;


    public AccountingType() {
        super(FORM_NAME, null, "Параметры счета");
        reconfigure();
    }

    private void reconfigure() {
        masterActionBar.addAction(_action = new SimpleDlgAction(masterGrid, DlgMode.BROWSE, 10));
        masterActionBar.addSecureAction(editAccTypeAction(), SecurityActionCode.ReferAccTypeChng);
        masterActionBar.addSecureAction(createAccTypeAction(), SecurityActionCode.ReferAccTypeChng);
        masterActionBar.addSecureAction(deleteAccTypeAction(), SecurityActionCode.ReferAccTypeDel);
        masterActionBar.addSecureAction(linkAccTypeAction(), SecurityActionCode.ReferAccTypeChng);


        detailActionBar.addAction(new SimpleDlgAction(detailGrid, DlgMode.BROWSE, 10));
        detailActionBar.addSecureAction(editPrmAction(), SecurityActionCode.ReferAccTypeChng);
        detailActionBar.addSecureAction(createDetailAction = createPrmAction(), SecurityActionCode.ReferAccTypeChng);
        detailActionBar.addSecureAction(deletePrmAction(), SecurityActionCode.ReferAccTypeDel);

        _action.setAfterRefreshEvent(this);
    }

    private GridAction linkAccTypeAction(){
        return new GridAction(masterGrid, null, "Источники", new Image(ImageConstants.INSTANCE.function()), 10, true) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                if (linkAccType2SourceDlg == null) {
                    linkAccType2SourceDlg = new LinkAccType2SourceDlg();
                    linkAccType2SourceDlg.setCaption("Источники");
                    linkAccType2SourceDlg.setDlgEvents(this);
                }

                getWrapperContent(row, linkAccType2SourceDlg);
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception{
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                BarsGLEntryPoint.dictionaryService.setSrcLinkedToAccType((AccTypeSourceWrapper) prms, new AuthCheckAsyncCallback<RpcRes_Base<AccTypeSourceWrapper>>() {
                    @Override
                    public void onSuccess(RpcRes_Base<AccTypeSourceWrapper> res) {
                        if (res.isError()) {
                            DialogManager.error("Ошибка", "Операция сохранения источников сделки не удалась.\nОшибка: " + res.getMessage());
                        } else {
                            linkAccType2SourceDlg.hide();
                        }

                        WaitingManager.hide();
                    }
                });
            }

            private void getWrapperContent(final Row row, final LinkAccType2SourceDlg dlg) {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                String accType = (String) row.getField(4).getValue();

               BarsGLEntryPoint.dictionaryService.getSrcLinkedToAccType(accType, new AuthCheckAsyncCallback<RpcRes_Base<AccTypeSourceWrapper>>() {
                   @Override
                   public void onSuccess(RpcRes_Base<AccTypeSourceWrapper> res) {
                       if (res.isError()) {
                           DialogManager.error("Ошибка", "Операция получения источников сделки не удалась.\nОшибка: " + res.getMessage());
                       } else {
                           AccTypeSourceWrapper wrapper = res.getResult();
                           wrapper.setAcctype((String) row.getField(4).getValue());
                           wrapper.setAcctypeName((String) row.getField(5).getValue());
                           dlg.show(wrapper);
                       }
                       WaitingManager.hide();
                   }
               });
            }
        };
    }

    private GridAction createAccTypeAction(){
        return new GridAction(masterGrid, null, AccountingTypeDlg.CREATE, new Image(ImageConstants.INSTANCE.new24()), 10) {
            @Override
            public void execute() {
               if (createDlg == null){
                   createDlg = new AccountingTypeCreateDlg(){
                       @Override
                       protected void additionalCheck(Object prms){
                           try {
                               saveMaster((AccTypeWrapper) prms, FormAction.CREATE,
                                       "AccType не создан",
                                       "Ошибка создания AccType",
                                       Utils.Fmt("AccType с кодом {0} создан успешно", ((AccTypeWrapper) prms).getAcctype()));
                           } catch (Exception e) {
                              DialogManager.error("Ошибка", e.getMessage());
                           }
                       }
                   };
                   createDlg.setCaption(AccountingTypeDlg.CREATE);
                   createDlg.setDlgEvents(this);
               }
               createDlg.show(null);
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception{
                createDlg.checkAccTypeAndPartsInDB(prms);
            }
        };
    }

    private GridAction editAccTypeAction(){
        return new GridAction(masterGrid, null, AccountingTypeDlg.EDIT, new Image(ImageConstants.INSTANCE.edit24()), 10, true) {
            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                if (master_dlg == null) master_dlg = new AccountingTypeDlg(masterTable.getColumns());
                master_dlg.setFormAction(FormAction.UPDATE);
                master_dlg.setDlgEvents(this);
                master_dlg.show(row);
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception{
                saveMaster((AccTypeWrapper) prms, FormAction.UPDATE,
                        "AccType не сохранен",
                        "Ошибка изменения AccType",
                        Utils.Fmt("AccType с кодом {0} изменен успешно", ((AccTypeWrapper) prms).getAcctype()));
            }
        };
    }

    private GridAction deleteAccTypeAction(){
        return new GridAction(masterGrid, null, AccountingTypeDlg.DELETE, new Image(ImageConstants.INSTANCE.stop()), 10, true) {

            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                if (master_dlg == null) master_dlg = new AccountingTypeDlg(masterTable.getColumns());
                master_dlg.setFormAction(FormAction.DELETE);
                master_dlg.setDlgEvents(this);
                master_dlg.show(row);
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception{
                saveMaster((AccTypeWrapper) prms, FormAction.DELETE,
                        "AccType не удален",
                        "Ошибка удаления AccType",
                        "AccType удален успешно");
            }
        };
    }

    private void saveMaster(final AccTypeWrapper wrapper, final FormAction action, final String failureMessage, final String errorMessage, final String successMessage) throws Exception {
        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
        BarsGLEntryPoint.dictionaryService.saveAccType(wrapper, action, new AuthCheckAsyncCallback<RpcRes_Base<AccTypeWrapper>>() {
            @Override
            public void onFailureOthers(Throwable caught) {
                WaitingManager.hide();
                showInfo(failureMessage, caught.getLocalizedMessage());
            }

            @Override
            public void onSuccess(RpcRes_Base<AccTypeWrapper> res) {
                if (res.isError()) {
                    showInfo(errorMessage, res.getMessage());
                } else {
                    showInfo(successMessage);
                    if (action == FormAction.CREATE) {
                        createDlg.hide();
                    } else{
                        master_dlg.hide();
                    }
                    if (FormAction.CREATE == action){
                        ArrayList<FilterItem> list = new ArrayList<FilterItem>();

                        FilterItem item = new FilterItem(sectionColumn, FilterCriteria.EQ, wrapper.getSection());
                        list.add(item);

                        item = new FilterItem(productColumn, FilterCriteria.EQ, wrapper.getProduct());
                        list.add(item);

                        item = new FilterItem(subProductColumn, FilterCriteria.EQ, wrapper.getSubProduct());
                        list.add(item);

                        item = new FilterItem(modifierColumn, FilterCriteria.EQ, wrapper.getModifier());
                        list.add(item);

                        masterRefreshAction.setFilterCriteria(list);
                    }
                    masterRefreshAction.execute();
                }
                WaitingManager.hide();
            }
        });
    }

    private void saveDetail(ActParmWrapper wrapper, FormAction action, final String failureMessage, final String errorMessage, final String successMessage) throws Exception {
        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
        BarsGLEntryPoint.dictionaryService.saveActParm(wrapper, action, new AuthCheckAsyncCallback<RpcRes_Base<ActParmWrapper>>() {
            @Override
            public void onFailureOthers(Throwable caught) {
                WaitingManager.hide();
                showInfo(failureMessage, caught.getLocalizedMessage());
            }

            @Override
            public void onSuccess(RpcRes_Base<ActParmWrapper> res) {
                if (res.isError()) {
                    showInfo(errorMessage, res.getMessage());
                } else {
                    showInfo(successMessage);
                    detail_dlg.hide();
                    detailRefreshAction.execute();
                }
                WaitingManager.hide();
            }
        });
    }

    private GridAction createPrmAction(){
        return new GridAction(detailGrid, null, ActParmDlg.CREATE, new Image(ImageConstants.INSTANCE.new24()), 10) {

            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (masterGrid.getRowCount() == 0 || row == null) return;

                if (detail_dlg == null) detail_dlg = new ActParmDlg();
                detail_dlg.setFormAction(FormAction.CREATE);
                detail_dlg.setDlgEvents(this);

                detail_dlg.show(masterGrid.getCurrentRow().getField(4).getValue());
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception{
                ActParmWrapper w = (ActParmWrapper) prms;
                saveDetail(w, FormAction.CREATE,
                        "Параметры счета не созданы",
                        "Ошибка создания параметров счета",
                        Utils.Fmt("Параметры счета {0} созданы успешно.\n" +
                                 "Тип собственности: {1}\n" +
                                 "Код срока: {2}\n" +
                                 "Б/счет 2-го порядка: {3}\n" +
                                 "Символ доходов/расходов: {4}\n" +
                                 "ACOD Midas: {5}\n" +
                                 "SQ Midas: {6}\n" +
                                 "Дата начала: {7}\n" +
                                 "Дата конца: {8}",
                                Utils.toStr(w.getAccType()), Utils.toStr(w.getCusType()),
                                Utils.toStr(w.getTerm()), Utils.toStr(w.getAcc2()),
                                Utils.toStr(w.getPlcode()), Utils.toStr(w.getAcod()),
                                Utils.toStr(w.getAc_sq()), Utils.toStr(w.getDtb()),
                                Utils.toStr(w.getDte())));
            }
        };
    }

    private GridAction editPrmAction(){
        return new GridAction(detailGrid, null, ActParmDlg.EDIT, new Image(ImageConstants.INSTANCE.edit24()), 10, true) {

            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                if (detail_dlg == null) detail_dlg = new ActParmDlg();
                detail_dlg.setFormAction(FormAction.UPDATE);
                detail_dlg.setDlgEvents(this);
                detail_dlg.show(row);
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception{
                ActParmWrapper w = (ActParmWrapper) prms;
                saveDetail(w, FormAction.UPDATE,
                        "Параметры счета не сохранены",
                        "Ошибка изменения параметров счета",
                        Utils.Fmt("Параметры счета {0} изменены успешно.\n" +
                                        "Тип собственности: {1}\n" +
                                        "Код срока: {2}\n" +
                                        "Б/счет 2-го порядка: {3}\n" +
                                        "Символ доходов/расходов: {4}\n" +
                                        "ACOD Midas: {5}\n" +
                                        "SQ Midas: {6}\n" +
                                        "Дата начала: {7}\n" +
                                        "Дата конца: {8}",
                                Utils.toStr(w.getAccType()), Utils.toStr(w.getCusType()),
                                Utils.toStr(w.getTerm()), Utils.toStr(w.getAcc2()),
                                Utils.toStr(w.getPlcode()), Utils.toStr(w.getAcod()),
                                Utils.toStr(w.getAc_sq()), Utils.toStr(w.getDtb()),
                                Utils.toStr(w.getDte())));
            }
        };
    }

    private GridAction deletePrmAction(){
        return new GridAction(detailGrid, null, ActParmDlg.DELETE, new Image(ImageConstants.INSTANCE.stop()), 10, true) {

            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                if (detail_dlg == null) detail_dlg = new ActParmDlg();
                detail_dlg.setFormAction(FormAction.DELETE);
                detail_dlg.setDlgEvents(this);
                detail_dlg.show(row);
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception{
                ActParmWrapper w = (ActParmWrapper) prms;
                saveDetail(w, FormAction.DELETE,
                        "Параметры счета не удалены",
                        "Ошибка удаления параметров счета",
                        Utils.Fmt("Параметры счета {0} удалены успешно.", Utils.toStr(w.getAccType())));
            }
        };
    }

    @Override
    protected Table prepareMasterTable() {
        Table result = new Table();

        result.addColumn(sectionColumn = new Column("SECTION", Column.Type.STRING, FIELD_SECTION, 8));
        result.addColumn(productColumn = new Column("PRODUCT", Column.Type.STRING, FIELD_PRODUCT, 9));
        result.addColumn(subProductColumn = new Column("SUBPRODUCT", Column.Type.STRING, FIELD_SUBPRODUCT, 13));
        result.addColumn(modifierColumn = new Column("MODIFIER", Column.Type.STRING, FIELD_MODIFIER, 15));
        result.addColumn(new Column("ACCTYPE", Column.Type.STRING, FIELD_ACCTYPE, 10, true, false, Column.Sort.ASC, ""));
        result.addColumn(new Column("ACCNAME", Column.Type.STRING, FIELD_ACCTYPENAME, 100));
        result.addColumn(new Column("PL_ACT", Column.Type.STRING, FIELD_PL_ACT, 10));
        result.addColumn(new Column("FL_CTRL", Column.Type.STRING, FIELD_FL_CTRL, 12));
        result.addColumn(new Column("TECH_ACT", Column.Type.STRING, FIELD_TECH_ACT, 12));


        return result;
    }

    @Override
    protected String prepareMasterSql() {
        return "select * from ( " +
                "select left(ACCTYPE, 3) as SECTION, substr(ACCTYPE, 4, 2) as PRODUCT, substr(ACCTYPE, 6, 2) as SUBPRODUCT, " +
                "right(ACCTYPE, 2) as MODIFIER, ACCTYPE, ACCNAME, PL_ACT, FL_CTRL, case when TECH_ACT='Y' then 'Y' else 'N' end as TECH_ACT from GL_ACTNAME) v";
    }

    @Override
    protected ArrayList<SortItem> getInitialMasterSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("ACCTYPE", Column.Sort.ASC));

        return list;
    }

    @Override
    protected Table prepareDetailTable() {
        Table result = new Table();
        Column col;

        result.addColumn(acc_type = new Column("ACCTYPE", Column.Type.STRING, ActParm.FIELD_ACCTYPE, 24, true, false, Column.Sort.ASC, ""));
        acc_type.setFilterable(false);
        acc_type.setEditable(false);
        result.addColumn(new Column("CUSTYPE", Column.Type.STRING, ActParm.FIELD_CUSTYPE, 20, true, false, Column.Sort.ASC, ""));
        result.addColumn(new Column("TERM", Column.Type.STRING, ActParm.FIELD_TERM, 20, true, false, Column.Sort.ASC, ""));
        result.addColumn(new Column("ACC2", Column.Type.STRING, ActParm.FIELD_ACC2, 22, true, false, Column.Sort.ASC, ""));
        result.addColumn(new Column("PLCODE", Column.Type.STRING, ActParm.FIELD_PLCODE, 25));
        result.addColumn(new Column("ACOD", Column.Type.STRING, ActParm.FIELD_ACOD, 20));
        result.addColumn(new Column("AC_SQ", Column.Type.STRING, ActParm.FIELD_AC_SQ, 20));
        result.addColumn(new Column("DTB", Column.Type.DATE, ActParm.FIELD_DTB, 25));
        result.addColumn(new Column("DTE", Column.Type.DATE, ActParm.FIELD_DTE, 25));
        result.addColumn(col = new Column("ACCNAME", Column.Type.STRING, ActParm.FIELD_ACCNAME, 240));
        col.setFilterable(false);
        col.setEditable(false);

        return result;
    }

    @Override
    protected String prepareDetailSql() {
        return "select PARM.ACCTYPE, PARM.CUSTYPE, PARM.TERM, PARM.ACC2, PARM.PLCODE, PARM.ACOD, PARM.AC_SQ, PARM.DTB, PARM.DTE, NM.ACCNAME "
                + "from GL_ACTPARM PARM, GL_ACTNAME NM "
                + "where NM.ACCTYPE = PARM.ACCTYPE";
    }

    @Override
    public ArrayList<SortItem> getInitialDetailSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("ACCTYPE", Column.Sort.ASC));
        list.add(new SortItem("CUSTYPE", Column.Sort.ASC));
        list.add(new SortItem("TERM", Column.Sort.ASC));
        list.add(new SortItem("ACC2", Column.Sort.ASC));
        list.add(new SortItem("DTB", Column.Sort.ASC));
        return list;
    }

    @Override
    public ArrayList<FilterItem> createLinkFilterCriteria(Row row) {
        ArrayList<FilterItem> list = new ArrayList<FilterItem>();
        list.add(new FilterItem(acc_type, FilterCriteria.EQ, row == null ? "" : row.getField(4).getValue()));

        return list;
    }

    @Override
    public void afterRefresh(int rowCount) {
        if (_action != null) {
            createDetailAction.setEnable(rowCount != 0);
        }
    }
}
