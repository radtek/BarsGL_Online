package ru.rbt.barsgl.gwt.client.dict;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.dict.dlg.AccTypeDlg;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.security.gwt.client.formmanager.FormManagerUI;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.IAfterRefreshEvent;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.shared.filter.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.AccTypeWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.shared.enums.SecurityActionCode;

import java.util.ArrayList;

/**
 * Created by akichigi on 23.08.16.
 */
public class AccType extends EditableDictionary<AccTypeWrapper>  implements IAfterRefreshEvent {
    public final static String FORM_NAME = "AccType по разделу {0}, продукту {1}, подпродукту {2} и модификатору {3}";
    public final static String FIELD_SECTION = "Раздел";
    public final static String FIELD_PRODUCT = "Продукт";
    public final static String FIELD_SUBPRODUCT = "Подпродукт";
    public final static String FIELD_MODIFIER = "Модификатор";
    public final static String FIELD_ACCTYPE = "AccType";
    public final static String FIELD_ACCTYPENAME = "Наименование";
    public final static String FIELD_PL_ACT = "Счет ОФР - BARSGL";
    public final static String FIELD_FL_CTRL = "Контролир. счет";
    public final static String FIELD_TECH_ACT = "Тех. счёт";

    private Column sectionColumn;
    private String  initSection;
    private Column  productColumn;
    private String  initProduct;
    private Column  subProductColumn;
    private String  initSubProduct;
    private Column  modifierColumn;
    private String  initModifier;

    private  GridAction actionCreate;

    public AccType(String section, String product, String subproduct, String modifier) {
        super("");
        initSection = section;
        initProduct = product;
        initSubProduct = subproduct;
        initModifier = modifier;
        title.setText(Utils.Fmt(FORM_NAME, section, product, subproduct, modifier));
        exportToExcel.setFormTitle(title.getText());
        reconfigure(false);
    }

    public AccType(){
        super("План счетов по AccType");
        reconfigure(true);
    }

    @Override
    protected Object[] getInitParams(){ return new String[]{initSection, initProduct, initSubProduct, initModifier};}

    private void reconfigure(boolean isLookUp){
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        if (isLookUp) return;
        abw.addSecureAction(commonLazyAction("AccTypeDlg",
                AccTypeDlg.EDIT,
                FormAction.UPDATE,
                table.getColumns(),
                "AccType не сохранен",
                "Ошибка изменения AccType",
                "AccType изменен успешно"
        ), SecurityActionCode.ReferAccTypeChng);


        abw.addSecureAction(actionCreate = (GridAction) commonLazyAction("AccTypeDlg",
                AccTypeDlg.CREATE,
                FormAction.CREATE,
                table.getColumns(),
                "AccType не создан",
                "Ошибка создания AccType",
                "AccType создан успешно"
        ), SecurityActionCode.ReferAccTypeChng);

        actionCreate.setAfterRefreshEvent(this);

        abw.addSecureAction(commonLazyAction("AccTypeDlg",
                        AccTypeDlg.DELETE,
                        FormAction.DELETE,
                        table.getColumns(),
                        "AccType не удален",
                        "Ошибка удаления AccType",
                        "AccType удален успешно"),
                SecurityActionCode.ReferAccTypeDel);

        abw.addAction(gotoParams());

        abw.addAction(gotoModifierAction());
        abw.addAction(gotoSectiontAction());
    }

    private GridAction gotoModifierAction() {
        return new GridAction(grid, "Модификатор", "Управление модификаторами", null, 10) {
            @Override
            public void execute() {
                FormManagerUI.show(new AccTypeModifier(initSection, initProduct, initSubProduct));
            }
        };
    }

    private GridAction gotoSectiontAction() {
        return new GridAction(grid, "Разделы", "Управление разделами", null, 10) {
            @Override
            public void execute() {
                FormManagerUI.show(new AccTypeSection());
            }
        };
    }

    private GridAction gotoParams(){
        return new GridAction(grid, null, "Параметры счета AccType", new Image(ImageConstants.INSTANCE.function()), 10, true) {
            @Override
            public void execute() {
                Row row = grid.getCurrentRow();
                String tech_flag = row != null ? (String)row.getField(8).getValue(): null;

                FormManagerUI.show(new ActParm(initSection, initProduct, initSubProduct, initModifier, tech_flag));
            }
        };
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();

        result.addColumn(sectionColumn = new Column("SECTION", Column.Type.STRING, FIELD_SECTION, 15, false, false));
        result.addColumn(productColumn = new Column("PRODUCT", Column.Type.STRING, FIELD_PRODUCT, 17, false, false));
        result.addColumn(subProductColumn = new Column("SUBPRODUCT", Column.Type.STRING, FIELD_SUBPRODUCT, 22, false, false));
        result.addColumn(modifierColumn = new Column("MODIFIER", Column.Type.STRING, FIELD_MODIFIER, 26, false, false));
        result.addColumn(new Column("ACCTYPE", Column.Type.STRING, FIELD_ACCTYPE, 18));
        result.addColumn(new Column("ACCNAME", Column.Type.STRING, FIELD_ACCTYPENAME, 240));
        result.addColumn(new Column("PL_ACT", Column.Type.STRING, FIELD_PL_ACT, 18));
        result.addColumn(new Column("FL_CTRL", Column.Type.STRING, FIELD_FL_CTRL, 18));
        result.addColumn(new Column("TECH_ACT", Column.Type.STRING, FIELD_TECH_ACT, 18));


        return result;
    }

    @Override
    protected String prepareSql() {
        return  "select * from ( " +
                "select left(ACCTYPE, 3) as SECTION, substr(ACCTYPE, 4, 2) as PRODUCT, substr(ACCTYPE, 6, 2) as SUBPRODUCT, " +
                "right(ACCTYPE, 2) as MODIFIER, ACCTYPE, ACCNAME, PL_ACT, FL_CTRL, case when TECH_ACT = 'Y' then 'Y' else 'N' end TECH_ACT from GL_ACTNAME) v";
    }

    @Override
    protected void save(AccTypeWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<AccTypeWrapper>> asyncCallbackImpl) throws Exception {
        BarsGLEntryPoint.dictionaryService.saveAccType(cnw, action, asyncCallbackImpl);
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

        item = new FilterItem(modifierColumn, FilterCriteria.EQ, initModifier, true);
        item.setReadOnly(true);
        list.add(item);

        return list;
    }

    @Override
    public void afterRefresh(int rowCount) {
        if (actionCreate != null) {
            actionCreate.setEnable(rowCount == 0);
        }
    }

}
