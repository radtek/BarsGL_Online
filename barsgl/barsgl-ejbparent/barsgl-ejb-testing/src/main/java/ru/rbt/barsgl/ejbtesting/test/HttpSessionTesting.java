package ru.rbt.barsgl.ejbtesting.test;

import ru.rbt.barsgl.ejb.entity.monitor.AppHttpSession;
import ru.rbt.barsgl.ejb.monitoring.SessionSupportBean;
import ru.rbt.barsgl.ejb.repository.monitor.AppHttpSessionRepository;
import ru.rbt.barsgl.shared.LoginParams;
import ru.rbt.shared.Assert;
import ru.rbt.shared.ctx.UserRequestHolder;
import ru.rbt.shared.security.RequestContext;

import javax.inject.Inject;

import static ru.rbt.barsgl.shared.enums.AuthorizationInfoPath.USER_LOGIN_RESULT;

/**
 * Created by Ivan Sevastyanov on 17.11.2017.
 */
public class HttpSessionTesting {


    @Inject
    private RequestContext context;

    @Inject
    private SessionSupportBean sessionSupportBean;

    @Inject
    private AppHttpSessionRepository repository;

    public void invalidateAll(String butNotThis) throws Exception {
        try {
            final UserRequestHolder holder = new UserRequestHolder();
            // проверяем передачу ссылки
            context.setRequest(holder);
            LoginParams params = new LoginParams();
            params.setSessionId(butNotThis);
            AppHttpSession session = getSession(butNotThis);
            params.setUserName(session.getUserName());
            holder.setUser(session.getUserName());
            holder.setDynamicValue(USER_LOGIN_RESULT.getPath(), params);

            sessionSupportBean.invalidateAllSessions();
        } finally {
            context.setRequest(null);
        }
    }

    private AppHttpSession getSession(String sessionId) {
        AppHttpSession session = repository.selectFirst(AppHttpSession.class, "from AppHttpSession s where s.sessionId = ?1", sessionId);
        Assert.notNull(session);
        return session;
    }

}
