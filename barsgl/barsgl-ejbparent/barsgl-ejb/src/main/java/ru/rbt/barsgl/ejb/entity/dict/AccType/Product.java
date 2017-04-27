package ru.rbt.barsgl.ejb.entity.dict.AccType;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by akichigi on 22.08.16.
 */

@Entity
@Table(name = "GL_ACT2")
public class Product extends BaseEntity<ProductId> {
    @EmbeddedId
    private ProductId id;

    @Column(name = "NAME")
    private String name;

    public Product(){}

    public Product(ProductId id, String name){
        this.id = id;
        this.name = name;
    }

    @Override
    public ProductId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
