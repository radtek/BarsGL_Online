package ru.rbt.barsgl.ejb.controller.operday.task.srvacc;

import java.io.Serializable;

/**
 * Created by er18837 on 23.03.2018.
 */
public class InputMessage implements Serializable {
    private String textMessage;
    private String requestId;
    private String replyTo;

    public InputMessage(String textMessage, String requestId, String replyTo) {
        this.textMessage = textMessage;
        this.requestId = requestId;
        this.replyTo = replyTo;
    }

    public InputMessage(String textMessage) {
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
