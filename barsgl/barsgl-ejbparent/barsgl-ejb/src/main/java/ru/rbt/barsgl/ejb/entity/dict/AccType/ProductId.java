package ru.rbt.barsgl.ejb.entity.dict.AccType;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Created by akichigi on 22.08.16.
 */

@Embeddable
public class ProductId implements Serializable {

    @Column(name = "SECTCODE")
    private String sectcode;

    @Column(name = "PRODCODE")
    private String prodcode;

    public ProductId(){}

    public ProductId(String sectcode, String prodcode){
        this.sectcode = sectcode;
        this.prodcode = prodcode;
    }

    public String getSectcode() {
        return sectcode;
    }

    public void setSectcode(String sectcode) {
        this.sectcode = sectcode;
    }

    public String getProdcode() {
        return prodcode;
    }

    public void setProdcode(String prodcode) {
        this.prodcode = prodcode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductId productId = (ProductId) o;
        return Objects.equals(sectcode, productId.sectcode) &&
                Objects.equals(prodcode, productId.prodcode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sectcode, prodcode);
    }
}
