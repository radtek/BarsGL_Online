package ru.rbt.barsgl.ejb.entity.dict;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by Ivan Sevastyanov
 */
@Entity
@Table(name = "CURRENCY")
public class BankCurrency extends BaseEntity<String> {

    public static final BankCurrency RUB = new BankCurrency("RUR");
    public static final BankCurrency USD = new BankCurrency("USD");
    public static final BankCurrency EUR = new BankCurrency("EUR");
    public static final BankCurrency AUD = new BankCurrency("AUD");
    public static final BankCurrency BGN = new BankCurrency("BGN");
    public static final BankCurrency XAG = new BankCurrency("XAG");

    /**
     * default constructor is required
     */
    public BankCurrency() {}

    public BankCurrency(String currencyCode) {
        this.id = currencyCode;
    }

    @Id
    @Column(name = "GLCCY")
    private String id;

    @Column(name = "CBCCY")
    private String digitalCode;

    @Column(name = "NBDP")
    private Long scale;

    @Override
    public String getId() {
        return id;
    }

    public String getCurrencyCode() {
        return getId();
    }

    public String getDigitalCode() {
        return digitalCode;
    }

    public Long getScale() {
        return scale;
    }

    public void setDigitalCode(String digitalCode) {
        this.digitalCode = digitalCode;
    }

}
