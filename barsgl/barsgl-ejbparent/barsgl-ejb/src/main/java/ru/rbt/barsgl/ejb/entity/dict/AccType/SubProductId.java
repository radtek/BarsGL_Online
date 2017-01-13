package ru.rbt.barsgl.ejb.entity.dict.AccType;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * Created by akichigi on 23.08.16.
 */

@Embeddable
public class SubProductId implements Serializable {

    @Column(name = "SECTCODE")
    private String sectcode;

    @Column(name = "PRODCODE")
    private String prodcode;

    @Column(name = "SUBPRODCODE")
    private String subprodcode;

    public SubProductId(){}

    public SubProductId(String sectcode, String prodcode, String subprodcode){
        this.sectcode = sectcode;
        this.prodcode = prodcode;
        this.subprodcode = subprodcode;
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

    public String getSubprodcode() {
        return subprodcode;
    }

    public void setSubprodcode(String subprodcode) {
        this.subprodcode = subprodcode;
    }
}
