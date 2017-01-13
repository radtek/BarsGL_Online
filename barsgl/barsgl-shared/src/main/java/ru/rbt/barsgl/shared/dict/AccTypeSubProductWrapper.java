package ru.rbt.barsgl.shared.dict;

/**
 * Created by akichigi on 12.08.16.
 */
public class AccTypeSubProductWrapper extends AccTypeProductWrapper {
    private String subProduct;
    private String subProductName;

    public String getSubProduct() {
        return subProduct;
    }

    public void setSubProduct(String subProduct) {
        this.subProduct = subProduct;
    }

    public String getSubProductName() {
        return subProductName;
    }

    public void setSubProductName(String name) {
        subProductName = name;
    }
}
