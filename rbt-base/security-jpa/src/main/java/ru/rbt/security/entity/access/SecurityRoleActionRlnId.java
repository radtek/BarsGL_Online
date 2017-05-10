package ru.rbt.security.entity.access;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Created by Ivan Sevastyanov on 19.04.2016.
 */
@Embeddable
public class SecurityRoleActionRlnId implements Serializable {

    @Column(name = "ID_ROLE")
    private Integer roleId;

    @Column(name = "ID_ACT")
    private Integer actionId;

    public SecurityRoleActionRlnId() {
    }

    public SecurityRoleActionRlnId(Role role, SecurityAction action) {
        this.roleId = role.getId();
        this.actionId = action.getId();
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public Integer getActionId() {
        return actionId;
    }

    public void setActionId(Integer actionId) {
        this.actionId = actionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecurityRoleActionRlnId that = (SecurityRoleActionRlnId) o;
        return Objects.equals(roleId, that.roleId) &&
                Objects.equals(actionId, that.actionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId, actionId);
    }
}
