package ru.rbt.barsgl.ejb.entity.access;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Created by Ivan Sevastyanov on 21.04.2016.
 */
@Entity
@DiscriminatorValue("N")
public class UserMenuNode extends UserMenuItem {

}
