package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.entity.monitor.AppHttpSession;
import ru.rbt.barsgl.ejb.monitoring.SessionSupportBean;
import ru.rbt.barsgl.ejbtesting.test.HttpSessionTesting;
import ru.rbt.barsgl.shared.access.HttpSessionWrapper;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.DateUtils;

import java.sql.SQLException;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov on 14.11.2017.
 */
public class HttpSessionIT extends AbstractRemoteIT {

    @Test public void testRegister() {
        HttpSessionWrapper httpSession = createSession();
        AppHttpSession dbsess = remoteAccess.invoke(SessionSupportBean.class, "registerHttpSession", httpSession);
        Assert.assertNotNull(dbsess);

        remoteAccess.invoke(SessionSupportBean.class, "unregisterHttpSession", httpSession);
        Assert.assertNull(baseEntityRepository.selectFirst(AppHttpSession.class, "from AppHttpSession s where sessionId = ?1", httpSession.getSessionId()));

        httpSession = createSession();
        remoteAccess.invoke(SessionSupportBean.class, "registerHttpSession", httpSession);
        remoteAccess.invoke(SessionSupportBean.class, "invalidateSession", httpSession.getSessionId());
        Assert.assertEquals(YesNo.Y
                , ((AppHttpSession)baseEntityRepository.selectFirst(AppHttpSession.class
                        , "from AppHttpSession s where sessionId = ?1", httpSession.getSessionId())).getInvalidated());
    }

    @Test public void testInvalidateByName() throws SQLException {
        remoteAccess.invoke(SessionSupportBean.class, "invalidateAllSessions");
        Assert.assertEquals(0, (baseEntityRepository.selectOne("select count(1) from gl_httpsess").getInteger(0)).longValue());

        AppHttpSession dbsess1 = remoteAccess.invoke(SessionSupportBean.class, "registerHttpSession", createSession());
        Assert.assertNotNull(dbsess1);

        AppHttpSession dbsess2 = remoteAccess.invoke(SessionSupportBean.class, "registerHttpSession", createSession());
        Assert.assertNotNull(dbsess2);

        Assert.assertEquals(2, (baseEntityRepository.selectOne("select count(1) from gl_httpsess").getInteger(0)).longValue());
        remoteAccess.invoke(SessionSupportBean.class, "invalidateUserSessions", createSession().getUserName());
        Assert.assertEquals(0, (baseEntityRepository.selectOne("select count(1) from gl_httpsess").getInteger(0)).longValue());
    }

    @Test public void testInvalidateAllButNotMy() throws SQLException {
        remoteAccess.invoke(SessionSupportBean.class, "invalidateAllSessions");
        AppHttpSession dbsess1 = remoteAccess.invoke(SessionSupportBean.class, "registerHttpSession", createSession());
        Assert.assertNotNull(dbsess1);

        AppHttpSession dbsess2 = remoteAccess.invoke(SessionSupportBean.class, "registerHttpSession", createSession());
        Assert.assertNotNull(dbsess2);

        Assert.assertEquals(2, (baseEntityRepository.selectOne("select count(1) from gl_httpsess").getInteger(0)).longValue());

        remoteAccess.invoke(HttpSessionTesting.class, "invalidateAll", dbsess1.getSessionId());

        Assert.assertEquals(1, (baseEntityRepository.selectOne("select count(1) from gl_httpsess").getInteger(0)).longValue());

        AppHttpSession fromDb = (AppHttpSession) baseEntityRepository.selectFirst(AppHttpSession.class, "from AppHttpSession s where s.sessionId = ?1", dbsess1.getSessionId());
        Assert.assertNotNull(fromDb);
    }

    private HttpSessionWrapper createSession() {
        HttpSessionWrapper httpSession = new HttpSessionWrapper();
        httpSession.setSessionId(System.currentTimeMillis() + "");
        httpSession.setUserName("user1");
        httpSession.setCreateDate(new Date());
        httpSession.setLastAccessDate(DateUtils.addSeconds(new Date(), -100));
        return httpSession;
    }
}
