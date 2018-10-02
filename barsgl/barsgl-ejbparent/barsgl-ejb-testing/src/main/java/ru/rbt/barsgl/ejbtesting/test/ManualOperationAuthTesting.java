package ru.rbt.barsgl.ejbtesting.test;

import ru.rbt.barsgl.ejb.entity.monitor.AppHttpSession;
import ru.rbt.barsgl.ejb.integr.bg.BatchPackageController;
import ru.rbt.barsgl.ejb.integr.bg.ManualPostingController;
import ru.rbt.barsgl.ejb.monitoring.SessionSupportBean;
import ru.rbt.barsgl.ejb.repository.monitor.AppHttpSessionRepository;
import ru.rbt.barsgl.shared.LoginParams;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.access.HttpSessionWrapper;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.security.ejb.repository.AppUserRepository;
import ru.rbt.security.entity.AppUser;
import ru.rbt.shared.Assert;
import ru.rbt.shared.ctx.UserRequestHolder;
import ru.rbt.shared.security.RequestContext;

import javax.inject.Inject;

import java.util.Date;

import static ru.rbt.barsgl.shared.enums.AuthorizationInfoPath.USER_LOGIN_RESULT;

/**
 * Created by er18837 on 02.10.2018.
 */
public class ManualOperationAuthTesting {

    @Inject
    private RequestContext context;

    @Inject
    private AppUserRepository userRepository;

    @Inject
    private AppHttpSessionRepository repository;

    @Inject
    private SessionSupportBean sessionSupportBean;

    @Inject
    private ManualPostingController postingController;

    @Inject
    BatchPackageController packageController;

    public RpcRes_Base<ManualOperationWrapper> authorizeOperationRq(ManualOperationWrapper wrapper) throws Exception {
        try {
            logon(wrapper.getUserId());
            RpcRes_Base<ManualOperationWrapper> res = postingController.authorizeOperationRq(wrapper);
            return res;
        } finally {
            logoff();
        }
    }

    public RpcRes_Base<ManualOperationWrapper> deleteOperationRq(ManualOperationWrapper wrapper) throws Exception {
         try {
            logon(wrapper.getUserId());
            RpcRes_Base<ManualOperationWrapper> res = postingController.deleteOperationRq(wrapper);
            return res;
        } finally {
            logoff();
        }
    }

    public RpcRes_Base<ManualOperationWrapper> refuseOperationRq(ManualOperationWrapper wrapper, BatchPostStatus postStatus) throws Exception {
        try {
            logon(wrapper.getUserId());
            RpcRes_Base<ManualOperationWrapper> res = postingController.refuseOperationRq(wrapper, postStatus);
            return res;
        } finally {
            logoff();
        }
    }

    public RpcRes_Base<ManualOperationWrapper> authorizePackageRq(ManualOperationWrapper wrapper, BatchPostStep postStep) throws Exception {
        try {
            logon(wrapper.getUserId());
            RpcRes_Base<ManualOperationWrapper> res = packageController.authorizePackageRq(wrapper, postStep);
            return res;
        } finally {
            logoff();
        }
    }

    public RpcRes_Base<ManualOperationWrapper> deletePackageRq(ManualOperationWrapper wrapper) throws Exception {
        try {
            logon(wrapper.getUserId());
            RpcRes_Base<ManualOperationWrapper> res = packageController.deletePackageRq(wrapper);
            return res;
        } finally {
            logoff();
        }
    }

    private void logon(long userId) throws Exception {
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
        holder.setDynamicValue(USER_LOGIN_RESULT.getPath(), params);
    }

    private void logoff() {
        context.setRequest(null);
    }

    private AppHttpSession getSession(String sessionId) {
        AppHttpSession session = repository.selectFirst(AppHttpSession.class, "from AppHttpSession s where s.sessionId = ?1", sessionId);
        Assert.notNull(session);
        return session;
    }

    private HttpSessionWrapper createSession(String userName) {
        HttpSessionWrapper httpSession = new HttpSessionWrapper();
        httpSession.setSessionId(System.currentTimeMillis() + "");
        httpSession.setUserName(userName);
        httpSession.setCreateDate(new Date());
        httpSession.setLastAccessDate(DateUtils.addSeconds(new Date(), -100));
        return httpSession;
    }

}
