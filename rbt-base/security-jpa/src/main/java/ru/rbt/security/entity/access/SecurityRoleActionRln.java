package ru.rbt.security.entity.access;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Ivan Sevastyanov on 19.04.2016.
 */
@Entity
@Table(name = "GL_AU_ACTRL")
public class SecurityRoleActionRln extends BaseEntity<SecurityRoleActionRlnId> {

    @EmbeddedId
    private SecurityRoleActionRlnId id;

    @Column(name = "USR_AUT")
    private String userAuth;

    @Column(name = "DT_AUT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateAuth;

    @Override
    public SecurityRoleActionRlnId getId() {
        return id;
    }

    @Override
    public void setId(SecurityRoleActionRlnId id) {
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
