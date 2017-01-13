package ru.rbt.barsgl.shared.dict;

/**
 * Created by akichigi on 12.08.16.
 */
public class AccTypeProductWrapper extends AccTypeSectionWrapper {
    private String product;
    private String productName;

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String name) {
        productName = name;
    }
}
