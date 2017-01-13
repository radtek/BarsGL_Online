package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.shared.Assert;
import ru.rbt.barsgl.ejb.entity.acc.Acc;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;

import javax.persistence.NoResultException;
import java.sql.SQLException;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.substr;

/**
 * Created by ER18837 on 05.05.15.
 */
public class AccRepository extends AbstractBaseEntityRepository<Acc, String> {

    public Acc createAcc(GLAccount glAccount) {
        Acc acc = new Acc();
        acc.setId(glAccount.getAcid());
        acc.setBranch(glAccount.getBranch());
        acc.setCustomerNumberD(glAccount.getCustomerNumberD());
        acc.setCurrency(glAccount.getCurrency().getCurrencyCode());
        acc.setAccountCode(glAccount.getAccountCode());
        acc.setAccountSequence(glAccount.getAccountSequence());
        acc.setDateOpen(glAccount.getDateOpen());
        acc.setDateClose(glAccount.getDateClose());
        acc.setAccountName(substr(glAccount.getDescription(), 20));

        acc.setType("");
        acc.setSybtype("");
        acc.setStatus("");
        acc.setTypeChange("");
        acc.setLimitOverdraft(0L);

        save(acc);

        return acc;
    }

    /**
     * Обновляет дату закрытия счета
     * @param glAccount
     */
    public void setDateClose(GLAccount glAccount) {
        int cnt = executeNativeUpdate("update ACC set DACC = ? where ID = ?", glAccount.getDateClose(), glAccount.getAcid());
        Assert.isTrue(1 == cnt, format("Update count = %d: table '%s', ID = '%s'",
                cnt, "ACC", glAccount.getAcid()));
    }

    /**
     * Поиск по счету ЦБ
     *
     * @param bsaacid счет ЦБ
     * @return
     */
    public DataRecord findByAcid(String bsaacid) throws SQLException {
        try {
            return selectFirst("SELECT * FROM ACC WHERE ID = ? ORDER BY DACC DESC", bsaacid);
        } catch (NoResultException e) {
            return null;
        }
    }

}
