package ru.rbt.security.gwt.client.formmanager;

//import ru.rbt.barsgl.gwt.client.formmanager.MenuBuilder;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import ru.rbt.security.gwt.client.CommonEntryPoint;
import ru.rbt.security.gwt.client.monitoring.Monitor;
import ru.rbt.security.gwt.client.operday.IDataConsumer;
import static ru.rbt.security.gwt.client.operday.OperDayGetter.getOperday;
//import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
//import ru.rbt.barsgl.gwt.client.formmanager.FormEvent;
//import ru.rbt.barsgl.gwt.client.formmanager.FormEventHandler;
//import ru.rbt.barsgl.gwt.client.formmanager.MenuBuilder;
//import ru.rbt.barsgl.gwt.client.formmanager.StatusBarEvent;
//import ru.rbt.barsgl.gwt.client.formmanager.StatusBarEventHandler;
//import ru.rbt.barsgl.gwt.client.info.SystemInfoForm;
//import ru.rbt.barsgl.gwt.client.monitoring.Monitor;
//import ru.rbt.barsgl.gwt.client.operday.IDataConsumer;
import ru.rbt.barsgl.gwt.core.SecurityChecker;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.forms.IDisposable;
import ru.rbt.barsgl.shared.access.UserMenuWrapper;
import ru.rbt.barsgl.shared.enums.SecurityActionCode;
import ru.rbt.barsgl.shared.operday.OperDayWrapper;

//import static ru.rbt.barsgl.gwt.client.operday.OperDayGetter.getOperday;


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
    
    private IMenuBuilder menuBuilder;

    public static FormManagerUI getFormManager(final UserMenuWrapper menuWrapper, IMenuBuilder menuBuilder){
        return formManager == null ? new FormManagerUI(menuWrapper, menuBuilder) : formManager;
    }

    protected FormManagerUI(final UserMenuWrapper menuWrapper, IMenuBuilder menuBuilder) {
        initWidget(uiBinder.createAndBindUi(this));

        setWidgetToMaxWidthAndHeight();

        LocalEventBus.addHandler(StatusBarEvent.TYPE, createStatusBarEventHandler());
        LocalEventBus.addHandler(FormEvent.TYPE, createFormHandler());
        
        //menuBuilder = new MenuBuilder(menuWrapper, dataPanel).build(menuBar);
        if(menuBuilder != null){
            this.menuBuilder = menuBuilder;

            menuBuilder.init(menuWrapper, dataPanel);
            menuBuilder.build(menuBar);        
        }

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
                CommonEntryPoint.CURRENT_WORKDAY = operDayWrapper.getPreviosODDate();
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