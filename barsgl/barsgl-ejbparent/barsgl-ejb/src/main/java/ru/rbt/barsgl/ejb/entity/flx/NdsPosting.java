package ru.rbt.barsgl.ejb.entity.flx;

import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by Ivan Sevastyanov on 23.11.2016.
 */
@Entity
@Table(name = "GL_NDSOPR")
public class NdsPosting extends BaseEntity <Long> {

    @Id
    @Column(name = "IDPD")
    private Long id;

    @Column(name = "TR_ACC")
    private String transitAccount;

    @Column(name = "AMOUNT")
    private Long amount;

    @Column(name = "DOCN")
    private String docNumber;

    @Column(name = "NRT")
    private String narrative;

    @Column(name = "RNRTL")
    private String narrativeRU;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getTransitAccount() {
        return transitAccount;
    }

    public void setTransitAccount(String transitAccount) {
        this.transitAccount = transitAccount;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getDocNumber() {
        return docNumber;
    }

    public void setDocNumber(String docNumber) {
        this.docNumber = docNumber;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }

    public String getNarrativeRU() {
        return narrativeRU;
    }

    public void setNarrativeRU(String narrativeRU) {
        this.narrativeRU = narrativeRU;
    }
}
