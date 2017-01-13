package ru.rbt.barsgl.gwt.core.widgets;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PushButton;
import ru.rbt.barsgl.gwt.core.SecurityChecker;
import ru.rbt.barsgl.gwt.core.actions.Action;
import ru.rbt.barsgl.gwt.core.actions.Action.ChangeReason;
import ru.rbt.barsgl.gwt.core.events.ActionEvent;
import ru.rbt.barsgl.gwt.core.events.ActionEventHandler;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;
import ru.rbt.barsgl.gwt.core.forms.IDisposable;
import ru.rbt.barsgl.shared.enums.SecurityActionCode;

import java.util.ArrayList;
import java.util.HashMap;

public class ActionBarWidget extends Composite implements IDisposable{
	private HorizontalPanel panel;
	private ArrayList<PushButton> buttons;
	private HashMap<Action, PushButton> actions;

	public ActionBarWidget() {
		panel = new HorizontalPanel();
		buttons = new ArrayList<PushButton>();
		actions = new HashMap<Action, PushButton>();
		LocalEventBus.addHandler(ActionEvent.TYPE, createActionChangeEventHandler());
		initWidget(panel);
	}
	
	public Action addAction(final Action action) {
		if (!actions.containsKey(action)) {
			PushButton button = new PushButton(action.getImage(), new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					action.execute();
				}
			});
			button.setTitle(action.getHint());
			if (!action.getName().isEmpty()) {
				button.setText(action.getName());
                button.getElement().getStyle().setLineHeight(24, Style.Unit.PX);
			} else {
				button.setWidth("24px");
			}
			button.setHeight("24px");

            button.getElement().getStyle().setMarginLeft(action.getSeparator(), Style.Unit.PX);

			actions.put(action, button);
			buttons.add(button);
			refreshPanel();
		}
		return action;
	}

	public Action addSecureAction(final Action action, SecurityActionCode actionCode) {
		if (SecurityChecker.checkAction(actionCode)){
			return addAction(action);
		}
		return null;
	}

	public Action addSecureAction(final Action action, SecurityActionCode ... actionCodes) {
		if (SecurityChecker.checkActions(actionCodes)){
			return addAction(action);
		}
		return null;
	}

	public void delAction(Action action) {
		if (actions.containsKey(action)) {
			buttons.remove(actions.get(action));
			actions.remove(action);
			refreshPanel();
		}
	}

	public PushButton getButton(Action action) {
		return actions.containsKey(action) ? actions.get(action) : null;
	}

	private void refreshPanel() {
		panel.clear();
		for (int i = 0; i < buttons.size(); i++) {
			panel.add(buttons.get(i));
		}
	}

	private ActionEventHandler createActionChangeEventHandler() {
		return new ActionEventHandler() {
			@Override
			public void doActionChange(Action action, ChangeReason reason) {
				if (actions.containsKey(action)) {
					PushButton btn = actions.get(action);
					switch (reason) {
					case NAME:
						btn.setText(action.getName());
						break;
					case HINT:
						btn.setTitle(action.getHint());
						break;
					case IMAGE:
						btn.getUpFace().setImage(action.getImage());
						break;
					case ENABLE:
						btn.setEnabled(action.isEnable());
						break;
					case VISIBLE:
						btn.setVisible(action.isVisible());
						break;
					}
				}
			}
		};
	}
	
	@Override
	public void dispose() {
        for (Action action: actions.keySet()){
           action.dispose();
        }
	}
}
