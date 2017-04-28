package ru.rbt.barsgl.gwt.client.monitoring;

import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.AbstractHeaderOrFooterBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.forms.BaseForm;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.monitoring.MonitoringWrapper;
import ru.rbt.barsgl.shared.monitoring.OperTableItem;
import ru.rbt.barsgl.shared.monitoring.ReplTableItem;
import ru.rbt.barsgl.shared.monitoring.ReplTableItem2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by akichigi on 05.12.16.
 */
public class Monitor extends BaseForm {
    public static final String FORM_NAME = "Мониторинг";
//    private final String quest_str = "?";
    private final String backColor = "#cfcfcf";
    private Label pd_Total;
    private Label pd_Wait;
    private Label pd_Moved;
    private Label blt_Total;
    private Label blt_Wait;
    private Label blt_Moved;
    private Label pdSpeed;
    private Label pdRest;
    private Label bltSpeed;
    private Label bltRest;
    private DataGrid<ReplTableItem2> dg;

//    private Label repl_Total;
//    private Label repl_Wait;
//    private Label repl_Done;
//    private Label repl_Error;
//    private Label repl_Unknown;
//    private ScrollPanel repl_table_section;
    private ScrollPanel oper_table_section;
    private HPanel hBuffer, hRepl, hOper;

    public Monitor(){
        super();
        title.setText(FORM_NAME);
    }

    @Override
    public Widget createContent() {
        ScrollPanel mainSPan = new ScrollPanel();
        mainSPan.setWidth("100%");
        mainSPan.setHeight("100%");

        VerticalPanel mainVPan = new VerticalPanel();
        HorizontalPanel mainHPan = new HorizontalPanel();
        mainHPan.setSpacing(4);
        mainHPan.add(makeBufferPan());
        mainHPan.add(makeOperPan());
        HorizontalPanel mainHPan2 = new HorizontalPanel();
        mainHPan2.setSpacing(4);
        mainHPan2.add(makeReplPan());

        mainVPan.add(mainHPan);
        mainVPan.add(mainHPan2);

        mainSPan.add(mainVPan);
//        return mainSPan;
        DockLayoutPanel panel = new DockLayoutPanel(Style.Unit.MM);
//        panel.addNorth(hp, 10);
        panel.add(mainSPan);

        //getBufferInfo();
        return panel;
    }

    private HPanel makeReplPan(){
        hRepl = new HPanel(616, 430){
            public void handlerBody(ClickEvent event) {
                hRepl.setButVisible(false);
                getRepls();
            }
        };
        hRepl.addHeader();
        hRepl.setButVisible(false);
        setReplHeader("?", "?");
        dg = new DataGrid<ReplTableItem2>();
        dg.setWidth("616px");
        dg.setHeight("400px");
        dg.setAutoHeaderRefreshDisabled(true);
//      dg.setEmptyTableWidget(new Label("нет данных"));
        initTableCols(dg);
        dg.setFooterBuilder(new CustomFooterBuilder());
        VerticalPanel mainHPan3 = new VerticalPanel();
        mainHPan3.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        mainHPan3.add(dg);
        getRepls();
        hRepl.setBody(mainHPan3);
        return  hRepl;
    }

    private void setReplHeader(String dateRefresh, String interval){
        hRepl.setHeader("Репликация ("+ dateRefresh + " - " + interval + ")");
    }

    private void initTableCols(DataGrid tb){
        Column<ReplTableItem2, String> colName = new Column<ReplTableItem2, String>(
                new TextCell()) {
            @Override
            public String getValue(ReplTableItem2 object) {
                return object.getName();
            }
        };
        Column<ReplTableItem2, Number> colWait = new Column<ReplTableItem2, Number>( new NumberCell()) {
            @Override
            public Number getValue(ReplTableItem2 object) {
                return object.getCntWait();
            }
        };
        colWait.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        Column<ReplTableItem2, Number> colProc = new Column<ReplTableItem2, Number>( new NumberCell()) {
            @Override
            public Number getValue(ReplTableItem2 object) {
                return object.getCntProc();
            }
        };
        colProc.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        Column<ReplTableItem2, Number> colErr = new Column<ReplTableItem2, Number>( new NumberCell()) {
            @Override
            public Number getValue(ReplTableItem2 object) {
                return object.getCntErr();
            }
        };
        colErr.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        Column<ReplTableItem2, String> colSpeed = new Column<ReplTableItem2, String>( new TextCell()) {
            @Override
            public String getValue(ReplTableItem2 object) {
                return object.getStrSpeed();
            }
        };
        colSpeed.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        Column<ReplTableItem2, String> colRest = new Column<ReplTableItem2, String>( new TextCell()) {
            @Override
            public String getValue(ReplTableItem2 object) {
                return object.getStrRestTime();
            }
        };
        colRest.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        tb.addColumn(colName, "Таблица");
        tb.addColumn(colWait, "Ожидание");
        tb.addColumn(colProc, "Обработанно");
        tb.addColumn(colErr, "Ошибки");
        tb.addColumn(colSpeed, "Скорость(в сек))");
        tb.addColumn(colRest, "Осталось");
    }

