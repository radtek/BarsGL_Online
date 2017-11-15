package ru.rbt.barsgl.ejb.entity.monitor;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov on 14.11.2017.
 */
@Entity
@Table(name = "GL_HTTPSESS")
@SequenceGenerator(name = "AppHttpSessionIdSeq", sequenceName = "GL_SEQ_SESS", allocationSize = 1)
public class AppHttpSession extends BaseEntity<Long> {

    @Id
    @Column(name = "ID_ROW")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "AppHttpSessionIdSeq")
    private Long id;

    @Column(name = "SESSION_ID", nullable = false)
    private String sessionId;

    @Column(name = "CREATED_DT", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createDate;

    @Column(name = "USER_NAME", nullable = false)
    private String userName;

    @Column(name = "LASTACS_DT")
    private Date lastAccessTime;

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(Date lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

}
