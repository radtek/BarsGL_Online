package ru.rbt.barsgl.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import ru.rbt.barsgl.shared.user.AppUserWrapper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ivan Sevastyanov
 */
public class ContextData implements Serializable, IsSerializable {

    public static final String USER_NAME_PATH = "userName";
    public static final String USER_PATH = "user";
    public static final String USER_HOST_PATH = "userHost";
    public static final String USER_TYPE_PATH = "userType";
    public static final String MESSAGE_PATH = "message";
    public static final String AVAILABLE_ACTIONS_PATH = "actions";
    public static final String USER_MENU_PATH = "menu";

    private Map<String, Object> contextParams = new HashMap<>();

    public void setParam(String name, Object value) {
        contextParams.put(name, value);
    }

    public Object getParam(String name) {
        return contextParams.get(name);
    }

    public String getUserName() {
        return (String) getParam(USER_NAME_PATH);
    }

    public void setUserName(String userName) {
        setParam(USER_NAME_PATH, userName);
    }

    public AppUserWrapper getUser(){
        return (AppUserWrapper) getParam(USER_PATH);
    }

    public void setUser(AppUserWrapper user){
        setParam(USER_PATH, user);
    }

    public String getUserType() {return (String) getParam(USER_TYPE_PATH); }

    public void setUserType(String userType) {
        setParam(USER_TYPE_PATH, userType);
    }

    public String getUserHost() {
        return (String) getParam(USER_HOST_PATH);
    }

    public void setUserHost(String userHost) {
        setParam(USER_HOST_PATH, userHost);
    }
}
