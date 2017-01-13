package ru.rbt.barsgl.gwt.core.ui;

import java.io.Serializable;

/**
 * Created by akichigi on 13.04.16.
 */
public class ImmutableLinkedBoxes extends LinkedBoxes {

    public ImmutableLinkedBoxes(String boxInTitle, String boxOutTitle) {
        super(boxInTitle, boxOutTitle);
    }

    public ImmutableLinkedBoxes(ValuesBox boxIn, ValuesBox boxOut, String boxInTitle, String boxOutTitle) {
        super(boxIn, boxOut, boxInTitle, boxOutTitle);
    }

    @Override
    protected void addButtonClick(){
        try {
            if (boxIn.getItemCount() > 0 && boxIn.getSelectedIndex() != -1) {
                Serializable value = boxIn.getValue();
                String text = boxIn.getText();

                if (boxOut.getListIndex(text) == -1) {
                    boxOut.addItem(value, text);
                }
                boxOut.setValue(value);
            }
        } finally {
            updateButtonState();
        }
    }

    @Override
    protected void removeButtonClick(){
        try {
            if (boxOut.getItemCount() > 0 && boxOut.getSelectedIndex() != -1) {
                boxOut.removeItem();
            }
        } finally {
            updateButtonState();
        }
    }
}
