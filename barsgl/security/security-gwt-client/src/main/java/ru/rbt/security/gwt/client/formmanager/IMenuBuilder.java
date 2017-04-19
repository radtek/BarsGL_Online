/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.security.gwt.client.formmanager;

import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.shared.access.UserMenuWrapper;

/**
 *
 * @author Andrew Samsonov
 */
public interface IMenuBuilder {

  public void formLoad(Widget form);
  public void init(UserMenuWrapper wrapper, DockLayoutPanel dataPanel);  
  public IMenuBuilder build(MenuBar menu)  ;
}
