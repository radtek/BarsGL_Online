package ru.rbt.security.gwt.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.security.gwt.client.security.SecurityEntryPoint;
//import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;

/**
 * Created by Ivan Sevastyanov
 */
public abstract class AuthCheckAsyncCallback<T> implements AsyncCallback<T> {

    @Override
    public final void onFailure(Throwable throwable) {
        if (WaitingManager.isWaiting()) {
            WaitingManager.hide();
        }
        if (SecurityEntryPoint.isNotAuthorizedUserException(throwable)) {
            SecurityEntryPoint.showLoginForm();
        } else {
            onFailureOthers(throwable);
        }
    }

    public void onFailureOthers(Throwable throwable) {
        throw new RuntimeException(throwable);
    }

}
