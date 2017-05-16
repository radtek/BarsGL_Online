package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadFullTask;
import ru.rbt.barsgl.ejb.controller.operday.task.DwhUnloadParams;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.enums.OperState;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.CLOSED;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.COB;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.PRE_COB;
import static ru.rbt.barsgl.ejbtest.utl.Utl4Tests.cleanHeader;

/**
 * Created by ER18837 on 18.06.15.
 * Выгрузка данных о проводках в DWH
 * @fsd 8.2
 */
public class DwhUnloadTest extends AbstractTimerJobTest {

    public static final Logger logger = Logger.getLogger(DwhUnloadTest.class.getName());

    /**
     * Выгрузка данных о проводках, созданных в текущий опердень
     * @fsd 8.2.3
     * @throws Exception
     */
    @Test
    public void testFull() throws Exception {

        Date currDate = getOperday().getCurrentDate();

        updateOperday(ONLINE, OPEN);

        logger.info("updated: " + baseEntityRepository.executeNativeUpdate(
                "update gl_oper set procdate = ? where procdate = ?", DateUtils.addDays(currDate, -10), currDate));

        String pmtRef = StringUtils.rsubstr(System.currentTimeMillis() + "_ref", 10);
        GLOperation oper = createSimpleOper(currDate, pmtRef);

        cleanHeader(baseEntityRepository, DwhUnloadParams.UnloadFullPostings.getParamDesc());

        checkHeadersCount(0);

        updateOperday(COB, CLOSED);

        startupAndWait(DwhUnloadFullTask.DWH_UNLOAD_FULL_DATE_KEY + "=" + Utl4Tests.toString(currDate, "dd.MM.yyyy"));
        checkHeadersCount(1);

        List<DataRecord> records = baseEntityRepository.select("select * from GLVD_PST");
        Assert.assertEquals(2, records.size());
        Assert.assertTrue(format("'%s' : '%s'",
                pmtRef.trim(), records.get(0).getString("PREF").trim()), pmtRef.trim().equalsIgnoreCase(records.get(0).getString("PREF").trim()));

        // повторная обработка с той же датой не проходит
        startupAndWait(DwhUnloadFullTask.DWH_UNLOAD_FULL_DATE_KEY + "=" + Utl4Tests.toString(currDate, "dd.MM.yyyy"));
        checkHeadersCount(1);

        // без свойств - за текущий ОД
        Date currentDay = getOperday().getCurrentDate();

        cleanHeader(baseEntityRepository, DwhUnloadParams.UnloadFullPostings.getParamDesc());
        startupAndWait(null);

        List<DataRecord> recs = baseEntityRepository.select("select * from GL_ETLDWHS where PARDESC = ?"
                , DwhUnloadParams.UnloadFullPostings.getParamDesc());
        Assert.assertEquals(1, recs.size());
        Assert.assertEquals(currentDay, recs.get(0).getDate("operday"));
    }

    @Test
    public void checkRunTest() throws IOException {
        cleanHeader(baseEntityRepository, DwhUnloadParams.UnloadFullPostings.getParamDesc());
        updateOperday(ONLINE, CLOSED);

        Properties properties = new Properties();
        properties.load(new StringReader("checkRun=false"));
        Assert.assertTrue(remoteAccess.invoke(DwhUnloadFullTask.class, "checkRun", getOperday().getCurrentDate(), properties));

        updateOperday(PRE_COB, CLOSED);
        properties = new Properties();
        Assert.assertFalse(remoteAccess.invoke(DwhUnloadFullTask.class, "checkRun", getOperday().getCurrentDate(), properties));

        updateOperday(ONLINE, CLOSED);
        properties = new Properties();
        Assert.assertTrue(remoteAccess.invoke(DwhUnloadFullTask.class, "checkRun", getOperday().getCurrentDate(), properties));

        updateOperday(COB, CLOSED);
        Assert.assertTrue(remoteAccess.invoke(DwhUnloadFullTask.class, "checkRun", getOperday().getCurrentDate(), properties));
    }

    private void startupAndWait(String propertiesString) throws IOException {
        Properties properties = new Properties();
        if (!isEmpty(propertiesString)) {
            properties.load(new StringReader(propertiesString));
        }
        remoteAccess.invoke(DwhUnloadFullTask.class, "run", new Object[]{"testdwhunload", properties});
    }

    private static void checkHeadersCount(int cnt) throws SQLException {
        DataRecord rec = baseEntityRepository.selectFirst("select count (1) cnt from GL_ETLDWHS where PARDESC = ?"
                , DwhUnloadParams.UnloadFullPostings.getParamDesc());

        Assert.assertTrue(rec.getInteger("cnt")+"", cnt == rec.getInteger("cnt"));
    }

    private GLOperation createSimpleOper(Date valueDate, String ref) throws SQLException {
        long stamp = System.currentTimeMillis();
        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(valueDate);

        String bsaCt = getBsaacid("40817%", null);
        pst.setAccountCredit(bsaCt);
        pst.setAccountDebit(getBsaacid("40817%", bsaCt));
        pst.setAmountCredit(new BigDecimal("12.0056"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.AUD);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setSourcePosting(GLOperation.srcKondorPlus);
        pst.setDealId("123");
        pst.setPaymentRefernce(ref);

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertEquals(getOperday().getCurrentDate(), operation.getCurrentDate());
        Assert.assertEquals(getOperday().getLastWorkdayStatus(), operation.getLastWorkdayStatus());

        return operation;
    }

    private String getBsaacid(String like, String not) throws SQLException {
        if (isEmpty(not)) {
            return Optional.ofNullable(baseEntityRepository.selectFirst(
                    "select * from accrln where bsaacid like ? and value(acid,'') <> ''", like))
                    .orElseThrow(() -> new IllegalArgumentException(like + ":" + not)).getString("bsaacid");
        } else {
            return Optional.ofNullable(baseEntityRepository.selectFirst(
                    "select * from accrln where bsaacid like ? and value(acid,'') <> '' and bsaacid <> ? and value(acid,'') <> ''", like, not))
                    .orElseThrow(() -> new IllegalArgumentException(like + ":" + not)).getString("bsaacid");
        }
    }
}
