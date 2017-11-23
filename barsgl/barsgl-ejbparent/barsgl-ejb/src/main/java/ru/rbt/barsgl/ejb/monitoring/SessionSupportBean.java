package ru.rbt.barsgl.ejb.monitoring;

import ru.rbt.barsgl.ejb.entity.monitor.AppHttpSession;
import ru.rbt.barsgl.ejb.repository.monitor.AppHttpSessionRepository;
import ru.rbt.barsgl.shared.LoginParams;
import ru.rbt.barsgl.shared.access.HttpSessionWrapper;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.shared.Assert;
import ru.rbt.shared.ctx.UserRequestHolder;
import ru.rbt.shared.security.RequestContext;

import javax.annotation.PostConstruct;
import javax.ejb.AccessTimeout;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.barsgl.shared.enums.AuthorizationInfoPath.USER_LOGIN_RESULT;
import static ru.rbt.ejbcore.mapping.YesNo.N;

/**
 * Created by Ivan Sevastyanov on 13.11.2017.
 */
@Singleton
@AccessTimeout(value = 10, unit = TimeUnit.MINUTES)
@Startup
public class SessionSupportBean {

    private static final Logger log = Logger.getLogger(SessionSupportBean.class.getName());

    @Inject
    private AppHttpSessionRepository repository;

    @Inject
    private RequestContext contextBean;

    private List<HttpSessionWrapper> store = new ArrayList<>();

    public AppHttpSession registerHttpSession (HttpSessionWrapper httpSession) {
        Assert.isTrue(null != httpSession && null != httpSession.getUserName() && !StringUtils.isEmpty(httpSession.getSessionId())
                , ()->new DefaultApplicationException(format("Session '%s' is not authorized"
                        , null != httpSession ? httpSession.getSessionId() : "<sessionId=null>")));
        AppHttpSession dbSession = new AppHttpSession();
        dbSession.setUserName(httpSession.getUserName());
        dbSession.setSessionId(httpSession.getSessionId());
        dbSession.setCreateDate(httpSession.getCreateDate());
        dbSession.setLastAccessTime(httpSession.getLastAccessDate());
        dbSession.setInvalidated(N);
        store.add(httpSession);
        return repository.save(dbSession);
    }

    public boolean unregisterHttpSession(HttpSessionWrapper httpSession) {
        if (null != httpSession && null != httpSession.getSessionId()) {
            boolean clearStore = store.remove(httpSession);
            log.info(format("Удалена сессия из локального хранилища %s", clearStore));
            int cnt = repository.executeUpdate("delete from AppHttpSession s where s.sessionId = ?1", httpSession.getSessionId());
            log.info(format("Удалено строк из БД хранилища сессий %s по session_id %s", cnt, httpSession.getSessionId()));
            return cnt > 0 && clearStore;
        }
        return false;
    }

    public boolean checkSessionInStore(String sessionId) {
        if (store.contains(new HttpSessionWrapper(sessionId))) {
            repository.pingSession(sessionId);
            return true;
        }
        return false;
    }

    public boolean invalidateSession(String sessionId) throws Exception {
        return unregisterHttpSession(new HttpSessionWrapper(sessionId));
    }

    public void invalidateUserSessions(String userName) throws Exception {
        List<AppHttpSession> sessions = repository.select(AppHttpSession.class, "from AppHttpSession s where s.userName = ?1", userName);
        for (AppHttpSession session : sessions) {
            unregisterHttpSession(new HttpSessionWrapper(session.getSessionId()));
        }
    }

    public void invalidateAllSessions() throws Exception {
        String sessionId = getCurrentSessionId();
        List<AppHttpSession> sessions = repository.select(AppHttpSession.class, "from AppHttpSession s where s.sessionId <> ?1", null != sessionId ? sessionId : "-1");
        for (AppHttpSession session : sessions) {
            unregisterHttpSession(new HttpSessionWrapper(session.getSessionId()));
        }
    }

    private String getCurrentSessionId() {
        UserRequestHolder holder = contextBean.getRequest().orElse(UserRequestHolder.empty());
        LoginParams params = holder.isEmptyHolder() ? null : holder.getDynamicValue(USER_LOGIN_RESULT.getPath());
        if (null != params) return params.getSessionId(); else return null;
    }

    @PostConstruct
    public void init() {
        log.info(format("Инициализация хранилища сессий. Удалено старых сеансов: %s"
                , repository.executeUpdate("delete from AppHttpSession s")));
    }

}
