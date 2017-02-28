package ru.rbt.barsgl.gwt.client.events.ae;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.client.quickFilter.ErrorQFilterAction;
import ru.rbt.barsgl.gwt.client.quickFilter.QuickFilterAction;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.user.AppUserWrapper;

import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.client.security.AuthWherePart.getSourcePart;

/**
 * Created by akichigi on 14.02.17.
 */
public class LoadErrorHandlingForm  extends GridForm {
    public static final String FORM_NAME = "Обработка ошибок";
    protected Column colProcDate;
    protected Column colDealSource;

    private Column colCorrect;

    public LoadErrorHandlingForm() {
        super(FORM_NAME, true);
        reconfigure();
    }

    private void reconfigure() {
        GridAction quickFilterAction;
        abw.addAction(quickFilterAction = new ErrorQFilterAction(grid, colDealSource, colProcDate));
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        GridAction action = manualCorrection();
        AppUserWrapper current_user = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        if (current_user != null){
            String permit = current_user.getErrorListProcPermit();
            if (permit != null && permit.equals("Y")) action = manualCorrectionList();
        }
        abw.addAction(action); //TODO SecureAction
        abw.addAction(processCorrection()); //TODO SecureAction
        //Init quick filter with additional parameter
        ArrayList<FilterItem> list = new ArrayList<FilterItem>();
        FilterItem item = new FilterItem(colCorrect, FilterCriteria.EQ, "N", true);
        list.add(item);
        ((QuickFilterAction)quickFilterAction).setInitFilterItems(list);

        quickFilterAction.execute();
    }

    private GridAction executeAction(final DlgFrame dlg){

        return new GridAction(grid, "", "", null, 0) {

            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                dlg.setDlgEvents(this);

                Object[] data = {(String) grid.getFieldValue("ID_PST"), grid.getVisibleItems()};
                dlg.show(data);
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception{
                System.out.println(prms.toString());
                dlg.hide(); //???
            }
        };
    }


   private GridAction manualCorrection(){
       return new GridAction(grid, null, "Отметить как исправленное", new Image(ImageConstants.INSTANCE.ok()), 10, true) {
           DlgFrame errorCorrectionDlg = null;

           @Override
           public void execute() {
               executeAction(errorCorrectionDlg == null ? errorCorrectionDlg = new ErrorCorrectionDlg()
                                                        : errorCorrectionDlg).execute();
           }
       };
   }


    private GridAction manualCorrectionList(){
        final PopupPanel sidePanel = new PopupPanel(true, true);

        MenuItem itemCurrent = new MenuItem("Текущее сообщение", new Command() {
            DlgFrame errorCorrectionDlg = null;

            @Override
            public void execute() {
                sidePanel.hide();
                executeAction(errorCorrectionDlg == null ? errorCorrectionDlg = new ErrorCorrectionDlg()
                                                         : errorCorrectionDlg).execute();
            }
        });

        MenuItem itemFiltered = new MenuItem("Сообщение по фильтру", new Command() {
            DlgFrame errorCorrectionFilteredDlg = null;

            @Override
            public void execute() {
                sidePanel.hide();
                executeAction(errorCorrectionFilteredDlg == null ? errorCorrectionFilteredDlg = new ErrorFilteredDlg(ErrorFilteredDlg.Mode.CORRECTION)
                                                       : errorCorrectionFilteredDlg).execute();
            }
        });

        MenuBar bar = new MenuBar(true);
        bar.addItem(itemCurrent);
        bar.addSeparator();
        bar.addItem(itemFiltered);

        sidePanel.setWidget(bar);

        return new GridAction(grid, null, "Отметить как исправленное", new Image(ImageConstants.INSTANCE.ok()), 10, true) {

            @Override
            public void execute() {
                final PushButton button = abw.getButton(this);
                sidePanel.setPopupPositionAndShow(new PopupPanel.PositionCallback() {

                    @Override
                    public void setPosition(int i, int i1) {
                        sidePanel.setPopupPosition(button.getAbsoluteLeft(), button.getAbsoluteTop() + button.getOffsetHeight());
                    }
                });
            }
        };
    }

    private GridAction processCorrection(){
        final PopupPanel sidePanel = new PopupPanel(true, true);
        MenuItem itemCurrent = new MenuItem("Текущее сообщение", new Command() {
            DlgFrame errorProcessingDlg = null;

            @Override
            public void execute() {
                sidePanel.hide();
                executeAction(errorProcessingDlg == null ? errorProcessingDlg = new ErrorProcessingDlg()
                                                         : errorProcessingDlg).execute();
            }
        });

        MenuItem itemFiltered = new MenuItem("Сообщение по фильтру", new Command() {
            DlgFrame errorProcessingFilteredDlg = null;

            @Override
            public void execute() {
                sidePanel.hide();
                executeAction(errorProcessingFilteredDlg == null ? errorProcessingFilteredDlg = new ErrorFilteredDlg(ErrorFilteredDlg.Mode.PROCESSING)
                                                       : errorProcessingFilteredDlg).execute();
            }
        });

        MenuBar bar = new MenuBar(true);
        bar.addItem(itemCurrent);
        bar.addSeparator();
        bar.addItem(itemFiltered);

        sidePanel.setWidget(bar);

        return new GridAction(grid, null, "Переобработать", new Image(ImageConstants.INSTANCE.process()), 10, true) {
            @Override
            public void execute() {
                final PushButton button = abw.getButton(this);
                sidePanel.setPopupPositionAndShow(new PopupPanel.PositionCallback() {

                    @Override
                    public void setPosition(int i, int i1) {
                        sidePanel.setPopupPosition(button.getAbsoluteLeft(), button.getAbsoluteTop() + button.getOffsetHeight());
                    }
                });
            }
        };
    }

