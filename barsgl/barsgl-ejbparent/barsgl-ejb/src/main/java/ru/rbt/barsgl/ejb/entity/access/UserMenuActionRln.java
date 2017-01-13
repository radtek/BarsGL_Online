package ru.rbt.barsgl.ejb.entity.access;

import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;

import javax.persistence.*;

/**
 * Created by Ivan Sevastyanov on 21.04.2016.
 */
@Entity
@Table(name = "GL_AU_MENUACT")
public class UserMenuActionRln extends BaseEntity<UserMenuActionRlnId>{

    @EmbeddedId
    private UserMenuActionRlnId id;

    public UserMenuActionRln() {
    }

    public UserMenuActionRln(SecurityAction action, UserMenuItem menuItem) {
        this.id = new UserMenuActionRlnId(action.getId(), menuItem.getId());
    }

    @Override
    public UserMenuActionRlnId getId() {
        return id;
    }

    @Override
    public void setId(UserMenuActionRlnId id) {
        this.id = id;
    }
}