    private void getRepls() {
        try{
            BarsGLEntryPoint.monitorService.getRepl(new AuthCheckAsyncCallback<RpcRes_Base<HashMap>>() {
                @Override
                public void onSuccess(RpcRes_Base<HashMap> result) {
                    if (((RpcRes_Base)result).isError()){
                        dg.setEmptyTableWidget(new Label("нет данных"));
                        setReplHeader("?","?");
                    }else{
                        setReplHeader(((HashMap)((RpcRes_Base)result).getResult()).get("strNewDate").toString(), ((HashMap)((RpcRes_Base)result).getResult()).get("strInterval").toString());
                        dg.setRowData( (ArrayList<ReplTableItem2>) ((HashMap)((RpcRes_Base)result).getResult()).get("repl") );
                    }
                    hRepl.setButVisible(true);
                }
                @Override
                public final void onFailureOthers(Throwable throwable) {
                    hRepl.setButVisible(true);
                }

            });
        }catch(Exception e){
            DialogManager.error("Ошибка", "Операция получения данных по репликациям не удалась.\nОшибка: " + e.getMessage());
        }
    }

    private HPanel makeOperPan() {
        hOper = new HPanel(260, 158){
            public void handlerBody(ClickEvent event){
                hOper.setButVisible(false);
                getOpers();
            }
        };
        hOper.addHeader();
        hOper.setButVisible(false);
        hOper.setHeader("Операции");

        VerticalPanel vp = new VerticalPanel();
        vp.setWidth("250px");
        vp.getElement().getStyle().setMargin( 5, Style.Unit.PX);

        FlexTable ft = new FlexTable();

        ft.setWidget(0, 0, new Label("Состояние"));
        ft.setWidget(0, 1, new Label("Веер"));
        ft.setWidget(0, 2, new Label("Количество"));
        ft.setCellSpacing(0);
        ft.setCellPadding(0);

        FlexTable.FlexCellFormatter fmt = ft.getFlexCellFormatter();

        fmt.getElement(0, 0).getStyle().setWidth(50, Style.Unit.PX);
        fmt.getElement(0, 1).getStyle().setWidth(20, Style.Unit.PX);
        fmt.getElement(0, 2).getStyle().setWidth(30, Style.Unit.PX);

        for (int i = 0; i < 3; i++){
            fmt.getElement(0, i).getStyle().setBackgroundColor(backColor);
            fmt.setHorizontalAlignment(0, i, HasHorizontalAlignment.ALIGN_CENTER);
            fmt.getElement(0, i).getStyle().setProperty("borderBottom", "1px solid #003366");
        }

        ft.setWidth("100%");

        oper_table_section = new ScrollPanel();

        oper_table_section.setWidth("100%");
        oper_table_section.setHeight("100px");
        oper_table_section.setAlwaysShowScrollBars(true);
        oper_table_section.getElement().getStyle().setOverflowX(Style.Overflow.HIDDEN);
        vp.getElement().getStyle().setProperty("border", "1px solid black");
        vp.add(ft);
        vp.add(oper_table_section);
        hOper.setBody(vp);
        getOpers();
        return hOper;
    }

