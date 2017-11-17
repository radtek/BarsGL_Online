package ru.rbt.shared;

import ru.rbt.shared.access.UserMenuWrapper;
import ru.rbt.shared.enums.SecurityActionCode;
import ru.rbt.shared.user.AppUserWrapper;

import java.io.Serializable;
import java.util.List;

import static ru.rbt.shared.ContextData.*;
import static ru.rbt.shared.LoginResult.LoginResultStatus.*;

/**
 * Created by Ivan Sevastyanov
 */
public class LoginResult implements Serializable {

    public enum LoginResultStatus {
        SUCCEEDED, FAILED, LIMIT
    }
    private LoginResultStatus loginResultStatus = FAILED;

    private ContextData context = new ContextData();

    public String getHost() {
        return context.getUserHost();
    }

    public void setHost(String host) {
        context.setUserHost(host);
    }

    public void setMessage(String message) {
        context.setParam(MESSAGE_PATH, message);
    }

    public void setUserName(String userName) {
        context.setUserName(userName);
    }

    public String getUserName() {
        return context.getUserName();
    }

    public void setUser(AppUserWrapper user){
        context.setUser(user);
    }

    public AppUserWrapper getUser(){
        return context.getUser();
    }

    public void setUserType(String userType) {
        context.setUserType(userType);
    }

    public String getUserType() {
        return context.getUserType();
    }

    /**
     * @return the context
     */
    public ContextData getContext() {
        return context;
    }

    public String getMessage() {
        return (String) context.getParam(MESSAGE_PATH);
    }

    public void setLoginResultStatus(LoginResultStatus loginResultStatus) {
        this.loginResultStatus = loginResultStatus;
    }

    public LoginResultStatus getLoginResultStatus() {
        return loginResultStatus;
    }

    public boolean isSucceeded() {
        return loginResultStatus == SUCCEEDED || loginResultStatus == LIMIT;
    }

    public void setFailed(String message) {
        loginResultStatus = FAILED;
        setMessage(message);
    }

    public void setSucceeded() {
        loginResultStatus = SUCCEEDED;
    }

    public List<SecurityActionCode> getAvailableActions() {
        return (List<SecurityActionCode>) context.getParam(AVAILABLE_ACTIONS_PATH);
    }

    public void setAvailableActions(List<SecurityActionCode> availableActions) {
        context.setParam(AVAILABLE_ACTIONS_PATH, availableActions);
    }

    public void setUserMenu(UserMenuWrapper menu) {
        context.setParam(USER_MENU_PATH, menu);
    }

    public UserMenuWrapper getUserMenu() {
        return (UserMenuWrapper) context.getParam(USER_MENU_PATH);
    }

    public static LoginResult buildInvalidUsernameLoginResult(String username) {
        LoginResult result = new LoginResult();
        result.setFailed("Неверное имя пользователя '" + username + "' или пароль");
        return result;
    }

    public static LoginResult buildPasswordRequiredLoginResult() {
        LoginResult result = new LoginResult();
        result.setFailed("Вход без пароля запрещен");
        return result;
    }

    public static LoginResult buildSusccessLoginResult(String userName) {
        LoginResult result = new LoginResult();
        result.setSucceeded();
        result.setUserName(userName);
        return result;
    }

    public static LoginResult buildFailed(String message) {
        LoginResult result = new LoginResult();
        result.setFailed(message);
        return result;
    }
}
