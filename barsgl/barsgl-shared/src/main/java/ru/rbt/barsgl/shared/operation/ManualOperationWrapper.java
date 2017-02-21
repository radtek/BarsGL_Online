package ru.rbt.barsgl.shared.operation;

import com.google.gwt.user.client.rpc.IsSerializable;
import ru.rbt.barsgl.shared.ErrorList;
import ru.rbt.barsgl.shared.enums.BatchPostAction;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.PostingChoice;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Created by ER18837 on 13.08.15.
 * Для передачи параметров по операциям, создаваемым вручную
 */
public class ManualOperationWrapper implements Serializable, IsSerializable {
    public final String dateFormat = "dd.MM.yyyy";


    // Передаются с формы =================================
    private String templateName;        // только для шаблона
    private boolean isExtended;         // только для шаблона
    private boolean isSystem;           // только для шаблона

    private boolean withCheck;          // с проверкой баланса
    private String dealSrc;             // 7    //    @Column(name = "PST_SRC")
    private String dealId;              // 20   //    @Column(name = "DEAL_ID")
    private String paymentRefernce;     // 20   //    @Column(name = "PMT_REF")
    private String subdealId;
    private String deptId;              // 4    //    @Column(name = "DEPT_ID")
    private String profitCenter;        // 4    //    @Column(name = "PRFCNTR")
    private boolean isCorrection;       // 1    //    @Column(name = "FCHNG")
    private boolean isStorno;           // 1    //    @Column(name = "STRN")
    private boolean isFan;              // 1    //    @Column(name = "FAN")
    private boolean isInvisible;        // 1    //    @Column(name = "INVISIBLE") in PD

    private String valueDateStr;        //    @Column(name = "VDATE")
    private String postDateStr;         //    @Column(name = "POSTDATE")

    private String narrative;           // 30   //    @Column(name = "NRT")
    private String rusNarrativeLong;    // 300  //    @Column(name = "RNRTL")
    private String rusNarrativeShort;   // 100  //    @Column(name = "RNRTS")

    // Дебет ----------------------------------------------
    private String accountDebit;        // 20   //    @Column(name = "AC_DR")
    private String currencyDebit;       // 3    //    @JoinColumn(name = "CCY_DR")
    private String filialDebit;         // 3    //    @Column(name = "CBCC_DR")

    private BigDecimal amountDebit;     //    @Column(name = "AMT_DR")
    private BigDecimal amountRu;     //    @Column(name = "AMTRU_DR")

    // Кредит ---------------------------------------------
    private String accountCredit;       // 20   //    @Column(name = "AC_CR")
    private String currencyCredit;      // 3    //    @JoinColumn(name = "CCY_CR", nullable = false)
    private String filialCredit;        // 3    //    @Column(name = "CBCC_CR")

    private BigDecimal amountCredit;    //    @Column(name = "AMT_CR")

    // Возвращаются на форму ==============================
    private Long id;                            //    @Column(name = "GLOID")
    private Long operationId;
    private String pstScheme;           // 16   //    @Column(name = "PST_SCHEME")

    // только для проводок - список ID полупроводок
    private ArrayList<Long> pdIdList;
    private PostingChoice postingChoice;

    private InputMethod inputMethod;
    private String pdMode;

    private BatchPostStatus status;
    private BatchPostAction action;
    private String reasonOfDeny;

    //UserID для проверки ограничений для действий со счетом для конкретного пользователя
    private Long userId;
    // ID пакета
    private Long pkgId;

    // список ошибок
    private ErrorList errorList = new ErrorList();

    public String getDealSrc() {
        return dealSrc;
    }

    public void setDealSrc(String dealSrc) {
        this.dealSrc = dealSrc;
    }

    public String getDealId() {
        return dealId;
    }

    public void setDealId(String dealId) {
        this.dealId = dealId;
    }

    public String getPaymentRefernce() {
        return paymentRefernce;
    }

