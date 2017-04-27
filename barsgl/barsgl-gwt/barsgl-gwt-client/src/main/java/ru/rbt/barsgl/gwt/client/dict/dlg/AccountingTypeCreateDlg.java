package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.*;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.client.dict.*;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.*;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.gwt.core.ui.BtnTxtBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.AccTypeWrapper;
import ru.rbt.barsgl.shared.enums.AccTypeParts;
import ru.rbt.barsgl.shared.enums.BoolType;

import java.util.HashMap;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by akichigi on 03.10.16.
 */
public class AccountingTypeCreateDlg extends DlgFrame implements IAfterShowEvent {
    private final static String OK_CAPTION = "Выбрать";
    private BtnTxtBox section;
    private AreaBox sectionName;
    private PushButton sectionBtn;
    private BtnTxtBox product;
    private AreaBox productName;
    private PushButton productBtn;
    private BtnTxtBox subproduct;
    private AreaBox subproductName;
    private PushButton subproductBtn;
    private BtnTxtBox modifier;
    private AreaBox modifierName;
    private PushButton modifierBtn;
    private TxtBox acctype;
    private AreaBox accname;
    private CheckBox pl_act;
    private CheckBox fl_ctrl;
    private Button checkBtn;

    @Override
    public Widget createContent() {
        setAfterShowEvent(this);

        btnPanel.insert(checkBtn = new Button("Проверить"), 0);
        checkBtn.addStyleName("dlg-button");
        checkBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                try{
                    checkUp(true);
                }catch(Exception ex) {
                    DialogManager.error("Ошибка", ex.getMessage());
                }
            }
        });

        Grid grid = new Grid(4,4);
        grid.setWidget(0, 0, new Label("Раздел"));
        //Section
        section = new BtnTxtBox(){
            @Override
            public void onBntClick(){
                lookUpSection();
            }
        };
        section.setMaxLength(3);
        section.setMask("[0-9]");        

        section.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent keyDownEvent) {
                clearFields(AccTypeParts.section);
            }
        });

        grid.setWidget(0, 1, section);

        sectionBtn = new PushButton(new Image(ImageConstants.INSTANCE.new16()));
        sectionBtn.setTitle("Создание раздела");
        sectionBtn.setWidth("16px");
        sectionBtn.setHeight("16px");
        sectionBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                createSection();
            }
        });
        grid.setWidget(0, 2, sectionBtn);

        grid.setWidget(0, 3, sectionName = new AreaBox());
        sectionName.setHeight("50px");
        sectionName.setWidth("370px");

        //Product
        grid.setWidget(1, 0, new Label("Продукт"));
        product = new BtnTxtBox(){
            @Override
            public void onBntClick(){
                lookUpProduct();
            }
        };
        product.setMaxLength(2);
        product.setMask("[0-9]");
        product.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent keyDownEvent) {
                clearFields(AccTypeParts.product);
            }
        });

        grid.setWidget(1, 1, product);

        productBtn = new PushButton(new Image(ImageConstants.INSTANCE.new16()));
        productBtn.setTitle("Создание продукта");
        productBtn.setWidth("16px");
        productBtn.setHeight("16px");
        productBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                createProduct();
            }
        });
        grid.setWidget(1, 2, productBtn);

        grid.setWidget(1, 3, productName = new AreaBox());
        productName.setHeight("50px");
        productName.setWidth("370px");

        //Subproduct
        grid.setWidget(2, 0, new Label("Подпродукт"));
        subproduct = new BtnTxtBox(){
            @Override
            public void onBntClick(){
                lookUpSubProduct();
            }
        };

        subproduct.setMaxLength(2);
        subproduct.setMask("[0-9]");
        subproduct.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent keyDownEvent) {
                clearFields(AccTypeParts.subproduct);
            }
        });

        grid.setWidget(2, 1, subproduct);

        subproductBtn = new PushButton(new Image(ImageConstants.INSTANCE.new16()));
        subproductBtn.setTitle("Создание подпродукта");
        subproductBtn.setWidth("16px");
        subproductBtn.setHeight("16px");
        subproductBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                createSubProduct();
            }
        });
        grid.setWidget(2, 2, subproductBtn);

        grid.setWidget(2, 3, subproductName = new AreaBox());
        subproductName.setHeight("50px");
        subproductName.setWidth("370px");

        //Modifier
        grid.setWidget(3, 0, new Label("Модификатор"));
        modifier = new BtnTxtBox(){
            @Override
            public void onBntClick(){
                lookUpModifier();
            }
        };
        modifier.setMaxLength(2);
        modifier.setMask("[0-9]");
        modifier.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent keyDownEvent) {
                clearFields(AccTypeParts.modifier);
            }
        });

        grid.setWidget(3, 1, modifier);

        modifierBtn = new PushButton(new Image(ImageConstants.INSTANCE.new16()));
        modifierBtn.setTitle("Создание модификатора");
        modifierBtn.setWidth("16px");
        modifierBtn.setHeight("16px");
        modifierBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                createModifier();
            }
        });
        grid.setWidget(3, 2, modifierBtn);

        grid.setWidget(3, 3, modifierName = new AreaBox());
        modifierName.setHeight("50px");
        modifierName.setWidth("370px");

        //AccType
        Grid grid2 = new Grid(1,4);
        grid2.getElement().getStyle().setMarginTop(15, Style.Unit.PX);
        grid2.setWidget(0, 0, new Label("AccountingType"));
        grid2.setWidget(0, 1, acctype = new TxtBox());
        acctype.setMaxLength(9);
        acctype.setMask("[0-9]");
        acctype.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent keyDownEvent) {
                section.clear();
                sectionName.clear();
                product.clear();
                productName.clear();
                subproduct.clear();
                subproductName.clear();
                modifier.clear();
                modifierName.clear();
            }
        });

        acctype.setWidth("70px");

        grid2.setWidget(0, 2, pl_act = new CheckBox("Доходно-расходный счет BARSGL"));
        grid2.setWidget(0, 3, fl_ctrl = new CheckBox("Контролируемый счет"));

        Grid grid3 = new Grid(1, 2);
        grid3.setWidget(0, 0, new Label("Наименование счета"));
        grid3.setWidget(0, 1, accname = new AreaBox());
        accname.setHeight("50px");
        accname.setWidth("475px");

        VerticalPanel panel = new VerticalPanel();
        panel.add(grid);
        panel.add(grid2);
        panel.add(grid3);

        return panel;
    }

    private void makeAccType(){
        String val = (section.getValue() == null ? "" : section.getValue()) +
                     (product.getValue() == null ? "" : product.getValue()) +
                     (subproduct.getValue() == null ? "" : subproduct.getValue()) +
                     (modifier.getValue() == null ? "" : modifier.getValue());
        acctype.setValue(val);
    }

    private void makeParts(){
        String a = acctype.getValue();
        section.setValue(a.substring(0, 3));
        product.setValue(a.substring(3, 5));
        subproduct.setValue(a.substring(5, 7));
        modifier.setValue(a.substring(7, 9));
    }

    private String checkLength(String value, int len, String fieldName) throws Exception {
        if (value == null || value.trim().length() != len)
            throw new Exception( Utils.Fmt("Требуемое количество символов в поле {0} должно быть {1}", fieldName, len));

        return value;
    }

    private void checkAccType(boolean checkName) throws Exception {
        checkLength(acctype.getValue(), 9, "AccountingType");

        if (!checkName) return;
        if (accname.getValue() == null || accname.getValue().trim().length() == 0)
            throw  new Exception("Незаполнено поле Наименование счета");
        if (accname.getValue().length() > 255) throw new Exception("Количество символов превышает 255");
    }

    private void checkAccParts() throws Exception {
        checkLength(section.getValue(), 3, "Раздел");
        int val = Integer.parseInt(section.getValue());
        if (val < 1) throw new Exception("Некорректное значение в поле Раздел");

        checkLength(product.getValue(), 2, "Продукт");
        val = Integer.parseInt(product.getValue());
        if (val < 1) throw new Exception("Некорректное значение в поле Продукт");

        checkLength(subproduct.getValue(), 2, "Подпродукт");
        val = Integer.parseInt(subproduct.getValue());
        if (val < 0) throw new Exception("Некорректное значение в поле Подпродукт");

        checkLength(modifier.getValue(), 2, "Модификатор");
        val = Integer.parseInt(modifier.getValue());
        if (val < 0) throw new Exception("Некорректное значение в поле Модификатор");
    }

    public void checkAccTypeAndPartsInDB(final Object prms) throws Exception {
        sectionName.setReadOnly(true);
        sectionBtn.setEnabled(false);

        productName.setReadOnly(true);
        productBtn.setEnabled(false);

        subproductName.setReadOnly(true);
        subproductBtn.setEnabled(false);

        modifierName.setReadOnly(true);
        modifierBtn.setEnabled(false);

        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Check());

        AccTypeWrapper atw = getWrapper();
        BarsGLEntryPoint.dictionaryService.checkAccType(atw, new AuthCheckAsyncCallback<RpcRes_Base<AccTypeWrapper>>() {

            @Override
            public void onSuccess(RpcRes_Base<AccTypeWrapper> res) {
                AccTypeWrapper wrapper = res.getResult();
                if (res.isError()) {
                    if (wrapper.getSection() == null){
                        sectionName.setReadOnly(false);
                        sectionBtn.setEnabled(true);
                    }
                    if (wrapper.getProduct() == null){
                        productName.setReadOnly(false);
                        productBtn.setEnabled(true);
                    }
                    if (wrapper.getSubProduct() == null){
                        subproductName.setReadOnly(false);
                        subproductBtn.setEnabled(true);
                    }
                    if (wrapper.getModifier() == null){
                        modifierName.setReadOnly(false);
                        modifierBtn.setEnabled(true);
                    }

                    sectionName.setValue(wrapper.getSectionName());
                    productName.setValue(wrapper.getProductName());
                    subproductName.setValue(wrapper.getSubProductName());
                    modifierName.setValue(wrapper.getModifierName());

                    DialogManager.error("Ошибка", "Ошибка проверки.\n " + res.getMessage());
                } else {
                    ok.setEnabled(true);
                    sectionName.setValue(wrapper.getSectionName());
                    productName.setValue(wrapper.getProductName());
                    subproductName.setValue(wrapper.getSubProductName());
                    modifierName.setValue(wrapper.getModifierName());
                    if (prms != null) {additionalCheck(prms);
                    }else {
                        DialogManager.message("Проверка", "Проверено успешно.");
                    }
                }
                WaitingManager.hide();
            }
        });
    }

    protected void additionalCheck(Object prms) {

    }

    private void checkUp(boolean dbcheck) throws Exception {
        if ((acctype.getValue() == null || acctype.getValue().isEmpty()) &&
            (section.getValue() == null || section.getValue().isEmpty()) &&
            (product.getValue() == null || product.getValue().isEmpty()) &&
            (subproduct.getValue() == null || subproduct.getValue().isEmpty()) &&
            (modifier.getValue() == null || modifier.getValue().isEmpty()))
        {

            throw new Exception("Нет данных для формирования значения AccountingType");
        }

        if (acctype.getValue() != null && !acctype.getValue().isEmpty()) {
            //check acctype
            checkAccType(!dbcheck);
            makeParts();
            checkAccParts();
            if (dbcheck) checkAccTypeAndPartsInDB(null);
        }else
        {
            //check parts
            checkAccParts();
            makeAccType();
            checkAccType(!dbcheck);
            if (dbcheck) checkAccTypeAndPartsInDB(null);
        }
    }

    private void clearFields(AccTypeParts part){
        switch (part){
            case section:
                sectionName.clear();
                product.clear();
                productName.clear();
                subproduct.clear();
                subproductName.clear();
                modifier.clear();
                modifierName.clear();
                acctype.clear();
                break;
            case product:
                productName.clear();
                subproduct.clear();
                subproductName.clear();
                modifier.clear();
                modifierName.clear();
                acctype.clear();
                break;
            case subproduct:
                subproductName.clear();
                modifier.clear();
                modifierName.clear();
                acctype.clear();
                break;
            case modifier:
                modifierName.clear();
                acctype.clear();
                break;
        }
    }

    public boolean parseIntValue(String value, int threshold){
        try{
               int val = Integer.parseInt(value);
               if (val < threshold) return false;
               return true;
        }catch (Exception ex) {
            return false;
        }
    }

    private void internalPartsCreate(final AccTypeParts parts, final PushButton createBtn, final AreaBox partName){
        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Check());

        AccTypeWrapper atw = getWrapper();
        try {
            BarsGLEntryPoint.dictionaryService.createAccTypeParts(atw, parts , new AuthCheckAsyncCallback<RpcRes_Base<AccTypeWrapper>>(){

                @Override
                public void onSuccess(RpcRes_Base<AccTypeWrapper> res) {
                    if (res.isError()) {
                        partName.setReadOnly(false);
                        createBtn.setEnabled(true);
                        DialogManager.error("Ошибка", "Ошибка проверки.\n" + res.getMessage());
                    } else {
                        partName.setReadOnly(true);
                        createBtn.setEnabled(false);
                        showInfo("Информация", res.getMessage());
                    }
                    WaitingManager.hide();
                }
            });
        } catch (Exception e) {
            DialogManager.error("Ошибка", e.getMessage());
        }
    }

    private void createSection(){
        String err = "";
        if (section.getValue() == null || section.getValue().length() != 3 ||
                !parseIntValue(section.getValue(), 1)){
            err += "Некорректное значение в поле Раздел.\n";
        }

        if (sectionName.getValue() == null || (sectionName.getValue().length() == 0) ||
                (sectionName.getValue().length() > 255)){
            err += "Некорректное значение в наименовании раздела.";
        }

        if (!err.trim().isEmpty()) {
            DialogManager.error("Ошибка", err);
            return;
        }

        internalPartsCreate(AccTypeParts.section, sectionBtn, sectionName);
    }

    private void createProduct(){
        String err = "";
        if (section.getValue() == null || section.getValue().length() != 3 ||
                !parseIntValue(section.getValue(), 1)){
            err += "Некорректное значение в поле Раздел.\n";
        }

        if (product.getValue() == null || product.getValue().length() != 2 ||
                !parseIntValue(product.getValue(), 1)){
            err += "Некорректное значение в поле Продукт.\n";
        }

        if (productName.getValue() == null || (productName.getValue().length() == 0) ||
                (productName.getValue().length() > 255)){
            err += "Некорректное значение в наименовании продукта.";
        }

        if (!err.trim().isEmpty()) {
            DialogManager.error("Ошибка", err);
            return;
        }

        internalPartsCreate(AccTypeParts.product, productBtn, productName);
    }

    private void createSubProduct(){
        String err = "";
        if (section.getValue() == null || section.getValue().length() != 3 ||
                !parseIntValue(section.getValue(), 1)){
            err += "Некорректное значение в поле Раздел.\n";
        }

        if (product.getValue() == null || product.getValue().length() != 2 ||
                !parseIntValue(product.getValue(), 1)){
            err += "Некорректное значение в поле Продукт.\n";
        }

        if (subproduct.getValue() == null || subproduct.getValue().length() != 2 ||
                !parseIntValue(subproduct.getValue(), 0)){
            err += "Некорректное значение в поле Подпродукт.\n";
        }

        if (subproductName.getValue() == null || (subproductName.getValue().length() == 0) ||
                (subproductName.getValue().length() > 255)){
            err += "Некорректное значение в наименовании подпродукта.";
        }

        if (!err.trim().isEmpty()) {
            DialogManager.error("Ошибка", err);
            return;
        }

        internalPartsCreate(AccTypeParts.subproduct, subproductBtn, subproductName);
    }

    private void createModifier(){
        String err = "";
        if (section.getValue() == null || section.getValue().length() != 3 ||
                !parseIntValue(section.getValue(), 1)){
            err += "Некорректное значение в поле Раздел.\n";
        }

        if (product.getValue() == null || product.getValue().length() != 2 ||
                !parseIntValue(product.getValue(), 1)){
            err += "Некорректное значение в поле Продукт.\n";
        }

        if (subproduct.getValue() == null || subproduct.getValue().length() != 2 ||
                !parseIntValue(subproduct.getValue(), 0)){
            err += "Некорректное значение в поле Подпродукт.\n";
        }

        if (modifier.getValue() == null || modifier.getValue().length() != 2 ||
                !parseIntValue(modifier.getValue(), 0)){
            err += "Некорректное значение в поле Модификатор.\n";
        }

        if (modifierName.getValue() == null || (modifierName.getValue().length() == 0) ||
                (modifierName.getValue().length() > 255)){
            err += "Некорректное значение в наименовании модификатора.";
        }

        if (!err.trim().isEmpty()) {
            DialogManager.error("Ошибка", err);
            return;
        }

        internalPartsCreate(AccTypeParts.modifier, modifierBtn, modifierName);
    }


    private void lookUpSection(){
        GridFormDlgBase dlg = new GridFormDlgBase("Справочник разделов AccountingType") {
            @Override
            protected GridForm getGridForm() {
                return new AccTypeSection(){
                    @Override
                    protected void reconfigure() {
                        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
                    }

                    @Override
                    protected Table prepareTable() {
                        Table result = new Table();

                        result.addColumn(new Column("sectcode", Column.Type.STRING, AccTypeSection.FIELD_SECTION, 25));
                        result.addColumn(new Column("name", Column.Type.STRING, AccTypeSection.FIELD_NAME, 250));

                        return result;
                    }
                };
            }

            @Override
            protected String getGridWidth() {
                return "700px";
            }

            @Override
            protected boolean setResultList(HashMap<String, Object> result) {
                if (null != result) {
                    clearFields(AccTypeParts.section);
                    section.setValue(Utils.value((String) result.get("sectcode")));
                    sectionName.setValue((String) result.get("name"));
                    product.setFocus(true);
                }
                return true;
            }

            @Override
            protected Object[] getInitialFilterParams() {
                return null;
            }
        };
        dlg.setOkButtonCaption(OK_CAPTION);
        dlg.show();
    }

    private void lookUpProduct(){
        if (section.getValue() == null || section.getValue().trim().length() != 3) return;

        GridFormDlgBase dlg = new GridFormDlgBase("Справочник продуктов AccountingType") {
            @Override
            protected GridForm getGridForm() {
                return new AccTypeProduct(section.getValue()){
                    @Override
                    protected void reconfigure() {
                        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
                    }

                   @Override
                    protected Table prepareTable() {
                        Table result = new Table();

                        result.addColumn(sectionColumn = new Column("sectcode", Column.Type.STRING, FIELD_SECTION, 27));
                        result.addColumn(new Column("prodcode", Column.Type.STRING, FIELD_PRODUCT, 30));
                        result.addColumn(new Column("name", Column.Type.STRING, FIELD_NAME, 240));

                        return result;
                    }
                };
            }

            @Override
            protected String getGridWidth() {
                return "700px";
            }

            @Override
            protected boolean setResultList(HashMap<String, Object> result) {
                if (null != result) {
                    clearFields(AccTypeParts.product);
                    product.setValue(Utils.value((String) result.get("prodcode")));
                    productName.setValue((String)result.get("name"));
                    subproduct.setFocus(true);
                }
                return true;
            }

            @Override
            protected Object[] getInitialFilterParams() {
                return null;
            }
        };
        dlg.setOkButtonCaption(OK_CAPTION);
        dlg.show();
    }

    private void lookUpSubProduct(){
        if ((section.getValue() == null || section.getValue().trim().length() != 3) ||
            (product.getValue() == null || product.getValue().trim().length() != 2)) return;

        GridFormDlgBase dlg = new GridFormDlgBase("Справочник подпродуктов AccountingType") {
            @Override
            protected GridForm getGridForm() {
                return new AccTypeSubProduct(section.getValue(), product.getValue()){
                    @Override
                    protected void reconfigure() {
                        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
                    }

                    @Override
                    protected Table prepareTable() {
                        Table result = new Table();

                        result.addColumn(sectionColumn = new Column("sectcode", Column.Type.STRING, FIELD_SECTION, 27));
                        result.addColumn(productColumn = new Column("prodcode", Column.Type.STRING, FIELD_PRODUCT, 30));
                        result.addColumn(new Column("subprodcode", Column.Type.STRING, FIELD_SUBPRODUCT, 42));
                        result.addColumn(new Column("name", Column.Type.STRING, "Наименование", 240));

                        return result;
                    }
                };
            }

            @Override
            protected String getGridWidth() {
                return "750px";
            }

            @Override
            protected boolean setResultList(HashMap<String, Object> result) {
                if (null != result) {
                    clearFields(AccTypeParts.subproduct);
                    subproduct.setValue(Utils.value((String) result.get("subprodcode")));
                    subproductName.setValue((String)result.get("name"));
                    modifier.setFocus(true);
                }
                return true;
            }

            @Override
            protected Object[] getInitialFilterParams() {
                return null;
            }
        };
        dlg.setOkButtonCaption(OK_CAPTION);
        dlg.show();
    }

    private void lookUpModifier(){
        if ((section.getValue() == null || section.getValue().trim().length() != 3) ||
            (product.getValue() == null || product.getValue().trim().length() != 2) ||
            (subproduct.getValue() == null || subproduct.getValue().trim().length() != 2)) return;

        GridFormDlgBase dlg = new GridFormDlgBase("Справочник модификаторов AccountingType") {
            @Override
            protected GridForm getGridForm() {
                return new AccTypeModifier(section.getValue(), product.getValue(), subproduct.getValue()){
                    @Override
                    protected void reconfigure() {
                        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
                    }

                    @Override
                    protected Table prepareTable() {
                        Table result = new Table();

                        result.addColumn(sectionColumn = new Column("sectcode", Column.Type.STRING, FIELD_SECTION, 30));
                        result.addColumn(productColumn = new Column("prodcode", Column.Type.STRING, FIELD_PRODUCT, 34));
                        result.addColumn(subProductColumn = new Column("subprodcode", Column.Type.STRING, FIELD_SUBPRODUCT, 46));
                        result.addColumn(new Column("modifcode", Column.Type.STRING, FIELD_MODIFIER, 52));
                        result.addColumn(new Column("name", Column.Type.STRING, FIELD_MODIFNAME, 240));

                        return result;
                    }
                };
            }

            @Override
            protected String getGridWidth() {
                return "800px";
            }

            @Override
            protected boolean setResultList(HashMap<String, Object> result) {
                if (null != result) {
                    clearFields(AccTypeParts.modifier);
                    modifier.setValue(Utils.value((String) result.get("modifcode")));
                    modifierName.setValue((String)result.get("name"));
                    accname.setFocus(true);
                }
                return true;
            }

            @Override
            protected Object[] getInitialFilterParams() {
                return null;
            }
        };
        dlg.setOkButtonCaption(OK_CAPTION);
        dlg.show();
    }


    public void clearContent(){
        ok.setEnabled(false);
        section.clear();
        sectionName.clear();
        sectionName.setReadOnly(true);
        sectionBtn.setEnabled(false);

        product.clear();
        productName.clear();
        productName.setReadOnly(true);
        productBtn.setEnabled(false);

        subproduct.clear();
        subproductName.clear();
        subproductName.setReadOnly(true);
        subproductBtn.setEnabled(false);

        modifier.clear();
        modifierName.clear();
        modifierName.setReadOnly(true);
        modifierBtn.setEnabled(false);

        acctype.clear();
        accname.clear();
        pl_act.setValue(false);
        fl_ctrl.setValue(false);
    }

    private AccTypeWrapper getWrapper(){
        AccTypeWrapper wrapper = new AccTypeWrapper();
        wrapper.setSection(section.getValue());
        wrapper.setSectionName(sectionName.getValue());
        wrapper.setProduct(product.getValue());
        wrapper.setProductName(productName.getValue());
        wrapper.setSubProduct(subproduct.getValue());
        wrapper.setSubProductName(subproductName.getValue());
        wrapper.setModifier(modifier.getValue());
        wrapper.setModifierName(modifierName.getValue());
        wrapper.setAcctype(acctype.getValue());
        wrapper.setAcctypeName(accname.getValue());
        wrapper.setFl_ctrl(fl_ctrl.getValue() ? BoolType.Y : BoolType.N);
        wrapper.setPl_act(pl_act.getValue() ? BoolType.Y : BoolType.N);

        return wrapper;
    }

    @Override
    protected void fillContent() {
        clearContent();
    }

    @Override
    protected boolean onClickOK() throws Exception {
        try {
            checkUp(false);
            params = getWrapper();
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
       section.setFocus(true);
    }
}
