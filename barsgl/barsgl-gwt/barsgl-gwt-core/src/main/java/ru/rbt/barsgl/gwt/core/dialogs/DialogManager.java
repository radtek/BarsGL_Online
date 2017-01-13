package ru.rbt.barsgl.gwt.core.dialogs;

import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;

/**
 * Created by akichigi on 17.03.15.
 */
public class DialogManager {

    private static final String C_HEADERWIDTH = "250px";
    private static DialogBox popupPanel = new DialogBox(false, true);
    private static Integer counter = new Integer(0);

    public static void message(String headerText, String messageText) {
        if (counter.intValue() == 0) {
            message(headerText, messageText, false);
        }
    }

    public static void error(String headerText, String messageText) {
        if (counter.intValue() == 0) {
            message(headerText, messageText, true);
        }
    }

    public static void error(String headerText, List<String> messageList) {
        message(headerText, messageList, true);
    }

    private static void message(String headerText,
                                List<String> messageList, boolean isError) {
        if (counter.intValue() > 0) {
            return;
        }

        counter = Integer.valueOf(counter.intValue() + 1);

        int len = messageList.size();
        String width = len > 1 ? C_HEADERWIDTH : "200px";

        popupPanel.setWidth(width);
        popupPanel.setAnimationEnabled(true);

        FlexTable table = new FlexTable();

        Label labelHeader = new Label(headerText);
        labelHeader.setWidth(width);

        table.setWidget(0, 0, labelHeader);
        table.getFlexCellFormatter().setHeight(1, 0, "5px");

        for (int i = 0; i < len; i++) {
            Label labelMessage = new Label((String) messageList.get(i));
            labelMessage.setStyleName(len > 1 ? "text_left" : "text_center");

            labelMessage.setWidth(width);

            table.setWidget(i + 2, 0, labelMessage);
            table.getFlexCellFormatter().setStyleName(i + 2, 0, "filter-td");
        }

        Button btn = createButton("OK", null);

        table.setWidget(len * 2 + 4, 0, btn);

        table.getFlexCellFormatter().setStyleName(0, 0,
                isError ? "err_popup_header" : "Table_Header");

        table.getFlexCellFormatter().setStyleName(len * 2 + 2, 0, "line-td");
        table.getFlexCellFormatter().setStyleName(len * 2 + 4, 0,
                "filter_ButtonsPanel");

        table.getFlexCellFormatter().setAlignment(len * 2 + 4, 0,
                HasHorizontalAlignment.ALIGN_CENTER,
                HasVerticalAlignment.ALIGN_MIDDLE);

        popupPanel.setWidget(table);
        popupPanel.setGlassEnabled(true);

        popupPanel.center();

        popupPanel.setGlassEnabled(true);
        popupPanel.center();
        popupPanel.show();
        btn.setFocus(true);
    }

    private static void message(String headerText, String messageText,
                                boolean isError) {
        if (counter.intValue() > 0) {
            return;
        }

        counter = Integer.valueOf(counter.intValue() + 1);

        popupPanel.setWidth(C_HEADERWIDTH);

        FlexTable table = new FlexTable();

        Label labelHeader = new Label(headerText);
        labelHeader.setWidth(C_HEADERWIDTH);

        Label labelMessage = new Label(messageText);
        labelMessage.setWidth(C_HEADERWIDTH);

        table.setWidget(0, 0, labelHeader);
        table.setWidget(2, 0, labelMessage);

        Button btn = createButton("OK", null);

        table.setWidget(5, 0, btn);

        table.getFlexCellFormatter().setStyleName(0, 0,
                isError ? "err_popup_header" : "Table_Header");
        table.getFlexCellFormatter().setStyleName(4, 0, "line-td");

        table.getFlexCellFormatter().setHeight(1, 0, "5px");
        table.getFlexCellFormatter().setHeight(3, 0, "5px");

        table.getFlexCellFormatter().setAlignment(5, 0,
                HasHorizontalAlignment.ALIGN_CENTER,
                HasVerticalAlignment.ALIGN_MIDDLE);

        popupPanel.setWidget(table);
        popupPanel.setGlassEnabled(true);

        popupPanel.center();
        btn.setFocus(true);
    }

    public static void confirm(String headerText, String confirmText, String okButtonText, ClickHandler clickHandler) {
        confirm(headerText, confirmText, okButtonText, "Отмена", clickHandler, null);
    }

    public static void confirm(String headerText, String confirmText,
                               String okButtonText, String cancelButtonText, ClickHandler clickOKHandler, ClickHandler clickCancelHandler) {
        if (counter.intValue() > 0) {
            return;
        }

        counter = Integer.valueOf(counter.intValue() + 1);

        popupPanel.setWidth(C_HEADERWIDTH);

        FlexTable table = new FlexTable();

        Label labelHeader = new Label(headerText);
        labelHeader.setWidth(C_HEADERWIDTH);

        Label labelConfirm = new Label(confirmText);
        labelConfirm.setWidth(C_HEADERWIDTH);

        table.setWidget(0, 0, labelHeader);
        table.setWidget(2, 0, labelConfirm);

        FlexTable actionTable = new FlexTable();

        actionTable.setWidget(0, 0, createButton(okButtonText, clickOKHandler));
        actionTable.getFlexCellFormatter().setWidth(0, 1, "20px");
        actionTable.setWidget(0, 2, createButton(cancelButtonText, clickCancelHandler));

        table.setWidget(6, 0, actionTable);

        table.getFlexCellFormatter().setStyleName(0, 0, "Table_Header");

        table.getFlexCellFormatter().setStyleName(4, 0, "line-td");
        table.getFlexCellFormatter().setStyleName(6, 0, "filter_ButtonsPanel");

        table.getFlexCellFormatter().setHeight(1, 0, "5px");
        table.getFlexCellFormatter().setHeight(3, 0, "5px");
        table.getFlexCellFormatter().setHeight(5, 0, "5px");

        popupPanel.setWidget(table);
        popupPanel.setGlassEnabled(true);

        popupPanel.center();
    }

    public static void hideForm() {
        popupPanel.hide();
    }

    private static Button createButton(String text, ClickHandler clickHandler) {
        Button btn = new Button(text);

        if (clickHandler != null) {
            btn.addClickHandler(clickHandler);
        }

        btn.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                DialogManager.popupPanel.hide();
                DialogManager.counter = Integer.valueOf(0);
            }
        });
        btn.setStyleName("dlg-button");
        btn.setWidth("100px");

        return btn;
    }
}
