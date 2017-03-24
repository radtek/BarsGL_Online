package ru.rbt.barsgl.gwt.client.dict;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.dict.dlg.AccTypeModifierDlg;
import ru.rbt.security.gwt.client.formmanager.FormManagerUI;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Field;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.AccTypeModifierWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.SecurityActionCode;

import java.util.ArrayList;

/**
 * Created by akichigi on 19.08.16.
 */
public class AccTypeModifier extends EditableDictionary<AccTypeModifierWrapper> {
    public final static String FORM_NAME = "Модификатор по разделу {0}, продукту {1} и подпродукту {2}";
    public final static String FIELD_SECTION = "Раздел";
    public final static String FIELD_PRODUCT = "Продукт";
    public final static String FIELD_SUBPRODUCT = "Подпродукт";
    public final static String FIELD_MODIFIER = "Модификатор";
    public final static String FIELD_MODIFNAME = "Наименование";

    protected Column sectionColumn;
    private String  initSection;
    protected Column  productColumn;
    private String  initProduct;
    protected Column  subProductColumn;
    private String  initSubProduct;

    public AccTypeModifier(String section, String product, String subproduct) {
        super("");
        initSection = section;
        initProduct = product;
        initSubProduct = subproduct;
        title.setText(Utils.Fmt(FORM_NAME, section, product, subproduct));
        exportToExcel.setFormTitle(title.getText());
        reconfigure();
    }

    @Override
    protected Object[] getInitParams(){ return new String[]{initSection, initProduct, initSubProduct};}

    protected void reconfigure(){
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addSecureAction(commonLazyAction("AccTypeModifierDlg",
                AccTypeModifierDlg.EDIT,
                FormAction.UPDATE,
                table.getColumns(),
                "Модификатор не сохранен",
                "Ошибка изменения модификатора",
                "Модификатор изменен успешно"
        ), SecurityActionCode.ReferAccTypeChng);


        abw.addSecureAction(commonLazyAction("AccTypeModifierDlg",
                AccTypeModifierDlg.CREATE,
                FormAction.CREATE,
                table.getColumns(),
                "Модификатор не создан",
                "Ошибка создания модификатора",
                "Модификатор создан успешно"
        ), SecurityActionCode.ReferAccTypeChng);

        abw.addSecureAction(commonLazyAction("AccTypeModifierDlg",
                        AccTypeModifierDlg.DELETE,
                        FormAction.DELETE,
                        table.getColumns(),
                        "Модификатор не удален",
                        "Ошибка удаления модификатора",
                        "Модификатор удален успешно"),
                SecurityActionCode.ReferAccTypeDel);

        abw.addAction(gotoSubProductAction());
        abw.addAction(gotoAccTypeAction());
    }

    private GridAction gotoSubProductAction() {
        return new GridAction(grid, "Подпродукты", "Управление подпродуктами", null, 10) {
            @Override
            public void execute() {
                FormManagerUI.show(new AccTypeSubProduct(initSection, initProduct));
            }
        };
    }

    private GridAction gotoAccTypeAction() {
        return new GridAction(grid, "Счет AccType", "Управление счетом AccType", null, 10, true) {
            @Override
            public void execute() {
                Field fieldModifier = getFieldByName(AccTypeModifier.FIELD_MODIFIER);
                if (fieldModifier == null) return;
                FormManagerUI.show(new AccType(initSection, initProduct, initSubProduct, (String)fieldModifier.getValue()));
            }
        };
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();

        result.addColumn(sectionColumn = new Column("sectcode", Column.Type.STRING, FIELD_SECTION, 15));
        result.addColumn(productColumn = new Column("prodcode", Column.Type.STRING, FIELD_PRODUCT, 17));
        result.addColumn(subProductColumn = new Column("subprodcode", Column.Type.STRING, FIELD_SUBPRODUCT, 22));
        result.addColumn(new Column("modifcode", Column.Type.STRING, FIELD_MODIFIER, 26));
        result.addColumn(new Column("name", Column.Type.STRING, FIELD_MODIFNAME, 240));

        return result;
    }

    @Override
    protected String prepareSql() {
        return  "select sectcode, prodcode, subprodcode, modifcode, name from GL_ACT4";
    }

    @Override
    protected void save(AccTypeModifierWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<AccTypeModifierWrapper>> asyncCallbackImpl) throws Exception {
        BarsGLEntryPoint.dictionaryService.saveAccTypeModifier(cnw, action, asyncCallbackImpl);
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

        item = new FilterItem(subProductColumn, FilterCriteria.EQ, initSubProduct, true);
        item.setReadOnly(true);
        list.add(item);

        return list;
    }
}
