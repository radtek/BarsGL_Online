package ru.rbt.audit.entity;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov
 */
@Entity
@Table(name = "GL_AUDIT")
public class AuditRecord extends BaseEntity<Long> {

    public enum LogCode {
        Authorization
        , OpenOperday
        , Operday
        , Posting
        , PreCob
        , Package
        , EtlOperation
        , Operation
        , FanOperation
        , Account
        , Task
        , User
        , LoadRatesTask
        , DwhUnloadPosting
        , StamtUnload
        , StamtUnloadFull
        , StamtUnloadDelta
        , StamtUnloadBalFull
        , StamtUnloadBalDelta
        , StamtUnloadBalStep3
        , AccountBalanceUnload
        , AccountSepBalanceUnload
        , AccountOvervaluedBalanceUnload
        , MidasPLReplication
        , AcccountBalanceOndemandUnload
        , BufferModeSync
        , BufferModeSyncTask
        , BufferModeSyncBackvalue
        , RecalcBS2
        , RecalcWTAC
        , ReplAfterBufferRelease
        , UnloadPDandUnspents
        , AccountDetailsNotify
        , JobControl
        , GLVD_PST_DU
        , GLVD_BAL4
        , PSTR_ACC_R_LOAD
        , GLVD_PSTR_UD
        , GLVD_PSTR_LOAD
        , GLVD_BAL_R
        , AccountQuery
        , TestRunner
        , AccountQueryRunner
        , StamtIncrement
        , BalturRecalc
        , KeyValueStorageSetter
        , MovementCreate
        , BatchOperation
        , ManualOperation
        , BackValue
        , Role
        , AccountListTester
        , CardMessageProcessorBean
        , ActSrc
        , FreeAcod
        , OverValueAcc2GlAcc
        , FlexNdsFan
        , PlClose707Del
        , PlClose707Create
        , AS400runner
        , TechnicalPosting
        , Monitoring
        , StartLoaderTask
        , TechoverTask
        , CurrencyExchange
        , ReprocessAEOper
        , BulkOpeningAccountsTask
        , Acc2ForDeals
    }

    public enum LogLevel {
        Info,
        Warning,
        Error,
        SysError
    }

    @Id
    @Column(name = "ID_RECORD")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "SYS_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date logTime;

    @Column(name = "USER_NAME")
    private String userName;

    @Column(name = "USER_HOST")
    private String userHost;

    @Column(name = "LOG_CODE")
    @Enumerated(EnumType.STRING)
    private LogCode logCode;

    @Column(name = "LOG_LEVEL")
    @Enumerated(EnumType.STRING)
    private LogLevel logLevel;

    @Column(name = "MESSAGE")
    private String message;

    @Column(name = "ERRORMSG")
    private String errorMessage;

    @Lob
    @Column(name = "STCK_TRACE")
    private String stackTrace;

    @Column(name = "ENTITY_ID")
    private String entityId;

    @Column(name = "ENTITYTYPE")
    private String entityType;

    @Column(name = "TRANSACTID")
    private String transactionId;

    @Column(name = "SRC")
    private String source;

    @Column(name = "ERRORSRC")
    private String errorSource;

    @Lob
    @Column(name = "ATTACHMENT")
    private String attachment;

    @Column(name = "PROCTIMEMS")
    private Integer processDuration;


    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Date getLogTime() {
        return logTime;
    }

    public void setLogTime(Date logTime) {
        this.logTime = logTime;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserHost() {
        return userHost;
    }

    public void setUserHost(String userHost) {
        this.userHost = userHost;
    }

    public LogCode getLogCode() {
        return logCode;
    }

    public void setLogCode(LogCode logCode) {
        this.logCode = logCode;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public Integer getProcessDuration() {
        return processDuration;
    }

    public void setProcessDuration(Integer processDuration) {
        this.processDuration = processDuration;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorSource() {
        return errorSource;
    }

    public void setErrorSource(String errorSource) {
        this.errorSource = errorSource;
    }
}