    public void setPaymentRefernce(String paymentRefernce) {
        this.paymentRefernce = paymentRefernce;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
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

    public String getRusNarrativeShort() {
        return rusNarrativeShort;
    }

    public void setRusNarrativeShort(String rusNarrativeShort) {
        this.rusNarrativeShort = rusNarrativeShort;
    }

    public String getAccountDebit() {
        return accountDebit;
    }

    public void setAccountDebit(String accountDebit) {
        this.accountDebit = accountDebit;
    }

    public String getCurrencyDebit() {
        return currencyDebit;
    }

    public void setCurrencyDebit(String currencyDebit) {
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

    public String getCurrencyCredit() {
        return currencyCredit;
    }

    public void setCurrencyCredit(String currencyCredit) {
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPstScheme() {
        return pstScheme;
    }

    public void setPstScheme(String pstScheme) {
        this.pstScheme = pstScheme;
    }

    public ErrorList getErrorList() {
        return errorList;
    }

    public String getErrorMessage() {
        return errorList.getErrorMessage();
    }

    public String getValueDateStr() {
        return valueDateStr;
    }

    public void setValueDateStr(String valueDateStr) {
        this.valueDateStr = valueDateStr;
    }

    public String getPostDateStr() {
        return postDateStr;
    }

    public void setPostDateStr(String postDateStr) {
        this.postDateStr = postDateStr;
    }

    public String getSubdealId() {
        return subdealId;
    }

    public ManualOperationWrapper setSubdealId(String subdealId) {
        this.subdealId = subdealId;
        return this;
    }

    public String getProfitCenter() {
        return profitCenter;
    }

    public void setProfitCenter(String profitCenter) {
        this.profitCenter = profitCenter;
    }

    public InputMethod getInputMethod() {
        return inputMethod;
    }

    public void setInputMethod(InputMethod inputMethod) {
        this.inputMethod = inputMethod;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public boolean isExtended() {
        return isExtended;
    }

    public void setExtended(boolean extended) {
        isExtended = extended;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public void setSystem(boolean system) {
        isSystem = system;
    }

    public ArrayList<Long> getPdIdList() {
        return pdIdList;
    }

    public void setPdIdList(ArrayList<Long> pdIdList) {
        this.pdIdList = pdIdList;
    }

    public PostingChoice getPostingChoice() {
        return postingChoice;
    }

    public void setPostingChoice(PostingChoice postingChoice) {
        this.postingChoice = postingChoice;
    }

    public String getPdMode() {
        return pdMode;
    }

    public void setPdMode(String pdMode) {
        this.pdMode = pdMode;
    }

    public boolean isCorrection() {
        return isCorrection;
    }

    public void setCorrection(boolean correction) {
        isCorrection = correction;
    }

    public boolean isStorno() {
        return isStorno;
    }

    public void setStorno(boolean storno) {
        isStorno = storno;
    }

    public boolean isFan() {
        return isFan;
    }

    public void setFan(boolean fan) {
        isFan = fan;
    }

    public boolean isInvisible() {
        return isInvisible;
    }

    public void setInvisible(boolean invisible) {
        isInvisible = invisible;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getAmountRu() {
        return amountRu;
    }

    public void setAmountRu(BigDecimal amountRu) {
        this.amountRu = amountRu;
    }

    public BatchPostStatus getStatus() {
        return status;
    }

    public void setStatus(BatchPostStatus status) {
        this.status = status;
    }

    public BatchPostAction getAction() {
        return action;
    }

    public void setAction(BatchPostAction action) {
        this.action = action;
    }

    public Long getOperationId() {
        return operationId;
    }

    public void setOperationId(Long operationId) {
        this.operationId = operationId;
    }

    public String getReasonOfDeny() {
        return reasonOfDeny;
    }

    public void setReasonOfDeny(String reasonOfDeny) {
        this.reasonOfDeny = reasonOfDeny;
    }

    public Long getPkgId() {
        return pkgId;
    }

    public void setPkgId(Long pkgId) {
        this.pkgId = pkgId;
    }

    public boolean isWithCheck() {
        return withCheck;
    }

    public void setWithCheck(boolean withCheck) {
        this.withCheck = withCheck;
    }
}
