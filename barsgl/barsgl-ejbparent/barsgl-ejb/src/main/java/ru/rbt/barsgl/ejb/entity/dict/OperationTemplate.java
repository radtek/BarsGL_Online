package ru.rbt.barsgl.ejb.entity.dict;

import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Created by ER18837 on 17.03.16.
 */
@Entity
@Table(name = "GL_OPRTMPL")
@SequenceGenerator(name = "OperationTemplateIdSeq", sequenceName = "GL_OPRTMPL_SEQ", allocationSize = 1)
public class OperationTemplate extends BaseEntity<Long> {

    public enum TemplateType {S, E};

    @Id
    @Column(name = "ID_TMPL")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "OperationTemplateIdSeq")
    private Long id;

    @Column(name = "TMPL_NAME")
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "TMPL_TYPE")
    private TemplateType templateType;

    @Column(name = "SRC_PST")
    private String sourcePosting;

    @Column(name = "AC_DR")
    private String accountDebit;

    @ManyToOne
    @JoinColumn(name = "CCY_DR")
    private BankCurrency currencyDebit;

    @Column(name = "CBCC_DR")
    private String filialDebit;

    @Column(name = "AMT_DR")
    private BigDecimal amountDebit;

    @Column(name = "AC_CR")
    private String accountCredit;

    @ManyToOne
    @JoinColumn(name = "CCY_CR", nullable = false)
    private BankCurrency currencyCredit;

    @Column(name = "CBCC_CR")
    private String filialCredit;

    @Column(name = "AMT_CR")
    private BigDecimal amountCredit;

    @Column(name = "NRT")
    private String narrative;

    @Column(name = "RNRTL")
    private String rusNarrativeLong;

    @Column(name = "DEPT_ID")
    private String deptId;

    @Column(name = "PRFCNTR")
    private String profitCenter;

    @Enumerated(EnumType.STRING)
    @Column(name = "SYS")
    private YesNo isSystem;

    @Column(name = "USER_NAME")
    private String userName;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public TemplateType getTemplateType() {
        return templateType;
    }

    public void setTemplateType(TemplateType templateType) {
        this.templateType = templateType;
    }

    public String getSourcePosting() {
        return sourcePosting;
    }

    public void setSourcePosting(String sourcePosting) {
        this.sourcePosting = sourcePosting;
    }

    public String getAccountDebit() {
        return accountDebit;
    }

    public void setAccountDebit(String accountDebit) {
        this.accountDebit = accountDebit;
    }

    public BankCurrency getCurrencyDebit() {
        return currencyDebit;
    }

    public void setCurrencyDebit(BankCurrency currencyDebit) {
        this.currencyDebit = currencyDebit;
    }

    public String getFilialDebit() {
        return filialDebit;
    }

    public void setFilialDebit(String filialDebit) {
        this.filialDebit = filialDebit;
    }

    public BigDecimal getAmountDebit() {
        return amountDebit;
    }

    public void setAmountDebit(BigDecimal amountDebit) {
        this.amountDebit = amountDebit;
    }

    public String getAccountCredit() {
        return accountCredit;
    }

    public void setAccountCredit(String accountCredit) {
        this.accountCredit = accountCredit;
    }

    public BankCurrency getCurrencyCredit() {
        return currencyCredit;
    }

    public void setCurrencyCredit(BankCurrency currencyCredit) {
        this.currencyCredit = currencyCredit;
    }

    public String getFilialCredit() {
        return filialCredit;
    }

    public void setFilialCredit(String filialCredit) {
        this.filialCredit = filialCredit;
    }

    public BigDecimal getAmountCredit() {
        return amountCredit;
    }

    public void setAmountCredit(BigDecimal amountCredit) {
        this.amountCredit = amountCredit;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }

    public String getRusNarrativeLong() {
        return rusNarrativeLong;
    }

    public void setRusNarrativeLong(String rusNarrativeLong) {
        this.rusNarrativeLong = rusNarrativeLong;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public String getProfitCenter() {
        return profitCenter;
    }

    public void setProfitCenter(String profitCenter) {
        this.profitCenter = profitCenter;
    }

    public YesNo getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(YesNo isSystem) {
        this.isSystem = isSystem;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
