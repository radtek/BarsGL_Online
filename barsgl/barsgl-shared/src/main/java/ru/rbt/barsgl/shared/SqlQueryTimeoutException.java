package ru.rbt.barsgl.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.sql.SQLException;

/**
 * Created by er18837 on 05.09.2017.
 */
public class SqlQueryTimeoutException extends Exception {

    public static final String SQL_TIMEOUT_MESSAGE = "Sql query timeout";
    private int limit = 10;

    public SqlQueryTimeoutException() {
    }

    public SqlQueryTimeoutException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getMessage() {
        return SQL_TIMEOUT_MESSAGE;
    }

    public String getUserMessage() {
        return "Время выполнения запроса превышает лимит\nПопробуйте выполнить другой запрос";
    }

}
