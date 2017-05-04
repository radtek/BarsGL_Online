package ru.rbt.barsgl.ejb.entity.gl;

import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

import static java.math.BigDecimal.ZERO;
import static ru.rbt.ejbcore.mapping.YesNo.Y;

/**
 * Created by ER18837 on 20.02.15.
 */
@Entity
@Table(name = "GL_OPER")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "OPER_CLASS")
@DiscriminatorValue("AUTOMATIC")
@SequenceGenerator(name = "GLOperationIdSeq", sequenceName = "GL_OPER_SEQ", allocationSize = 1)
public class GLOperation extends BaseEntity<Long> {

    public static final String srcPaymentHub = "PH";
    public static final String srcKondorPlus = "K+TP";

    public enum OperClass {
        AUTOMATIC, MANUAL
    }

    public enum OperType {
        S("Simple", 1),
        M("Mfo", 2),
        E("Exch", 3),
        ME("MfoExch", 4),
        F("Fan", 5),
        ST("Storno", 6);

        private final String name;
        private int owerCode;

        private OperType(String name, int owerCode) {
            this.name = name;
            this.owerCode = owerCode;
        }

        public String getName() {
            return name;
        }

        public int getOwerCode() {
            return owerCode;
        }
    }

    public enum OperSide {
        N(""),
        D("по дебету"),
        C("по кредиту");

        private final String msgName;
        private OperSide(String msgName) {
            this.msgName = msgName;
        }

        public String getMsgName() {
            return msgName;
        }
    };
    public enum StornoType {C, S};
//    public enum InputMethod {AE, M, F};   // -> shared

    public GLOperation() {
        this.id = id;
    }

    public GLOperation(Long id) {
        this.id = id;
    }

