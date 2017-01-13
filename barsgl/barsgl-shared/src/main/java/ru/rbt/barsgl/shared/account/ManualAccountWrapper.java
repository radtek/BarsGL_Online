package ru.rbt.barsgl.shared.account;

import com.google.gwt.user.client.rpc.IsSerializable;
import ru.rbt.barsgl.shared.ErrorList;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * Created by ER18837 on 22.08.15.
 */
public class ManualAccountWrapper implements Serializable, IsSerializable {
    public static final String dateFormat = "dd.MM.yyyy";
    public static final String dateNull = "01.01.2029";

    // определяются после создания счета
    private Long id;
    private String bsaAcid;

    // --- Вводятся с экрана -----
    private String branch;
    private String currency;
    private String customerNumber;
    private Long accountType;
    private Short term;
    private String dealId;
    private String dealSource;
    private String dateOpenStr;
    private String dateCloseStr;

    // --- Вычисляемые поля  ---
    private String filial;
    private String companyCode;
    private Short cbCustomerType;
    private String balanceAccount2;
    private String plCode;

    private Short accountCode;
    private Short accountSequence;
    private String acid;

    private String description;
    private String passiveActive;

    // Пока не используются
    private String glSequence;
    private String subDealId;
    private BigInteger operation;
    private String operSide;
    private String openType;

    //UserID для проверки ограничений для действий со счетом для конкретного пользователя
    private Long userId;

    // --- Cписок ошибок ---------
    final private ErrorList errorList = new ErrorList();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBsaAcid() {
        return bsaAcid;
    }

    public void setBsaAcid(String bsaAcid) {
        this.bsaAcid = bsaAcid;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public Long getAccountType() {
        return accountType;
    }

    public void setAccountType(Long accountType) {
        this.accountType = accountType;
    }

    public String getDateOpenStr() {
        return dateOpenStr;
    }

    public void setDateOpenStr(String dateOpenStr) {
        this.dateOpenStr = dateOpenStr;
    }

    public String getDateCloseStr() {
        return dateCloseStr;
    }

    public void setDateCloseStr(String dateCloseStr) {
        this.dateCloseStr = dateCloseStr;
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

    public Short getCbCustomerType() {
        return cbCustomerType;
    }

    public void setCbCustomerType(Short cbCustomerType) {
        this.cbCustomerType = cbCustomerType;
    }

    public String getBalanceAccount2() {
        return balanceAccount2;
    }

    public void setBalanceAccount2(String balanceAccount2) {
        this.balanceAccount2 = balanceAccount2;
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

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPassiveActive() {
        return passiveActive;
    }

    public void setPassiveActive(String passiveActive) {
        this.passiveActive = passiveActive;
    }

    public String getGlSequence() {
        return glSequence;
    }

    public void setGlSequence(String glSequence) {
        this.glSequence = glSequence;
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

    public Short getTerm() {
        return term;
    }

    public void setTerm(Short term) {
        this.term = term;
    }

    public String getSubDealId() {
        return subDealId;
    }

    public void setSubDealId(String subDealId) {
        this.subDealId = subDealId;
    }

    public BigInteger getOperation() {
        return operation;
    }

    public void setOperation(BigInteger operation) {
        this.operation = operation;
    }

    public String getOperSide() {
        return operSide;
    }

    public void setOperSide(String operSide) {
        this.operSide = operSide;
    }

    public String getOpenType() {
        return openType;
    }

    public void setOpenType(String openType) {
        this.openType = openType;
    }

    public ErrorList getErrorList() {
        return errorList;
    }

    public String getErrorMessage() {
        return errorList.getErrorMessage();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

}