    @Override
    protected String prepareSql() {
        return "select * from V_GL_ERRPROC " + getSourcePart("where", "SRC_PST");
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();
        result.addColumn(new Column("ID_PKG", Column.Type.LONG, "ID пакета", 60, false, false));
        result.addColumn(new Column("PKG_STATE", Column.Type.STRING, "Статус пакета", 80, false, false));
        result.addColumn(new Column("DT_LOAD", Column.Type.DATETIME, "Время загрузки", 130, false, false));
        result.addColumn(new Column("CR_DT", Column.Type.DATETIME, "Время обработки", 130));
        result.addColumn(new Column("LWD_STATUS", Column.Type.STRING, "Баланс пред.дня", 80, false, false));
        result.addColumn(new Column("PST_REF", Column.Type.LONG, "ID сообщения АЕ", 70, false, false));
        result.addColumn(new Column("GLOID", Column.Type.LONG, "ID операции", 70, false, false));
        result.addColumn(new Column("INP_METHOD", Column.Type.STRING, "Способ ввода", 50));
        result.addColumn(new Column("STATE", Column.Type.STRING, "Статус", 70));
        result.addColumn(new Column("ID_PST", Column.Type.STRING, "ИД сообщ АЕ", 80));
        result.addColumn(colDealSource = new Column("SRC_PST", Column.Type.STRING, "Источник сделки", 70));
        result.addColumn(new Column("EVTP", Column.Type.STRING, "Тип события", 80, false, false));
        result.addColumn(colProcDate = new Column("PROCDATE", Column.Type.DATE, "Дата опердня", 80));
        result.addColumn(new Column("VDATE", Column.Type.DATE, "Дата валютирования", 80));
        result.addColumn(new Column("POSTDATE", Column.Type.DATE, "Дата проводки", 80));
        result.addColumn(new Column("DEAL_ID", Column.Type.STRING, "ИД сделки", 120));
        result.addColumn(new Column("SUBDEALID", Column.Type.STRING, "ИД субсделки", 120));
        result.addColumn(new Column("PMT_REF", Column.Type.STRING, "ИД платежа", 120));
        result.addColumn(new Column("AC_DR", Column.Type.STRING, "Счет ДБ", 160));
        result.addColumn(new Column("CCY_DR", Column.Type.STRING, "Валюта ДБ", 60));
        result.addColumn(new Column("AMT_DR", Column.Type.DECIMAL, "Сумма ДБ", 100));
        result.addColumn(new Column("AMTRU_DR", Column.Type.DECIMAL, "Сумма в рублях ДБ", 120, false, false));
        result.addColumn(new Column("ACCKEY_DR", Column.Type.STRING, "Ключи счета ДБ", 400));
        result.addColumn(new Column("AC_CR", Column.Type.STRING, "Счет КР", 160));
        result.addColumn(new Column("CCY_CR", Column.Type.STRING, "Валюта КР", 60));
        result.addColumn(new Column("AMT_CR", Column.Type.DECIMAL, "Сумма КР", 100));
        result.addColumn(new Column("AMTRU_CR", Column.Type.DECIMAL, "Сумма в рублях КР", 120, false, false));
        result.addColumn(new Column("ACCKEY_CR", Column.Type.STRING, "Ключи счета КР", 400));
        result.addColumn(new Column("NRT", Column.Type.STRING, "Назначение анг.", 175, false, false));
        result.addColumn(new Column("RNRTL", Column.Type.STRING, "Назначение рус.", 175));
        result.addColumn(new Column("STRN", Column.Type.STRING, "Сторно", 40));
        result.addColumn(new Column("STRNRF", Column.Type.STRING, "Сторно операция", 80, false, false));
        result.addColumn(new Column("FAN", Column.Type.STRING, "Веер", 40));
        result.addColumn(new Column("PAR_RF", Column.Type.STRING, "Голова веера", 80, false, false));
        result.addColumn(new Column("ERR_CODE", Column.Type.STRING, "Код ошибки", 100));
        result.addColumn(new Column("ERR_TYPE", Column.Type.STRING, "Тип ошибки", 500));
        result.addColumn(new Column("ERR_MSG", Column.Type.STRING, "Описание ошибки", 1200));
        result.addColumn(new Column("OTS_ERR", Column.Type.DATETIME, "Время возник.ошибки", 130, false, false));
        result.addColumn(new Column("USER_NAME", Column.Type.STRING, "Исполнитель", 80, false, false));
        result.addColumn(new Column("OTS_PROC", Column.Type.DATETIME, "Время исправления", 130, false, false));
        result.addColumn(colCorrect = new Column("CORRECT", Column.Type.STRING, "Исправлено", 80));
        result.addColumn(new Column("ID_PST_NEW", Column.Type.STRING, "ИД исправ. сообщ АЕ", 100, false, false));
        result.addColumn(new Column("COMMENT", Column.Type.STRING, "Комментарий", 300, false, false));

        return result;
    }

    @Override
    protected ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("CR_DT", Column.Sort.DESC));
        return list;
    }
}
