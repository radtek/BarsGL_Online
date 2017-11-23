package ru.rbt.barsgl.gwt.server.service;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.monitoring.SessionSupportBean;
import ru.rbt.barsgl.ejbcore.remote.ServerAccess;
import ru.rbt.barsgl.shared.access.HttpSessionWrapper;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.rbt.barsgl.gwt.serverutil.GwtServerUtils.findServerAccess;
import static ru.rbt.barsgl.shared.enums.AuthorizationInfoPath.USER_NAME;

/**
 * Created by Ivan Sevastyanov on 13.11.2017.
 */
public class AppSessionListener implements HttpSessionListener {

    private static Logger logger = Logger.getLogger(AppSessionListener.class.getName());

    private final ServerAccess localInvoker;

    public AppSessionListener() {
        localInvoker = findServerAccess();
    }

    @Override
    public void sessionCreated(HttpSessionEvent httpSessionEvent) {
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
        try {
            if (null != httpSessionEvent.getSession().getAttribute(USER_NAME.getPath())) {
                localInvoker.invoke(SessionSupportBean.class, "unregisterHttpSession", new HttpSessionWrapper(httpSessionEvent.getSession().getId()));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка снятия с регистрации httpsession пользователя", e);
            try {
                localInvoker.invoke(AuditController.class, "error", AuditRecord.LogCode.User, "Ошибка снятия регистрации httpsession пользователя", null, e);
            } catch (Exception e1) {
                logger.log(Level.SEVERE, "Unable to register error in audit record: " + e1.getMessage(), e1);
            }
        }
    }
}
