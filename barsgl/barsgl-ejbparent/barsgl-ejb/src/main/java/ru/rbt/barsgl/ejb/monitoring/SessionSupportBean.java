package ru.rbt.barsgl.ejb.monitoring;

import ru.rbt.barsgl.ejb.entity.monitor.AppHttpSession;
import ru.rbt.barsgl.ejb.repository.monitor.AppSessionListenerRepository;
import ru.rbt.barsgl.shared.LoginParams;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.shared.Assert;

import javax.ejb.AccessTimeout;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.barsgl.shared.enums.AuthorizationInfoPath.USER_NAME;

/**
 * Created by Ivan Sevastyanov on 13.11.2017.
 */
@Singleton
@AccessTimeout(value = 10, unit = TimeUnit.MINUTES)
public class SessionSupportBean {

    private static final Logger log = Logger.getLogger(SessionSupportBean.class.getName());

    @Inject
    private AppSessionListenerRepository repository;

    private List<HttpSession> store = new ArrayList<>();

    public AppHttpSession registerHttpSession (HttpSession httpSession) {
        LoginParams params = (LoginParams) httpSession.getAttribute(USER_NAME.getPath());
        Assert.isTrue(null != params, ()->new DefaultApplicationException(format("Session '%s' is not authorized", httpSession.getId())));
        AppHttpSession dbSession = new AppHttpSession();
        dbSession.setUserName(params.getUserName());
        dbSession.setSessionId(httpSession.getId());
        dbSession.setCreateDate(new Date(httpSession.getCreationTime()));
        dbSession.setLastAccessTime(new Date(httpSession.getLastAccessedTime()));
        store.add(httpSession);
        return repository.save(dbSession);
    }

    public void unregisterHttpSession(HttpSession httpSession) {
        LoginParams params = (LoginParams) httpSession.getAttribute(USER_NAME.getPath());
        if (null != params) {
            store.remove(httpSession);
            int cnt = repository.executeUpdate("delete from AppHttpSession s where s.sessionId = ?1", httpSession.getId());
            log.info(format("Удалено строк из хранилища сессий %s по session_id %s", cnt, httpSession.getId()));
        }
    }

}
