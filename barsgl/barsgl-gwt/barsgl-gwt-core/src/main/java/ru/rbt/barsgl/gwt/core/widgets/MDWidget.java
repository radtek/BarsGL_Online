package ru.rbt.barsgl.gwt.core.widgets;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.dom.client.Style.TextAlign;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.VerticalAlign;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.events.GridEvents;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;

import java.util.ArrayList;


public class MDWidget extends Composite{
    private GridWidget masterGrid;
    private ActionBarWidget masterActionBar;
    private GridWidget detailGrid;
    private ActionBarWidget detailActionBar;

    private String masterCaption;
    private String detailCaption;

    private boolean lazyRefresh;

    private ILinkFilterCriteria linkFilterCriteria;
    private boolean useCurtain;

    public MDWidget(GridWidget masterGrid, ActionBarWidget masterActionBar,
                    GridWidget detailGrid, ActionBarWidget detailActionBar){
        this(masterGrid, masterActionBar, null, detailGrid, detailActionBar, null);
    }

    public MDWidget(GridWidget masterGrid, ActionBarWidget masterActionBar, String masterCaption,
                    GridWidget detailGrid, ActionBarWidget detailActionBar, String detailCaption){
        this.masterGrid = masterGrid;
        this.masterActionBar = masterActionBar;
        this.masterCaption = masterCaption == null ? "" : masterCaption;

        this.detailGrid = detailGrid;
        this.detailActionBar = detailActionBar;
        this.detailCaption = detailCaption == null ? "" : detailCaption;
        this.detailGrid.setRefreshEvent(new IRefreshEvent() {
            @Override
            public void refresh() {
                if (useCurtain) showCurtain(false);
            }
        });

        createRowChangedEvent(masterGrid);

        initWidget(create());
    }

    public Widget create(){
        SplitLayoutPanel  mainPanel = new SplitLayoutPanel(7){
            @Override
            public void onResize(){
                super.onResize();
                masterGrid.resize();
                detailGrid.resize();
            }
        };

        DockLayoutPanel masterPanel = new DockLayoutPanel(Unit.MM);
        masterPanel.addNorth(createCaption(masterCaption), masterCaption.isEmpty() ? 0 : 5);
        masterPanel.addNorth(masterActionBar, 10);
        masterPanel.add(masterGrid);

        DockLayoutPanel detailPanel = new DockLayoutPanel(Unit.MM);
        detailPanel.getElement().getStyle().setMarginTop(2, Unit.MM);
        detailPanel.addNorth(createCaption(detailCaption), detailCaption.isEmpty() ? 0 : 5);
        detailPanel.addNorth(detailActionBar, 10);
        detailPanel.add(detailGrid);

        mainPanel.addNorth(masterPanel, 500);
        mainPanel.add(detailPanel);
        return mainPanel;
    }

    private HTML createCaption(String caption){
        HTML Html = new HTML("<div>" + caption + "</div>");

        Style style = Html.getElement().getStyle();
        style.setVerticalAlign(VerticalAlign.MIDDLE);
        style.setTextAlign(TextAlign.CENTER);
        style.setFontWeight(FontWeight.BOLD);
        style.setFontSize(14, Unit.PX);

        return Html;
    }

    public boolean isLazyRefresh() {
        return lazyRefresh;
    }

    public void setLazyRefresh(boolean lazyRefresh) {
        this.lazyRefresh = lazyRefresh;
    }

    private void createRowChangedEvent(GridWidget grid){
         grid.setRowChangedEvent(new IGridRowChanged() {
             @Override
             public void onRowChanged(Row row) {
                 ArrayList<FilterItem> filterItems = null;
                 if (linkFilterCriteria != null){
                     filterItems = linkFilterCriteria.createLinkFilterCriteria(row);
                 }

                 //Window.alert(row == null ? "null" : row.getField(0).getValue().toString());

                 LocalEventBus.fireEvent(new GridEvents(detailGrid.getId(),
                                         lazyRefresh ? GridEvents.EventType.LAZY_MASTER_ROW_CHANGED :
                                         GridEvents.EventType.MASTER_ROW_CHANGED, filterItems));
                 if (useCurtain)showCurtain(true);
             }
         });
    }

    public void setLinkFilterCriteria(ILinkFilterCriteria linkFilterCriteria) {
        this.linkFilterCriteria = linkFilterCriteria;
    }

    public void setUseCurtain(boolean useCurtain) {
        this.useCurtain = useCurtain;
        if (!useCurtain) {
            detailGrid.clear();
            showCurtain(false);
        }
    }

    public boolean isUseCurtain() {
        return useCurtain;
    }

    Node curtain;
    boolean _isCurtainOn;

    private void showCurtain(boolean on){
        if (_isCurtainOn == on) return;
        Element parent = detailGrid.getElement().getParentElement();

        if (on){
            curtain = parent.appendChild(createGlass());
            detailGrid.clear();
        } else{
            parent.removeChild(curtain);
        }
        _isCurtainOn = on;
    }

    private Element createGlass(){
        SimplePanel panel = new SimplePanel();
        Element div  = panel.getElement();
        div.setId("_curtain_");
        div.getStyle().setBackgroundColor("grey");
        div.getStyle().setOpacity(0.3);
        div.getStyle().setWidth(100, Unit.PCT);
        div.getStyle().setHeight(100, Unit.PCT);
        div.getStyle().setLeft(0, Unit.PX);
        div.getStyle().setTop(0, Unit.PX);
        div.getStyle().setPosition(Style.Position.ABSOLUTE);
        //div.getStyle().setZIndex(77777);
        return div;
    }
}
