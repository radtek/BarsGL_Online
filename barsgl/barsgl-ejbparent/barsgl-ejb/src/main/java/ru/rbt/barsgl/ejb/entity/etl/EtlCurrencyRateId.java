package ru.rbt.barsgl.ejb.entity.etl;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov
 */
//@AttributeOverrides({
//        @AttributeOverride(name = "rateDt", column = @Column(name = "ON_DATE")),
//        @AttributeOverride(name = "bankCurrency", column = @Column(name = "CCY_ALPHA_CODE"))
//})
@Embeddable
public class EtlCurrencyRateId implements Serializable {

    @Column (name = "ON_DATE")
    @Temporal(TemporalType.DATE)
    private Date rateDt;

    @Column(name = "CCY_ALPHA_CODE")
    private String bankCurrency;

    public EtlCurrencyRateId() {
    }

    public EtlCurrencyRateId(String bankCurrency, Date rateDt) {
        this.bankCurrency = bankCurrency;
        this.rateDt = rateDt;
    }

    public Date getRateDt() {
        return rateDt;
    }

    public void setRateDt(Date rateDt) {
        this.rateDt = rateDt;
    }

    public String getBankCurrency() {
        return bankCurrency;
    }

    public void setBankCurrency(String bankCurrency) {
        this.bankCurrency = bankCurrency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EtlCurrencyRateId that = (EtlCurrencyRateId) o;

        if (!rateDt.equals(that.rateDt)) return false;
        return bankCurrency.equals(that.bankCurrency);

    }

    @Override
    public int hashCode() {
        int result = rateDt.hashCode();
        result = 31 * result + bankCurrency.hashCode();
        return result;
    }
}
