package ru.rbt.barsgl.ejb.entity.gl;

import ru.rbt.barsgl.ejb.entity.gl.Memorder.CancelFlag;
import ru.rbt.barsgl.ejb.entity.gl.Memorder.DocType;

import javax.persistence.*;

/**
 * Created by Ivan Sevastyanov on 05.02.2016.
 * буферная таблица проводок, используется в режиме "GL_OD.PDMOD(BUFFER)"
 */
@Entity
@Table(name = "GL_PD")
@AttributeOverrides({
        @AttributeOverride(name="pref", column=@Column(name = "PREF"))
        , @AttributeOverride(name="dlId", column=@Column(name = "DLID"))
        , @AttributeOverride(name="dlType", column=@Column(name = "DLTYPE"))
        , @AttributeOverride(name="operator", column=@Column(name = "OPERATOR"))
        , @AttributeOverride(name="operatorDepartment", column=@Column(name = "OPER_DEPT"))
        , @AttributeOverride(name="authorizer", column=@Column(name = "AUTHORIZER"))
        , @AttributeOverride(name="authorizerDepartment", column=@Column(name = "AUTH_DEPT"))
        , @AttributeOverride(name="glOperationId", column=@Column(name = "GLO_REF", table = "GL_PD"))
        , @AttributeOverride(name="eventType", column=@Column(name = "EVTP", table = "GL_PD"))
        , @AttributeOverride(name="procDate", column=@Column(name = "PROCDATE", table = "GL_PD"))
        , @AttributeOverride(name="dealId", column=@Column(name = "DEAL_ID", table = "GL_PD"))
        , @AttributeOverride(name="subdealId", column=@Column(name = "SUBDEALID", table = "GL_PD"))
        , @AttributeOverride(name="rusNarrLong", column=@Column(name = "RNARLNG"))
        , @AttributeOverride(name="rusNarrShort", column=@Column(name = "RNARSHT"))
        , @AttributeOverride(name="operReference", column=@Column(name = "OREF"))
        , @AttributeOverride(name="docNumber", column=@Column(name = "DOCN"))
        , @AttributeOverride(name="operRefSource", column=@Column(name = "OREF_SRC"))
        , @AttributeOverride(name="department", column=@Column(name = "DPMT"))
        , @AttributeOverride(name="flexCode", column=@Column(name = "FLEX_EVENT_CODE"))
        , @AttributeOverride(name="eventId", column=@Column(name = "EVT_ID", table = "GL_PD"))
        , @AttributeOverride(name="paymentRef", column=@Column(name = "PMT_REF", table = "GL_PD"))
        , @AttributeOverride(name="isCorrection", column=@Column(name = "FCHNG", table = "GL_PD"))
        , @AttributeOverride(name="profitCenter", column=@Column(name = "PRFCNTR", table = "GL_PD"))
        , @AttributeOverride(name="narrative", column=@Column(name = "NRT", table = "GL_PD"))
})
public class GLPd extends AbstractPd {

    public GLPd() {}

    public GLPd(Long pcId) {
        setPcId(pcId);
    }

    // PCID_MO
    @Column(name = "MO_NO")
    private String memorderNumber;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "AN_IND")
    private CancelFlag cancelFlag;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "BO_IND")
    private DocType docType;

    // GL_POSTING
    @Column(name = "POST_TYPE")
    private String postType;

    @Column(name = "STRN_PCID")
    private Long stornoPcid;

    // OWN
    @Column(name = "PD_ID", insertable = false)
    private Long pdId;

    public String getMemorderNumber() {
        return memorderNumber;
    }

    public void setMemorderNumber(String memorderNumber) {
        this.memorderNumber = memorderNumber;
    }

    public CancelFlag getCancelFlag() {
        return cancelFlag;
    }

    public void setCancelFlag(CancelFlag cancelFlag) {
        this.cancelFlag = cancelFlag;
    }

    public DocType getDocType() {
        return docType;
    }

    public void setDocType(DocType docType) {
        this.docType = docType;
    }

    public String getPostType() {
        return postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
    }

    public Long getStornoPcid() {
        return stornoPcid;
    }

    public void setStornoPcid(Long stornoPcid) {
        this.stornoPcid = stornoPcid;
    }

    public Long getPdId() {
        return pdId;
    }

    public void setPdId(Long pdId) {
        this.pdId = pdId;
    }
}
