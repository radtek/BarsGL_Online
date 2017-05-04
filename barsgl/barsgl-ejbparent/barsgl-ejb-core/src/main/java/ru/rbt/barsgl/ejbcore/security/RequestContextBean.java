package ru.rbt.barsgl.ejbcore.security;

import ru.rbt.shared.ctx.UserRequestHolder;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import java.util.Optional;
import ru.rbt.shared.security.RequestContext;

/**
 * Created by Ivan Sevastyanov on 20.02.2016.
 */

@Singleton(name = "ApplicationRequestContext")
@Lock(LockType.READ)
public class RequestContextBean implements RequestContext {

    private static final ThreadLocal<UserRequestHolder> threadLocalRequestHolder = new ThreadLocal<>();

    public void setRequest(UserRequestHolder requestHolder) {
        threadLocalRequestHolder.set(requestHolder);
    }

    public Optional<UserRequestHolder> getRequest() {
        return Optional.ofNullable(threadLocalRequestHolder.get());
    }

}
