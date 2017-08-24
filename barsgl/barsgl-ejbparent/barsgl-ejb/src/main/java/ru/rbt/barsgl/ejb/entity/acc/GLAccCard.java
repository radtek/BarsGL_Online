package ru.rbt.barsgl.ejb.entity.acc;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by er18837 on 21.08.2017.
 */
@Entity
@Table(name = "GL_ACCCARD")
public class GLAccCard extends BaseEntity<AccCardId> {

    @EmbeddedId
    AccCardId id;

    @Column(name = "Datto")
    @Temporal(TemporalType.DATE)
    private Date endDate;

    @Column(name = "ACID")
    private String acid;

    @Column(name = "Cbcc")
    private String filial;

    @Column(name = "Cbccn")
    private String companyCode;

    @Column(name = "Branch")
    private String branch;

    @Column(name = "CCY")
    private String ccy;

    @Column(name = "Card")
    private String card;

    @Column(name = "Obac")
    private BigDecimal startBalance;

    @Column(name = "DtCt")
    private BigDecimal turnovers;

    public GLAccCard() {
    }

    public GLAccCard(String bsaAcid, Date startDate) {
        this.id = new AccCardId(bsaAcid, startDate);
    }

    @Override
    public AccCardId getId() {
        return id;
    }

    @Override
    public void setId(AccCardId id) {
        this.id = id;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getAcid() {
        return acid;
    }

    public void setAcid(String acid) {
        this.acid = acid;
    }

    public String getFilial() {
        return filial;
    }

    public void setFilial(String filial) {
        this.filial = filial;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCcy() {
        return ccy;
    }

    public void setCcy(String ccy) {
        this.ccy = ccy;
    }

    public String getCard() {
        return card;
    }

    public void setCard(String card) {
        this.card = card;
    }

    public BigDecimal getStartBalance() {
        return startBalance;
    }

    public void setStartBalance(BigDecimal startBalance) {
        this.startBalance = startBalance;
    }

    public BigDecimal getTurnovers() {
        return turnovers;
    }

    public void setTurnovers(BigDecimal turnovers) {
        this.turnovers = turnovers;
    }

}
