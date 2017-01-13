package ru.rbt.barsgl.ejb.entity.dict.AccType;

import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by akichigi on 22.08.16.
 */

@Entity
@Table(name = "GL_ACT1")
public class Section extends BaseEntity<String> {
    @Id
    @Column(name = "SECTCODE")
    private String id;

    @Column(name = "NAME")
    private String name;

    public Section(){}

    public  Section(String id, String name){
        this.id = id;
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
