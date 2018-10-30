package ru.rbt.barsgl.ejbtesting.test;

import ru.rbt.barsgl.ejb.entity.monitor.AppHttpSession;
import ru.rbt.barsgl.ejb.monitoring.SessionSupportBean;
import ru.rbt.barsgl.ejb.repository.monitor.AppHttpSessionRepository;
import ru.rbt.barsgl.shared.LoginParams;
import ru.rbt.barsgl.shared.access.HttpSessionWrapper;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.security.ejb.repository.AppUserRepository;
import ru.rbt.security.entity.AppUser;
import ru.rbt.shared.Assert;
import ru.rbt.shared.ctx.UserRequestHolder;
import ru.rbt.shared.security.RequestContext;
import ru.rbt.shared.user.AppUserWrapper;

import javax.inject.Inject;
import java.util.Date;

import static ru.rbt.barsgl.shared.enums.AuthorizationInfoPath.USER_LOGIN_RESULT;

/**
 * Created by er18837 on 30.10.2018.
 */
public class ManualAuthTesting {

    @Inject
    private RequestContext context;

    @Inject
    private AppUserRepository userRepository;

    @Inject
    private SessionSupportBean sessionSupportBean;

    @Inject
    private AppHttpSessionRepository repository;

    public void logon(long userId) throws Exception {
        AppUser user = userRepository.selectFirst(AppUser.class, "from AppUser u where u.id = ?1", userId);
        Assert.notNull(user, "Не найден пользователь ID =" + userId);
        AppHttpSession session = sessionSupportBean.registerHttpSession(createSession(user.getUserName()));
        final UserRequestHolder holder = new UserRequestHolder();
        // проверяем передачу ссылки
        context.setRequest(holder);
        LoginParams params = new LoginParams();
        params.setSessionId(session.getSessionId());
        params.setUserName(session.getUserName());
        holder.setUser(session.getUserName());
        AppUserWrapper wrapper = new AppUserWrapper();
        wrapper.setId(userId);
        wrapper.setUserName(holder.getUser());
        holder.setUserWrapper(wrapper);    // TODO
        holder.setDynamicValue(USER_LOGIN_RESULT.getPath(), params);
    }

    public void logoff() {
        context.setRequest(null);
    }

    public AppHttpSession getSession(String sessionId) {
        AppHttpSession session = repository.selectFirst(AppHttpSession.class, "from AppHttpSession s where s.sessionId = ?1", sessionId);
        Assert.notNull(session);
        return session;
    }

    public HttpSessionWrapper createSession(String userName) {
        HttpSessionWrapper httpSession = new HttpSessionWrapper();
        httpSession.setSessionId(System.currentTimeMillis() + "");
        httpSession.setUserName(userName);
        httpSession.setCreateDate(new Date());
        httpSession.setLastAccessDate(DateUtils.addSeconds(new Date(), -100));
        return httpSession;
    }

}
