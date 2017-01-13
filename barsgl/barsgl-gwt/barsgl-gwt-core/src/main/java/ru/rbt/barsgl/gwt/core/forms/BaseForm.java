package ru.rbt.barsgl.gwt.core.forms;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;

/**
 * Created by akichigi on 02.04.15.
 */
public abstract class BaseForm extends Composite {
    private static BaseFormUIBinder uiBinder = GWT.create(BaseFormUIBinder.class);

    public interface BaseFormUIBinder extends UiBinder<Widget, BaseForm> { }

    @UiField
    protected Label title;
    @UiField
    protected DockLayoutPanel form;

    private DockLayoutPanel contentPanel;
    protected Widget content;
    protected boolean delayLoad;

    public BaseForm(){
    	this(false);
    }

    public BaseForm(boolean delayLoad){
        initWidget(uiBinder.createAndBindUi(this));
        this.delayLoad = delayLoad;
        contentPanel = new DockLayoutPanel(Style.Unit.PX);
        content = createContent();
        if (content != null) contentPanel.add(content);

        form.add(contentPanel);
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
}
