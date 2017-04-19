package ru.rbt.barsgl.ejb.entity.etl;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * Created by Ivan Sevastyanov
 */
@Entity
@Table(name = "GL_ETLRATE")
public class EtlCurrencyRate extends BaseEntity<EtlCurrencyRateId> {

    @EmbeddedId
    private EtlCurrencyRateId id;

    @Column(name = "CCY_NAME")
    private String currencyName;

    @Column(name = "CCY_NOM")
    private Integer nominal;

    @Column(name = "MID_RATE")
    private BigDecimal rate;

    @Column(name = "CCY_NUM_CODE")
    private String digitalCode;

    @Override
    public EtlCurrencyRateId getId() {
        return id;
    }

    @Override
    public void setId(EtlCurrencyRateId id) {
        this.id = id;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }

    public Integer getNominal() {
        return nominal;
    }

    public void setNominal(Integer nominal) {
        this.nominal = nominal;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public String getDigitalCode() {
        return digitalCode;
    }

    public void setDigitalCode(String digitalCode) {
        this.digitalCode = digitalCode;
    }
}
