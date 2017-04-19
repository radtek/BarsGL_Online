package ru.rbt.barsgl.ejb.entity.gl;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov
 */
@Entity
@Table(name = "PCID_MO")
public class Memorder extends BaseEntity<Long> {

    /**
     * признак отмены проводки
     */
    public enum CancelFlag {
        /**
         * действующий
         */
        N,
        /**
         * удаленный
         */
        Y
    }

    /**
     * тип документа
     */
    public enum DocType {
        /**
         * мем. ордер
         */
        MEMORDER,
        /**
         * банковский ордер
         */
        BANK_ORDER
    }

    @Id
    @Column(name = "PCID")
    private Long id;

    @Temporal(TemporalType.DATE)
    @Column(name = "POD")
    private Date postDate;

    @Column(name = "MO_NO")
    private String number;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "AN_IND")
    private CancelFlag cancelFlag;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "BO_IND")
    private DocType docType;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Date getPostDate() {
        return postDate;
    }

    public void setPostDate(Date postDate) {
        this.postDate = postDate;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
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
}
