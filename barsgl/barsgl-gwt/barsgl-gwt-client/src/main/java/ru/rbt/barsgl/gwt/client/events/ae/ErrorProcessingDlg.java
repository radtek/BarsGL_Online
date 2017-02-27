package ru.rbt.barsgl.gwt.client.events.ae;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.gwt.core.ui.ValuesBox;
import ru.rbt.barsgl.shared.Utils;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by akichigi on 16.02.17.
 */
public class ErrorProcessingDlg extends DlgFrame {
    private final String ERROR_LABEL = "ИД сообщ.АЕ";
    private ValuesBox comment;
    private TxtBox errorID;
    private AreaBox commentBox;

   public ErrorProcessingDlg() {
       super();
       setCaption("Повторная обработка сообщения");
       ok.setText("Обработать");
   }

    @Override
    public Widget createContent() {
        Grid grid = new Grid(1, 2);
        grid.setWidget(0, 0, new Label(ERROR_LABEL));
        grid.setWidget(0, 1, errorID = new TxtBox());
        errorID.setReadOnly(true);
        errorID.setWidth("280px");

        Grid grid2 = new Grid(1, 2);
        grid2.setWidget(0, 0, new Label("Комментарий"));
        grid2.setWidget(0, 1, comment = new ValuesBox());

        comment.setWidth("280px");
        comment.addItem(0, "");
        comment.addItem(1, "Операция прислана ошибочно");
        comment.addItem(2, "Исправлено бухгалтерией");

        comment.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                commentBox.clear();
                if (comment.getSelectedIndex() != 0)
                    commentBox.setValue(comment.getText());
            }
        });

        commentBox = new AreaBox();
        commentBox.setHeight("50px");
        commentBox.setWidth("370px");
        commentBox.getElement().getStyle().setMarginTop(10, Style.Unit.PX);
        commentBox.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);

        VerticalPanel vPanel = new VerticalPanel();
        vPanel.add(grid);
        vPanel.add(grid2);
        vPanel.add(commentBox);

        return vPanel;
    }

    private void clear(){
        errorID.clear();
        comment.setSelectedIndex(0);
        commentBox.clear();
    }

    @Override
    protected void fillContent(){
        clear();
        Object[] data = (Object[])params;
        errorID.setValue((String) data[0]);
    }

    private void checkUp(){
        try{
            if (commentBox.getText() != null && commentBox.getText().length() > 255) throw new Exception(Utils.Fmt("Количество символов в поле {0} превышает 255","Комментарий"));
        }catch(Exception e){
            showInfo("Ошибка", e.getMessage());

            throw new IllegalArgumentException("column");
        }
    }


    @Override
    protected boolean onClickOK() throws Exception {
        try {
            checkUp();
           /* setWrapperFields();
            params = wrapper;*/
            params = ">>>> ErrorProcessingDlg";
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().equals("column")) {
                return false;
            } else {
                throw e;
            }
        }

        return true;
    }
}
