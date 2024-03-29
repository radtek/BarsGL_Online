package ru.rbt.barsgl.gwt.client.events.ae;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.gwt.core.ui.ValuesBox;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.enums.ErrorCorrectType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by akichigi on 06.03.17.
 */
public class ErrorHandlingEditListDlg extends DlgFrame implements IAfterShowEvent {
    private final int IDX_SRC = 11;
    private final int IDX_DATE = 15;
    private final int IDX_ERRCODE = 37;

    private ValuesBox comment;
    private List<Row> rows;
    private AreaBox commentBox;
    private Label totalOper;
    private Label labelComment;
    private final String COMMENT_LABEL = "Отметка о причине {0} списком";

    public ErrorHandlingEditListDlg(){
        super();
        setCaption("Редактирование списка сообщений");
        setAfterShowEvent(this);
    }

    @Override
    public Widget createContent(){
        Grid grid = new Grid(1, 2);
        grid.setWidget(0, 0, labelComment = new Label(""));
        grid.setWidget(0, 1, comment = new ValuesBox());
        grid.getCellFormatter().getElement(0, 0).getStyle().setWidth(150, Style.Unit.PX);

        comment.setWidth("280px");
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
        commentBox.setWidth("441px");
        commentBox.getElement().getStyle().setMarginTop(10, Style.Unit.PX);
        commentBox.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);

        Grid grid2 = new Grid(1, 2);
        grid2.setWidget(0, 0, new Label("Всего (на странице) операций для редактирования:"));
        grid2.setWidget(0, 1, totalOper = new Label());

        VerticalPanel vPanel = new VerticalPanel();
        vPanel.add(grid2);
        vPanel.add(grid);
        vPanel.add(commentBox);

        return vPanel;
    }

    private void clear(){
        totalOper.setText("");
        comment.clear();
        commentBox.clear();
    }

    @Override
    protected void fillContent() {
        clear();
        Object[] data = (Object[])params;

        rows = (List<Row>)data[4];
        if (rows != null) totalOper.setText("" + rows.size());
        comment.addItem(0, "");
        if (((Boolean) data[3])){
            //New
            comment.addItem(1, "Операция прислана ошибочно");
            comment.addItem(2, "Исправлено бухгалтерией");
            labelComment.setText(Utils.Fmt(COMMENT_LABEL, "закрытия"));
        } else {
            //Reproc
            comment.addItem(1, "Исправлены справочники");
            comment.addItem(2, "Переобработка после системной ошибки");
            labelComment.setText(Utils.Fmt(COMMENT_LABEL, "переобработки"));
        }
        comment.setSelectedIndex(0);
        commentBox.setValue((String) data[2]);
    }

    private void checkRowsUniformity() throws Exception {
        if (rows == null || rows.size() == 0) throw new Exception("Отсутствуют данные для исправления");

        String src = Utils.toStr((String) rows.get(0).getField(IDX_SRC).getValue());
        Date operDate = (Date) rows.get(0).getField(IDX_DATE).getValue();
        String errCode = Utils.toStr((String) rows.get(0).getField(IDX_ERRCODE).getValue());

        int count = 0;

        for ( int i = 1; i < rows.size(); i++){
            if ((operDate.compareTo((Date) rows.get(i).getField(IDX_DATE).getValue()) != 0) ||
                    src.compareTo(Utils.toStr((String) rows.get(i).getField(IDX_SRC).getValue()))!= 0 ||
                    errCode.compareTo(Utils.toStr((String) rows.get(i).getField(IDX_ERRCODE).getValue()))!= 0){

                count++;
            }
        }
        if (count > 0) throw new Exception(Utils.Fmt("Выборка содержит операции за разные даты опердня\nили разные источники сделки\nили разные коды ошибки в количестве {0} шт.",
                count));
    }

    private void checkUp(){
        try{
            checkRowsUniformity();

            if (commentBox.getText() == null || commentBox.getText().trim().isEmpty()) throw new Exception(Utils.Fmt("Пустое значение в поле {0}", COMMENT_LABEL));
            if (commentBox.getText() != null && commentBox.getText().length() > 255) throw new Exception(Utils.Fmt("Количество символов в поле {0} превышает 255",COMMENT_LABEL));
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
            for (Row row : rows){
                listID.add((Long)row.getField(0).getValue());
            }

            params = new Object[]{listID, commentBox.getValue(), null, ErrorCorrectType.EDIT_COMMENT};

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
        commentBox.setFocus(true);
    }
}
