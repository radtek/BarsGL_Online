package ru.rbt.barsgl.gwt.client.audit;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.security.gwt.client.CommonEntryPoint;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.Utils;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by akichigi on 29.04.15.
 */
public class AuditFormDlg extends DlgFrame {
   private Row row;
   private boolean isGridMeasured = false;
   private int gridHeight;
    private int gridWidth;
   private boolean errorPageLoaded = false;
   private boolean attachmentPageLoaded = false;
     
   private AreaBox srcBox;
   private AreaBox errorBox;
   private AreaBox addInfo;
   private Grid grid;
   private Label id;
   private Label logTime;
   private Label userName;
   private Label userHost;
   private Label logCode;
   private Label logLevel;
   private AreaBox info;
   private TxtBox entity_id;
   private TxtBox entity_type;
   private TxtBox trans_id;
   private TxtBox source;
//   private Label process_time_ms;

   private final String sql = "select {0} from GL_AUDIT where ID_RECORD = ?";

   public AuditFormDlg(){
       super();
       setCaption("Свойства");
       ok.setVisible(false);       
   }

    @Override
    protected void fillContent() {
        row = (Row) params;

        // 0 ID_RECORD, 1 SYS_TIME, 2 LOG_CODE, 3 LOG_LEVEL, 4 MESSAGE, 5 ENTITYTYPE, 6 ENTITY_ID,
        // 7 SRC, 8 ERRORSRC, 9 ERRORMSG, 10 TRANSACTID, 11 USER_NAME, 12 USER_HOST,  13 PROCTIMEMS

        id.setText(row.getField(0).getValue().toString());       
        logTime.setText(DateTimeFormat.getFormat("dd.MM.yyyy hh:mm:ss").format((Date) row.getField(1).getValue()));
        logCode.setText((String) row.getField(2).getValue());
        logLevel.setText((String) row.getField(3).getValue());
        info.setValue((String) row.getField(4).getValue());
        entity_type.setValue((String)row.getField(5).getValue());
        entity_id.setValue((String)row.getField(6).getValue());
        source.setValue((String) row.getField(7).getValue());
        srcBox.setValue((String)row.getField(8).getValue());
        trans_id.setValue((String) row.getField(10).getValue());
        userName.setText((String) row.getField(11).getValue());
        userHost.setText((String) row.getField(12).getValue());
//        Integer val = (Integer)row.getField(13).getValue();
//        process_time_ms.setText( val == null ? "" : val.toString());

    }

    @Override
    public Widget createContent() {
        SimplePanel panel = new SimplePanel();
        panel.add(createTabPanel());

        return panel;
    }

    private enum pagesEnum{ERROR_PAGE, ATTACHMENT_PAGE}

    private TabPanel createTabPanel(){
       TabPanel tab = new TabPanel();
          
       tab.add(createPageMain(), "Основное");
       tab.add(createPageErrorSource(), "Информации об ошибке");
       tab.add(createPageErrorMessage(), "Источник ошибки");
       tab.add(createPageAdditional(), "Расширенная информация");
 
       tab.selectTab(0);

       tab.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {
           @Override
           public void onBeforeSelection(BeforeSelectionEvent<Integer> event) {
               if (!isGridMeasured) {
                   gridHeight = grid.getOffsetHeight();
                   gridWidth = grid.getOffsetWidth();
                   isGridMeasured = true;
               }
               switch (event.getItem()) {
                   case 1:
                       errorBox.setSize(gridWidth + "px", gridHeight + "px");
                       if (!errorPageLoaded) {
                          select(pagesEnum.ERROR_PAGE);
                       }
                       break;
                   case 2:
                       srcBox.setSize(gridWidth + "px", gridHeight + "px");
                       break;
                   case 3:
                       addInfo.setSize(gridWidth + "px", gridHeight + "px");
                       if (!attachmentPageLoaded) {
                           select(pagesEnum.ATTACHMENT_PAGE);
                       }
                       break;
               }
           }
       });

