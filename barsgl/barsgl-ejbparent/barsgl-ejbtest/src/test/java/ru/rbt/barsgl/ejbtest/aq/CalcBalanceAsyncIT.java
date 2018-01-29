package ru.rbt.barsgl.ejbtest.aq;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejbtest.AbstractRemoteIT;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.sql.SQLException;

public class CalcBalanceAsyncIT extends AbstractRemoteIT {

    @Test public void test() throws SQLException {
        Operday operday = getOperday();
        // отключены все триггера, кроме AQ

        // удаление baltur по счету
        GLAccount account = findAccount("40702810%");
        baseEntityRepository.executeNativeUpdate("delete from baltur where bsaacid = ?", account.getBsaAcid());

        // создание проводки
        Pd pd = new Pd();
        pd.setId(baseEntityRepository.nextId("PD_SEQ"));
        pd.setPcId(pd.getId());
        pd.setAcid(account.getAcid());
        pd.setBsaAcid(account.getBsaAcid());
        pd.setAmount(100L);
        pd.setAmountBC(100L);
        pd.setPbr("@@GL-K+");
        pd.setPod(operday.getCurrentDate());
        pd.setVald(operday.getCurrentDate());
        pd.setCcy(BankCurrency.RUB);
        // pdext2
        pd.setOperReference("12");
        // pdext
        pd.setPref("123");

        pd = (Pd) baseEntityRepository.save(pd);

        Assert.assertNotNull(pd);

        // проверка остатков - нет изменений
        // есть изменения (проверка сумм!)
    }

    private static GLAccount findAccount(String bsaacidLike) throws SQLException {
        DataRecord record = baseEntityRepository.selectFirst("select id from gl_acc where bsaacid like ?", bsaacidLike);
        if (record != null) {
            return (GLAccount) baseEntityRepository.findById(GLAccount.class, record.getLong("id"));
        } else {
            throw new RuntimeException(bsaacidLike + " not found");
        }
    }
}
