package ru.rbt.barsgl.ejb.entity.acc;

import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by ER22228 on 30.03.2016
 */
@Entity
@Table(name = "GL_ACDENO")
public class AcDNJournal extends BaseEntity<Long> {

    public enum Sources {MIDAS_OPEN, FCC, FCC_CLOSE}
    public enum Status {RAW,PARSED,VALIDATED,ENRICHED,PROCESSED,ERROR}

    @Id
    @Column(name = "MESSAGE_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "SOURCE")
    private Sources source;

    @Column(name = "MESSAGE")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private Status status;

    @Column(name = "STATUS_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date statusDate;

    @Column(name = "COMMENT")
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

    public Sources getSource() {
        return source;
    }

    public void setSource(Sources source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "AcDNJournal{" +
                "id='" + id + '\'' +
                ", source=" + source +
                ", status=" + status +
                ", statusDate=" + statusDate +
                ", comment='" + comment + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
