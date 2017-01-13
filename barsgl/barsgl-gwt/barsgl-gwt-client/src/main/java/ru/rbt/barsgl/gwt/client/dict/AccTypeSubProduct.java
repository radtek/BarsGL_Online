package ru.rbt.barsgl.gwt.client.dict;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.dict.dlg.AccTypeSubProductDlg;
import ru.rbt.barsgl.gwt.client.formmanager.FormManagerUI;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Field;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.AccTypeSubProductWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.SecurityActionCode;

import java.util.ArrayList;

/**
 * Created by akichigi on 12.08.16.
 */
public class AccTypeSubProduct extends EditableDictionary<AccTypeSubProductWrapper>{
    public final static String FORM_NAME = "Подпродукты по разделу {0} и продукту {1}";
    public final static String FIELD_SECTION = "Раздел";
    public final static String FIELD_PRODUCT = "Продукт";
    public final static String FIELD_SUBPRODUCT = "Подпродукт";
    public final static String FIELD_NAME = "Наименование";
    protected Column  sectionColumn;
    private String  initSection;
    protected Column  productColumn;
    private String  initProduct;

    public AccTypeSubProduct(String section, String product) {
        super("");
        initSection = section;
        initProduct = product;
        title.setText(Utils.Fmt(FORM_NAME, section,  product));
        reconfigure();
    }

    @Override
    protected Object[] getInitParams(){ return new String[]{initSection, initProduct};}

    protected void reconfigure(){
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addSecureAction(commonLazyAction("AccTypeSubProductDlg",
                AccTypeSubProductDlg.EDIT,
                FormAction.UPDATE,
                table.getColumns(),
                "Подпродукт не сохранен",
                "Ошибка изменения подпродукта",
                "Подпродукт изменен успешно"), SecurityActionCode.ReferAccTypeChng);
        abw.addSecureAction(commonLazyAction("AccTypeSubProductDlg",
                AccTypeSubProductDlg.CREATE,
                FormAction.CREATE,
                table.getColumns(),
                "Подпродукт не создан",
                "Ошибка создания подпродукта",
                "Подпродукт создан успешно"), SecurityActionCode.ReferAccTypeChng);
        abw.addSecureAction(commonLazyAction("AccTypeSubProductDlg",
                AccTypeSubProductDlg.DELETE,
                FormAction.DELETE,
                table.getColumns(),
                "Подпродукт не удален",
                "Ошибка удаления подпродукта",
                "Подпродукт удален успешно"), SecurityActionCode.ReferAccTypeDel);

        abw.addAction(gotoProductAction());
        abw.addAction(gotoModifierAction());
    }

    private GridAction gotoProductAction(){
        return new GridAction(grid, "Продукты", "Управление продуктами", null, 10) {
            @Override
            public void execute() {
                FormManagerUI.show(new AccTypeProduct(initSection));
            }
        };
    }

    private GridAction gotoModifierAction() {
        return new GridAction(grid, "Модификатор", "Управление модификаторами", null, 10, true) {
            @Override
            public void execute() {
                Field fieldSubProduct = getFieldByName(AccTypeSubProduct.FIELD_SUBPRODUCT);
                if (fieldSubProduct == null) return;
                FormManagerUI.show(new AccTypeModifier(initSection, initProduct, (String)fieldSubProduct.getValue()));
            }
        };
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();

        result.addColumn(sectionColumn = new Column("sectcode", Column.Type.STRING, FIELD_SECTION, 12));
        result.addColumn(productColumn = new Column("prodcode", Column.Type.STRING, FIELD_PRODUCT, 15));
        result.addColumn(new Column("subprodcode", Column.Type.STRING, FIELD_SUBPRODUCT, 20));
        result.addColumn(new Column("name", Column.Type.STRING, "Наименование", 240));

        return result;
    }

    @Override
    protected String prepareSql() {
        return "select sectcode, prodcode, subprodcode, name from GL_ACT3";
    }

    @Override
    protected ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("sectcode", Column.Sort.ASC));
        list.add(new SortItem("prodcode", Column.Sort.ASC));
        list.add(new SortItem("subprodcode", Column.Sort.ASC));
        return list;
    }

    @Override
    protected void save(AccTypeSubProductWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<AccTypeSubProductWrapper>> asyncCallbackImpl) throws Exception {
        BarsGLEntryPoint.dictionaryService.saveAccTypeSubProduct(cnw, action, asyncCallbackImpl);
    }

    @Override
    protected ArrayList<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams) {
        ArrayList<FilterItem> list = new ArrayList<FilterItem>();

        FilterItem item = new FilterItem(sectionColumn, FilterCriteria.EQ, initSection, true);
        item.setReadOnly(true);
        list.add(item);

        item = new FilterItem(productColumn, FilterCriteria.EQ, initProduct, true);
        item.setReadOnly(true);
        list.add(item);

        return list;
    }
}
