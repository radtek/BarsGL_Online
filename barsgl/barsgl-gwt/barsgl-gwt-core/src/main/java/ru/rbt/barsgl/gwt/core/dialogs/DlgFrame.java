package ru.rbt.barsgl.gwt.core.dialogs;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by akichigi on 19.03.15.
 */
public abstract class DlgFrame {
    protected DialogBox dlg;
    private SimplePanel contentPanel;
    protected HorizontalPanel btnPanel;
    protected Button ok;
    protected Button cancel;
    protected Object params;
    protected Widget content;
    protected Widget frame;
    
    private String caption = "Диалог";

    private IDlgEvents dlgEvents;
    private IAfterShowEvent afterShowEvent;
    private IAfterCancelEvent afterCancelEvent;

    public DlgFrame()
    {
        dlg = new DialogBox(false, true);
        dlg.setGlassEnabled(true);
        dlg.setText(caption);
        dlg.setAnimationEnabled(true);
        dlg.add(frame = createFrame());
    }

    public void setCaption(String caption){
        dlg.setText(caption);
    }

    public String getCaption() {
        return caption;
    }

    private Widget createFrame() {
        btnPanel = new HorizontalPanel();
        btnPanel.addStyleName("btn-panel");
        btnPanel.add(createOKButton());
        btnPanel.add(createCancelButton());

        VerticalPanel vp = new VerticalPanel();
        beforeCreateContent();
        contentPanel = new SimplePanel();
        content = createContent();
        if (content != null) contentPanel.add(content);
        vp.add(contentPanel);

        vp.add(btnPanel);

        return vp ;
    }

    public void setContent(Widget content){
    	this.content = content;
        contentPanel.clear();
    	contentPanel.add(content);
    }
    
    public Widget getContent(){
    	return content;
    }
    
    public Widget createContent(){
    	return null;
    }

    protected void fillContent()  {}

    protected abstract boolean onClickOK() throws Exception;

    public void setDlgEvents(IDlgEvents dlgEvents){
        this.dlgEvents = dlgEvents;
    }

    public IDlgEvents getDlgEvents(){
        return this.dlgEvents;
    }

    private Button createOKButton() {
        ok = new Button(TEXT_CONSTANTS.btn_Save());

        ok.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                try {
                    if (onClickOK()) {
                        doOnOkClick();
                    }
                } catch (Exception e) {
                    Window.alert(e.getMessage());
                }
            }
        });
        ok.addStyleName("dlg-button");
        return ok;
    }

    protected void doOnOkClick() throws Exception {
        if (dlgEvents != null) {
            dlgEvents.onDlgOkClick(params);
        }
    }

    private Button createCancelButton() {
        cancel = new Button(TEXT_CONSTANTS.btn_Close());

        cancel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                onCancelClick();
            }
        });

        cancel.addStyleName("dlg-button");

        return cancel;
    }

    protected void onCancelClick(){
        dlg.hide();
        if (afterCancelEvent != null){
            afterCancelEvent.afterCancel();
        }
    }

    public void show() {
        dlg.center();
        setPopupPosition();
        dlg.setGlassEnabled(true);
        dlg.show();
        if (afterShowEvent != null) {
        	afterShowEvent.afterShow();
        }
    }

    public void show(Object params) {
        this.params = params;
        fillContent();
        show();
    }

    public void hide() {
        dlg.hide();
    }
    
    public void setModal(boolean modal) {
        dlg.setModal(modal);
    }

    protected void setPopupPosition() {};

    public void setAfterShowEvent(IAfterShowEvent event){
        afterShowEvent = event;
    }

    public void setAfterCancelEvent(IAfterCancelEvent event) {
       afterCancelEvent = event;
    }

    public void setOkButtonCaption(String caption){
        ok.setText(caption);
    }

    protected void beforeCreateContent(){
    }

    private Label label = null;
    protected void showPreload(boolean isShow){
        if (isShow){
            if (label != null) return;
            label = new Label("Загрузка данных...  Подождите!");
            label.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
            dlg.remove(frame);
            dlg.add(label);
        } else{
            if (label == null) return;
            dlg.remove(label);
            label = null;
            dlg.add(frame);
            dlg.center();
        }
    }
}

