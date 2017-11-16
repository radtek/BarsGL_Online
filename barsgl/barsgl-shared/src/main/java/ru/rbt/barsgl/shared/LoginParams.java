package ru.rbt.barsgl.shared;

import java.io.Serializable;

/**
 * Created by ER18837 on 15.09.15.
 */
public class LoginParams implements Serializable{

    private String userName;
    private String userType;
    private String hostName;

    public LoginParams() {
    }

    public LoginParams(String userName, String userType, String hostName) {
        this.userName = userName;
        this.userType = userType;
        this.hostName = hostName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public static LoginParams createNotAuthorizedLoginParams() {
        return new LoginParams("Not authorized user", "no type", "no host");
    }
}
