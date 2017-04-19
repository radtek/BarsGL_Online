package ru.rbt.barsgl.ejb.entity.dict;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov
 */

@Entity
@Table(name = "CURRATES")
public class CurrencyRate extends BaseEntity<CurrencyRateId> {

    @EmbeddedId
    private CurrencyRateId id;

    @Column(name = "RATE")
    private BigDecimal rate;

    @Column(name = "AMNT")
    private BigDecimal amount;

    @Column(name = "RATE0")
    private BigDecimal ratePrev;

    public CurrencyRate() {}

    public CurrencyRate(BankCurrency currency, Date ondate, BigDecimal rate, BigDecimal amount, BigDecimal rate0 ) {
        this(currency, ondate, rate, amount);
        this.ratePrev = rate0;
    }

    public CurrencyRate(BankCurrency currency, Date ondate, BigDecimal rate, BigDecimal amount) {
        this(currency, ondate, rate);
        this.amount = amount;
    }

    public CurrencyRate(BankCurrency currency, Date ondate, BigDecimal rate) {
        id = new CurrencyRateId(currency.getCurrencyCode(), ondate);
        this.rate = rate;
    }

    public BigDecimal getRatePrev() {
        return ratePrev;
    }

    public void setRatePrev(BigDecimal ratePrev) {
        this.ratePrev = ratePrev;
    }

    @Override
    public CurrencyRateId getId() {
        return id;
    }

    public BigDecimal getRate() {
        return rate;
    }

    @Override
    public void setId(CurrencyRateId id) {
        this.id = id;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
