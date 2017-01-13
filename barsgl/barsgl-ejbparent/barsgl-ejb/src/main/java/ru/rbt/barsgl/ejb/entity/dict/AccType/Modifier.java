package ru.rbt.barsgl.ejb.entity.dict.AccType;

import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by akichigi on 24.08.16.
 */

@Entity
@Table(name = "GL_ACT4")
public class Modifier extends BaseEntity<ModifierId> {

    @EmbeddedId
    private ModifierId id;

    @Column(name = "NAME")
    private String name;

    public Modifier(){}

    public Modifier(ModifierId id, String name){
        this.id = id;
        this.name = name;
    }

    @Override
    public ModifierId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
