package ru.rbt.security.entity.access;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Created by Ivan Sevastyanov on 21.04.2016.
 */
@Embeddable
public class UserMenuActionRlnId implements Serializable {

    @Column(name = "ID_ACT")
    private Integer actionId;

    @Column(name = "ID_MENU")
    private Integer menuId;

    public UserMenuActionRlnId() {
    }

    public UserMenuActionRlnId(Integer actionId, Integer menuId) {
        this.actionId = actionId;
        this.menuId = menuId;
    }

    public Integer getActionId() {
        return actionId;
    }

    public void setActionId(Integer actionId) {
        this.actionId = actionId;
    }

    public Integer getMenuId() {
        return menuId;
    }

    public void setMenuId(Integer menuId) {
        this.menuId = menuId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserMenuActionRlnId that = (UserMenuActionRlnId) o;
        return Objects.equals(actionId, that.actionId) &&
                Objects.equals(menuId, that.menuId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionId, menuId);
    }
}
