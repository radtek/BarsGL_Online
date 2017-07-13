package ru.rbt.security.gwt.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.security.gwt.client.security.SecurityEntryPoint;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Ivan Sevastyanov
 */
public abstract class AuthCheckAsyncCallback<T> implements AsyncCallback<T> {

    public static final Logger logger = Logger.getLogger("AuthCheckAsyncCallback");

    @Override
    public final void onFailure(Throwable throwable) {
        logger.log(Level.SEVERE, "on failure triggered: message '" + throwable.getMessage() + "'");
        if (WaitingManager.isWaiting()) {
            WaitingManager.hide();
        }
        if (SecurityEntryPoint.isNotAuthorizedUserException(throwable)) {
            SecurityEntryPoint.showLoginForm();
            afterReauthorize(throwable);
        } else {
            onFailureOthers(throwable);
        }
    }

    public void onFailureOthers(Throwable throwable) {
        throw new RuntimeException(throwable);
    }

    protected void afterReauthorize(Throwable afterException) {}
}
