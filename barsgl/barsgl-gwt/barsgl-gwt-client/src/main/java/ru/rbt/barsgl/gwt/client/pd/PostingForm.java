package ru.rbt.barsgl.gwt.client.pd;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.gridForm.MDForm;
import ru.rbt.barsgl.gwt.client.quickFilter.DateQuickFilterAction;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.*;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.utils.UUID;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.Export.ExcelExportHead;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.PostingChoice;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;
import ru.rbt.grid.gwt.client.export.Export2Excel;
import ru.rbt.grid.gwt.client.export.ExportActionCallback;
import ru.rbt.security.gwt.client.operday.IDataConsumer;
import ru.rbt.security.gwt.client.operday.OperDayGetter;
import ru.rbt.shared.user.AppUserWrapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import static ru.rbt.barsgl.gwt.client.quickFilter.DateQuickFilterParams.DateFilterField.CREATE_DATE;
import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndCodeFilialPart;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.*;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.*;
import static ru.rbt.barsgl.shared.enums.PostingChoice.*;
import static ru.rbt.security.gwt.client.operday.OperDayGetter.getOperday;
import static ru.rbt.shared.enums.SecurityActionCode.*;

/**
 * Created by ER18837 on 04.04.16.
 */
public class PostingForm extends MDForm {
    public static final String FORM_NAME = "Проводки";
    public static final int DAYS_EDIT = 30;
    protected Column colProcDate;
    protected Column colPostDate;
    protected Column colValueDate;
    protected Column colGloid;

    private Date operday, editday;

    PopupPanel editPanel;
    private int podIndex, invisibleIndex, idDrIndex, idCrIndex, fanIndex, fanTypeIndex;

    public PostingForm(){
        super(FORM_NAME, null, "Все проводки по операции", true);
        setLazyDetailRefresh(true);
        reconfigure();
    }

    private void reconfigure() {
        GridAction quickFilterAction;
        masterActionBar.addAction(quickFilterAction = new DateQuickFilterAction(masterGrid, colProcDate, colValueDate, colPostDate, CREATE_DATE, false));
        masterActionBar.addAction(new SimpleDlgAction(masterGrid, DlgMode.BROWSE, 10));
        detailActionBar.addAction(new SimpleDlgAction(detailGrid, DlgMode.BROWSE, 10));
        masterActionBar.addAction(createPreview());
        masterActionBar.addSecureAction(editChoiceAction(), OperPstChng, OperPstChngDate, OperPstChngDateArcRight);
        masterActionBar.addSecureAction(new DeleteAction(), OperPstMakeInvisible);
        masterActionBar.addAction(BackValuePostingReport());

        getOperday(new IDataConsumer<OperDayWrapper>() {
            @Override
            public void accept(OperDayWrapper wrapper) {
                operday = DateTimeFormat.getFormat(OperDayGetter.dateFormat).parse(wrapper.getCurrentOD());
                editday = addDays(operday, -DAYS_EDIT);
            }
        });
        quickFilterAction.execute();
    }

    @Override
    protected String prepareMasterSql() {
        return "select * from (select a.*, a.BSAACID_DR || ' ' || a.BSAACID_CR as DR_CR from V_GL_PDLINK as a) tl "
                + getSourceAndCodeFilialPart("where", "SRC_PST", "FILIAL_CR", "FILIAL_DR");
    }

    @Override
    protected Table prepareMasterTable() {
        return prepareTable(true);
    }

    @Override
    protected String prepareDetailSql() {
        return "select * from V_GL_PDLINK";
    }

    @Override
    protected Table prepareDetailTable() {
        return prepareTable(false);
    }

    @Override
    public ArrayList<FilterItem> createLinkFilterCriteria(Row row) {
        ArrayList<FilterItem> list = new ArrayList<FilterItem>();
        list.add(new FilterItem(colGloid, FilterCriteria.EQ, row == null ? -1 : row.getField(0).getValue()));

        return list;
    }


