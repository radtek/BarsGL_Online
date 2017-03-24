package ru.rbt.barsgl.gwt.client.pd;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.account.AccountFormTech;
import ru.rbt.barsgl.gwt.client.dict.EditableDictionary;
import ru.rbt.barsgl.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.client.gridForm.MDForm;
import ru.rbt.barsgl.gwt.client.operday.IDataConsumer;
import ru.rbt.barsgl.gwt.client.operday.OperDayGetter;
import ru.rbt.barsgl.gwt.client.quickFilter.AccountQuickFilterParams;
import ru.rbt.barsgl.gwt.client.quickFilter.DateQuickFilterAction;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.PostingChoice;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.*;
import static ru.rbt.barsgl.gwt.client.operday.OperDayGetter.getOperday;
import static ru.rbt.barsgl.gwt.client.quickFilter.DateQuickFilterParams.DateFilterField.CREATE_DATE;
import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourceAndCodeFilialPart;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.*;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.*;
import static ru.rbt.barsgl.shared.enums.PostingChoice.*;
import static ru.rbt.barsgl.shared.enums.SecurityActionCode.*;

/**
 * Created by ER18837 on 04.04.16.
 */
public class PostingFormTech extends GridForm {
    public static final String FORM_NAME = "Проводки (учёт по техническим счетам)";
    public static final int DAYS_EDIT = 30;
    protected Column colProcDate;
    protected Column colPostDate;
    protected Column colValueDate;
    protected Column colGloid;

    private Date operday, editday;
    AccountQuickFilterParams quickFilterParams;
    GridAction quickFilterAction;

    PopupPanel editPanel;
    private int podIndex, invisibleIndex, idDrIndex, idCrIndex, fanIndex, fanTypeIndex;

    public PostingFormTech() {
        super(FORM_NAME);
        reconfigure();
    }

    private void reconfigure() {
        //abw.addAction(quickFilterAction = new AccountFormTech.AccountQuickFilterAction(grid, quickFilterParams) );
        //abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        //abw.addSecureAction(editAccount(), SecurityActionCode.AccChng);
        //abw.addSecureAction(createAccount(), SecurityActionCode.AccInp);
        //abw.addSecureAction(closeAccount(), SecurityActionCode.AccClose);
        //abw.addSecureAction(createNewOperation(), SecurityActionCode.AccOperInp);
        //quickFilterAction.execute();

       // abw.addAction(quickFilterAction = new DateQuickFilterAction(masterGrid, colProcDate, colValueDate, colPostDate, CREATE_DATE, false));
        abw.addAction(new SimpleDlgAction(this.getGrid(), DlgMode.BROWSE, 10));
        //abw.addSecureAction(editChoiceAction(), OperPstChng, OperPstChngDate, OperPstChngDateArcRight);
        //abw.addSecureAction(new PostingForm.DeleteAction(), OperPstMakeInvisible);

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
    protected Table prepareTable() {
    /*
	PAR_GLO, GLOID, INP_METHOD, SRC_PST, FAN, STRN, STRN_GLO,
	POST_TYPE, PCID, ID_DR, ID_CR,
	POD, VALD, PBR, INVISIBLE, FCHNG, PRFCNTR, DPMT,
	ACID_DR, BSAACID_DR, CCY_DR, AMT_DR, AMTRU_DR, AMNT_DR, AMNTBC_DR,
	ACID_CR, BSAACID_CR, CCY_CR, AMT_CR, AMTRU_CR, AMNT_CR, AMNTBC_CR,
	CBCC_DR, CBCC_CR,
	AMTRU, DEAL_ID, SUBDEALID, PMT_REF, PREF,
	PNAR, RNARLNG, RNARSHT, NRT,
	GLO_DR, GLO_CR, FAN_TYPE, MO_NO, PDMODE
    */
        Table result = new Table();
        Column col;

        HashMap<Serializable, String> yesNoList = getYesNoList();
        result.addColumn(new Column("GLO_REF", LONG, "ID операции", 70));
        result.addColumn(col = new Column("INP_METHOD", STRING, "Способ ввода", 50));
        col.setList(getEnumLabelsList(InputMethod.values()));
        result.addColumn(new Column("PCID", LONG, "ID проводки", 80));
        result.addColumn(new Column("SRC_PST", STRING, "Источник сделки", 60));
        result.addColumn(new Column("DEAL_ID", STRING, "ИД сделки", 120));
        result.addColumn(new Column("SUBDEALID", STRING, "ИД субсделки", 120));
        result.addColumn(new Column("PMT_REF", STRING, "ИД платежа", 120, false, false));
        result.addColumn(colProcDate = new Column("PROCDATE", DATE, "Дата опердня", 80));
        colProcDate.setFormat("dd.MM.yyyy");
        result.addColumn(colValueDate = new Column("VALD", DATE, "Дата валютирования", 80));
        colValueDate.setFormat("dd.MM.yyyy");
        podIndex = result.addColumn(colPostDate = new Column("POD", DATE, "Дата проводки", 80));
        colPostDate.setFormat("dd.MM.yyyy");
        result.addColumn(new Column("ACCTYPE_DR", STRING, "Тип счёта ДБ", 160));
        result.addColumn(new Column("BSAACID_DR", STRING, "Счет ДБ", 160));
        result.addColumn(new Column("CCY_DR", STRING, "Валюта ДБ", 60));
        result.addColumn(new Column("AMNT_DR", DECIMAL, "Сумма ДБ", 100));
        result.addColumn(new Column("AMNTBC_DR", DECIMAL, "Сумма в руб. ДБ", 100));
        result.addColumn(new Column("ACCTYPE_CR", STRING, "Тип счёта КР", 160));
        result.addColumn(new Column("BSAACID_CR", STRING, "Счет КР", 160));
        result.addColumn(new Column("CCY_CR", STRING, "Валюта КР", 60));
        result.addColumn(new Column("AMNT_CR", DECIMAL, "Сумма КР", 100));
        result.addColumn(new Column("AMNTBC_CR", DECIMAL, "Сумма в руб. КР", 100));
        result.addColumn(new Column("NRT", STRING, "Описание", 500, false, false));
        result.addColumn(new Column("RNARSHT", STRING, "Основание RUS", 200, false, false));
        result.addColumn(col = new Column("STRN", STRING, "Сторно", 40));
        col.setList(yesNoList);
        result.addColumn(col = new Column("FCHNG", STRING, "Исправительная", 40));
        col.setList(yesNoList);
        result.addColumn(new Column("DEPT_ID", STRING, "Подразделение", 60));

        return result;
    }

    private PopupPanel getChoicePanel() {
        final PopupPanel panel = new PopupPanel(true, true);
        VerticalPanel vp = new VerticalPanel();
        vp.add(new HTML("<b>Редактировать проводки</b>"));
        MenuItem itemSingle = new MenuItem("Только выбранную", new Command() {

            @Override
            public void execute() {
                panel.hide();
            }
        });
        MenuItem itemAll = new MenuItem("Выбранную и все связанные проводки по операции", new Command() {

            @Override
            public void execute() {
                panel.hide();
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

    @Override
    protected String prepareSql() {
        return "SELECT * FROM V_GL_PDTH "
                + getSourceAndCodeFilialPart("where", "SRC_PST", "FILIAL_DR", "");
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
            }
            WaitingManager.hide();
        }


    }
}




