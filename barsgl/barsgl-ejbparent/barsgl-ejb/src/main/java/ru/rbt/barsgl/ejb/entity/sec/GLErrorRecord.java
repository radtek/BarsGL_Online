package ru.rbt.barsgl.ejb.entity.sec;

import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.mapping.YesNo;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by ER18837 on 07.02.17.
 */
@Entity
@Table(name = "GL_ERRORS")
@SequenceGenerator(name = "GLErrorRecordIdSeq", sequenceName = "GL_ERRORS_SEQ", allocationSize = 1)
public class GLErrorRecord extends BaseEntity<Long> {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "GLErrorRecordIdSeq")
    private Long id;

    @Column(name = "PST_REF")
    private Long etlPostingRef;

    @Column(name = "GLO_REF")
    private Long glOperRef;

    @Column(name = "ID_PST")
    private String aePostingId;

    @Column(name = "SRC_PST")
    private String sourcePosting;

    @Column(name = "ERR_TYPE")
    private String errorType;

    @Column(name = "ERR_CODE")
    private String errorCode;

    @Column(name = "ERR_MSG")
    private String errorMessage;

    @Temporal(TemporalType.DATE)
    @Column(name = "PROCDATE")
    private Date procDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "OTS_ERR", insertable = false, updatable = false)
    private Date errorTimestamp;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "OTS_PROC")
    private Date processTimestamp;

    @Column(name = "USER_NAME")
    private String userName;

    @Column(name = "\"COMMENT\"")// COMMENT is oracle db reserved word
    private String comment;

    @Column(name = "ID_PST_NEW")
    private String aePostingIdNew;

    @Column(name = "ID_PKG")
    private Long idPackage;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "OLD_PKG_DT")
    private Date reprocPackageTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "CORRECT")
    private YesNo correct;

    @Column(name = "CORR_TYPE")
    private String correctType;

    @Override
    public Long getId() {
        return id;
    }

    public String getAePostingId() {
        return aePostingId;
    }

    public void setAePostingId(String aePostingId) {
        this.aePostingId = aePostingId;
    }

    public Long getEtlPostingRef() {
        return etlPostingRef;
    }

    public void setEtlPostingRef(Long etlPostingRef) {
        this.etlPostingRef = etlPostingRef;
    }

    public Long getGlOperRef() {
        return glOperRef;
    }

    public void setGlOperRef(Long glOperRef) {
        this.glOperRef = glOperRef;
    }

    public String getSourcePosting() {
        return sourcePosting;
    }

    public void setSourcePosting(String sourcePosting) {
        this.sourcePosting = sourcePosting;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Date getProcDate() {
        return procDate;
    }

    public void setProcDate(Date procDate) {
        this.procDate = procDate;
    }

    public Date getErrorTimestamp() {
        return errorTimestamp;
    }

    public void setErrorTimestamp(Date errorTimestamp) {
        this.errorTimestamp = errorTimestamp;
    }

    public Date getProcessTimestamp() {
        return processTimestamp;
    }

    public void setProcessTimestamp(Date processTimestamp) {
        this.processTimestamp = processTimestamp;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAePostingIdNew() {
        return aePostingIdNew;
    }

    public void setAePostingIdNew(String aePostingIdNew) {
        this.aePostingIdNew = aePostingIdNew;
    }

    public YesNo getCorrect() {
        return correct;
    }

    public void setCorrect(YesNo correct) {
        this.correct = correct;
    }

    public String getCorrectType() {
        return correctType;
    }

    public void setCorrectType(String correctType) {
        this.correctType = correctType;
    }

    public Long getIdPackage() {
        return idPackage;
    }

    public void setIdPackage(Long idPackage) {
        this.idPackage = idPackage;
    }

    public Date getReprocPackageTimestamp() {
        return reprocPackageTimestamp;
    }

    public void setReprocPackageTimestamp(Date reprocPackageTimestamp) {
        this.reprocPackageTimestamp = reprocPackageTimestamp;
    }
}
