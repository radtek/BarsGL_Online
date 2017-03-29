package ru.rbt.barsgl.shared.operday;

import java.io.Serializable;

/**
 * Created by akichigi on 28.03.17.
 */
public class COB_OKWrapper implements Serializable {
    private Integer state;
    private String reason;

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
