package ru.rbt.security.entity.access;

import ru.rbt.security.entity.AppUser;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * Created by ER21006 on 19.04.2016.
 */
@Embeddable
public class UserRoleRlnId implements Serializable {

    @Column(name = "ID_USER")
    private Long userId;

    @Column(name = "ID_ROLE")
    private Integer roleId;

    public UserRoleRlnId() {
    }

    public UserRoleRlnId(AppUser user, Role role) {
        this.userId = user.getId();
        this.roleId = role.getId();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }
}
