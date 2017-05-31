package ru.rbt.barsgl.ejb.repository.dict.AccType;

import ru.rbt.barsgl.ejb.entity.dict.AccountingType;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import java.sql.SQLException;

/**
 * Created by akichigi on 24.08.16.
 */
public class AccTypeRepository extends AbstractBaseEntityRepository<AccountingType, String> {
    public boolean isModifierExists(String section, String product, String subproduct, String modifier) {
        return null != selectFirst(AccountingType.class, "from AccountingType T where T.id = ?1",
                section + product + subproduct + modifier);
    }

    public boolean isAccountingTypeCodeExists(String id) {
        return null != selectFirst(AccountingType.class, "from AccountingType T where T.id = ?1", id);
    }

    public boolean isAccountingTypeNameExists(String name) {
        return null != selectFirst(AccountingType.class, "from AccountingType T where T.accountName = ?1", name);
    }

    public boolean isAccountingTypeExists(String id, String name) {
        return null != selectFirst(AccountingType.class, "from AccountingType T where T.accountName = ?1 and T.id <> ?2",
                name, id);
    }

    public boolean isAccTypeInAcc(String accType){
        try {
            return null != selectFirst("select 1 from gl_acc where acctype=?", accType);
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public AccountingType getAccType(String accType){
        return selectFirst(AccountingType.class, "from AccountingType T where T.id = ?1", accType);
    }

    public boolean isAccTypePL_ACT_yes(String id) {
        AccountingType act = selectFirst(AccountingType.class, "from AccountingType T where T.id = ?1", id);
        return (act != null) && (act.isBarsAllowed());
    }

    public boolean isAccTypeTechAct(String accType){
        AccountingType act = getAccType(accType);
        if (act!=null)
        {
            return act.isTech();
        }

        return false;
    }
}