    // Внутренный идентификатор ---------------------------
    @Id
    @Column(name = "GLOID")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "GLOperationIdSeq")
    private Long id;

    // Внешние идентификаторы проводки --------------------
    @Column(name = "PST_REF")
    private Long etlPostingRef;

    @Column(name = "ID_PST")
    private String aePostingId;        // 20

    @Column(name = "SRC_PST")
    private String sourcePosting;       // 7

    @Enumerated (EnumType.STRING)
    @Column(name = "INP_METHOD")
    private InputMethod inputMethod;    // 7

    @Column(name = "EVT_ID")
    private String eventId;             // 20

    @Column(name = "EVTP")
    private String eventType;           // 20

    @Column(name = "DEAL_ID")
    private String dealId;              // 20

    @Column(name = "SUBDEALID")
    private String subdealId;              // 20

    @Column(name = "CHNL_NAME")
    private String chnlName;            // 128

    @Column(name = "PMT_REF")
    private String paymentRefernce;     // 20

    @Column(name = "DEPT_ID")
    private String deptId;              // 4

    // Даты -----------------------------------------------
    @Temporal(TemporalType.DATE)
    @Column(name = "VDATE")
    private Date valueDate;

    @Column(name = "POSTDATE")
    @Temporal(TemporalType.DATE)
    private Date postDate;

    @Column(name = "PROCDATE")
    @Temporal(TemporalType.DATE)
    private Date procDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "OTS")
    private Date operationTimestamp;

    /**
     * дата создания
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CR_DT", insertable = false,updatable = false)
    private Date creationDate;

    /**
     * дата изменения
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "MOD_DT")
    private Date modificationDate;

    /**
     * дата обработки
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "PRC_DT")
    private Date processingDate;

    // Описание -------------------------------------------
    @Column(name = "NRT")
    private String narrative;           // 30

    @Column(name = "RNRTL")
    private String rusNarrativeLong;    // 300

    @Column(name = "RNRTS")
    private String rusNarrativeShort;   // 100

    // Дебет ----------------------------------------------
    @Column(name = "AC_DR")
    private String accountDebit;        // 20

    @ManyToOne
    @JoinColumn(name = "CCY_DR")
    private BankCurrency currencyDebit;

    @Column(name = "CBCC_DR")
    private String filialDebit;         // 3

    @Column(name = "AMT_DR")
    private BigDecimal amountDebit;

    @Column(name = "AMTRU_DR")
    private BigDecimal amountDebitRu;

    @Column(name = "RATE_DR")
    private BigDecimal rateDebit;

    @Column(name = "EQV_DR")
    private BigDecimal equivalentDebit;

    // Кредит ---------------------------------------------
    @Column(name = "AC_CR")
    private String accountCredit;       // 20

    @ManyToOne
    @JoinColumn(name = "CCY_CR", nullable = false)
    private BankCurrency currencyCredit;

    /**
     * Символьный код филиала счета Кредита
     */
    @Column(name = "CBCC_CR")
    private String filialCredit;        // 3

    @Column(name = "AMT_CR")
    private BigDecimal amountCredit;

    @Column(name = "AMTRU_CR")
    private BigDecimal amountCreditRu;

    @Column(name = "RATE_CR")
    private BigDecimal rateCredit;

    @Column(name = "EQV_CR")
    private BigDecimal equivalentCredit;

    // Сторно ---------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(name = "STRN")
    private YesNo storno;               // 1

    @Column(name = "STRN_REF")
    private String stornoReference;     // 128

    @Enumerated(EnumType.STRING)
    @Column(name = "STRN_REG")
    private StornoType stornoRegistration;  // 1

    @ManyToOne
    @JoinColumn(name = "STRN_GLO")
    private GLOperation stornoOperation;

    // Веер -----------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(name = "FAN")
    private YesNo fan;                  // 1

    @Column(name = "PAR_RF")
    private String parentReference;     // 128

    @ManyToOne
    @JoinColumn(name = "PAR_GLO")
    private GLOperation parentOperation;

    @Enumerated(EnumType.STRING)
    @Column(name = "FP_SIDE")
    private OperSide fpSide;

    @Enumerated(EnumType.STRING)
    @Column(name = "FB_SIDE")
    private OperSide fbSide;

    @Column(name = "FB_AMT")
    private BigDecimal amountFan;

    @Column(name = "FB_AMTRU")
    private BigDecimal amountFanRu;

    // Схема проводки -------------------------------------
    @Column(name = "PST_SCHEME")
    @Enumerated(EnumType.STRING)
    private OperType pstScheme;         // 16

    // Курсовая разница -----------------------------------
    @ManyToOne
    @JoinColumn(name = "MAIN_CCY", nullable = false)
    private BankCurrency currencyMain;

    @Column(name = "AMTR_POST")
    private BigDecimal amountPosting;

    @Column(name = "EXCH_DIFF")
    private BigDecimal exchangeDifference;

    @Column(name="AC_CCYEXCH")
    private String accountExchange;     // 20

    @Column(name="BS_CHAPTER")          // 1
    // @Enumerated(EnumType.STRING)
    private String bsChapter;

    // Межфилиальные проводки -----------------------------
    @ManyToOne
    @JoinColumn(name = "CCY_MFO", nullable = false)
    private BankCurrency currencyMfo;

    @Column(name="AC_MFOASST")
    private String accountAsset;        // 20

    @Column(name="AC_MFOLIAB")
    private String accountLiability;    // 20

    @Column(name="EMSG")
    private String errorMessage;

    // Статус, параметры опердня --------------------------
    @Enumerated(EnumType.STRING)
    @Column(name = "STATE")
    private OperState state;

    @Temporal(TemporalType.DATE)
    @Column(name = "CURDATE")
    private Date currentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "LWD_STATUS")
    private Operday.LastWorkdayStatus lastWorkdayStatus;

    // Параметры открываемых счетов -----------------------
    @Column(name="ACCKEY_DR")
    private String accountKeyDebit;     // 128

    @Column(name="ACCKEY_CR")
    private String accountKeyCredit;    // 128

    @Transient
    private AccountKeys accountParamDebit;

    @Transient
    private AccountKeys accountParamCredit;

    // ====================================================

    @Column(name = "OPER_CLASS", insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private OperClass operClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "FCHNG")
    private YesNo isCorrection;               // 1

    @Column(name = "PRFCNTR")
    private String profitCenter;              // 4

    @Column(name = "USER_NAME")
    private String userName;              // 64

    public Long getId() {
        return id;
    }

    public String getAePostingId() {
        return aePostingId;
    }

    public void setAePostingId(String aePostingId) {
        this.aePostingId = aePostingId;
    }

    public String getSourcePosting() {
        return sourcePosting;
    }

    public void setSourcePosting(String sourcePosting) {
        this.sourcePosting = sourcePosting;
    }

    public InputMethod getInputMethod() {
        return inputMethod;
    }

    public void setInputMethod(InputMethod inputMethod) {
        this.inputMethod = inputMethod;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDealId() {
        return dealId;
    }

    public void setDealId(String dealId) {
        this.dealId = dealId;
    }

    public String getChnlName() {
        return chnlName;
    }

    public void setChnlName(String chnlName) {
        this.chnlName = chnlName;
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

    public Date getValueDate() {
        return valueDate;
    }

    public void setValueDate(Date valueDate) {
        this.valueDate = valueDate;
    }

    public Date getPostDate() {
        return postDate;
    }

    public void setPostDate(Date postDate) {
        this.postDate = postDate;
    }

    public Date getProcDate() {
        return procDate;
    }

    public void setProcDate(Date procDate) {
        this.procDate = procDate;
    }

    public Date getOperationTimestamp() {
        return operationTimestamp;
    }

    public void setOperationTimestamp(Date operationTimestamp) {
        this.operationTimestamp = operationTimestamp;
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

    public BigDecimal getAmountDebitRu() {
        return amountDebitRu;
    }

    public void setAmountDebitRu(BigDecimal amountDebitRu) {
        this.amountDebitRu = amountDebitRu;
    }

    public BigDecimal getRateDebit() {
        return rateDebit;
    }

    public void setRateDebit(BigDecimal rateDebit) {
        this.rateDebit = rateDebit;
    }

    public BigDecimal getEquivalentDebit() {
        return equivalentDebit;
    }

    public void setEquivalentDebit(BigDecimal equivalentDebit) {
        this.equivalentDebit = equivalentDebit;
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

    public BigDecimal getAmountCreditRu() {
        return amountCreditRu;
    }

    public void setAmountCreditRu(BigDecimal amountCreditRu) {
        this.amountCreditRu = amountCreditRu;
    }

    public BigDecimal getRateCredit() {
        return rateCredit;
    }

    public void setRateCredit(BigDecimal rateCr) {
        this.rateCredit = rateCr;
    }

    public BigDecimal getEquivalentCredit() {
        return equivalentCredit;
    }

    public void setEquivalentCredit(BigDecimal equivalentCredit) {
        this.equivalentCredit = equivalentCredit;
    }

    public YesNo getStorno() {
        return storno;
    }

    public void setStorno(YesNo storno) {
        this.storno = storno;
    }

    public String getStornoReference() {
        return stornoReference;
    }

    public void setStornoReference(String stornoReference) {
        this.stornoReference = stornoReference;
    }

    public StornoType getStornoRegistration() {
        return stornoRegistration;
    }

    public void setStornoRegistration(StornoType stornoRegistration) {
        this.stornoRegistration = stornoRegistration;
    }

    public GLOperation getStornoOperation() {
        return stornoOperation;
    }

    public void setStornoOperation(GLOperation stornoOperation) {
        this.stornoOperation = stornoOperation;
    }

    public YesNo getFan() {
        return fan;
    }

    public void setFan(YesNo fan) {
        this.fan = fan;
    }

    public String getParentReference() {
        return parentReference;
    }

    public void setParentReference(String parentReference) {
        this.parentReference = parentReference;
    }

    public GLOperation getParentOperation() {
        return parentOperation;
    }

    public void setParentOperation(GLOperation parentOperation) {
        this.parentOperation = parentOperation;
    }

    public OperType getPstScheme() {
        return pstScheme;
    }

    public void setPstScheme(OperType pstScheme) {
        this.pstScheme = pstScheme;
    }

    public BankCurrency getCurrencyMain() {
        return currencyMain;
    }

    public void setCurrencyMain(BankCurrency currencyMain) {
        this.currencyMain = currencyMain;
    }

    public BigDecimal getAmountPosting() {
        return amountPosting;
    }

    public void setAmountPosting(BigDecimal amountPosting) {
        this.amountPosting = amountPosting;
    }

    public BigDecimal getExchangeDifference() {
        return exchangeDifference;
    }

    public void setExchangeDifference(BigDecimal exchangeDifference) {
        this.exchangeDifference = exchangeDifference;
    }

    public BankCurrency getCurrencyMfo() {
        return currencyMfo;
    }

    public void setCurrencyMfo(BankCurrency currencyMfo) {
        this.currencyMfo = currencyMfo;
    }

    public String getAccountExchange() {
        return accountExchange;
    }

    public void setAccountExchange(String accountExchange) {
        this.accountExchange = accountExchange;
    }

    public String getAccountAsset() {
        return accountAsset;
    }

    public void setAccountAsset(String accountAsset) {
        this.accountAsset = accountAsset;
    }

    public String getAccountLiability() {
        return accountLiability;
    }

    public void setAccountLiability(String accountLiability) {
        this.accountLiability = accountLiability;
    }

    public String getBsChapter() {
        return bsChapter;
    }

    public void setBsChapter(String bsChapter) {
        this.bsChapter = bsChapter;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(Date modificationDate) {
        this.modificationDate = modificationDate;
    }

    public Date getProcessingDate() {
        return processingDate;
    }

    public void setProcessingDate(Date processingDate) {
        this.processingDate = processingDate;
    }

    public OperState getState() {
        return state;
    }

    public void setState(OperState state) {
        this.state = state;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getPref() {
        return paymentRefernce != null ? paymentRefernce : dealId;
    }

    public OperSide getFpSide() {
        return fpSide;
    }

    public void setFpSide(OperSide fpSide) {
        this.fpSide = fpSide;
    }

    public OperSide getFbSide() {
        return fbSide;
    }

    public void setFbSide(OperSide fbSide) {
        this.fbSide = fbSide;
    }

    public BigDecimal getAmountFan() {
        return amountFan;
    }

    public void setAmountFan(BigDecimal amountFan) {
        this.amountFan = amountFan;
    }

    public BigDecimal getAmountFanRu() {
        return amountFanRu;
    }

    public void setAmountFanRu(BigDecimal amountFanRu) {
        this.amountFanRu = amountFanRu;
    }

    public boolean isStorno() {
        return Y == storno;
    }

    public boolean isFan() {
        return Y == fan;
    }

    public boolean isInterFilial() {
        return (null != filialDebit) && !filialDebit.equals(filialCredit);                   // разные филиалы
    }

    public boolean isExchangeDifferenceA() {
        return (null != exchangeDifference) && exchangeDifference.compareTo(ZERO) != 0              // есть курсовая разницы
                && BalanceChapter.A.name().equals(bsChapter);       // глава А
    }

    // сторнирующая и сторнируемая операции в опердень
    public boolean stornoOneday(Date operday) {
        return null != stornoOperation
                && valueDate.equals(operday)
                && stornoOperation.getValueDate().equals(operday);
    }

    public boolean isFilialDebit() {
        return (null != filialDebit) && !filialDebit.isEmpty() ;
    }

    public boolean isFilialCredit() {
        return (null != filialCredit) && !filialCredit.isEmpty() ;
    }

    public boolean fromPaymentHub() {
        return srcPaymentHub.equals(sourcePosting);
    }

    public boolean fromKondorPlus() {
        return srcKondorPlus.equals(sourcePosting);
    }

    public boolean hasParent() {
        return null != parentReference;
    }

    public boolean isParent() {
        return (null != parentReference) && parentReference.equals(paymentRefernce);
    }

    public boolean isChild() {
        return (null != parentReference) && !parentReference.equals(paymentRefernce);
    }

    public Date getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(Date currentDate) {
        this.currentDate = currentDate;
    }

    public Operday.LastWorkdayStatus getLastWorkdayStatus() {
        return lastWorkdayStatus;
    }

    public void setLastWorkdayStatus(Operday.LastWorkdayStatus lastWorkdayStatus) {
        this.lastWorkdayStatus = lastWorkdayStatus;
    }

    public String getAccountKeyDebit() {
        return accountKeyDebit;
    }

    public void setAccountKeyDebit(String accountKeyDebit) {
        this.accountKeyDebit = accountKeyDebit;
    }

    public String getAccountKeyCredit() {
        return accountKeyCredit;
    }

    public void setAccountKeyCredit(String accountKeyCredit) {
        this.accountKeyCredit = accountKeyCredit;
    }

    public void createAccountParamDebit() {
        this.accountParamDebit = new AccountKeys(this.accountKeyDebit);
    }

    public void createAccountParamCredit() {
        this.accountParamCredit = new AccountKeys(this.accountKeyCredit);;
    }

    public AccountKeys getAccountParamDebit() {
        return accountParamDebit;
    }

    public AccountKeys getAccountParamCredit() {
        return accountParamCredit;
    }

    public OperClass getOperClass() {
        return operClass;
    }

    // если валюта - РУБЛЬ, возвращает поле "сумма в валюте", иначе "сумму в рублях"
    public BigDecimal getEquivalentDebitRu() {
        return (this.amountDebitRu == null) ? this.equivalentDebit : this.amountDebitRu;
    }

    public BigDecimal getEquivalentCreditRu() {
        return (this.amountCreditRu == null) ? this.equivalentCredit : this.amountCreditRu;
    }

    public boolean isCurrencyDebitRUR() {
        return BankCurrency.RUB.equals(currencyDebit);
    }

    public boolean isCurrencyCreditRUR() {
        return BankCurrency.RUB.equals(currencyCredit);
    }

    public Long getEtlPostingRef() {
        return etlPostingRef;
    }

    public void setEtlPostingRef(Long etlPostingRef) {
        this.etlPostingRef = etlPostingRef;
    }

    public String getSubdealId() {
        return subdealId;
    }

    public void setSubdealId(String subdealId) {
        this.subdealId = subdealId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getProfitCenter() {
        return profitCenter;
    }

    public void setProfitCenter(String profitCenter) {
        this.profitCenter = profitCenter;
    }

    public YesNo getIsCorrection() {
        return isCorrection;
    }

    public void setIsCorrection(YesNo isCorrection) {
        this.isCorrection = isCorrection;
    }

    public boolean isCorrection() {
        return (Y == storno) || (Y == isCorrection);
    }
}

