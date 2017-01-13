package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.LinkedBoxes;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.access.UserProductsWrapper;
import ru.rbt.barsgl.shared.dict.AccTypeSourceWrapper;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by akichigi on 29.09.16.
 */
public class LinkAccType2SourceDlg extends DlgFrame implements IAfterShowEvent {
    private TxtBox code;
    private TxtBox ident;
    private LinkedBoxes products;
    private AccTypeSourceWrapper wrapper;

    @Override
    public Widget createContent() {
        Grid grid_code = new Grid(1, 3);

        grid_code.setWidget(0, 0, new Label("AccType"));

        grid_code.setWidget(0, 1, code = new TxtBox());
        code.setWidth("70px");
        code.setReadOnly(true);

        grid_code.setWidget(0, 2, ident = new TxtBox());
        ident.setWidth("518px");
        ident.setReadOnly(true);

        products = new LinkedBoxes("Все источники:", "Выбранные источники:");
        products.setBoxesHeight("150px");
        products.setBoxInWidth("300px");
        products.setBoxOutWidth("300px");

        VerticalPanel panel = new VerticalPanel();
        panel.add(grid_code);
        panel.add(products);

        setAfterShowEvent(this);

        return panel;
    }

    public void clearContent(){
        code.clear();
        ident.clear();
        products.clearBoxIn();
        products.clearBoxOut();
    }

    @Override
    protected void fillContent() {
        clearContent();

        wrapper = (AccTypeSourceWrapper) params;
        code.setValue(wrapper.getAcctype());
        ident.setValue(wrapper.getAcctypeName());

        for(UserProductsWrapper w : wrapper.getProducts()){
            products.addBoxInItem(w.getCode(), w.getCode());
        }

        for(UserProductsWrapper w : wrapper.getGranted_products()){
            products.addBoxOutItem(w.getCode(), w.getCode());
        }

        products.updateButtonState();
    }

    private void setWrapperFields(){
        ArrayList<UserProductsWrapper> productsList = new ArrayList<UserProductsWrapper>();
        UserProductsWrapper productsWrapper;
        for(Serializable v : products.getBoxOutValues()){
            productsWrapper = new UserProductsWrapper();
            productsWrapper.setCode((String) v);
            productsList.add(productsWrapper);
        }
        wrapper.setGranted_products(productsList);
    }

    @Override
    protected boolean onClickOK() throws Exception {
        try {
            setWrapperFields();
            params = wrapper;
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
        products.setSelectedBoxInIndex(0);
        products.setSelectedBoxOutIndex(0);
    }
}
