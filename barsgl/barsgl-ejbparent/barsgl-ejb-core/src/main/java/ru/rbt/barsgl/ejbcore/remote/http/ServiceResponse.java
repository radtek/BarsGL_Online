package ru.rbt.barsgl.ejbcore.remote.http;

import java.io.Serializable;

public final class ServiceResponse implements Serializable {

    private Object result;
    private ExceptionInfo error;

    public ServiceResponse(Object result) {
        this.result = result;
        error = null;
    }

    public ServiceResponse(ExceptionInfo error) {
        this.error = error;
        result = null;
    }

    public boolean isSuccessful() {
        return error == null;
    }

    public Object getResult() {
        return result;
    }

    public ExceptionInfo getError() {
        return error;
    }

}
