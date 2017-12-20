package ru.rbt.barsgl.ejb.entity.cust;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by er18837 on 12.12.2017.
 */
@Entity
@Table(name = "GL_CUDENO1")
@SequenceGenerator(name = "CustDNJournalIdSeq", sequenceName = "GL_CUDENO_SEQ", allocationSize = 1)
public class CustDNJournal extends BaseEntity<Long> {

    public enum Status {RAW, SKIPPED, VALIDATED, MAPPED, PROCESSED, ERR_VAL, ERR_MAP, ERR_PROC, EMULATED};

    @Id
    @Column(name = "MESSAGE_ID")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "CustDNJournalIdSeq")
    private Long id;

    @Column(name = "MESSAGE")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private Status status;

    @Column(name = "LOAD_DATE", insertable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date loadDate;

    @Column(name = "STATUS_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date statusDate;

    @Column(name = "\"COMMENT\"") // COMMENT is oracle db reserved word
    private String comment;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getStatusDate() {
        return statusDate;
    }

    public void setStatusDate(Date statusDate) {
        this.statusDate = statusDate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Date getLoadDate() {
        return loadDate;
    }

    @Override
    public String toString() {
        return "CustDNJournal{" +
                "id='" + id + '\'' +
                ", status=" + status +
                ", loadDate=" + loadDate +
                ", statusDate=" + statusDate +
                ", comment='" + comment + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}

