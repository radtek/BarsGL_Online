package ru.rbt.barsgl.ejb.entity.dict;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov
 */
@Embeddable
@MappedSuperclass
public class CurrencyRateId  implements Serializable {

    @Column (name = "DAT")
    @Temporal(TemporalType.DATE)
    private Date rateDt;

    @Column(name = "CCY")
    private String bankCurrency;

    public CurrencyRateId() {
    }

    public CurrencyRateId(String bankCurrency, Date rateDt) {
        this.rateDt = rateDt;
        this.bankCurrency = bankCurrency;
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

        CurrencyRateId that = (CurrencyRateId) o;

        if (!bankCurrency.equals(that.bankCurrency)) return false;
        if (!rateDt.equals(that.rateDt)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = rateDt.hashCode();
        result = 31 * result + bankCurrency.hashCode();
        return result;
    }
}
