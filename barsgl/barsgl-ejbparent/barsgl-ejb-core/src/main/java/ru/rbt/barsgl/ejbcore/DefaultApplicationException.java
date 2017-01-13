package ru.rbt.barsgl.ejbcore;

import java.io.Serializable;

/**
 * Created by Ivan Sevastyanov
 */
public final class DefaultApplicationException extends RuntimeException implements Serializable {

    public DefaultApplicationException() {
    }

    public DefaultApplicationException(Throwable cause) {
        super(cause);
    }

    public DefaultApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DefaultApplicationException(String message) {
        super(message);
    }
}
