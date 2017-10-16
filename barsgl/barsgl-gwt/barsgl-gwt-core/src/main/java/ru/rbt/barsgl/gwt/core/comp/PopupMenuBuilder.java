package ru.rbt.barsgl.gwt.core.comp;

import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.widgets.ActionBarWidget;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;

/**
 * Created by er17503 on 27.07.2017.
 */
public class PopupMenuBuilder {
   protected final PopupPanel popup;
   private ActionBarWidget actionBar;
   private String title;
   private Image image;
   private MenuBar bar;

   public PopupMenuBuilder(ActionBarWidget actionBar, String title, Image image){
       popup = new PopupPanel(true, true);
       this.actionBar = actionBar;
       this.title = title;
       this.image = image;
       bar = new MenuBar(true);
       popup.setWidget(bar);
   }

   public MenuBar addItem(MenuItem item){
       bar.addItem(item);
       return bar;
   }

   public MenuBar addSeparator(){
       bar.addSeparator();
       return bar;
   }

   public void hidePopup(){
       popup.hide();
   }

   public GridAction toAction(GridWidget grid){
       return new GridAction(grid,null, title, image,10,true) {
           @Override
           public void execute() {
               final PushButton button = actionBar.getButton(this);
               popup.setPopupPositionAndShow(new PopupPanel.PositionCallback() {

                   @Override
                   public void setPosition(int i, int i1) {
                       popup.setPopupPosition(button.getAbsoluteLeft(), button.getAbsoluteTop() + button.getOffsetHeight());
                   }
               });
           }
       };
   }
}
