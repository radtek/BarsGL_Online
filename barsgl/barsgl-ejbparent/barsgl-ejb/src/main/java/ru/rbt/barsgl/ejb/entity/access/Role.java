package ru.rbt.barsgl.ejb.entity.access;

import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;
import ru.rbt.barsgl.shared.enums.RoleSys;

import javax.persistence.*;

/**
 * Created by akichigi on 12.04.16.
 */
@Entity
@Table(name = "GL_AU_ROLE")
public class Role extends BaseEntity<Integer> {

    @Id
    @Column(name = "ID_ROLE")
    private Integer id;

    @Column(name = "ROLE_NAME")
    private String name;

    @Column(name = "SYS")
    @Enumerated(EnumType.STRING)
    private RoleSys sys;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id){
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RoleSys getSys(){
        return this.sys;
    }

    public void setSys(RoleSys sys) {
        this.sys = sys;
    }
}
