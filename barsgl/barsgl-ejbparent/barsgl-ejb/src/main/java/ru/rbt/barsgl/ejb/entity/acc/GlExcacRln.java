package ru.rbt.barsgl.ejb.entity.acc;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;

/**
 * Created by ER18837 on 28.04.15.
 */
@Entity
@Table(name = "EXCACRLN")
public class GlExcacRln extends BaseEntity<AccRlnId> {

    @EmbeddedId
    private AccRlnId id;

    @Column(name = "CCODE")
    private String companyCode;

    @Column(name = "CCY")
    private String currency;

    @Column(name = "CASH")
    private String cash;

    @Column(name = "PSAV")
    private String passiveActive;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCash() {
        return cash;
    }

    public void setCash(String cash) {
        this.cash = cash;
    }

    public String getPassiveActive() {
        return passiveActive;
    }

    public void setPassiveActive(String passiveActive) {
        this.passiveActive = passiveActive;
    }

    @Override
    public AccRlnId getId() {
        return id;
    }

    @Override
    public void setId(AccRlnId id) {
        this.id = id;
    }
}
