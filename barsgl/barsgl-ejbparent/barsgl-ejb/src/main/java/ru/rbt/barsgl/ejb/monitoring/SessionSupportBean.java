package ru.rbt.barsgl.ejb.monitoring;

import ru.rbt.barsgl.ejb.entity.monitor.AppHttpSession;
import ru.rbt.barsgl.ejb.repository.monitor.AppSessionListenerRepository;
import ru.rbt.barsgl.shared.LoginParams;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.shared.Assert;

import javax.annotation.PostConstruct;
import javax.ejb.AccessTimeout;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.barsgl.shared.enums.AuthorizationInfoPath.USER_NAME;
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
        dbSession.setInvalidated(N);
        store.add(httpSession);
        return repository.save(dbSession);
    }

    public void unregisterHttpSession(HttpSession httpSession) {
        LoginParams params = (LoginParams) httpSession.getAttribute(USER_NAME.getPath());
        if (null != params) {
            log.info(format("Удалена сессия из локального хранилища %s",store.remove(httpSession)));
            int cnt = repository.executeUpdate("delete from AppHttpSession s where s.sessionId = ?1", httpSession.getId());
            log.info(format("Удалено строк из БД хранилища сессий %s по session_id %s", cnt, httpSession.getId()));
        }
    }

    public boolean invalidateSession(String sessionId) throws Exception {
        HttpSession target = store.stream().filter(a -> a.getId().split("!")[0].equals(sessionId.split("!")[0])).findFirst().orElse(null);
        if (null != target) {
            repository.invalidateSession(sessionId);
            invalidateMbeanSession(sessionId);
            return true;
        }
        return false;
    }

    public boolean invalidateTest(String sessionId) throws Exception {
        InitialContext ctx = new InitialContext();
        MBeanServer server = (MBeanServer)ctx.lookup("java:comp/env/jmx/runtime");
        getServletData(server);
        return false;
    }

    public void getServletData(MBeanServer server) throws Exception {
        ObjectName srv0 = (ObjectName)server.getAttribute(new ObjectName("com.bea:Name=RuntimeService,Type=weblogic.management.mbeanservers.runtime.RuntimeServiceMBean"), "ServerRuntime");
        ObjectName[] serverRT = (ObjectName[]) server.getAttribute(srv0, "ApplicationRuntimes");
        int length = (int) serverRT.length;
        for (int i = 0; i < length; i++) {
//            ObjectName[] appRT =
//                    (ObjectName[]) server.getAttribute(serverRT[i],
//                            "ApplicationRuntimes");
//            int appLength = (int) appRT.length;
            int appLength = (int) serverRT.length;
            for (int x = 0; x < appLength; x++) {
                System.out.println("Application name: " +
                        (String) server.getAttribute(serverRT[x], "Name"));
                ObjectName[] compRT =
                        (ObjectName[]) server.getAttribute(serverRT[x],
                                "ComponentRuntimes");
                int compLength = (int) compRT.length;
                for (int y = 0; y < compLength; y++) {
                    System.out.println("  Component name: " +
                            (String) server.getAttribute(compRT[y], "Name"));
                    String componentType =
                            (String) server.getAttribute(compRT[y], "Type");
                    System.out.println(componentType.toString());
                    if (componentType.toString().equals("WebAppComponentRuntime")) {
                        ObjectName[] servletRTs = (ObjectName[])
                                server.getAttribute(compRT[y], "Servlets");
                        int servletLength = (int) servletRTs.length;
                        for (int z = 0; z < servletLength; z++) {
                            System.out.println("    Servlet name: " +
                                    (String) server.getAttribute(servletRTs[z],
                                            "Name"));
                            System.out.println("       Servlet context path: " +
                                    (String) server.getAttribute(servletRTs[z],
                                            "ContextPath"));
                            System.out.println("       Invocation Total Count : " +
                                    (Object) server.getAttribute(servletRTs[z],
                                            "InvocationTotalCount"));
                        }
                    }
                }
            }
        }
    }

    private boolean invalidateMbeanSession(String sessionId) throws Exception {
        InitialContext ctx = new InitialContext();
        MBeanServer server = (MBeanServer)ctx.lookup("java:comp/env/jmx/runtime");
        ObjectName webAppComponentRuntime = findWebAppComponentRuntime(server);
        Assert.notNull(webAppComponentRuntime);
        try {
            String monitoringSessionId = (String) server.invoke(webAppComponentRuntime, "getMonitoringId", new Object[]{sessionId}, new String[]{"java.lang.String"});
            server.invoke(webAppComponentRuntime, "invalidateServletSession", new Object[]{monitoringSessionId}, new String[]{"java.lang.String"});
        } catch (Exception e) {
            e.printStackTrace();
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

    @PostConstruct
    public void init() {
        log.info(format("Инициализация хранилища сессий. Удалено старых сеансов: %s"
                , repository.executeUpdate("delete from AppHttpSession s")));
    }

}
