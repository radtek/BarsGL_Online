package ru.rbt.barsgl.ejb.entity.flx;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by Ivan Sevastyanov on 23.11.2016.
 * Справочник транзитных счетов для отвода НДС
 */
@Entity
@Table(name = "GL_ACCNDS")
public class TransitNdsReference extends BaseEntity<String> {

    public TransitNdsReference() {
    }

    public TransitNdsReference(String transitAccount, String profitAccount, String ndsAccount, String evtp) {
        this.transitAccount = transitAccount;
        this.profitAccount = profitAccount;
        this.ndsAccount = ndsAccount;
        this.evtp = evtp;
    }

    @Id
    @Column(name = "TR_ACC")
    private String transitAccount;

    @Column(name = "COM_ACC")
    private String profitAccount;

    @Column(name = "NDS_ACC")
    private String ndsAccount;

    @Column(name = "EVTP")
    private String evtp;

    @Override
    public String getId() {
        return transitAccount;
    }

    public String getTransitAccount() {
        return transitAccount;
    }

    public void setTransitAccount(String transitAccount) {
        this.transitAccount = transitAccount;
    }

    public String getProfitAccount() {
        return profitAccount;
    }

    public void setProfitAccount(String profitAccount) {
        this.profitAccount = profitAccount;
    }

    public String getNdsAccount() {
        return ndsAccount;
    }

    public void setNdsAccount(String ndsAccount) {
        this.ndsAccount = ndsAccount;
    }

    public String getEvtp() {
        return evtp;
    }

    public void setEvtp(String evtp) {
        this.evtp = evtp;
    }
}
