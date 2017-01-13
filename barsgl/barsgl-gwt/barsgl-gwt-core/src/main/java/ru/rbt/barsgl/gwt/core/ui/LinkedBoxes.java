package ru.rbt.barsgl.gwt.core.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;

import java.io.Serializable;
import java.util.ArrayList;


/**
 * Created by akichigi on 06.04.16.
 */
public class LinkedBoxes extends Composite {
    protected ValuesBox boxIn;
    protected ValuesBox boxOut;

    protected PushButton addButton;
    protected PushButton removeButton;
    private String boxInTitle;
    private String boxOutTitle;

    protected String initBoxHeight = "200px";
    protected String initBoxWidth = "100px";
    protected int initVisibleItemCount = 10;

    public LinkedBoxes(String boxInTitle, String boxOutTitle){
       this(new ValuesBox(), new ValuesBox(), boxInTitle, boxOutTitle);
    }

    public LinkedBoxes(ValuesBox boxIn, ValuesBox boxOut, String boxInTitle, String boxOutTitle ){
        this.boxIn = boxIn;
        this.boxOut = boxOut;
        this.boxInTitle = boxInTitle;
        this.boxOutTitle = boxOutTitle;

        initWidget(configure());
    }

    private Widget configure(){
        FlexTable table = new FlexTable();

        table.setText(0, 0, boxInTitle);
        table.setText(0, 2, boxOutTitle);

        boxIn.setVisibleItemCount(initVisibleItemCount);
        boxIn.setHeight(initBoxHeight);
        boxIn.setWidth(initBoxWidth);
        table.setWidget(1, 0, boxIn);

        VerticalPanel vPanel = new VerticalPanel();
        vPanel.add(addButton = createAddButton());
        addButton.getElement().getStyle().setMarginBottom(5, Style.Unit.PX);
        vPanel.add(removeButton = createRemoveButton());
        table.setWidget(1, 1, vPanel);

        boxOut.setVisibleItemCount(initVisibleItemCount);
        boxOut.setHeight(initBoxHeight);
        boxOut.setWidth(initBoxWidth);
        table.setWidget(1, 2, boxOut);

        updateButtonState();

        return table;
    }

    public void updateButtonState(){
        addButton.setEnabled(boxIn.getItemCount() > 0);
        removeButton.setEnabled(boxOut.getItemCount() > 0);
    }

    private PushButton createAddButton() {
        PushButton plusButton;

        plusButton = new PushButton(new Image(ImageConstants.INSTANCE.forward()));
        plusButton.setWidth("24px");
        plusButton.setHeight("24px");
        plusButton.setTitle("Добавить");

        plusButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                addButtonClick();
            }
        });
        return plusButton;
    }

    private PushButton createRemoveButton() {
        PushButton plusButton;

        plusButton = new PushButton(new Image(ImageConstants.INSTANCE.backward()));
        plusButton.setWidth("24px");
        plusButton.setHeight("24px");
        plusButton.setTitle("Исключить");

        plusButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                removeButtonClick();
            }
        });
        return plusButton;
    }

    protected void addButtonClick(){
        try {
            if (boxIn.getItemCount() > 0 && boxIn.getSelectedIndex() != -1) {
                Serializable value = boxIn.getValue();
                String text = boxIn.getText();

                if (boxOut.getListIndex(text) == -1) {
                    boxOut.addItem(value, text);
                    boxOut.setValue(value);

                    boxIn.removeItem();
                }
            }
        } finally {
            updateButtonState();
        }
    }

    protected void removeButtonClick(){
        try {
            if (boxOut.getItemCount() > 0 && boxOut.getSelectedIndex() != -1) {
                Serializable value = boxOut.getValue();
                String text = boxOut.getText();

                if (boxIn.getListIndex(text) == -1) {
                    boxIn.addItem(value, text);
                    boxIn.setValue(value);

                    boxOut.removeItem();
                }
            }
        } finally {
            updateButtonState();
        }
    }

    public void moveInToOut(Serializable value){
        boxIn.setValue(value);
        addButtonClick();
    }

    public void setBoxesHeight(String height){
        boxIn.setHeight(height);
        boxOut.setHeight(height);
    }

    public void setBoxInWidth(String width){
        boxIn.setWidth(width);
    }

    public void setBoxOutWidth(String width){
        boxOut.setWidth(width);
    }

    public void setVisibleItemCount(int count){
        boxIn.setVisibleItemCount(count);
        boxOut.setVisibleItemCount(count);
    }

    public void addBoxInItem(Serializable key, String value){
        boxIn.addItem(key, value);
    }

    public void addBoxOutItem(Serializable key, String value){
        boxOut.addItem(key, value);
    }

    public void clearBoxIn(){
        boxIn.clear();
    }

    public void clearBoxOut(){
        boxOut.clear();
    }

    public void setSelectedBoxInIndex(int idx){
        boxIn.setSelectedIndex(idx);
    }

    public void setSelectedBoxOutIndex(int idx){
        boxOut.setSelectedIndex(idx);
    }

    public ArrayList<Serializable> getBoxInValues(){
        return boxIn.getValues();
    }

    public ArrayList<Serializable> getBoxOutValues(){
        return boxOut.getValues();
    }

    public int getBoxInCount(){
        return boxIn.getItemCount();
    }

    public int getBoxOutCount(){
        return boxOut.getItemCount();
    }
}
