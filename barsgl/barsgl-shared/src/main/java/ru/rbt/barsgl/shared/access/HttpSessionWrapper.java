package ru.rbt.barsgl.shared.access;

import com.google.gwt.user.client.rpc.IsSerializable;
import ru.rbt.barsgl.shared.enums.YesNoType;
import ru.rbt.shared.Assert;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Created by Ivan Sevastyanov on 17.11.2017.
 */
public class HttpSessionWrapper implements Serializable, IsSerializable {

    private String userName;
    private String sessionId;
    private Date createDate;
    private Date lastAccessDate;
    private YesNoType invalidated;

    public HttpSessionWrapper() {
    }

    public HttpSessionWrapper(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        Assert.isTrue(null != sessionId && !"".equals(sessionId), "SesssionId is empty");
        this.sessionId = sessionId;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getLastAccessDate() {
        return lastAccessDate;
    }

    public void setLastAccessDate(Date lastAccessDate) {
        this.lastAccessDate = lastAccessDate;
    }

    public YesNoType getInvalidated() {
        return invalidated;
    }

    public void setInvalidated(YesNoType invalidated) {
        this.invalidated = invalidated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpSessionWrapper that = (HttpSessionWrapper) o;
        return Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }
}
