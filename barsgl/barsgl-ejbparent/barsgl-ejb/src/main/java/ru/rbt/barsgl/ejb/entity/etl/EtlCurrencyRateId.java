package ru.rbt.barsgl.ejb.entity.etl;

import ru.rbt.barsgl.ejb.entity.dict.CurrencyRateId;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov
 */
@AttributeOverrides({
        @AttributeOverride(name = "rateDt", column = @Column(name = "ON_DATE")),
        @AttributeOverride(name = "bankCurrency", column = @Column(name = "CCY_ALPHA_CODE"))
})
@Embeddable
public class EtlCurrencyRateId extends CurrencyRateId {

    public EtlCurrencyRateId() {
    }

    public EtlCurrencyRateId(String bankCurrency, Date rateDt) {
        super(bankCurrency, rateDt);
    }
}
