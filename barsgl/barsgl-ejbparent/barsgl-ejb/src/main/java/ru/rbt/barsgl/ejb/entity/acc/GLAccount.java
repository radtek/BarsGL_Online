package ru.rbt.barsgl.ejb.entity.acc;

import org.apache.commons.lang3.time.DateUtils;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.shared.Assert;

import javax.persistence.*;
import java.text.ParseException;
import java.util.Date;

/**
 * Created by ER18837 on 29.04.15.
 */
@Entity
@Table(name = "GL_ACC")
public class GLAccount extends BaseEntity<Long> {

    public enum OpenType {AENEW, AEMID, MNL, SRV};

    public enum RelationType {
        TWO("2"), FOUR("4"), ZERO("0"), FIVE("5"), E("E");

        private String value;

        RelationType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static RelationType parse(String value) {
            for (RelationType type : values()) {
                if (type.getValue().equals(value)) return type;
            }
            throw new IllegalArgumentException(value);
        }
    }

    public GLAccount(){}

    public GLAccount(Long Id){
        id = Id;
    }

    @Id
    @Column(name = "ID")
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "BSAACID")
    private String bsaAcid;

    // --- Основные ключи ---

    @Column(name = "BRANCH")
    private String branch;

    @ManyToOne
    @JoinColumn(name = "CCY")
    private BankCurrency currency;

    @Column(name = "CUSTNO")
    private String customerNumber;

    @Transient
    private Integer customerNumberD;

    @Column(name = "ACCTYPE")
    private Long accountType;

    @Column(name = "CBCUSTTYPE")
    private Short cbCustomerType;

    @Column(name = "TERM")
    private Short term;

    @Column(name = "GL_SEQ")
    private String glSequence;

    // --- Дополнительные ключи ---

    @Column(name = "CBCCN")
    private String companyCode;

    @Column(name = "ACC2")
    private String balanceAccount2;

    @Column(name = "PLCODE")
    private String plCode;

    @Column(name = "ACOD")
    private Short accountCode;

    @Column(name = "SQ")
    private Short accountSequence;

    @Column(name = "DEALSRS")
    private String dealSource;

    @Column(name = "DEALID")
    private String dealId;

    @Column(name = "SUBDEALID")
    private String subDealId;

    // --- Вычисляемые поля  ---

    @Column(name = "CBCC")
    private String filial;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "ACID")
    private String acid;

    @Column(name = "PSAV")
    private String passiveActive;

    @Column(name = "OPENTYPE")
    private String openType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GLOID")
    private GLOperation operation;

    @Enumerated(EnumType.STRING)
    @Column(name = "GLO_DC")
    private GLOperation.OperSide operSide;

    @Column(name = "DTO")
    @Temporal(TemporalType.DATE)
    private Date dateOpen;

    @Column(name = "DTC")
    @Temporal(TemporalType.DATE)
    private Date dateClose;

    @Column(name = "DTR")
    @Temporal(TemporalType.DATE)
    private Date dateRegister;

    @Column(name = "DTM")
    @Temporal(TemporalType.DATE)
    private Date dateModify;

    @Column(name = "RLNTYPE")
    private String relationType;

    // -----------------------------------

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getBsaAcid() {
        return bsaAcid;
    }

    public void setBsaAcid(String bsaAcid) {
        this.bsaAcid = bsaAcid;
    }

    public long getAccountType() {
        return accountType;
    }

    public void setAccountType(long accountType) {
        this.accountType = accountType;
    }

    public BankCurrency getCurrency() {
        return currency;
    }

    public void setCurrency(BankCurrency currency) {
        this.currency = currency;
    }

    public Date getDateOpen() {
        return dateOpen;
    }

    public void setDateOpen(Date dateOpen) {
        this.dateOpen = dateOpen;
    }

    public Date getDateClose() {
        return dateClose;
    }

    public void setDateClose(Date dateClose) {
        this.dateClose = dateClose;
    }

    public String getFilial() {
        return filial;
    }

    public void setFilial(String filial) {
        this.filial = filial;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getPlCode() {
        return plCode;
    }

    public void setPlCode(String plCode) {
        this.plCode = plCode;
    }

    public Short getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(Short accountCode) {
        this.accountCode = accountCode;
    }

    public Short getAccountSequence() {
        return accountSequence;
    }

    public void setAccountSequence(Short accountSequence) {
        this.accountSequence = accountSequence;
    }

    public String getAcid() {
        return acid;
    }

    public void setAcid(String acid) {
        this.acid = acid;
    }

    public String getDescription() {
        return description;
    }

    public String getPassiveActive() {
        return passiveActive;
    }

    public void setPassiveActive(String passiveActive) {
        this.passiveActive = passiveActive;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRelationType() {
        return this.relationType;
    }

    public RelationType getRelationTypeEnum() {
        return null != this.relationType ? RelationType.parse(relationType) : null;
    }

    public void setRelationType(RelationType relationType) {
        Assert.notNull(relationType);
        this.relationType = relationType.getValue();
    }

    public String getGlSequence() {
        return glSequence;
    }

    public void setGlSequence(String glSequence) {
        this.glSequence = glSequence;
    }

    public String getBalanceAccount2() {
        return balanceAccount2;
    }

    public void setBalanceAccount2(String balanceAccount2) {
        this.balanceAccount2 = balanceAccount2;
    }

    public String getDealSource() {
        return dealSource;
    }

    public void setDealSource(String dealSource) {
        this.dealSource = dealSource;
    }

    public String getDealId() {
        return dealId;
    }

    public void setDealId(String dealId) {
        this.dealId = dealId;
    }

    public String getSubDealId() {
        return subDealId;
    }

    public void setSubdealId(String subDealId) {
        this.subDealId = subDealId;
    }

    public String getOpenType() {
        return openType;
    }

    public void setOpenType(String openType) {
        this.openType = openType;
    }

    public GLOperation getOperation() {
        return operation;
    }

    public void setOperation(GLOperation operation) {
        this.operation = operation;
    }

    public void setSubDealId(String subDealId) {
        this.subDealId = subDealId;
    }

    public GLOperation.OperSide getOperSide() {
        return operSide;
    }

    public void setOperSide(GLOperation.OperSide operSide) {
        this.operSide = operSide;
    }

    public Date getDateRegister() {
        return dateRegister;
    }

    public void setDateRegister(Date dateRegister) {
        this.dateRegister = dateRegister;
    }

    public Date getDateModify() {
        return dateModify;
    }

    public void setDateModify(Date dateModify) {
        this.dateModify = dateModify;
    }

    public Date getDateCloseNotNull() {
        return (null != dateClose) ? dateClose : getDateLast();
    }

    public void setAccountType(Long accountType) {
        this.accountType = accountType;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public Short getCbCustomerType() {
        return cbCustomerType;
    }

    public void setCbCustomerType(Short cbCustomerType) {
        this.cbCustomerType = cbCustomerType;
    }

    public Short getTerm() {
        return term;
    }

    public void setTerm(Short term) {
        this.term = term;
    }

    public Integer getCustomerNumberD() {
        return customerNumberD;
    }

    public void setCustomerNumberD(Integer customerNumberD) {
        this.customerNumberD = customerNumberD;
    }

    public Date getDateLast() {
        try {
            return DateUtils .parseDate("2029-01-01", "yyyy-MM-dd");
        } catch (ParseException e) {
            // TODO сделать константу ?
            e.printStackTrace();
        }
        return null;
    }

}
