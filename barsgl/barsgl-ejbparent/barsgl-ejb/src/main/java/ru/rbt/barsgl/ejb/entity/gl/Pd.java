package ru.rbt.barsgl.ejb.entity.gl;

import javax.persistence.*;

/**
 * Created by Ivan Sevastyanov on 05.02.2016.
 */
@Entity
@Table(name = "PST")
@AttributeOverrides({
        @AttributeOverride(name="pref", column=@Column(name = "PREF"))
        , @AttributeOverride(name="dlId", column=@Column(name = "DLID"))
        , @AttributeOverride(name="dlType", column=@Column(name = "DLTYPE"))
        , @AttributeOverride(name="operator", column=@Column(name = "OPERATOR"))
        , @AttributeOverride(name="operatorDepartment", column=@Column(name = "OPER_DEPT"))
        , @AttributeOverride(name="authorizer", column=@Column(name = "AUTHORIZER"))
        , @AttributeOverride(name="authorizerDepartment", column=@Column(name = "AUTH_DEPT"))
        , @AttributeOverride(name="glOperationId", column=@Column(name = "GLO_REF", table = "PST"))
        , @AttributeOverride(name="eventType", column=@Column(name = "EVTP", table = "PST"))
        , @AttributeOverride(name="procDate", column=@Column(name = "PROCDATE", table = "PST"))
        , @AttributeOverride(name="dealId", column=@Column(name = "DEAL_ID", table = "PST"))
        , @AttributeOverride(name="subdealId", column=@Column(name = "SUBDEALID", table = "PST"))
        , @AttributeOverride(name="rusNarrLong", column=@Column(name = "RNARLNG"))
        , @AttributeOverride(name="rusNarrShort", column=@Column(name = "RNARSHT"))
        , @AttributeOverride(name="operReference", column=@Column(name = "OREF"))
        , @AttributeOverride(name="docNumber", column=@Column(name = "DOCN"))
        , @AttributeOverride(name="operRefSource", column=@Column(name = "OREF_SRC"))
        , @AttributeOverride(name="department", column=@Column(name = "DPMT"))
        , @AttributeOverride(name="flexCode", column=@Column(name = "FLEX_EVENT_CODE"))
        , @AttributeOverride(name="eventId", column=@Column(name = "EVT_ID", table = "PST"))
        , @AttributeOverride(name="paymentRef", column=@Column(name = "PMT_REF", table = "PST"))
        , @AttributeOverride(name="isCorrection", column=@Column(name = "FCHNG", table = "PST"))
        , @AttributeOverride(name="profitCenter", column=@Column(name = "PRFCNTR", table = "PST"))
        , @AttributeOverride(name="narrative", column=@Column(name = "NRT", table = "PST"))
})
public class Pd extends AbstractPd {

}
