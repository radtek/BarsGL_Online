package ru.rbt.security.entity.access;

import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.shared.enums.UserMenuCode;
import ru.rbt.shared.enums.UserMenuType;

import javax.persistence.*;

import static javax.persistence.InheritanceType.SINGLE_TABLE;

/**
 * Created by Ivan Sevastyanov on 21.04.2016.
 */
@Entity
@Table(name = "GL_AU_MENU")
@Inheritance(strategy = SINGLE_TABLE)
@DiscriminatorColumn(name = "MENU_TYPE")
@DiscriminatorValue("L")
public class UserMenuItem extends BaseEntity<Integer> {

    @Id
    @Column(name = "ID_MENU")
    private Integer id;

    @Column(name = "MENU_NAME")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "MENU_TYPE", updatable = false, insertable = false)
    private UserMenuType menuType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID")
    private UserMenuItem parent;

    @Enumerated(EnumType.STRING)
    @Column(name = "MENU_CODE")
    private UserMenuCode menuCode;

    @Column(name = "ORDER_NUM")
    private Integer orderNum;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public UserMenuType getMenuType() {
        return menuType;
    }

    public void setMenuType(UserMenuType menuType) {
        this.menuType = menuType;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserMenuItem getParent() {
        return parent;
    }

    public void setParent(UserMenuItem parent) {
        this.parent = parent;
    }

    public UserMenuCode getMenuCode() {
        return menuCode;
    }

    public void setMenuCode(UserMenuCode menuCode) {
        this.menuCode = menuCode;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }
}
