package ru.rbt.barsgl.gwt.client.formmanager;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.info.SystemInfoForm;
import ru.rbt.barsgl.gwt.client.monitoring.Monitor;
import ru.rbt.barsgl.gwt.client.operday.IDataConsumer;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.SecurityChecker;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.forms.IDisposable;
import ru.rbt.barsgl.shared.access.UserMenuWrapper;
import ru.rbt.barsgl.shared.enums.SecurityActionCode;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

import static ru.rbt.barsgl.gwt.client.operday.OperDayGetter.getOperday;


public class FormManagerUI extends Composite {
    private static FormManagerUIBinder uiBinder = GWT.create(FormManagerUIBinder.class);

    public interface FormManagerUIBinder extends UiBinder<Widget, FormManagerUI> { }

    private static final FormManagerUI formManager = null;

    public enum MessageReason {MSG, INFO, ERROR}

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
    private MenuBuilder menuBuilder;



    public static FormManagerUI getFormManager(final UserMenuWrapper menuWrapper){
        return formManager == null ? new FormManagerUI(menuWrapper) : formManager;
    }

    protected FormManagerUI(final UserMenuWrapper menuWrapper) {
        initWidget(uiBinder.createAndBindUi(this));

        setWidgetToMaxWidthAndHeight();

        LocalEventBus.addHandler(StatusBarEvent.TYPE, createStatusBarEventHandler());
        LocalEventBus.addHandler(FormEvent.TYPE, createFormHandler());
        menuBuilder = new MenuBuilder(menuWrapper, dataPanel).build(menuBar);

        createOperdayPanel();

        showMonitoring();
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
            public void message(String msg, MessageReason reason) {
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

    public static void ChangeStatusBarText(String text, MessageReason reason) {
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
    }

    private void showOperday() {
        getOperday(new IDataConsumer<OperDayWrapper>() {
            @Override
            public void accept(OperDayWrapper operDayWrapper) {
                operdayDate.setText(operDayWrapper.getCurrentOD());
                BarsGLEntryPoint.CURRENT_WORKDAY = operDayWrapper.getPreviosODDate();
                BarsGLEntryPoint.CURRENT_OPER_DAY = operDayWrapper.getCurrentODDate();
                LocalDataStorage.putParam("current_od_date", operDayWrapper.getCurrentODDate());
            }
        });
    }

    public static void setBrowserWindowTitle(String text){
        if (Document.get() != null){
            Document.get().setTitle(text);
        }
    }

    private void showMonitoring(){
        if (SecurityChecker.checkAction(SecurityActionCode.TaskMonitor)){
            show(new Monitor());
        }
    }
}