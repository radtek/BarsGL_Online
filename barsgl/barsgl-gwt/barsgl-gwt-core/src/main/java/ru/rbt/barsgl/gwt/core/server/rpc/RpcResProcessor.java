package ru.rbt.barsgl.gwt.core.server.rpc;

import ru.rbt.barsgl.shared.SqlQueryTimeoutException;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.shared.NotAuthorizedUserException;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.shared.ExceptionUtils;

import java.io.Serializable;
import java.sql.SQLException;

import static ru.rbt.shared.ExceptionUtils.getErrorMessage;

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
            SQLException ex = ExceptionUtils.getSqlTimeoutException(throwable);
            if( null != ex ) {
//                throw new SqlQueryTimeoutException();
                return new RpcRes_Base<>(null, true, new SqlQueryTimeoutException(ex).getUserMessage());
            }
            else {
                throwable.printStackTrace();
                return new RpcRes_Base<>(null, true, getErrorMessage(throwable, SQLException.class, DefaultApplicationException.class));
            }
        }
    }
}
