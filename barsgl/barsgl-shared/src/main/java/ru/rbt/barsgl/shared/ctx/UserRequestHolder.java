package ru.rbt.barsgl.shared.ctx;

import com.google.gwt.user.client.rpc.IsSerializable;
import ru.rbt.barsgl.shared.user.AppUserWrapper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Ivan Sevastyanov on 20.02.2016.
 */
public class UserRequestHolder implements Serializable, IsSerializable {

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

    public <T extends IsSerializable> void setDynamicValue(String key, T value) {
        dynamicStore.put(key, value);
    }

    public <T extends IsSerializable> T getDynamicValue(String key) {
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

}
