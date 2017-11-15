package ru.rbt.barsgl.gwt.server.service;

import ru.rbt.barsgl.ejbcore.remote.ServerAccess;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import static ru.rbt.barsgl.gwt.serverutil.GwtServerUtils.findServerAccess;

/**
 * Created by Ivan Sevastyanov on 13.11.2017.
 */
public class AppSessionListener implements HttpSessionListener {

    private final ServerAccess localInvoker;

    public AppSessionListener() {
        localInvoker = findServerAccess();
    }

    @Override
    public void sessionCreated(HttpSessionEvent httpSessionEvent) {

    }

    @Override
    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {

    }
}
