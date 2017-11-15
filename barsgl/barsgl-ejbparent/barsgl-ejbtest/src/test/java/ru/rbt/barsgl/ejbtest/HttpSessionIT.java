package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.entity.monitor.AppHttpSession;
import ru.rbt.barsgl.ejb.monitoring.SessionSupportBean;
import ru.rbt.barsgl.ejbtesting.mock.HttpSessionImpl;
import ru.rbt.barsgl.shared.LoginParams;
import ru.rbt.ejbcore.util.DateUtils;

import java.util.Date;

import static ru.rbt.barsgl.shared.enums.AuthorizationInfoPath.USER_NAME;

/**
 * Created by Ivan Sevastyanov on 14.11.2017.
 */
public class HttpSessionIT extends AbstractRemoteIT {

    @Test public void test1() {
        HttpSessionImpl httpSession = new HttpSessionImpl();
        httpSession.setAttribute(USER_NAME.getPath(), new LoginParams("user1", null, "192.168.1.1"));
        httpSession.getLastAccessedTime();
        httpSession.setLastAccessedTime(DateUtils.addSeconds(new Date(), -100).getTime());
        AppHttpSession dbsess = remoteAccess.invoke(SessionSupportBean.class, "registerHttpSession", httpSession);
        Assert.assertNotNull(dbsess);

        remoteAccess.invoke(SessionSupportBean.class, "unregisterHttpSession", httpSession);
        Assert.assertNull(baseEntityRepository.selectFirst(AppHttpSession.class, "from AppHttpSession s where sessionId = ?1", httpSession.getId()));
    }
}