    private void getOpers(){
        try{
        BarsGLEntryPoint.monitorService.getOper(new AuthCheckAsyncCallback<RpcRes_Base<ArrayList>>() {
            @Override
            public void onSuccess(RpcRes_Base<ArrayList> result) {
                oper_table_section.clear();
                if (((RpcRes_Base)result).isError()){
                    oper_table_section.add(null);
//                    DialogManager.error("Ошибка", "Операция получения данных по операциям не удалась.\nОшибка: " + ((RpcRes_Base)result).getMessage());
                }else{
                    oper_table_section.add(createOperTable( (List<OperTableItem>) ((RpcRes_Base)result).getResult() ));
                }
                hOper.setButVisible(true);
            }
            @Override
            public final void onFailureOthers(Throwable throwable) {
                hOper.setButVisible(true);
            }
        });
    }catch(Exception e){
        DialogManager.error("Ошибка", "Операция получения данных по операциям не удалась.\nОшибка: " + e.getMessage());
    }

}

    private FlexTable createOperTable(List<OperTableItem> list) {

        FlexTable ft = new FlexTable();
        ft.setWidth("100%");
        FlexTable.FlexCellFormatter fmt = ft.getFlexCellFormatter();
        if (list == null || list.size() == 0){
            ft.setText(0, 0, "Нет данных");
            fmt.setColSpan(0, 0, 4);
            fmt.setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_CENTER);
        } else{
            int c = 0;
            for (OperTableItem item: list){
                ft.setText(c, 0, item.getState());
                ft.setText(c, 1, item.getFun());
                ft.setText(c, 2, item.getCount().toString());

                fmt.setHorizontalAlignment(c, 1, HasHorizontalAlignment.ALIGN_LEFT);
                fmt.setHorizontalAlignment(c, 2, HasHorizontalAlignment.ALIGN_RIGHT);
                fmt.getElement(c, 0).getStyle().setPaddingLeft(2, Style.Unit.PX);
                fmt.getElement(c, 2).getStyle().setPaddingRight(2, Style.Unit.PX);
                c++;
            };
            fmt.getElement(0, 0).getStyle().setWidth(50, Style.Unit.PX);
            fmt.getElement(0, 1).getStyle().setWidth(20, Style.Unit.PX);
            fmt.getElement(0, 2).getStyle().setWidth(30, Style.Unit.PX);
        }
        ft.setCellSpacing(0);
        ft.setCellPadding(0);

