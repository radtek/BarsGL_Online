package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by Ivan Sevastyanov on 07.04.2017.
 * тестирование триггеров по пересчету остатков BALTUR по проводкам PD
 */
public class BalturTest extends AbstractRemoteTest {

    @Test public void test() throws SQLException {
        final String bsaacid = "12345123451234512345";
        final String acid = "54321543215432154321";
        final Operday operday = getOperday();

        baseEntityRepository.executeNativeUpdate("delete from baltur where bsaacid = ?", bsaacid);

        Pd pd = new Pd();
        pd.setId(baseEntityRepository.nextId("pd_seq"));
        pd.setAcid(acid);
        pd.setBsaAcid(bsaacid);
        pd.setAmount(1200L);
        pd.setAmountBC(1200L);
        pd.setPcId(pd.getId());
        pd.setNarrative("nar1");
        pd.setPnar(pd.getNarrative());
        pd.setPbr("@@GL-K+");
        pd.setCcy(BankCurrency.AUD);
        pd.setVald(operday.getCurrentDate());
        pd.setPod(operday.getCurrentDate());
        pd.setDepartment("dep");
        pd.setPref("pref");
        pd.setOperReference("or");
        pd.setRusNarrShort("rnarsht");
        pd.setRusNarrLong("rnarlng");
        pd.setCtype("123");
        pd = (Pd) baseEntityRepository.save(pd);

        List<DataRecord> balturs = baseEntityRepository.select("select * from baltur b where b.bsaacid = ?", bsaacid);

        Assert.assertEquals(1, balturs.size());
        Assert.assertTrue(1200L == balturs.get(0).getLong("ctbc"));
        Assert.assertTrue(1200L == balturs.get(0).getLong("ctac"));


        pd = (Pd) baseEntityRepository.findById(Pd.class, pd.getId());
        Assert.assertNotNull(pd);

        pd.setAmount(-1200L);
        pd.setAmountBC(-1200L);

        baseEntityRepository.update(pd);

        balturs = baseEntityRepository.select("select * from baltur b where b.bsaacid = ?", bsaacid);
        Assert.assertEquals(1, balturs.size());
        Assert.assertTrue(balturs.get(0).getLong("dtbc").toString(), -1200L == balturs.get(0).getLong("dtbc"));
        Assert.assertTrue(balturs.get(0).getLong("dtac").toString(), -1200L == balturs.get(0).getLong("dtac"));

    }
}
