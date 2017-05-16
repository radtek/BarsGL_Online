package ru.rbt.security.entity.access;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by ER21006 on 19.04.2016.
 */
@Entity
@Table(name = "GL_AU_GRACT")
public class SecurityActionGroup extends BaseEntity<Integer> {

    @Id
    @Column(name = "ID_GROUP")
    private Integer id;

    @Column(name = "GROUP_NAME")
    private String name;

    @Column(name = "GROUP_CODE")
    private String code;

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

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
