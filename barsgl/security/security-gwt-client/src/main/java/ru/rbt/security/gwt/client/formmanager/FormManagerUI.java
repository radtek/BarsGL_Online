package ru.rbt.security.gwt.client.formmanager;

import com.google.gwt.user.client.Window;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.events.StatusBarEventHandler;
import ru.rbt.barsgl.gwt.core.events.StatusBarEvent;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.security.gwt.client.CommonEntryPoint;
import ru.rbt.security.gwt.client.operday.IDataConsumer;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.statusbar.StatusBarManager;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;
import ru.rbt.shared.access.UserMenuWrapper;

import static ru.rbt.security.gwt.client.operday.OperDayGetter.getOperday;

import java.util.ArrayList;
import java.util.logging.Logger;


public class FormManagerUI extends Composite {
    static Logger log = Logger.getLogger("FormManagerUI");

    private static FormManagerUIBinder uiBinder = GWT.create(FormManagerUIBinder.class);

    public interface FormManagerUIBinder extends UiBinder<Widget, FormManagerUI> { }

    private static final FormManagerUI formManager = null;

    @UiField
    DockLayoutPanel basePanel;

    @UiField
    HorizontalPanel operdayPanel;

    @UiField
    MenuBar menuBar;

    @UiField
    DockLayoutPanel dataPanel;

    @UiField
    SimplePanel statusBar;

    @UiField
    Label statusBarTitle;

    private Label operdayLabel;
    private Label operdayDate;
    private IMenuBuilder menuBuilder;

    public static FormManagerUI getFormManager(final UserMenuWrapper menuWrapper, IMenuBuilder menuBuilder){
        return formManager == null ? new FormManagerUI(menuWrapper, menuBuilder) : formManager;
    }

    protected FormManagerUI(final UserMenuWrapper menuWrapper, IMenuBuilder menuBuilder) {
        initWidget(uiBinder.createAndBindUi(this));

        setWidgetToMaxWidthAndHeight();

        LocalEventBus.addHandler(StatusBarEvent.TYPE, createStatusBarEventHandler());
        LocalEventBus.addHandler(FormEvent.TYPE, createFormHandler());

        if(menuBuilder != null){
            this.menuBuilder = menuBuilder;

            menuBuilder.init(menuWrapper, dataPanel);
            menuBuilder.build(menuBar);        
        }

        createOperdayPanel();
    }

    private FormEventHandler createFormHandler() {
        return new FormEventHandler() {
            @Override
            public void show(Widget form) {
                menuBuilder.formLoad(form);
            }
        };
    }

    private StatusBarEventHandler createStatusBarEventHandler() {
        return new StatusBarEventHandler() {
            @Override
            public void message(String msg, StatusBarManager.MessageReason reason) {
                String color ;

                switch (reason) {
                    case ERROR:
                        color = "red";
                        break;

                    case INFO:
                        color = "blue";
                        break;

                    default: color = "black";
                }
                statusBarTitle.getElement().getStyle().setColor(color);
                statusBarTitle.setText(msg);
            }
        };
    }

    public static void ChangeStatusBarText(String text, StatusBarManager.MessageReason reason) {
        LocalEventBus.fireEvent(new StatusBarEvent(text, reason));
    }

    public static void show(Widget form){
        LocalEventBus.fireEvent(new FormEvent(form));
    }

    private void setWidgetToMaxWidthAndHeight () {
        setWidth("100%");
        setHeight("100%");
    }

    private void createOperdayPanel() {
        ClickHandler operdayRefresh = new ClickHandler(){
            @Override
            public void onClick(ClickEvent event) {
                showOperday();
            }
        };

        operdayLabel = new Label("Операционный день:");
        operdayLabel.setWidth("150px");
        operdayLabel.setStyleName("operday-label");
        operdayLabel.addClickHandler(operdayRefresh);

        operdayDate = new Label();
        operdayDate.setWidth("80px");
        operdayDate.setStyleName("operday-text");
        operdayDate.addClickHandler(operdayRefresh);

        operdayPanel.add(operdayLabel);
        operdayPanel.add(operdayDate);

        showOperday();

        loadAcc2forDeals();
    }

    private void showOperday() {
        getOperday(new IDataConsumer<OperDayWrapper>() {
            @Override
            public void accept(OperDayWrapper operDayWrapper) {
                operdayDate.setText(operDayWrapper.getCurrentOD());
                CommonEntryPoint.CURRENT_WORKDAY = operDayWrapper.getPreviosODDate();
                CommonEntryPoint.CURRENT_OPER_DAY = operDayWrapper.getCurrentODDate();
                LocalDataStorage.putParam("current_od_date", operDayWrapper.getCurrentODDate());
            }
        });
    }

    public static void loadAcc2forDeals(){
        try {
            CommonEntryPoint.monitorService.getAcc2ForDeals(new AuthCheckAsyncCallback<RpcRes_Base<ArrayList>>() {
                @Override
                public void onFailureOthers(Throwable throwable) {
                    Window.alert("Операция не удалась loadAcc2forDeals().\nОшибка: " + throwable.getLocalizedMessage());
                }

                @Override
                public void onSuccess(RpcRes_Base<ArrayList> res) {
                    log.info("loadAcc2forDeals() onSuccess");
                    if (res.isError()){
                        DialogManager.error("Ошибка loadAcc2forDeals()", "Операция не удалась.\nОшибка: " + res.getMessage());
                    } else {
                        log.info("indexOf(\"45204\") = "+res.getResult().indexOf("45204"));
                        LocalDataStorage.putParam("Acc2ForDeals", res.getResult());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setBrowserWindowTitle(String text){
        if (Document.get() != null){
            Document.get().setTitle(text);
        }
    }

}