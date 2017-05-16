package ru.rbt.barsgl.ejb.entity.dict.AccType;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * Created by akichigi on 23.08.16.
 */

@Entity
@Table(name = "GL_ACT3")
public class SubProduct extends BaseEntity<SubProductId> {

    @EmbeddedId
    private SubProductId id;

    @Column(name = "NAME")
    private String name;

    public SubProduct(){}

    public SubProduct(SubProductId id, String name){
        this.id = id;
        this.name = name;
    }

    @Override
    public SubProductId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