        return ft;
    }

    private HPanel makeBufferPan(){
        hBuffer = new HPanel(350, 158){
            public void handlerBody(ClickEvent event) {
                hBuffer.setButVisible(false);
                getBuffs();
            }
        };
        hBuffer.addHeader();
        hBuffer.setButVisible(false);
        setBuffHeader( "?","?");

        FlexTable ft = new FlexTable();
        ft.setWidth("340px");

        ft.setWidget(0, 0, getHeaderLb("Тип"));
        ft.setWidget(0, 1, getHeaderLb("Проводки"));
        ft.setWidget(0, 2, getHeaderLb("Остатки"));

        ft.setWidget(1, 0, new Label("Всего:"));
        ft.setWidget(2, 0, new Label("Перенесено:"));
        ft.setWidget(3, 0, new Label("Осталось:"));
        ft.setWidget(4, 0, new Label("Скорость:"));
        ft.setWidget(5, 0, new Label("Ост.время:"));

        ft.setWidget(1, 1, pd_Total = new Label());
        ft.setWidget(2, 1, pd_Wait = new Label());
        ft.setWidget(3, 1, pd_Moved = new Label());
        ft.setWidget(1, 2, blt_Total = new Label());
        ft.setWidget(2, 2, blt_Wait = new Label());
        ft.setWidget(3, 2, blt_Moved = new Label());

        ft.setWidget(4, 1, pdSpeed = new Label());
        ft.setWidget(5, 1, pdRest = new Label());
        ft.setWidget(4, 2, bltSpeed = new Label());
        ft.setWidget(5, 2, bltRest = new Label());

        FlexTable.FlexCellFormatter fmt = ft.getFlexCellFormatter();

        for (int i = 1; i < 6; i++){
            fmt.setWidth( i, 0, "30%");
            fmt.setWidth( i, 1, "35%");
            fmt.setWidth( i, 2, "35%");
            fmt.setHorizontalAlignment(i, 1, HasHorizontalAlignment.ALIGN_RIGHT);
            fmt.setHorizontalAlignment(i, 2, HasHorizontalAlignment.ALIGN_RIGHT);
            fmt.getElement(i, 0).getStyle().setPaddingLeft(2, Style.Unit.PX);
            fmt.getElement(i, 1).getStyle().setPaddingRight(2, Style.Unit.PX);
            fmt.getElement(i, 2).getStyle().setPaddingRight(2, Style.Unit.PX);
        }
        ft.setBorderWidth(1);
        ft.setCellSpacing(0);
        ft.setCellPadding(0);
        ft.getElement().getStyle().setMargin( 5, Style.Unit.PX);
        hBuffer.setBody(ft);

        getBuffs();
        return hBuffer;
    }

    private void setBuffHeader(String dateRefresh, String interval){
        hBuffer.setHeader("Буфер ("+dateRefresh+" - " + interval + ")");
    }
    private Label getHeaderLb(String text){
        Label lb = new Label(text);
        lb.setWidth("100%");
        lb.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        lb.getElement().getStyle().setBackgroundColor(backColor);
        return lb;
    }

    private void getBuffs(){
      try {
          BarsGLEntryPoint.monitorService.getBuff(new AuthCheckAsyncCallback<RpcRes_Base<HashMap>>() {
              @Override
              public void onSuccess(RpcRes_Base<HashMap> result) {
                  if (((RpcRes_Base) result).isError()) {
//                    rootLogger.log(Level.INFO,  "Операция получения данных по буфферу не удалась.\nОшибка: " + ((RpcRes_Base)result).getMessage());
                      setBuffHeader( "?", "?");
//                    DialogManager.error("Ошибка", "Операция получения данных по операциям не удалась.\nОшибка: " + ((RpcRes_Base)result).getMessage());
                  } else {
//                    rootLogger.log(Level.INFO, "fillBuff");
                      fillBuff((HashMap) ((RpcRes_Base) result).getResult());
//                    rootLogger.log(Level.INFO, "fillBuff2");
                  }
                  hBuffer.setButVisible(true);
              }

              @Override
              public final void onFailureOthers(Throwable throwable) {
                  hBuffer.setButVisible(true);
              }
          });
      }catch(Exception e){
            DialogManager.error("Ошибка", "Операция получения данных по буферу не удалась.\nОшибка: " + e.getMessage());
        }
    }
    private void fillBuff(HashMap<String,String> m){
        if (m != null) {
            setBuffHeader(m.get("strNewDate").toString(), m.get("strInterval").toString());
            pd_Total.setText(m.get("pd_Total").toString());
            pd_Wait.setText(m.get("pd_Wait").toString());
            pd_Moved.setText(m.get("pd_Moved").toString());
            blt_Total.setText(m.get("blt_Total").toString());
            blt_Wait.setText(m.get("blt_Wait").toString());
            blt_Moved.setText(m.get("blt_Moved").toString());
            pdSpeed.setText(m.get("strPdSpeed").toString());
            pdRest.setText(m.get("strPdRest").toString());
            bltSpeed.setText(m.get("strBltSpeed").toString());
            bltRest.setText(m.get("strBltRest").toString());
        }else{
            setBuffHeader("?","?");
            pd_Total.setText("?");
            pd_Wait.setText("?");
            pd_Moved.setText("?");
            blt_Total.setText("?");
            blt_Wait.setText("?");
            blt_Moved.setText("?");
            pdSpeed.setText("?");
            pdRest.setText("?");
            bltSpeed.setText("?");
            bltRest.setText("?");
        }
    }

    private class CustomFooterBuilder extends AbstractHeaderOrFooterBuilder<ReplTableItem2> {

        public CustomFooterBuilder() {
            super(dg, true);
        }

        @Override
        protected boolean buildHeaderOrFooterImpl() {
            String footerStyle = dg.getResources().style().footer();

            int totalWait = 0;
            int totalProc = 0;
            int totalErr = 0;
            List<ReplTableItem2> items = dg.getVisibleItems();
            if (items.size() > 0) {
                for (ReplTableItem2 item : items) {
                    totalWait += item.getCntWait();
                    totalProc += item.getCntProc();
                    totalErr += item.getCntErr();
                }
            }

            // Cells before age column.
            TableRowBuilder tr = startRow();
            tr.startTH().colSpan(6).className(footerStyle).endTH();
            tr.endTR();

            TableRowBuilder tr1 = startRow();

            TableCellBuilder th0 =
                    tr1.startTH().align(HasHorizontalAlignment.ALIGN_RIGHT.getTextAlignString());
            th0.text("Итого:");
            th0.endTH();

            TableCellBuilder th =
                    tr1.startTH().align(HasHorizontalAlignment.ALIGN_RIGHT.getTextAlignString());
            th.text(String.valueOf(totalWait));
            th.endTH();

            TableCellBuilder th2 =
                    tr1.startTH().align(HasHorizontalAlignment.ALIGN_RIGHT.getTextAlignString());
            th2.text(String.valueOf(totalProc));
            th2.endTH();

            TableCellBuilder th3 =
                    tr1.startTH().align(HasHorizontalAlignment.ALIGN_RIGHT.getTextAlignString());
            th3.text(String.valueOf(totalErr));
            th3.endTH();
            tr1.endTR();

            return true;
        }
    }