    private Table prepareTable(boolean forMasterTable) {
        Table result = new Table();
        Column col;

        HashMap<Serializable, String> yesNoList = getYesNoList();
        result.addColumn(new Column("GLOID", LONG, "ID операции", 70));
        result.addColumn(colGloid = new Column("PAR_GLO", LONG, "ID осн. операции", 70, false, false));
        result.addColumn(col = new Column("INP_METHOD", STRING, "Способ ввода", 50));
        col.setList(getEnumLabelsList(InputMethod.values()));

        result.addColumn(col = new Column("POST_TYPE", STRING, "Тип проводки", 40, false, false));
        col.setList(getPostingTypeList());
        result.addColumn(new Column("PCID", LONG, "ID проводки", 100));
        idDrIndex = result.addColumn(new Column("ID_DR", LONG, "ID полупроводки ДБ", 80, false, false));
        idCrIndex = result.addColumn(new Column("ID_CR", LONG, "ID полупроводки КР", 80, false, false));

        invisibleIndex = result.addColumn(col = new Column("INVISIBLE", STRING, "Отменена", 40));
        col.setList(yesNoList);
        result.addColumn(new Column("SRC_PST", STRING, "Источник сделки", 80));
        result.addColumn(new Column("PBR", STRING, "Система-источник", 100, false, false));
        result.addColumn(new Column("DEAL_ID", STRING, "ИД сделки", 120));
        result.addColumn(new Column("SUBDEALID", STRING, "ИД субсделки", 180));
        result.addColumn(new Column("PMT_REF", STRING, "ИД платежа", 120, false, false));
        result.addColumn(new Column("PREF", STRING, "ИД сделки/ платежа", 120));
        result.addColumn(colProcDate = new Column("PROCDATE", DATE, "Дата опердня", 80));
        colProcDate.setFormat("dd.MM.yyyy");
        result.addColumn(colValueDate = new Column("VDATE", DATE, "Дата валютирования", 80));
        colValueDate.setFormat("dd.MM.yyyy");
        podIndex = result.addColumn(colPostDate = new Column("POSTDATE", DATE, "Дата проводки", 80));
        colPostDate.setFormat("dd.MM.yyyy");

        result.addColumn(new Column("ACID_DR", STRING, "Счет Midas ДБ", 170));
        result.addColumn(new Column("BSAACID_DR", STRING, "Счет ДБ", 160));
        result.addColumn(new Column("CBCC_DR", STRING, "Филиал ДБ (счет)", 60, false, false));
        result.addColumn(new Column("FILIAL_DR", STRING, "Филиал ДБ (опер)", 60, false, false));
        result.addColumn(new Column("CCY_DR", STRING, "Валюта ДБ", 60));
        result.addColumn(new Column("AMT_DR", DECIMAL, "Сумма ДБ", 100));
        result.addColumn(new Column("AMTRU_DR", DECIMAL, "Сумма в руб. ДБ", 100));

        result.addColumn(new Column("ACID_CR", STRING, "Счет Midas КР", 170));
        result.addColumn(new Column("BSAACID_CR", STRING, "Счет КР", 160));
        result.addColumn(new Column("CBCC_CR", STRING, "Филиал КР (счет)", 60, false, false));
        result.addColumn(new Column("FILIAL_CR", STRING, "Филиал КР (опер)", 60, false, false));
        result.addColumn(new Column("CCY_CR", STRING, "Валюта КР", 60));
        result.addColumn(new Column("AMT_CR", DECIMAL, "Сумма КР", 100));
        result.addColumn(new Column("AMTRU_CR", DECIMAL, "Сумма в руб. КР", 100));
        result.addColumn(new Column("AMTRU", DECIMAL, "Сумма в руб.", 100, false, false));

        result.addColumn(new Column("PNAR", STRING, "Описание", 150));
        result.addColumn(new Column("NRT", STRING, "Основание ENG", 500, false, false));
        result.addColumn(new Column("RNARLNG", STRING, "Основание RUS", 500));
        result.addColumn(new Column("RNARSHT", STRING, "Основание короткое", 200, false, false));

        result.addColumn(new Column("MO_NO", STRING, "Мем.ордер", 120));
        result.addColumn(col = new Column("STRN", STRING, "Сторно", 40));
        col.setList(yesNoList);
        result.addColumn(new Column("STRN_GLO", LONG, "Сторно операция", 70, false, false));
        result.addColumn(col = new Column("FCHNG", STRING, "Исправительная", 40));
        col.setList(yesNoList);
        result.addColumn(new Column("PRFCNTR", STRING, "Профит центр", 60));
        result.addColumn(new Column("DPMT", STRING, "Подразделение", 60));

        fanIndex = result.addColumn(col = new Column("FAN", STRING, "Веер", 40));
        col.setList(yesNoList);
        fanTypeIndex = result.addColumn(new Column("FAN_TYPE", STRING, "Тип веерной проводки", 40, false, false));
        result.addColumn(new Column("FAN_GLO", LONG, "ID операции веера", 70, false, false));
        result.addColumn(new Column("GLO_DR", LONG, "ID операции ДБ", 70, false, false));
        result.addColumn(new Column("GLO_CR", LONG, "ID операции КР", 70, false, false));
        result.addColumn(col = new Column("PDMODE", STRING, "Режим записи", 70, false, false));
        col.setList(getArrayValuesList(new String[]{"BUFFER", "DIRECT"}));
        //вычисляемое поле для фильтра по условию "ИЛИ"
        if (forMasterTable){
            result.addColumn(new Column("DR_CR", STRING, "Счета Дт/Кр", 70, false, false));
        }

        return result;
    }

