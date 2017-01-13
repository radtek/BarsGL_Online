package ru.rbt.barsgl.gwt.client.dict;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.dict.dlg.AccTypeSectionDlg;
import ru.rbt.barsgl.gwt.client.dict.dlg.EditableDialog;
import ru.rbt.barsgl.gwt.client.formmanager.FormManagerUI;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.*;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.AccTypeSectionWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.SecurityActionCode;

import java.util.ArrayList;



/**
 * Created by akichigi on 12.08.16.
 */
public class AccTypeSection  extends EditableDictionary<AccTypeSectionWrapper> {
    public final static String FORM_NAME = "Разделы";
    public final static String FIELD_SECTION = "Раздел";
    public final static String FIELD_NAME = "Наименование";

    public AccTypeSection() {
        super(FORM_NAME);
        reconfigure();
    }

    protected void reconfigure(){
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));

        abw.addSecureAction(commonLazyAction("AccTypeSectionDlg",
                AccTypeSectionDlg.EDIT,
                FormAction.UPDATE,
                table.getColumns(),
                "Раздел не сохранен",
                "Ошибка изменения раздела",
                "Раздел изменен успешно"
        ), SecurityActionCode.ReferAccTypeChng);


        abw.addSecureAction(commonLazyAction("AccTypeSectionDlg",
                AccTypeSectionDlg.CREATE,
                FormAction.CREATE,
                table.getColumns(),
                "Раздел не создан",
                "Ошибка создания раздела",
                "Раздел создан успешно"
                ), SecurityActionCode.ReferAccTypeChng);

        abw.addSecureAction(commonLazyAction("AccTypeSectionDlg",
                        AccTypeSectionDlg.DELETE,
                        FormAction.DELETE,
                        table.getColumns(),
                        "Раздел не удален",
                        "Ошибка удаления раздела",
                        "Раздел удален успешно"),
                        SecurityActionCode.ReferAccTypeDel);

        abw.addAction(gotoProductAction());
    }

    private GridAction gotoProductAction(){
        return new GridAction(grid, "Продукты", "Управление продуктами", null, 10, true) {
            @Override
            public void execute() {
                Field field = getFieldByName(AccTypeSection.FIELD_SECTION);
                if (field == null) return;

                FormManagerUI.show(new AccTypeProduct((String) field.getValue()));
            }
        };
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();

        result.addColumn(new Column("sectcode", Column.Type.STRING, FIELD_SECTION, 12));
        result.addColumn(new Column("name", Column.Type.STRING, FIELD_NAME, 250));

        return result;
    }

    @Override
    protected String prepareSql() {
        return "select sectcode, name from GL_ACT1";
    }

    @Override
    protected ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("sectcode", Column.Sort.ASC));
        return list;
    }

    @Override
    protected void save(AccTypeSectionWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<AccTypeSectionWrapper>> asyncCallbackImpl) throws Exception {
        BarsGLEntryPoint.dictionaryService.saveAccTypeSection(cnw, action, asyncCallbackImpl);
    }
}