//    private void getBufferInfo(){
//        try{
//            WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
//            BarsGLEntryPoint.monitorService.getInfo(new AuthCheckAsyncCallback<RpcRes_Base<MonitoringWrapper>>() {
//                @Override
//                public void onSuccess(RpcRes_Base<MonitoringWrapper> result) {
//                    if (result.isError()){
//                        DialogManager.error("Ошибка", "Операция получения данных по буферу не удалась.\nОшибка: " + result.getMessage());
//                    }else{
//                        MonitoringWrapper wrapper = result.getResult();
//                        refreshData(wrapper);
//                    }
//                    WaitingManager.hide();
//                }
//            });
//        }catch (Exception e){
//            DialogManager.error("Ошибка", "Операция получения данных по буферу не удалась.\nОшибка: " + e.getMessage());
//        }
//    }

//    @Override
//    public Widget createContent() {
//        HorizontalPanel hp = new HorizontalPanel();
//        hp.add(createRefreshButton());
//        hp.setWidth("100%");
//        hp.getElement().getStyle().setProperty("borderBottom", "1px solid #ccc");
//
//        VerticalPanel vp = new VerticalPanel();
//        vp.add(createBufferSection());
//        vp.add(createReplSection());
//        vp.add(createOperSection());
//
//        ScrollPanel scrollPanel = new ScrollPanel();
//        scrollPanel.setWidth("100%");
//        scrollPanel.setHeight("100%");
//
//        scrollPanel.add(vp);
//
//        DockLayoutPanel panel = new DockLayoutPanel(Style.Unit.MM);
//        panel.addNorth(hp, 10);
//        panel.add(scrollPanel);
//
//        //getBufferInfo();
//        return panel;
//    }
//
//    private Widget createPdSection(){
//        return createSection("Проводки", new Label[] {pd_Total = new Label(quest_str),
//                pd_Moved = new Label(quest_str), pd_Wait = new Label(quest_str)});
//    }
//
//    private Widget createBalturSection(){
//        return createSection("Остатки", new Label[] {blt_Total = new Label(quest_str),
//                blt_Moved = new Label(quest_str), blt_Wait = new Label(quest_str)});
//    }
//
//    private Widget createSection(String caption, Label[] label){
//        VerticalPanel vp = new VerticalPanel();
//        vp.setWidth("160px");
//        vp.setBorderWidth(1);
//        Label lb = new Label(caption);
//
//        FlexTable ft = new FlexTable();
//        ft.setWidth("100%");
//
//        Label t;
//        ft.setWidget(0, 0, t = new Label("Всего:"));
//        t.getElement().getStyle().setBackgroundColor("#fbec5d");
//        t.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);
//        ft.setWidget(1, 0, new Label("Перенесено:"));
//        ft.setWidget(2, 0, new Label("Осталось:"));
//
//        ft.setWidget(0, 1, label[0]);
//        ft.setWidget(1, 1, label[1]);
//        ft.setWidget(2, 1, label[2]);
//
//       FlexTable.FlexCellFormatter fmt = ft.getFlexCellFormatter();
//        for (int i = 0; i < 3; i++){
//            fmt.setHorizontalAlignment(i, 1, HasHorizontalAlignment.ALIGN_RIGHT);
//            fmt.getElement(i, 0).getStyle().setWidth(50, Style.Unit.PX);
//        }
//
//        vp.add(lb);
//        vp.add(ft);
//
//        lb.setWidth("100%");
//        lb.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
//        lb.getElement().getStyle().setBackgroundColor("blue");
//        lb.getElement().getStyle().setColor("white");
//        lb.getElement().getStyle().setProperty("borderBottom", "1px solid #003366");
//
//        return vp;
//    }
//
//    private Widget createBufferSection(){
//        CaptionPanel cp = new CaptionPanel();
//        cp.setWidth("325px");
//        cp.setCaptionHTML("<b>Буфер</b>");
//        cp.getElement().getStyle().setPadding(3, Style.Unit.PX);
//        cp.getElement().getStyle().setBorderWidth(2, Style.Unit.PX);
//
//        HorizontalPanel hp = new HorizontalPanel();
//        Widget w;
//        hp.add(w = createPdSection());
//        w.getElement().getStyle().setMarginRight(3, Style.Unit.PX);
//
//        hp.add(createBalturSection());
//
//
//        cp.add(hp);
//        return cp;
//    }
//
//    private Widget createReplSection(){
//        CaptionPanel cp = new CaptionPanel();
//        cp.setWidth("625px");
//        cp.setCaptionHTML("<b>Репликация</b>");
//        cp.getElement().getStyle().setPadding(3, Style.Unit.PX);
//        cp.getElement().getStyle().setBorderWidth(2, Style.Unit.PX);
//
//        HorizontalPanel hp = new HorizontalPanel();
//        Widget w;
//        hp.add(w = createReplTableSection(createReplTable(null)));
//        w.getElement().getStyle().setMarginRight(3, Style.Unit.PX);
//
//        hp.add(createReplHeader());
//
//        cp.add(hp);
//        return cp;
//    }
//
//    private Widget createReplHeader(){
//        VerticalPanel vp = new VerticalPanel();
//        vp.setWidth("160px");
//        vp.setBorderWidth(1);
//        Label lb = new Label("Итоги");
//
//        FlexTable ft = new FlexTable();
//        ft.setWidth("100%");
//
//        Label t;
//        ft.setWidget(0, 0, t = new Label("Всего:"));
//        t.getElement().getStyle().setBackgroundColor("#fbec5d");
//        t.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);
//        ft.setWidget(1, 0, new Label("Обработано:"));
//        ft.setWidget(2, 0, new Label("Ожидает:"));
//        ft.setWidget(3, 0, t = new Label("Ошибок:"));
//        t.getElement().getStyle().setColor("red");
//        ft.setWidget(4, 0, new Label("Остальных:"));
//
//        ft.setWidget(0, 1, repl_Total = new Label(quest_str));
//        ft.setWidget(1, 1, repl_Done = new Label(quest_str));
//        ft.setWidget(2, 1, repl_Wait = new Label(quest_str));
//        ft.setWidget(3, 1, repl_Error = new Label(quest_str));
//        repl_Error.getElement().getStyle().setColor("red");
//        ft.setWidget(4, 1, repl_Unknown = new Label(quest_str));
//
//        FlexTable.FlexCellFormatter fmt = ft.getFlexCellFormatter();
//        for (int i = 0; i < 5; i++){
//            fmt.setHorizontalAlignment(i, 1, HasHorizontalAlignment.ALIGN_RIGHT);
//            fmt.getElement(i, 0).getStyle().setWidth(50, Style.Unit.PX);
//        }
//
//        vp.add(lb);
//        vp.add(ft);
//
//        lb.setWidth("100%");
//        lb.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
//        lb.getElement().getStyle().setBackgroundColor("blue");
//        lb.getElement().getStyle().setColor("white");
//        lb.getElement().getStyle().setProperty("borderBottom", "1px solid #003366");
//
//        return vp;
//    }
//
//    private VerticalPanel createReplTableSection(Widget widget ){
//        VerticalPanel vp = new VerticalPanel();
//        vp.setWidth("460px");
//
//        FlexTable ft = new FlexTable();
//
//        ft.setWidget(0, 0, new Label("Таблица"));
//        ft.setWidget(0, 1, new Label("Тип"));
//        ft.setWidget(0, 2, new Label("Статус"));
//        ft.setWidget(0, 3, new Label("Количество"));
//
//        FlexTable.FlexCellFormatter fmt = ft.getFlexCellFormatter();
//
//        fmt.getElement(0, 0).getStyle().setWidth(190, Style.Unit.PX);
//        fmt.getElement(0, 1).getStyle().setWidth(30, Style.Unit.PX);
//        fmt.getElement(0, 2).getStyle().setWidth(90, Style.Unit.PX);
//        fmt.getElement(0, 3).getStyle().setWidth(145, Style.Unit.PX);
//
//        for (int i = 0; i < 4; i++){
//            fmt.getElement(0, i).getStyle().setBackgroundColor("blue");
//            fmt.getElement(0, i).getStyle().setColor("white");
//            fmt.setHorizontalAlignment(0, i, HasHorizontalAlignment.ALIGN_CENTER);
//            fmt.getElement(0, i).getStyle().setProperty("borderBottom", "1px solid #003366");
//        }
//
//        ft.setWidth("100%");
//
//        repl_table_section = new ScrollPanel();
//
//        repl_table_section.setWidth("100%");
//        repl_table_section.setHeight("200px");
//        repl_table_section.setAlwaysShowScrollBars(true);
//        repl_table_section.getElement().getStyle().setOverflowX(Style.Overflow.HIDDEN);
//        //repl_table_section.getElement().getStyle().setBackgroundColor("green");
//        repl_table_section.add(widget);
//
//        vp.getElement().getStyle().setProperty("border", "1px solid black");
//        vp.add(ft);
//        vp.add(repl_table_section);
//        return vp;
//    }
//
//    private Widget createReplTable(List<ReplTableItem> list) {
//        FlexTable ft = new FlexTable();
//        ft.setWidth("100%");
//        FlexTable.FlexCellFormatter fmt = ft.getFlexCellFormatter();
//        if (list == null || list.size() == 0){
//            ft.setText(0, 0, "Нет данных");
//            fmt.setColSpan(0, 0, 4);
//            fmt.setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_CENTER);
//        } else{
//            int c = 0;
//            for (ReplTableItem item: list){
//                ft.setText(c, 0, item.getName());
//                ft.setText(c, 1, item.getType());
//                ft.setText(c, 2, item.getStatus());
//                ft.setText(c, 3, item.getCount().toString());
//
//                fmt.setHorizontalAlignment(c, 1, HasHorizontalAlignment.ALIGN_CENTER);
//                fmt.setHorizontalAlignment(c, 2, HasHorizontalAlignment.ALIGN_CENTER);
//                fmt.setHorizontalAlignment(c, 3, HasHorizontalAlignment.ALIGN_RIGHT);
//                c++;
//            };
//            fmt.getElement(0, 0).getStyle().setWidth(198, Style.Unit.PX);
//            fmt.getElement(0, 1).getStyle().setWidth(38, Style.Unit.PX);
//            fmt.getElement(0, 2).getStyle().setWidth(90, Style.Unit.PX);
//            fmt.getElement(0, 3).getStyle().setWidth(145, Style.Unit.PX);
//        }
//
//        return ft;
//    }
//
//    private Widget createOperSection(){
//        CaptionPanel cp = new CaptionPanel();
//        cp.setWidth("372px");
//        cp.setCaptionHTML("<b>Операции</b>");
//        cp.getElement().getStyle().setPadding(3, Style.Unit.PX);
//        cp.getElement().getStyle().setBorderWidth(2, Style.Unit.PX);
//
//        cp.add(createOperTableSection(createOperTable(null)));
//        return cp;
//    }
//
//   private VerticalPanel createOperTableSection(Widget widget ){
//       VerticalPanel vp = new VerticalPanel();
//       vp.setWidth("370px");
//
//       FlexTable ft = new FlexTable();
//
//       ft.setWidget(0, 0, new Label("Состояние"));
//       ft.setWidget(0, 1, new Label("Веер"));
//       ft.setWidget(0, 2, new Label("Количество"));
//
//       FlexTable.FlexCellFormatter fmt = ft.getFlexCellFormatter();
//
//       fmt.getElement(0, 0).getStyle().setWidth(190, Style.Unit.PX);
//       fmt.getElement(0, 1).getStyle().setWidth(30, Style.Unit.PX);
//       fmt.getElement(0, 2).getStyle().setWidth(145, Style.Unit.PX);
//
//       for (int i = 0; i < 3; i++){
//           fmt.getElement(0, i).getStyle().setBackgroundColor("blue");
//           fmt.getElement(0, i).getStyle().setColor("white");
//           fmt.setHorizontalAlignment(0, i, HasHorizontalAlignment.ALIGN_CENTER);
//           fmt.getElement(0, i).getStyle().setProperty("borderBottom", "1px solid #003366");
//       }
//
//       ft.setWidth("100%");
//
//       oper_table_section = new ScrollPanel();
//
//       oper_table_section.setWidth("100%");
//       oper_table_section.setHeight("180px");
//       oper_table_section.setAlwaysShowScrollBars(true);
//       oper_table_section.getElement().getStyle().setOverflowX(Style.Overflow.HIDDEN);
//
//       oper_table_section.add(widget);
//
//       vp.getElement().getStyle().setProperty("border", "1px solid black");
//
//       vp.add(ft);
//       vp.add(oper_table_section);
//       return vp;
//   }
//
//    private Widget createOperTable(List<OperTableItem> list) {
//        FlexTable ft = new FlexTable();
//        ft.setWidth("100%");
//        FlexTable.FlexCellFormatter fmt = ft.getFlexCellFormatter();
//        if (list == null || list.size() == 0){
//            ft.setText(0, 0, "Нет данных");
//            fmt.setColSpan(0, 0, 4);
//            fmt.setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_CENTER);
//        } else{
//            int c = 0;
//            for (OperTableItem item: list){
//                ft.setText(c, 0, item.getState());
//                ft.setText(c, 1, item.getFun());
//                ft.setText(c, 2, item.getCount().toString());
//
//                fmt.setHorizontalAlignment(c, 1, HasHorizontalAlignment.ALIGN_CENTER);
//                fmt.setHorizontalAlignment(c, 2, HasHorizontalAlignment.ALIGN_RIGHT);
//                c++;
//            };
//            fmt.getElement(0, 0).getStyle().setWidth(206, Style.Unit.PX);
//            fmt.getElement(0, 1).getStyle().setWidth(38, Style.Unit.PX);
//            fmt.getElement(0, 2).getStyle().setWidth(145, Style.Unit.PX);
//        }
//
//        return ft;
//    }
//
//    private PushButton createRefreshButton(){
//        PushButton button = new PushButton(new Image(ImageConstants.INSTANCE.refresh24()), new ClickHandler() {
//            @Override
//            public void onClick(ClickEvent event) {
//                getBufferInfo();
//            }
//        });
//        button.setTitle("Обновить данные");
//        button.setWidth("24px");
//        button.setHeight("24px");
//        button.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
//        button.getElement().getStyle().setMarginBottom(2, Style.Unit.PX);
//        return button;
//    }
//
//    private void getBufferInfo(){
//        try{
//            WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
//            BarsGLEntryPoint.monitorService.getInfo(new AuthCheckAsyncCallback<RpcRes_Base<MonitoringWrapper>>() {
//                @Override
//                public void onSuccess(RpcRes_Base<MonitoringWrapper> result) {
//                    if (result.isError()){
//                        DialogManager.error("Ошибка", "Операция получения данных по буферу не удалась.\nОшибка: " + result.getMessage());
//                    }else{
//                        MonitoringWrapper wrapper = result.getResult();
//                        refreshData(wrapper);
//                    }
//                    WaitingManager.hide();
//                }
//            });
//        }catch (Exception e){
//            DialogManager.error("Ошибка", "Операция получения данных по буферу не удалась.\nОшибка: " + e.getMessage());
//        }
//    }
//
//    private void refreshData(MonitoringWrapper wrapper){
//        pd_Total.setText(wrapper.getPd().getTotal().toString());
//        pd_Moved.setText(wrapper.getPd().getTotal_moved().toString());
//        pd_Wait.setText(wrapper.getPd().getTotal_wait().toString());
//
//        blt_Total.setText(wrapper.getBaltur().getTotal().toString());
//        blt_Moved.setText(wrapper.getBaltur().getTotal_moved().toString());
//        blt_Wait.setText(wrapper.getBaltur().getTotal_wait().toString());
//
//        repl_Total.setText(wrapper.getReplTotal().getTotal().toString());
//        repl_Done.setText(wrapper.getReplTotal().getTotal_done().toString());
//        repl_Wait.setText(wrapper.getReplTotal().getTotal_wait().toString());
//        repl_Error.setText(wrapper.getReplTotal().getTotal_error().toString());
//        repl_Unknown.setText(wrapper.getReplTotal().getTotal_unknown().toString());
//
//        repl_table_section.clear();
//        repl_table_section.add(createReplTable(wrapper.getReplList()));
//
//        oper_table_section.clear();
//        oper_table_section.add(createOperTable(wrapper.getOperList()));
//    }
}
