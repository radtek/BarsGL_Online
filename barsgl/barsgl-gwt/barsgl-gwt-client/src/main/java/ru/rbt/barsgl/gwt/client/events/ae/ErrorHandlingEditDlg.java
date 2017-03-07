package ru.rbt.barsgl.gwt.client.events.ae;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.gwt.core.ui.ValuesBox;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.enums.ErrorCorrectType;

import java.util.ArrayList;
import java.util.List;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by akichigi on 06.03.17.
 */
public class ErrorHandlingEditDlg extends DlgFrame implements IAfterShowEvent {
    private final String CORRECT_LABEL = "ИД сообщ.АЕ исправительной операции";

    private ValuesBox comment;
    private Long id;
    private TxtBox id_pst_correct;
    private AreaBox commentBox;


    public ErrorHandlingEditDlg(){
        super();
        setCaption("Редактирование сообщения");
        setAfterShowEvent(this);
    }

    @Override
    public Widget createContent() {
        Grid grid = new Grid(1, 2);
        grid.setWidget(0, 0, new Label(CORRECT_LABEL));
        grid.setWidget(0, 1, id_pst_correct = new TxtBox());
        id_pst_correct.setWidth("210px");
        grid.getCellFormatter().getElement(0, 0).getStyle().setWidth(150, Style.Unit.PX);

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
        id_pst_correct.clear();
        comment.setSelectedIndex(0);
        commentBox.clear();
    }

    @Override
    protected void fillContent(){
        clear();
        Object[] data = (Object[])params;

        id = (Long)data[0];
        id_pst_correct.setValue((String) data[1]);
        commentBox.setValue((String) data[2]);

        id_pst_correct.setReadOnly(!((Boolean) data[3]));
    }

    private void checkUp(){
        try{
            if ((id_pst_correct.getText() == null || id_pst_correct.getText().trim().isEmpty()) &&
                    (commentBox.getText() == null || commentBox.getText().trim().isEmpty())) throw new Exception(Utils.Fmt("Пустое значение в поле {0} \nи {1}", CORRECT_LABEL, "Комментарий"));
            if (id_pst_correct.getText() != null &&  id_pst_correct.getText().length() > 128) throw new Exception(Utils.Fmt("Количество символов в поле {0} превышает 128", CORRECT_LABEL));

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

            List<Long> listID = new ArrayList<Long>();
            listID.add(id);

            params = new Object[]{listID, commentBox.getValue(), id_pst_correct.getValue(), ErrorCorrectType.EDIT_PST_COMMENT};

        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().equals("column")) {
                return false;
            } else {
                throw e;
            }
        }

        return true;
    }

    @Override
    public void afterShow() {
        id_pst_correct.setFocus(true);
    }
}
