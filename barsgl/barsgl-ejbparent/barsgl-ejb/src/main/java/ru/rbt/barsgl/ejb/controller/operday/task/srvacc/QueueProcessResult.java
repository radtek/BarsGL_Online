package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import java.io.Serializable;

/**
 * Created by er18837 on 23.04.2018.
 */
public class QueueProcessResult implements Serializable {
    protected String outMessage;
    protected boolean isError;
    protected boolean writeOut;

    public QueueProcessResult(String outMessage, boolean isError, boolean writeOut) {
        this.outMessage = outMessage;
        this.isError = isError;
        this.writeOut = writeOut;
    }

    public QueueProcessResult(String outMessage, boolean isError) {
        this(outMessage, isError, false);
    }

    public String getOutMessage() {
        return outMessage;
    }

    public boolean isError() {
        return isError;
    }

    public boolean isWriteOut() {
        return writeOut;
    }

    public void setWriteOut(boolean writeOut) {
        this.writeOut = writeOut;
    }
}
