package ru.rbt.barsgl.gwt.core.dialogs;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;

public class WaitingManager {	
	private static PopupPanel popupPanel = new PopupPanel();
	private static Integer counter = new Integer(0);
	private static String oldMessage = "";
	private static String nextMessage = "";

	public static void show(String message) {
		counter = Integer.valueOf(counter.intValue() + 1);
  
		if ((counter.intValue() == 0) || (!oldMessage.equals(message))) {
			oldMessage = message;

			popupPanel.setStyleName("info");
			popupPanel.setWidth("200px");

			Label label = new Label(message);
			label.setStyleName("label");

			popupPanel.setWidget(label);
			popupPanel.setGlassEnabled(true);
			popupPanel.center();
		}
	}

	public static void show() {
		if (nextMessage.length() > 0) {
			show(nextMessage);
			nextMessage = "";
		} else {
			show(TEXT_CONSTANTS.waitMessage_Load());
		}
	}

	public static void setMessage(String messqge) {
		nextMessage = messqge;
	}

	public static void hide() {
		counter = Integer.valueOf(counter.intValue() - 1);
		if (counter.intValue() <= 0) {
			oldMessage = "";
			counter = Integer.valueOf(0);
			popupPanel.hide();
		}
	}

	public static void refresh() {
		oldMessage = "";
		counter = Integer.valueOf(0);
		popupPanel.hide();
	}

	public static boolean isWaiting() {
		if (counter.intValue() <= 0) {
			return false;
		}
		return true;
	}
}