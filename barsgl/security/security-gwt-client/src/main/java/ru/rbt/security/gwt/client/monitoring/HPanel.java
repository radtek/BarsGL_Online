package ru.rbt.security.gwt.client.monitoring;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class HPanel extends HeaderPanel{
    Button b;
    Label header;

    public HPanel(int w, int h) {
        super();
        setPixelSize(w, h);
        getElement().getStyle().setProperty("border", "1px solid #003366");
    }

    public void setHeader(String text){
        header.setText(text);
    }

    public void addHeader(){
        HorizontalPanel pan = new HorizontalPanel();
        header = new Label();
        pan.add(header);
        pan.getElement().getStyle().setBackgroundColor("#cfcfcf");
        pan.getElement().getStyle().setColor("black");
        pan.getElement().getStyle().setProperty("borderBottom", "1px solid #003366");
        pan.getElement().getStyle().setPaddingRight( 4, Unit.PX);
        pan.getElement().getStyle().setPaddingLeft( 4, Unit.PX);
        pan.getElement().getStyle().setPaddingBottom( 4, Unit.PX);
        pan.setHeight("27px");

        b = new Button();
        b.addClickHandler( new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                handlerBody(event);
            }
        });
        b.setPixelSize(16, 16);
        pan.add(b);
        HorizontalPanel pan2 = new HorizontalPanel();
        pan2.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        pan2.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
        pan.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        pan.setWidth("100%");
        pan2.setWidth("100%");
        pan2.add(b);
        pan.add(pan2);
        setHeaderWidget(pan);
    }

    public void setButVisible(boolean e){
        b.setVisible(e);
    }

    public void setBody(Widget w){
        setContentWidget(w);
    }

    protected void handlerBody(ClickEvent event){
    }

}
