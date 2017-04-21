package ru.rbt.barsgl.gwt.client.dict;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.dict.dlg.AccTypeProductDlg;
import ru.rbt.barsgl.gwt.client.dict.dlg.EditableDialog;
import ru.rbt.security.gwt.client.formmanager.FormManagerUI;
import ru.rbt.barsgl.gwt.core.actions.Action;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Field;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.AccTypeProductWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.shared.enums.SecurityActionCode;

import java.util.ArrayList;

/**
 * Created by akichigi on 12.08.16.
 */
public class AccTypeProduct extends EditableDictionary<AccTypeProductWrapper> {
    public final static String FORM_NAME = "Продукты по разделу {0}";
    public final static String FIELD_SECTION = "Раздел";
    public final static String FIELD_PRODUCT = "Продукт";
    public final static String FIELD_NAME = "Наименование";
    protected Column  sectionColumn;
    private String  initSection;

    public AccTypeProduct(String section) {
        super("");
        initSection = section;
        title.setText(Utils.Fmt(FORM_NAME, section));
        exportToExcel.setFormTitle(title.getText());
        reconfigure();
    }

    @Override
    protected Object[] getInitParams(){ return new String[]{initSection};}

    protected void reconfigure(){
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addSecureAction(commonLazyAction("AccTypeProductDlg",
                AccTypeProductDlg.EDIT,
                FormAction.UPDATE,
                table.getColumns(),
                "Продукт не сохранен",
                "Ошибка изменения продукта",
                "Продукт изменен успешно"), SecurityActionCode.ReferAccTypeChng);
        abw.addSecureAction(commonLazyAction("AccTypeProductDlg",
                AccTypeProductDlg.CREATE,
                FormAction.CREATE,
                table.getColumns(),
                "Продукт не создан",
                "Ошибка создания продукта",
                "Продукт создан успешно"), SecurityActionCode.ReferAccTypeChng);
        abw.addSecureAction(commonLazyAction("AccTypeProductDlg",
                AccTypeProductDlg.DELETE,
                FormAction.DELETE,
                table.getColumns(),
                "Продукт не удален",
                "Ошибка удаления продукта",
                "Продукт удален успешно"), SecurityActionCode.ReferAccTypeDel);

        abw.addAction(gotoSectiontAction());
        abw.addAction(gotoSubProductAction());
    }

    private GridAction gotoSectiontAction() {
        return new GridAction(grid, "Разделы", "Управление разделами", null, 10) {
            @Override
            public void execute() {
                FormManagerUI.show(new AccTypeSection());
            }
        };
    }

    private GridAction gotoSubProductAction() {
        return new GridAction(grid, "Подпродукты", "Управление подпродуктами", null, 10, true) {
            @Override
            public void execute() {
                Field fieldProduct = getFieldByName(AccTypeProduct.FIELD_PRODUCT);
                if (fieldProduct == null) return;

                FormManagerUI.show(new AccTypeSubProduct(initSection, (String)fieldProduct.getValue()));
            }
        };
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();

        result.addColumn(sectionColumn = new Column("sectcode", Column.Type.STRING, FIELD_SECTION, 12));
        result.addColumn(new Column("prodcode", Column.Type.STRING, FIELD_PRODUCT, 15));
        result.addColumn(new Column("name", Column.Type.STRING, FIELD_NAME, 240));

        return result;
    }

    @Override
    protected String prepareSql() {
        return "select sectcode, prodcode, name from GL_ACT2";
    }

    @Override
    protected ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("sectcode", Column.Sort.ASC));
        list.add(new SortItem("prodcode", Column.Sort.ASC));
        return list;
    }

    @Override
    protected void save(AccTypeProductWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<AccTypeProductWrapper>> asyncCallbackImpl) throws Exception {
        BarsGLEntryPoint.dictionaryService.saveAccTypeProduct(cnw, action, asyncCallbackImpl);
    }

   @Override
    protected ArrayList<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams) {
        ArrayList<FilterItem> list = new ArrayList<FilterItem>();
        FilterItem item = new FilterItem(sectionColumn, FilterCriteria.EQ, initSection, true);
        item.setReadOnly(true);
        list.add(item);
        return list;
    }
}