       return tab;
    }

    private Widget createPageMain(){
        grid = new Grid(11, 2);
        grid.setText(0, 0, "ИД записи");
        grid.setWidget(0, 1, id = new Label() );

        grid.setText(1, 0, "Время записи");
        grid.setWidget(1, 1, logTime = new Label());
        
        grid.setText(2, 0, "Пользователь");
        grid.setWidget(2, 1, userName = new Label());
        
        grid.setText(3, 0, "Компьютер");
        grid.setWidget(3, 1, userHost = new Label());
        
        grid.setText(4, 0, "Код");
        grid.setWidget(4, 1, logCode = new Label());
        
        grid.setText(5, 0, "Уровень");
        grid.setWidget(5, 1, logLevel = new Label());
        
        grid.setText(6, 0, "Информация");
        grid.setWidget(6, 1, info = new AreaBox());
        info.setWidth("100%");
        info.setTextWrap(false);
        info.setReadOnly(true);
        
        grid.setText(7, 0, "ИД сущности");
        grid.setWidget(7, 1, entity_id = new TxtBox());
        entity_id.setReadOnly(true);
        entity_id.setWidth("100%");
        
        grid.setText(8, 0, "Тип сущности");
        grid.setWidget(8, 1, entity_type = new TxtBox());
        entity_type.setReadOnly(true);
        entity_type.setWidth("100%");

        grid.setText(9, 0, "ИД транзакции");
        grid.setWidget(9, 1, trans_id = new TxtBox());
        trans_id.setReadOnly(true);
        trans_id.setWidth("100%");

        grid.setText(10, 0, "Источник");
        grid.setWidget(10, 1, source = new TxtBox());
        source.setReadOnly(true);
        source .setWidth("100%");

//        grid.setText(10, 0, "Длительность процесса");
//        grid.setWidget(10, 1, process_time_ms = new Label());

        CellFormatter fmt = grid.getCellFormatter(); 
        fmt.setWidth(0, 0, "120px");
        fmt.setWidth(0, 1, "600px");
        
        for(int i = 0; i < grid.getRowCount(); i++){
        	fmt.getElement(i, 0).getStyle().setFontWeight(Style.FontWeight.BOLD);
        }
               
        
        return grid;
    }

    @Override
    protected boolean onClickOK() throws Exception {
        return true;
    }

    private Widget createPageErrorMessage(){
        srcBox = new AreaBox();
        srcBox.setTextWrap(false);
        srcBox.setReadOnly(true);

        return srcBox;
    }
    
    private Widget createPageErrorSource(){
        errorBox = new AreaBox();
        errorBox.setTextWrap(true);
        errorBox.setReadOnly(true);

        return errorBox;
    }

    private Widget createPageAdditional(){
        addInfo = new AreaBox();
        addInfo.setTextWrap(false);
        addInfo.setReadOnly(true);
        return addInfo;
    }

    private void select(final pagesEnum pages){
        WaitingManager.show("Запрос данных...");
        final AreaBox box;
        String colName;
        if (pages == pagesEnum.ERROR_PAGE){
            box = errorBox;
            colName = "ERRORMSG";
        } else {
            box = addInfo;
            colName = "STCK_TRACE";
        }


        String query = Utils.Fmt(sql, colName);

        CommonEntryPoint.asyncGridService.selectOne(query, new Serializable[]{row.getField(0).getValue()}, new AuthCheckAsyncCallback<Row>() {
            @Override
            public void onFailureOthers(Throwable throwable) {
                WaitingManager.hide();

                Window.alert("Операция не удалась.\n Ошибка: " + throwable.getLocalizedMessage());

                WaitingManager.hide();
            }

            @Override
            public void onSuccess(Row row) {
                box.setValue((String) row.getField(0).getValue());
                if (pages == pagesEnum.ERROR_PAGE) {
                    errorPageLoaded = true;
                }else {
                    attachmentPageLoaded = true;
                }
                WaitingManager.hide();
            }
        });
    }
}
