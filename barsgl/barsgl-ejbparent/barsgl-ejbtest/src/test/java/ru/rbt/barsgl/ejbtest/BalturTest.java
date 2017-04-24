package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.DateUtils;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * Created by Ivan Sevastyanov on 07.04.2017.
 * тестирование триггеров по пересчету остатков BALTUR по проводкам PD
 */
public class BalturTest extends AbstractRemoteTest {

    @Test public void test() throws SQLException, ParseException {
        final String bsaacid = "12345123451234512345";
        final String acid = "54321543215432154321";
        final Operday operday = getOperday();

        final Date date29 = DateUtils.dbDateParse("2029-01-01");

        baseEntityRepository.executeNativeUpdate("delete from baltur where bsaacid = ?", bsaacid);

        Pd pd = createDummyPd(acid, bsaacid, operday.getCurrentDate(), operday.getCurrentDate());
        pd = (Pd) baseEntityRepository.save(pd);

        List<DataRecord> balturs = baseEntityRepository.select("select * from baltur b where b.bsaacid = ?", bsaacid);

        Assert.assertEquals(1, balturs.size());
        Assert.assertTrue(1200L == balturs.get(0).getLong("ctbc"));
        Assert.assertTrue(1200L == balturs.get(0).getLong("ctac"));
        Assert.assertTrue(balturs.stream().allMatch(r -> r.getDate("datto").equals(date29)));

        pd = (Pd) baseEntityRepository.findById(Pd.class, pd.getId());
        Assert.assertNotNull(pd);

        pd.setAmount(-1200L);
        pd.setAmountBC(-1200L);

        baseEntityRepository.update(pd);

        balturs = baseEntityRepository.select("select * from baltur b where b.bsaacid = ?", bsaacid);
        Assert.assertEquals(1, balturs.size());
        Assert.assertTrue(balturs.get(0).getLong("dtbc").toString(), -1200L == balturs.get(0).getLong("dtbc"));
        Assert.assertTrue(balturs.get(0).getLong("dtac").toString(), -1200L == balturs.get(0).getLong("dtac"));

        final Date beforePod = DateUtils.addDay(operday.getCurrentDate(), -10);
        Pd beforePd = createDummyPd(acid, bsaacid, operday.getCurrentDate(), beforePod);
        beforePd.setAmount(1000L);
        beforePd.setAmountBC(1000L);
        beforePd = (Pd) baseEntityRepository.save(beforePd);

        balturs = baseEntityRepository.select("select * from baltur b where b.bsaacid = ? order by datto desc", bsaacid);

        Assert.assertEquals(2, balturs.size());
        Assert.assertTrue(balturs.get(0).getLong("dtbc").toString(), -1200L == balturs.get(0).getLong("dtbc"));
        Assert.assertTrue(balturs.get(0).getLong("dtac").toString(), -1200L == balturs.get(0).getLong("dtac"));
        Assert.assertTrue(balturs.get(0).getLong("obac").toString(), 1000L == balturs.get(0).getLong("obac"));
        Assert.assertTrue(balturs.get(0).getLong("obbc").toString(), 1000L == balturs.get(0).getLong("obbc"));

        Assert.assertTrue(balturs.get(1).getLong("ctbc").toString(), 1000L == balturs.get(1).getLong("ctbc"));
        Assert.assertTrue(balturs.get(1).getLong("ctac").toString(), 1000L == balturs.get(1).getLong("ctac"));

        Assert.assertTrue(balturs.stream().anyMatch(r -> r.getDate("datto").equals(date29) && r.getDate("dat").equals(operday.getCurrentDate())));
        Assert.assertTrue(balturs.stream().anyMatch(r -> r.getDate("datto").equals(DateUtils.addDay(operday.getCurrentDate(), -1))
            && r.getDate("dat").equals(beforePod)));

        Assert.assertEquals(1, baseEntityRepository.executeNativeUpdate("delete from pd where id = ?", pd.getId()));
        Assert.assertEquals(1, baseEntityRepository.executeNativeUpdate("delete from pd where id = ?", beforePd.getId()));

        balturs = baseEntityRepository.select("select obac + dtac + ctac sa, obbc + dtbc + ctbc sr from baltur b where b.bsaacid = ? order by datto desc", bsaacid);
        Assert.assertEquals(2L, balturs.size());
        Assert.assertTrue(balturs.get(0).getLong("sa").toString() + ":" + balturs.get(0).getLong("sr").toString()
                , balturs.stream().anyMatch(r -> r.getLong("sa").equals(new Long(0)) && r.getLong("sr").equals(new Long(0))));
    }

    private Pd createDummyPd(String acid, String bsaacid, Date valueDate, Date postDate) {
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
        pd.setVald(valueDate);
        pd.setPod(postDate);
        pd.setDepartment("dep");
        pd.setPref("pref");
        pd.setOperReference("or");
        pd.setRusNarrShort("rnarsht");
        pd.setRusNarrLong("rnarlng");
        pd.setCtype("123");
        return pd;
    }
}
