package ru.rbt.barsgl.gwt.core.actions;

import ru.rbt.barsgl.gwt.core.events.ActionEvent;
import ru.rbt.barsgl.gwt.core.events.LocalEventBus;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.core.forms.IDisposable;

public abstract class Action implements IDisposable {
	public enum ChangeReason {NAME, HINT, IMAGE, VISIBLE, ENABLE}
	
	private String name;
	private String hint;
	private Image image;
	private boolean visible;
	private boolean enable;
    private double leftMargin;

    public Action(String name, String hint, Image image, double separator){
		this.name = name == null ? "" : name;
		this.hint = hint == null ? "" : hint;
		this.image = image == null ? new Image() : image;
		leftMargin = separator;
		visible = true;
		enable = true;
    }
    
	public Action(String name, String hint, Image image) {
    	this(name, hint, image, 0);
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		if (!this.name.equalsIgnoreCase(name)) {
			this.name = name;
			fireEvent(ChangeReason.NAME);
		}
	}
	
	public String getHint() {
		return hint;
	}
	
	public void setHint(String hint) {
		if (!this.hint.equalsIgnoreCase(hint)) {
			this.hint = hint;
			fireEvent(ChangeReason.HINT);
		}
	}
	
	public Image getImage() {
		return image;
	}
	
	public void setImage(Image image) {
		if (this.image != image) {
			this.image = image;
			fireEvent(ChangeReason.IMAGE);
		}
	}
	
	public boolean isVisible() {
		return visible;
	}
	
	public void setVisible(boolean visible) {
		if (this.visible != visible) {
			this.visible = visible;
			fireEvent(ChangeReason.VISIBLE);
		}
	}
	
	public boolean isEnable() {
		return enable;
	}
	
	public void setEnable(boolean enable) {
		if (this.enable != enable) {
			this.enable = enable;
			fireEvent(ChangeReason.ENABLE);
		}
	}
	
    public double getSeparator() {
        return leftMargin;
    }

    public void setSeparator(double leftMargin) {
        this.leftMargin = leftMargin;
    }

	private void fireEvent(ChangeReason cr) {
		LocalEventBus.fireEvent(new ActionEvent(this, cr));
	}
	
	public abstract void execute();

	@Override
	public void dispose(){}
}
