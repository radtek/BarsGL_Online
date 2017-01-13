package ru.rbt.barsgl.shared.access;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ivan Sevastyanov on 21.04.2016.
 */
public class UserMenuWrapper implements Serializable, IsSerializable {

    private List<UserMenuItemWrapper> rootElements;

    public UserMenuWrapper() {
    }

    public UserMenuWrapper(List<UserMenuItemWrapper> rootElements) {
        this.rootElements = rootElements;
    }

    public List<UserMenuItemWrapper> getRootElements() {
        return rootElements;
    }

    public static final UserMenuWrapper emptyWrapper() {
        return new UserMenuWrapper(new ArrayList<UserMenuItemWrapper>());
    }

    public void setRootElements(List<UserMenuItemWrapper> rootElements) {
        this.rootElements = rootElements;
    }
}
