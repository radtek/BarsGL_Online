package ru.rbt.barsgl.gwt.core.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;

/**
 * Created by akichigi on 25.08.16.
 */
public class BtnTxtBox extends TxtBox {
    private  PushButton button;

    @Override
    protected Widget configure(){
        createTextBox();

        textBox.setWidth("75px");
        textBox.setMaxLength(10);

        button = new PushButton(new Image(ImageConstants.INSTANCE.fromdict16()));
        button.setWidth("16px");
        button.setHeight("16px");

        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                onBntClick();
            }
        });

        Grid grid = new Grid(1, 2);
        grid.setCellSpacing(0);
        grid.setWidget(0, 0, textBox);
        grid.setWidget(0, 1, button);

        return grid;
    }

    public void onBntClick(){};

    @Override
    public void setWidth(String width) {
        // TODO надо сделать нормально!
        if (button.isVisible() && (width.length() > 2) && (width.substring(width.length()-2).equals("px"))) {
            int w = Integer.parseInt(width.substring(0, width.length()-2)) - 31;
            String wid = Integer.toString(w) + "px";
            textBox.setWidth(wid);
        } else {
            textBox.setWidth(width);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        button.setVisible(true);
        button.setEnabled(enabled);
    }

    @Override
    public void setReadOnly(boolean readOnly){
        super.setReadOnly(readOnly);
        button.setVisible(!readOnly);
    }

    public void setButtonVisible(boolean visible){
        button.setVisible(visible);
    }

    public void setButtonEnabled(boolean enabled) {
        button.setEnabled(enabled);
    }

}
