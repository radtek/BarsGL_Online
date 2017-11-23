package ru.rbt.shared.ctx;

import ru.rbt.shared.user.AppUserWrapper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Ivan Sevastyanov on 20.02.2016.
 */
public class UserRequestHolder implements Serializable {

    private String user;
    private String userHost;
    private Map<String, Object> dynamicStore = new HashMap<>();
    private AppUserWrapper userWrapper;

    public UserRequestHolder() {
    }

    public UserRequestHolder(String user, String userHost) {
        this.user = user;
        this.userHost = userHost;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setDynamicValue(String key, Serializable value) {
        dynamicStore.put(key, value);
    }

    public <T extends Serializable> T getDynamicValue(String key) {
        return (T) dynamicStore.get(key);
    }

    public String getUserHost() {
        return userHost;
    }

    public void setUserHost(String userHost) {
        this.userHost = userHost;
    }

    public static UserRequestHolder empty() {
        return new UserRequestHolder("", "");
    }

    public AppUserWrapper getUserWrapper() {
        return Optional.ofNullable(userWrapper).orElse(new AppUserWrapper());
    }

    public void setUserWrapper(AppUserWrapper userWrapper) {
        this.userWrapper = userWrapper;
    }

    public boolean isEmptyHolder() {
        return null == user || "".equals(user);
    }

}
