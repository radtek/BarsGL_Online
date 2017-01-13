package ru.rbt.barsgl.ejb.entity.access;

import ru.rbt.barsgl.ejb.entity.AppUser;
import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by ER21006 on 19.04.2016.
 */
@Entity
@Table(name = "GL_AU_USRRL")
public class UserRoleRln extends BaseEntity<UserRoleRlnId>{

    @EmbeddedId
    private UserRoleRlnId id;

    @Column(name = "USR_AUT")
    private String userAuth;

    @Column(name = "DT_AUT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateAuth;

    public UserRoleRln() {
    }

    public UserRoleRln(AppUser user, Role role) {
        this.id = new UserRoleRlnId(user,role);
    }

    @Override
    public UserRoleRlnId getId() {
        return id;
    }

    @Override
    public void setId(UserRoleRlnId id) {
        this.id = id;
    }

    public String getUserAuth() {
        return userAuth;
    }

    public void setUserAuth(String userAuth) {
        this.userAuth = userAuth;
    }

    public Date getDateAuth() {
        return dateAuth;
    }

    public void setDateAuth(Date dateAuth) {
        this.dateAuth = dateAuth;
    }
}
