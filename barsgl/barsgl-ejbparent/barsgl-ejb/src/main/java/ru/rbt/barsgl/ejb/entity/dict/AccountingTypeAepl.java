package ru.rbt.barsgl.ejb.entity.dict;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by er18837 on 08.06.2018.
 */
@Entity
@Table(name = "GL_ACCTYPE_AEPL")
public class AccountingTypeAepl extends BaseEntity<String> {
    @Id
    @Column(name = "ACCTYPE", nullable = false, length = 10)
    private String id;

    @Override
    public String getId() {
        return id;
    }
}
