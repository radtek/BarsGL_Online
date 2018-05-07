package ru.rbt.security.gwt.client.monitoring;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;

public class HPanel extends HeaderPanel{
    PushButton b = new PushButton(new Image(ImageConstants.INSTANCE.reload16()));;
    Image image;
    Label header;

    public HPanel(int w, int h) {
        super();
        setPixelSize(w, h);
        getElement().getStyle().setProperty("border", "1px solid #003366");
    }

    public void setHeader(String text){
        header.setText(text);
    }

    public void addHeader(String text){
        HorizontalPanel pan = new HorizontalPanel();
        header = new Label();
        header.setText(text);
        pan.add(header);
        pan.getElement().getStyle().setBackgroundColor("#cfcfcf");
        pan.getElement().getStyle().setColor("black");
        pan.getElement().getStyle().setProperty("borderBottom", "1px solid #003366");
        pan.getElement().getStyle().setPaddingRight( 4, Unit.PX);
        pan.getElement().getStyle().setPaddingLeft( 4, Unit.PX);
//        pan.getElement().getStyle().setPaddingBottom( 4, Unit.PX);
        pan.setHeight("27px");

        b.addClickHandler( new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                handlerBody(event);
            }
        });
        b.setPixelSize(16, 16);
        HorizontalPanel pan2 = new HorizontalPanel();
        pan2.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        pan2.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
        pan.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        pan.setWidth("100%");
        pan2.setWidth("100%");
        HorizontalPanel img = new HorizontalPanel();
        pan2.add(b);
        image = new Image(ImageConstants.INSTANCE.refresh16());
        image.setVisible(false);
        pan2.add(image);
        pan.add(pan2);
        setHeaderWidget(pan);
    }

    public void setButTitle(String title){
        b.setTitle(title);
    }

    public void setButVisible(boolean e){
        b.setVisible(e);
        image.setVisible(!e);
    }

    public void setBody(Widget w){
        setContentWidget(w);
    }

    protected void handlerBody(ClickEvent event){
    }

}
