package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import java.io.Serializable;

/**
 * Created by er18837 on 23.03.2018.
 */
public class ProcessResult implements Serializable{
    private String outMessage;
    private boolean isError;
    private boolean writeOut;

    public ProcessResult(String outMessage, boolean isError, boolean writeOut) {
        this.outMessage = outMessage;
        this.isError = isError;
        this.writeOut = writeOut;
    }

    public ProcessResult(String outMessage, boolean isError) {
        this(outMessage, isError, false);
    }

    public String getOutMessage() {
        return outMessage;
    }

    public boolean isError() {
        return isError;
    }

    public void setWriteOut(boolean writeOut) {
        this.writeOut = writeOut;
    }

    public boolean isWriteOut() {
        return writeOut;
    }
}
