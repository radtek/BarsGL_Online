package ru.rbt.barsgl.gwt.client.pd;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.quickFilter.DateQuickFilterAction;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.operation.ManualTechOperationWrapper;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.security.gwt.client.operday.IDataConsumer;
import ru.rbt.security.gwt.client.operday.OperDayGetter;
import ru.rbt.shared.enums.SecurityActionCode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getEnumLabelsList;
import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getYesNoList;
import static ru.rbt.barsgl.gwt.client.quickFilter.DateQuickFilterParams.DateFilterField.CREATE_DATE;
import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndFilialPart;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.*;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.addDays;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;
import static ru.rbt.security.gwt.client.operday.OperDayGetter.getOperday;

/**
 * Created by ER18837 on 14.03.16.
 */
public class PostingFormTech extends GridForm {
    public static final String FORM_NAME = "Проводки (учёт по техническим счетам)";
    public static final int DAYS_EDIT = 30;


    protected Column colProcDate;
    protected Column colPostDate;
    protected Column colValueDate;

    private Date operday, editday;

    private int podIndex, invisibleIndex, idDrIndex;

    GridAction quickFilterAction;

    public PostingFormTech() {
        super(FORM_NAME, true);
        reconfigure();
    }

    private void reconfigure() {
        abw.addAction(quickFilterAction = new DateQuickFilterAction(grid, colProcDate, colValueDate, colPostDate, CREATE_DATE, false));
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addAction(createPreview());
        abw.addSecureAction(editPostingTech(), SecurityActionCode.TechOperPstChng, SecurityActionCode.TechOperPstChngDate);
        abw.addSecureAction(deletePostingTech(), SecurityActionCode.TechOperPstMakeInvisible);

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
    protected String prepareSql() {
        return "SELECT * FROM V_GL_PDTH "
               /* + getSourceAndFilialPart("where", "SRC_PST", "FILIAL_DR")*/;
    }

    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("GLO_REF", Column.Sort.DESC));
        return list;
    }

    protected Table prepareTable() {
        Table result = new Table();
        Column col;

        HashMap<Serializable, String> yesNoList = getYesNoList();
        result.addColumn(new Column("GLO_REF", LONG, "ID операции", 70));
        result.addColumn(col = new Column("INP_METHOD", STRING, "Способ ввода", 50));
        col.setList(getEnumLabelsList(InputMethod.values()));

        idDrIndex = result.addColumn(new Column("PCID", LONG, "ID проводки", 80));
        invisibleIndex = result.addColumn(new Column("INVISIBLE", STRING, "Отменена", 40));
        result.addColumn(new Column("SRC_PST", STRING, "Источник сделки", 60));
        result.addColumn(new Column("DEAL_ID", STRING, "ИД сделки", 120));
        Column colSubDealID;
        result.addColumn(colSubDealID = new Column("SUBDEALID", STRING, "ИД субсделки", 120));
        colSubDealID.setVisible(false);
        result.addColumn(new Column("PMT_REF", STRING, "ИД платежа", 120, false, false));

        result.addColumn(colProcDate = new Column("PROCDATE", DATE, "Дата опердня", 80));
        colProcDate.setFormat("dd.MM.yyyy");
        result.addColumn(colValueDate = new Column("VALD", DATE, "Дата валютирования", 80));
        colValueDate.setFormat("dd.MM.yyyy");
        podIndex = result.addColumn(colPostDate = new Column("POD", DATE, "Дата проводки", 80));
        colPostDate.setFormat("dd.MM.yyyy");

        result.addColumn(new Column("ACCTYPE_DR", DECIMAL, "AccType ДБ", 80, false, false, Column.Sort.ASC, "000000000"));
        result.addColumn(new Column("BSAACID_DR", STRING, "Счет ДБ", 160));
        result.addColumn(new Column("CBCC_DR", STRING, "Филиал ДБ (счет)", 60, false, false));
        result.addColumn(new Column("FILIAL_DR", STRING, "Филиал ДБ (опер)", 60, false, false));
        result.addColumn(new Column("CCY_DR", STRING, "Валюта ДБ", 60));
        result.addColumn(new Column("AMNT_DR", DECIMAL, "Сумма ДБ", 100));
        result.addColumn(new Column("AMNTBC_DR", DECIMAL, "Сумма в руб. ДБ", 100));

        result.addColumn(new Column("ACCTYPE_CR", DECIMAL, "AccType КР", 80,false, false, Column.Sort.ASC, "000000000"));
        result.addColumn(new Column("BSAACID_CR", STRING, "Счет КР", 160));
        result.addColumn(new Column("CBCC_CR", STRING, "Филиал КР (счет)", 60, false, false));
        result.addColumn(new Column("FILIAL_CR", STRING, "Филиал КР (опер)", 60, false, false));
        result.addColumn(new Column("CCY_CR", STRING, "Валюта КР", 60));
        result.addColumn(new Column("AMNT_CR", DECIMAL, "Сумма КР", 100));
        result.addColumn(new Column("AMNTBC_CR", DECIMAL, "Сумма в руб. КР", 100));

        result.addColumn(new Column("NRT", STRING, "Основание ENG", 500, false, false));
        result.addColumn(new Column("RNARLNG", STRING, "Основание RUS", 200, false, false));
        result.addColumn(new Column("RNARSHT", STRING, "Основание короткое", 200, false, false));

        result.addColumn(col = new Column("FCHNG", STRING, "Исправительная", 40));
        col.setList(yesNoList);
        result.addColumn(new Column("PRFCNTR", STRING, "Профит центр", 60));
        result.addColumn(new Column("DEPT_ID", STRING, "Подразделение", 60));

        return result;
    }

    private GridAction createPreview(){
        return new GridAction(this.getGrid(), null, "Просмотр", new Image(ImageConstants.INSTANCE.preview()), 10, true) {
            PostingTechDlg dlg;

            @Override
            public void execute() {
                Row row = grid.getCurrentRow();
                if ((row == null)) {
                    return;
                }

                dlg = new PostingTechDlg("Просмотр проводки по техническим счетам", FormAction.PREVIEW, this.grid.getTable().getColumns());
                dlg.setDlgEvents(this);
                dlg.show(row);
            }
        };
    }

    private GridAction editPostingTech(){

        return new GridAction(grid, null, "Редактирование ручной проводки по тех. счетам", new Image(ImageConstants.INSTANCE.edit24()), 10, true) {
            PostingTechDlg dlg;

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

                dlg = new PostingTechDlg("Редактирование ручной проводки по тех. счетам", FormAction.UPDATE, table.getColumns());
                dlg.setDlgEvents(this);
                dlg.show(row);
            }

            @Override
            public void onDlgOkClick(Object prms) {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                ManualTechOperationWrapper wrapper = (ManualTechOperationWrapper) prms;
                BarsGLEntryPoint.operationService.updateTechPostings(wrapper,  new PostingFormTech.PostingAsyncCallback(dlg));
            }
        };
    }

    private boolean checkPostDate(Row row) {
        boolean check = (podIndex >= 0) && !((Date) row.getField(podIndex).getValue()).before(editday);
        if (!check) {
            showInfo("Нельзя изменить проводку, учтенную в балансе более чем " + DAYS_EDIT + " дней назад");
        }
        return check;
    }

    private boolean isInvisible(Row row) {
        return (invisibleIndex >= 0) && "Y".equals(row.getField(invisibleIndex).getValue());
    }

    private ArrayList<Long> getPdIdList() {
        ArrayList<Long> pdList = new ArrayList<Long>();
        Row row = grid.getCurrentRow();
        if (null == row)
            return null;
        pdList.add((Long) row.getField(idDrIndex).getValue());
        return pdList;
    }

    private GridAction deletePostingTech() {
        return new GridAction(grid, null, "Подавить / восстановить проводку", new Image(ImageConstants.INSTANCE.close24()), 10, true) {
            PostingTechDlg dlg;

            @Override
            public void execute() {
                Row row = grid.getCurrentRow();
                int rowCount = grid.getRowCount();
                if ((row == null) || (rowCount < 1)) {
                    return;
                }
                if (!checkPostDate(row)) {
                    return ;
                }

                boolean isInvisible = isInvisible(row);
                String act = isInvisible ? "Отмена подавления" : "Подавление";

                dlg = new PostingTechDlg(act + " проводки", FormAction.OTHER, table.getColumns());
                dlg.setDlgEvents(this);
                dlg.show(row);
            }

            @Override
            public void onDlgOkClick(Object prms) {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                ManualTechOperationWrapper wrapper = (ManualTechOperationWrapper) prms;
                wrapper.setPdIdList(getPdIdList());

                BarsGLEntryPoint.operationService.suppressPdTh(wrapper, new PostingFormTech.PostingAsyncCallback(dlg));
            }
        };
    }

    class PostingAsyncCallback extends AuthCheckAsyncCallback<RpcRes_Base<ManualTechOperationWrapper>> {
        private PostingTechDlg dlg;

        public PostingAsyncCallback(PostingTechDlg dlg) {
            this.dlg = dlg;
        }

        @Override
        public void onFailureOthers(Throwable throwable) {
            WaitingManager.hide();
            showInfo("Ошибка при изменении проводки", throwable.getLocalizedMessage());
        }

        @Override
        public void onSuccess(RpcRes_Base<ManualTechOperationWrapper> wrapper) {
            if (wrapper.isError()) {
                showInfo("Ошибка при изменении проводки", wrapper.getMessage());
            } else {
                showInfo("Проводка изменена успешно");
                dlg.hide();
                refreshAction.execute();
            }
            WaitingManager.hide();
        }
    }
}
