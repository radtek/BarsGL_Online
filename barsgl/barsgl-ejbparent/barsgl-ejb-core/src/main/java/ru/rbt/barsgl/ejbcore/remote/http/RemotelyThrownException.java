package ru.rbt.barsgl.ejbcore.remote.http;

public final class RemotelyThrownException extends Exception {

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    public RemotelyThrownException(ExceptionInfo info) {
        super(info.getClassName() + ": " + info.getMessage());
        setStackTrace(info.getStackTrace());
        ExceptionInfo causeInfo = info.getCause();
        if (causeInfo != null) {
            initCause( causeInfo.createThrowable() );
        }
    }

}
