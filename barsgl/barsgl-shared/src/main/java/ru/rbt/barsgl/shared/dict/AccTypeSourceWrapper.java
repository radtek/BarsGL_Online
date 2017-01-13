package ru.rbt.barsgl.shared.dict;

import ru.rbt.barsgl.shared.access.UserProductsWrapper;

import java.util.ArrayList;

/**
 * Created by akichigi on 29.09.16.
 */
public class AccTypeSourceWrapper extends AccTypeWrapper {
    private ArrayList<UserProductsWrapper> products;
    private ArrayList<UserProductsWrapper> granted_products;

    public ArrayList<UserProductsWrapper> getProducts() {
        return products;
    }

    public void setProducts(ArrayList<UserProductsWrapper> products) {
        this.products = products;
    }

    public ArrayList<UserProductsWrapper> getGranted_products() {
        return granted_products;
    }

    public void setGranted_products(ArrayList<UserProductsWrapper> granted_products) {
        this.granted_products = granted_products;
    }
}
