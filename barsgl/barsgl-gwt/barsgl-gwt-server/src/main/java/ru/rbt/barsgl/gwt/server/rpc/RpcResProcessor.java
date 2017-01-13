package ru.rbt.barsgl.gwt.server.rpc;

import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.shared.NotAuthorizedUserException;
import ru.rbt.barsgl.shared.RpcRes_Base;

import java.io.Serializable;
import java.sql.SQLException;

import static ru.rbt.barsgl.shared.ExceptionUtils.getErrorMessage;

/**
 * Created by Ivan Sevastyanov
 */
public abstract class RpcResProcessor<T extends Serializable> {

    protected abstract RpcRes_Base<T> buildResponse() throws Throwable;

    public final RpcRes_Base<T> process() throws Exception {
        try {
            return buildResponse();
        } catch (NotAuthorizedUserException notAuthorized) {
            throw notAuthorized;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return new RpcRes_Base<>(null, true, getErrorMessage(throwable, SQLException.class, DefaultApplicationException.class));
        }
    }
}
