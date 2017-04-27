package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.etl.EtlAccount;
import ru.rbt.barsgl.ejb.entity.etl.EtlAccountId;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import java.util.List;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * Created by ER18837 on 05.05.15.
 */
@Stateless
@LocalBean
public class EtlAccountRepository extends AbstractBaseEntityRepository<EtlAccount, EtlAccountId> {
    public List<EtlAccount> getAccountByPackage(EtlPackage etlPackage) {
        return select(EtlAccount.class, "FROM EtlAccount a WHERE a.id.idPackage = ?1 ORDER BY a.id", etlPackage.getId());
    }

    public void updateAccountStateSuccess(EtlAccount etlAccount) {
        executeUpdate("update EtlAccount a set a.errorCode = ?1, a.errorMessage = ?2 where a = ?3",
                0, "SUCCESS", etlAccount);
    }

    public void updateAccountStateError(EtlAccount etlAccount, String message) {
        executeUpdate("update EtlAccount a set a.errorCode = ?1, a.errorMessage = ?2 where a = ?3",
                1, message, etlAccount);
    }


}
