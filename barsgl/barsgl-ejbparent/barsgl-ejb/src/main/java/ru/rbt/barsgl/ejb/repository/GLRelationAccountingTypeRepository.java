package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.dict.GLRelationAccountingType;
import ru.rbt.barsgl.ejb.entity.dict.GLRelationAccountingTypeId;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.barsgl.ejbcore.util.StringUtils;
import ru.rbt.barsgl.shared.Assert;

/**
 * Created by Ivan Sevastyanov on 31.03.2016.
 */
public class GLRelationAccountingTypeRepository extends AbstractBaseEntityRepository<GLRelationAccountingType,GLRelationAccountingTypeId> {

    public GLRelationAccountingType createRelation(String acid, String bsaacid, String accountingType) {
        Assert.isTrue(!StringUtils.isEmpty(bsaacid) && !StringUtils.isEmpty(accountingType));
        GLRelationAccountingType type = new GLRelationAccountingType();
        type.setId(new GLRelationAccountingTypeId(bsaacid, StringUtils.ifEmpty(acid, "")));
        type.setAccountingType(accountingType);
        return save(type);
    }
}
