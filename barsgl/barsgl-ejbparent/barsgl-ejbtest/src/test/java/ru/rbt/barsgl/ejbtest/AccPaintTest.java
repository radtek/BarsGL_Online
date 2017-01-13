package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.AccoutPaintTask;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by Ivan Sevastyanov
 */
public class AccPaintTest extends AbstractRemoteTest {

    public static final Logger logger = Logger.getLogger(AccPaintTest.class.getName());

    /**
     * Раскраска счетов по данным из excel таблицы
     * вместе с этим заполняется таблица GL_SQVALUE
     */
    @Test public void test() throws SQLException {

        baseEntityRepository.executeNativeUpdate("delete from GL_ACCPNT");

        final String custno = "00696251";
        final String dealid = "193_A025S_15";
        final String ccy = "RUR";
        baseEntityRepository.executeNativeUpdate("delete from GL_SQVALUE where CUSTNO = ? and DEAL_ID = ? and CCY = ?"
                , custno, dealid, ccy);

        String bsaacid = Optional.ofNullable(baseEntityRepository.selectFirst("select * from accrln where bsaacid like '913178105003047%'"))
                .orElseThrow(() -> new RuntimeException("Account not found")).getString("bsaacid");

        String bsaacidFailed = "hdsfgj2398479";

        baseEntityRepository.executeNativeUpdate(
                "insert into GL_ACCPNT (bsaacid, branch, ccy, custno, acctype, cbccn, acode, sq, dealid, subdealid, glseq, term, cbcusttype)\n" +
                "        values ('"  +  bsaacid + "', '030', '"+ccy+"', '" + custno + "', 891050300, '0030', 6571, 71, '"+dealid+"', '193_A025S_15', '0000003323', null, '18')");
        baseEntityRepository.executeNativeUpdate(
                "insert into GL_ACCPNT (bsaacid, branch, ccy, custno, acctype, cbccn, acode, sq, dealid, subdealid, glseq, term, cbcusttype)\n" +
                "        values ('"  +  bsaacidFailed + "', '030', '"+ccy+"', '" + custno + "', 891050300, '0030', 6571, 71, '"+dealid+"', '193_A025S_15', '0000003323', null, '18')");

        List<DataRecord> martRecords = baseEntityRepository.select("select * from gl_accpnt a");
        Assert.assertTrue(!martRecords.isEmpty());

        logger.info("deleted 0: " + baseEntityRepository.executeNativeUpdate("delete from GL_ACCPNT_LOG"));
        logger.info("deleted 1: " + baseEntityRepository.executeNativeUpdate("delete from gl_acc a1 where exists (select 1 from gl_accpnt a2 where a1.bsaacid = a2.bsaacid)"));

        remoteAccess.invoke(AccoutPaintTask.class, "execWork");

        Assert.assertEquals(1L, baseEntityRepository
                .selectFirst("select count(1) cnt from gl_acc a where a.bsaacid = ?", bsaacid).getLong("cnt").longValue());
        Assert.assertEquals(0L, baseEntityRepository
                .selectFirst("select count(1) cnt from gl_acc a where a.bsaacid = ?", bsaacidFailed).getLong("cnt").longValue());

        Assert.assertEquals(1, baseEntityRepository.select("select * from GL_SQVALUE where CUSTNO = ? and DEAL_ID = ? and CCY = ?"
                , custno, dealid, ccy).size());

    }
}
