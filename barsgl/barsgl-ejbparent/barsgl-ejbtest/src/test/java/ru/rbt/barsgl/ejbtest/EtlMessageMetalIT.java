package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.shared.enums.OperState;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Logger;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;

/**
 * Created by Ivan Sevastyanov
 * Обработка операций GL в формате простой проводки
 * @fsd 7.5
 */
public class EtlMessageMetalIT extends AbstractTimerJobIT {

    public static final Logger log = Logger.getLogger(EtlMessageMetalIT.class.getName());
    private static final Operday.PdMode pdMode = DIRECT;
    private static final String acc706 = "70601810200012620699";
//    private static final Operday.PdMode pdMode = BUFFER;

//    @BeforeClass
//    public static void beforeClass() throws ParseException {
//        Date operday = DateUtils.parseDate("2016-03-23", "yyyy-MM-dd");
//        setOperday(operday, DateUtils.addDays(operday, -1), ONLINE, OPEN, pdMode);
//
//        baseEntityRepository.executeNativeUpdate("update bsaacc set bsaaco = ? where id like '93307392%'", DateUtils.parseDate("2015-01-01", "yyyy-MM-dd"));
//
//        baseEntityRepository.executeNativeUpdate("delete from CAL where DAT between '2015-01-23' and '2015-01-26' and CCY = 'RUR'");
//        baseEntityRepository.executeNativeUpdate("insert into CAL values ('2015-01-23', ' ', 'RUR', ' ')");
//        baseEntityRepository.executeNativeUpdate("insert into CAL values ('2015-01-24', 'X', 'RUR', 'X')");
//        baseEntityRepository.executeNativeUpdate("insert into CAL values ('2015-01-25', 'X', 'RUR', 'X')");
//        baseEntityRepository.executeNativeUpdate("insert into CAL values ('2015-01-26', ' ', 'RUR', ' ')");
//        baseEntityRepository.executeNativeUpdate("delete from CAL where DAT = '2015-01-31' and CCY = 'RUR'");
//        baseEntityRepository.executeNativeUpdate("insert into CAL values ('2015-01-31', 'X', 'RUR', 'T')");
//    }

    /**
     * Обработка операции в одном филиале, без создания проводки курсовой разницы
     * @fsd 7.5.2.1
     * @throws ParseException
     */
    @Test public void testNoExch() throws ParseException, SQLException {

        updateOperday(ONLINE, OPEN, DIRECT);
        del706(acc706);

        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountDebit("61209810300014801057");
        pst.setCurrencyDebit(BankCurrency.RUB);

        pst.setAccountCredit("20308A99700014801064");
        pst.setCurrencyCredit(BankCurrency.XAG);
        pst.setAmountCredit(new BigDecimal("31.1"));
        pst.setAmountDebit(pst.getAmountCredit().multiply(getMetalRate(pst.getCurrencyCredit())));

        pst.setSourcePosting("FC12_CL");
        pst.setDealId("123");

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertEquals(getOperday().getCurrentDate(), operation.getCurrentDate());
        Assert.assertEquals(getOperday().getLastWorkdayStatus(), operation.getLastWorkdayStatus());

        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 1 проводка
        Assert.assertEquals(postList.size(), 1);

        List<Pd> pdList = getPostingPd(postList.get(0));
        Assert.assertEquals(pdList.size(), 2);
        Pd pdDr = pdList.get(0);
        Pd pdCr = pdList.get(1);
        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит
    }

    /**
     * Обработка операции в одном филиале, с созданием проводки курсовой разницы
     * @fsd 7.5.2.1
     * @throws ParseException
     */
    @Test public void testWithExch() throws ParseException, SQLException {

        updateOperday(ONLINE, OPEN, DIRECT);
        del706(acc706);
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountDebit("61209810300014801057");
        pst.setCurrencyDebit(BankCurrency.RUB);

        pst.setAccountCredit("20308A99700014801064");
        pst.setCurrencyCredit(BankCurrency.XAG);
        pst.setAmountCredit(new BigDecimal("31.1"));
        pst.setAmountDebit(pst.getAmountCredit().multiply(getMetalRate(pst.getCurrencyCredit()).add(new BigDecimal(1000.0))));

        pst.setSourcePosting("FC12_CL");
        pst.setDealId("123");

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertEquals(getOperday().getCurrentDate(), operation.getCurrentDate());
        Assert.assertEquals(getOperday().getLastWorkdayStatus(), operation.getLastWorkdayStatus());

        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);                 // 1 проводка
        Assert.assertEquals(postList.size(), 2);

        List<Pd> pdList = getPostingPd(postList.get(0));
        Assert.assertEquals(pdList.size(), 2);
        Pd pdDr = pdList.get(0);
        Pd pdCr = pdList.get(1);
        Assert.assertTrue(pdDr.getCcy().equals(operation.getCurrencyDebit()));  // валюта дебет
        Assert.assertTrue(pdCr.getCcy().equals(operation.getCurrencyCredit())); // валюта кредит

        pdList = getPostingPd(postList.get(1));
        Assert.assertEquals(pdList.size(), 2);

    }

    private BigDecimal getMetalRate(BankCurrency curr) throws SQLException {
        return baseEntityRepository.selectFirst("Select rate from GL_SUBCCY where glccy = ?", curr.getCurrencyCode()).getBigDecimal(0);
    }

    private void del706(String bsaacc){
        baseEntityRepository.executeNativeUpdate("delete from EXCACRLN where bsaacid=?", bsaacc);
        baseEntityRepository.executeNativeUpdate("delete from BsaAcc where id=?", bsaacc);
        baseEntityRepository.executeNativeUpdate("delete from accrln where bsaacid=?", bsaacc);
    }

}
;
