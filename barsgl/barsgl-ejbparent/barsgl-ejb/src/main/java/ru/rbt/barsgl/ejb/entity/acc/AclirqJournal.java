package ru.rbt.barsgl.ejb.entity.acc;

import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by ER22228 on 30.03.2016
 */
@Entity
@Table(name = "GL_ACLIRQ")
public class AclirqJournal extends BaseEntity<Long> {

    public enum Status {RAW, PARSED, VALIDATED, ENRICHED, PROCESSED, ERROR}

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "REQUEST_ID")
    private String requestId;

    @Basic(fetch = FetchType.LAZY)
    @Lob    
    @Column(name = "REQUEST")
    private String request;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private Status status;

    @Column(name = "STATUS_DATE")
    private Timestamp statusDate;

    @Column(name = "COMMENT")
    private String comment;

    @Basic(fetch = FetchType.LAZY)
    @Lob    
    @Column(name = "OUT")
    private String outMessage;
    
    @Override
    public Long getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Timestamp getStatusDate() {
        return statusDate;
    }

    public void setStatusDate(Timestamp statusDate) {
        this.statusDate = statusDate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getOutMessage() {
      return outMessage;
    }

    public void setOutMessage(String outMessage) {
      this.outMessage = outMessage;
    }
    
    @Override
    public String toString() {
        return "AcbalirqJournal{" +
                   "id=" + id +
                   ", requestId='" + requestId + '\'' +
                   ", request='" + request + '\'' +
                   ", status=" + status +
                   ", statusDate=" + statusDate +
                   ", comment='" + comment + '\'' +
                   ", outMessage='" + outMessage + '\'' +
                   '}';
    }
}
