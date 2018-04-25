package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import ru.rbt.ejbcore.util.StringUtils;

import java.io.Serializable;

/**
 * Created by er18837 on 23.04.2018.
 */
public class QueueInputMessage implements Serializable {
    protected String textMessage;
    protected String requestId;
    protected String replyTo;

    public QueueInputMessage(String textMessage, String requestId, String replyTo) {
        this.textMessage = StringUtils.trim(textMessage);
        this.requestId = requestId;
        this.replyTo = replyTo;
    }

    public QueueInputMessage(String textMessage) {
        this(textMessage, null, null);
    }

    public String getTextMessage() {
        return textMessage;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getReplyTo() {
        return replyTo;
    }
}