    @Override
    public ArrayList<SortItem> getInitialMasterSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("PAR_GLO", Column.Sort.DESC));
        return list;
    }

    @Override
    public ArrayList<SortItem> getInitialDetailSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("POST_TYPE", Column.Sort.ASC));
        return list;
    }

    private ArrayList<Long> getPdIdList() {
        ArrayList<Long> pdList = new ArrayList<Long>();
        Row row = masterGrid.getCurrentRow();
        if (null == row)
            return null;
        String fanType = (String)row.getField(fanTypeIndex).getValue();
        if (!"C".equals(fanType))       // не перо по кредиту
            pdList.add((Long) row.getField(idDrIndex).getValue());
        if (!"D".equals(fanType))       // не перо по дебиту
            pdList.add((Long) row.getField(idCrIndex).getValue());
        return pdList;
    }

    private PopupPanel getChoicePanel() {
        final PopupPanel panel = new PopupPanel(true, true);
        VerticalPanel vp = new VerticalPanel();
        vp.add(new HTML("<b>Редактировать проводки</b>"));
        MenuItem itemSingle = new MenuItem("Только выбранную", new Command() {

            @Override
            public void execute() {
                panel.hide();
                new EditAction(PST_ONE_OF).execute();
            }
        });
        MenuItem itemAll = new MenuItem("Выбранную и все связанные проводки по операции", new Command() {

            @Override
            public void execute() {
                panel.hide();
                new EditAction(PST_ALL).execute();
            }
        });
        MenuBar bar = new MenuBar(true);
        bar.addItem(itemSingle);
        bar.addItem(itemAll);
        vp.add(bar);
        panel.setWidget(vp);
        return panel;
    }

    private boolean isInvisible(Row row) {
        return (invisibleIndex >= 0) && "Y".equals((String) row.getField(invisibleIndex).getValue());
    }

    private boolean checkPostDate(Row row) {
        boolean check = (podIndex >= 0) && !((Date) row.getField(podIndex).getValue()).before(editday);
        if (!check) {
            showInfo("Нельзя изменить проводку, учтенную в балансе более чем " + DAYS_EDIT + " дней назад");
        }
        return check;
    }

    private boolean checkFan(Row row) {
        boolean fan = (fanIndex >= 0) && "Y".equals(row.getField(fanIndex).getValue());
        if (fan) {
            showInfo("Нельзя изменить проводку по веерной операции");
        }
        return fan;
    }

    private GridAction editChoiceAction(){
        editPanel = getChoicePanel();
        return new GridAction(masterGrid, null, "Редактировать проводку", new Image(ImageConstants.INSTANCE.edit24()), 10, true) {
            @Override
            public void execute() {
                Row row = grid.getCurrentRow();
                if ((row == null) || !checkPostDate(row)) {
                    return;
                }

                if (isInvisible(row)) {
                    showInfo("Нельзя изменить отмененную (подавленную) проводку");
                    return ;
                }
                int rowCount = detailGrid.getRowCount();
                if (!isEmpty((String)row.getField(fanTypeIndex).getValue())) {	// это веер
                    new EditAction(PST_ALL).execute();
                }
                else if (rowCount > 1) {
                    final PushButton button = masterActionBar.getButton(this);
                    editPanel.setPopupPositionAndShow(new PopupPanel.PositionCallback() {

                        @Override
                        public void setPosition(int i, int i1) {
                            editPanel.setPopupPosition(button.getAbsoluteLeft(), button.getAbsoluteTop() + button.getOffsetHeight());
                        }
                    });
                }
                else {
                    new EditAction(PST_SINGLE).execute();
                }
            }
        };
    }

    protected GridAction createPreview(){
        return new GridAction(masterGrid, null, "Просмотр", new Image(ImageConstants.INSTANCE.preview()), 10, true) {
            @Override
            public void execute() {
                Row row = grid.getCurrentRow();
                if (row == null) {
                    return;
                }

                PostingDlg dlg = new PostingDlg("Просмотр проводки GL", FormAction.PREVIEW, masterTable.getColumns());
                dlg.setDlgEvents(this);
                dlg.show(row);
            }
        };
    }

    class EditAction extends GridAction {
        final PostingChoice postingChoice;
        private PostingDlg dlg;

        public EditAction(PostingChoice postingChoice) {
            super(masterGrid, null, "Редактировать проводку", new Image(ImageConstants.INSTANCE.edit24()), 10, true);
            this.postingChoice = postingChoice;
        }

        @Override
        public void execute() {
            Row row = grid.getCurrentRow();
            if (row == null) {
                return;
            }

            String title = (postingChoice == PST_ALL) ? "Редактирование связанных проводок" : "Редактирование проводки";
            dlg = new PostingDlg(title, FormAction.UPDATE, masterTable.getColumns()) {
                @Override
                protected PostingChoice getPostingCoice() {
                    return EditAction.this.postingChoice;
                }
            };
            dlg.setDlgEvents(EditAction.this);
            dlg.show(row);
        }

        @Override
        public void onDlgOkClick(Object prms) {
            WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

            ManualOperationWrapper wrapper = (ManualOperationWrapper) prms;
            wrapper.setPdIdList(getPdIdList());
            wrapper.setPostingChoice(postingChoice);

            BarsGLEntryPoint.operationService.updatePostings(wrapper, new PostingAsyncCallback(postingChoice, dlg) );
        }
    }

    class DeleteAction extends GridAction {
        private PostingChoice postingChoice;
        private PostingDlg dlg;

        public DeleteAction() {
            super(masterGrid, null, "Подавить / восстановить проводку", new Image(ImageConstants.INSTANCE.close24()), 10, true);
        }

        @Override
        public void execute() {
            Row row = grid.getCurrentRow();
            int rowCount = detailGrid.getRowCount();
            if (row == null) {
                return;
            }
            if (!checkPostDate(row)) {
                return ;
            }

            boolean isInvisible = isInvisible(row);
            String act = isInvisible ? "Отмена подавления" : "Подавление";

            postingChoice = (rowCount > 1) ? PST_ALL : PST_SINGLE;
            String title = act + ((postingChoice == PST_ALL) ? " связанных проводок" : " проводки");
            dlg = new PostingDlg(title, FormAction.OTHER, masterTable.getColumns()) {
                @Override
                protected PostingChoice getPostingCoice() {
                    return postingChoice;
                }
            };
            dlg.setDlgEvents(DeleteAction.this);
            dlg.show(row);
        }

        @Override
        public void onDlgOkClick(Object prms) {
            WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

            ManualOperationWrapper wrapper = (ManualOperationWrapper) prms;
            wrapper.setPdIdList(getPdIdList());
            wrapper.setPostingChoice(postingChoice);

            BarsGLEntryPoint.operationService.suppressPostings(wrapper, new PostingAsyncCallback(postingChoice, dlg) );
        }
    }

    class PostingAsyncCallback extends AuthCheckAsyncCallback<RpcRes_Base<ManualOperationWrapper>> {
        final PostingChoice postingChoice;
        private PostingDlg dlg;

        public PostingAsyncCallback(PostingChoice postingChoice, PostingDlg dlg) {
            this.postingChoice = postingChoice;
            this.dlg = dlg;
        }

        @Override
        public void onFailureOthers(Throwable throwable) {
            WaitingManager.hide();
            showInfo("Ошибка при изменении проводки", throwable.getLocalizedMessage());
        }

        @Override
        public void onSuccess(RpcRes_Base<ManualOperationWrapper> wrapper) {
            if (wrapper.isError()) {
                showInfo("Ошибка при изменении проводки", wrapper.getMessage());
            } else {
                showInfo(postingChoice == PostingChoice.PST_ALL ? "Проводки изменены успешно" : "Проводка изменена успешно");
                dlg.hide();
                masterRefreshAction.execute();
                detailRefreshAction.execute();
            }
            WaitingManager.hide();
        }
    }


    private GridAction BackValuePostingReport() {
        return new GridAction(masterGrid, null, "Отчет по Back Value", new Image(ImageConstants.INSTANCE.report()), 5) {

            BackValueReportDlg dlg = null;
            GridAction act = this;

            @Override
            public void execute() {
                if (dlg == null) dlg = new BackValueReportDlg();
                dlg.setDlgEvents(this);
                dlg.show(null);
            }

            public void onDlgOkClick(Object prms){
                dlg.hide();

                final String date = (String)((Object[]) prms)[0];
                final String limit = (String)((Object[]) prms)[1];
                WaitingManager.show("Проверка наличия данных...");

                BarsGLEntryPoint.operationService.operExists(date, new AuthCheckAsyncCallback<RpcRes_Base<Boolean>>() {

                    @Override
                    public void onSuccess(RpcRes_Base<Boolean> res) {
                        if (res.isError()) {
                            WaitingManager.hide();
                            DialogManager.message("Отчет", res.getMessage());
                        } else {
                            dlg.hide();
                            WaitingManager.hide();
                            setEnable(false);

                            String user = "";
                            AppUserWrapper current_user = (AppUserWrapper) LocalDataStorage.getParam("current_user");
                            if (current_user != null){
                                user = Utils.Fmt("{0}({1})", current_user.getUserName(), current_user.getSurname());
                            }

                            ExcelExportHead head = new ExcelExportHead(Utils.Fmt("ОТЧЕТ по операциям BACK VALUE за {0}", date),
                                    user, Utils.Fmt("дата проводки меньше {0}", date));

                            Export2Excel e2e = new Export2Excel(new PostingBackValueReportData(date, limit), head,
                                    new ExportActionCallback(act, UUID.randomUUID().replace("-", "")));
                            e2e.export();
                        }
                    }
                });
            }
        };
    }
}

