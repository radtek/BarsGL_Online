package ru.rbt.barsgl.ejb.entity.dict;

import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by Ivan Sevastyanov on 31.03.2016.
 */
@Entity
@Table(name = "GL_RLNACT")
public class GLRelationAccountingType extends BaseEntity<GLRelationAccountingTypeId> {

    @EmbeddedId
    private GLRelationAccountingTypeId id;

    @Column(name = "ACCTYPE")
    private String accountingType;

    @Override
    public void setId(GLRelationAccountingTypeId id) {
        this.id = id;
    }

    public String getAccountingType() {
        return accountingType;
    }

    public void setAccountingType(String accountingType) {
        this.accountingType = accountingType;
    }

    @Override
    public GLRelationAccountingTypeId getId() {
        return id;
    }
}
