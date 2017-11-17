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
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
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

    public void unregisterHttpSession(HttpSessionWrapper httpSession) {
        if (null != httpSession && null != httpSession.getSessionId()) {
            log.info(format("Удалена сессия из локального хранилища %s",store.remove(httpSession)));
            int cnt = repository.executeUpdate("delete from AppHttpSession s where s.sessionId = ?1", httpSession.getSessionId());
            log.info(format("Удалено строк из БД хранилища сессий %s по session_id %s", cnt, httpSession.getSessionId()));
        }
    }

    public boolean invalidateSession(String sessionId) throws Exception {
        HttpSessionWrapper target = store.stream().filter(a -> a.getSessionId().split("!")[0].equals(sessionId.split("!")[0])).findFirst().orElse(null);
        if (null != target) {
            repository.invalidateSession(sessionId);
            return invalidateMbeanSession(sessionId);
        }
        return false;
    }

    public void invalidateUserSessions(String userName) throws Exception {
        List<AppHttpSession> sessions = repository.select(AppHttpSession.class, "from AppHttpSession s where s.userName = ?1", userName);
        for (AppHttpSession session : sessions) {
            unregisterHttpSession(new HttpSessionWrapper(session.getSessionId()));
            invalidateMbeanSession(session.getSessionId());
        }
    }

    public void invalidateAllSessions() throws Exception {
        String sessionId = getCurrentSessionId();
        List<AppHttpSession> sessions = repository.select(AppHttpSession.class, "from AppHttpSession s where s.sessionId <> ?1", null != sessionId ? sessionId : "-1");
        for (AppHttpSession session : sessions) {
            unregisterHttpSession(new HttpSessionWrapper(session.getSessionId()));
            invalidateMbeanSession(session.getSessionId());
        }
    }

    private boolean invalidateMbeanSession(String sessionId) throws Exception {
        InitialContext ctx = new InitialContext();
        MBeanServer server = (MBeanServer)ctx.lookup("java:comp/env/jmx/runtime");
        ObjectName webAppComponentRuntime = findWebAppComponentRuntime(server);
        Assert.notNull(webAppComponentRuntime, "MBean is not found");
        try {
            String monitoringSessionId = (String) server.invoke(webAppComponentRuntime, "getMonitoringId", new Object[]{sessionId}, new String[]{"java.lang.String"});
            server.invoke(webAppComponentRuntime, "invalidateServletSession", new Object[]{monitoringSessionId}, new String[]{"java.lang.String"});
            log.info(format("Session '%s' is invalidated successfully", sessionId));
        } catch (Exception e) {
            log.log(Level.WARNING, format("Error on invalidating sessionId '%s' ", sessionId), e);
            return false;
        }
        return true;
    }

    private ObjectName findWebAppComponentRuntime(MBeanServer server) throws Exception {
        ObjectName srv0 = (ObjectName)server.getAttribute(new ObjectName("com.bea:Name=RuntimeService,Type=weblogic.management.mbeanservers.runtime.RuntimeServiceMBean"), "ServerRuntime");
        ObjectName[] serverRT = (ObjectName[]) server.getAttribute(srv0, "ApplicationRuntimes");
        for (ObjectName objsrvrt : serverRT) {
            ObjectName[] compRT = (ObjectName[]) server.getAttribute(objsrvrt, "ComponentRuntimes");
            for (ObjectName objcmprt : compRT) {
                if ("WebAppComponentRuntime".equals(server.getAttribute(objcmprt, "Type"))
                        && server.getAttribute(objcmprt, "Name").toString().contains("barsgl")) {
                    return objcmprt;
                }
            }
        }
        return null;
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
