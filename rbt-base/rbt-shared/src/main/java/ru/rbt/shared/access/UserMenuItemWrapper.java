package ru.rbt.shared.access;

import ru.rbt.shared.enums.UserMenuCode;
import ru.rbt.shared.enums.UserMenuType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by Ivan Sevastyanov on 21.04.2016.
 */
public class UserMenuItemWrapper implements Serializable {

    private Integer menuId;
    private String menuName;
    private UserMenuCode menuCode;
    private UserMenuType type;

    private UserMenuItemWrapper parent;

    private List<UserMenuItemWrapper> children = new ArrayList<>();


    public UserMenuItemWrapper() {
    }

    public UserMenuItemWrapper(Integer menuId, String menuName, UserMenuCode menuCode, UserMenuType type) {
        this.menuId = menuId;
        this.menuName = menuName;
        this.menuCode = menuCode;
        this.type = type;
    }

    public String getMenuName() {
        return menuName;
    }

    public UserMenuCode getMenuCode() {
        return menuCode;
    }

    public UserMenuType getType() {
        return type;
    }

    public UserMenuItemWrapper getParent() {
        return parent;
    }

    public void setParent(UserMenuItemWrapper parent) {
        this.parent = parent;
    }

    public Integer getMenuId() {
        return menuId;
    }

    public void addChild(UserMenuItemWrapper child) {
        if (!children.contains(child)) {
            children.add(child);
        }
    }

    public List<UserMenuItemWrapper> getChildren() {
        return children;
    }

    public void setChildren(List<UserMenuItemWrapper> children) {
        this.children =  children;
    }


    public void setMenuId(Integer menuId) {
        this.menuId = menuId;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public void setMenuCode(UserMenuCode menuCode) {
        this.menuCode = menuCode;
    }

    public void setType(UserMenuType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserMenuItemWrapper that = (UserMenuItemWrapper) o;
        return Objects.equals(menuId, that.menuId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(menuId);
    }
}
