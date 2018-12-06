package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.Pd;
import ru.rbt.barsgl.shared.enums.OperState;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.barsgl.shared.enums.DealSource.KondorPlus;

/**
 * Created by er22317 on 06.03.2018.
 */
public class EtlMfoIT extends AbstractTimerJobIT{
    private static final Operday.PdMode pdMode = DIRECT;
    static final String mfoOut = "30301810500160000001";
    static final String mfoIn = "30302810100010000016";//
    static final String mf1 = "30301810800010000016";
    static final String mf2 = "30302810800160000001";//

    /*
    select * from ibcb where ibacou = '30301810900020000002'; -- or IBACIN = '30301810900020000002';
    select * from ibcb where IBACIN = '30302810500010000001'; -- ibacou = '30302810500010000001' or
    */
    @BeforeClass
    public static void beforeClass() throws ParseException {
        Date operday = DateUtils.parseDate("2017-11-01", "yyyy-MM-dd");
        setOperday(operday, DateUtils.addDays(operday, -1), ONLINE, OPEN, pdMode);
//        baseEntityRepository.executeNativeUpdate("delete from ibcb where IBACOU = ? or IBACIN = ?", mfoOut, mfoIn );
        baseEntityRepository.executeNativeUpdate("delete from ibcb where IBBRNM in (?,?)", "CHL", "MOS" );
        baseEntityRepository.executeNativeUpdate("delete from gl_acc where bsaacid in (?,?,?,?)", mfoOut, mfoIn, mf1, mf2);
    }

    @Test
    public void test() throws Exception {
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        GLAccount accCredit = findAccount("40802810___01%");
        Assert.assertNotNull(accCredit);
        GLAccount accDebit = findAccount("40802810___16%");
        Assert.assertNotNull(accDebit);

        // в разных бранчах
        Assert.assertFalse(accCredit.getBranch().equals(accDebit.getBranch()));

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());
        pst.setAccountCredit(accCredit.getBsaAcid());
        pst.setAccountDebit(accDebit.getBsaAcid());
        pst.setAmountCredit(new BigDecimal("1200"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setSourcePosting(KondorPlus.getLabel());
        pst.setDealId("123");

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        if (null == operation) {
            printAuditLog(10);
        }
        Assert.assertNotNull(operation);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertEquals(getOperday().getCurrentDate(), operation.getCurrentDate());

        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);
        Assert.assertEquals(2, postList.size());

        List<Pd> pdList = getPostingPd(postList.get(0));
        pdList.addAll(getPostingPd(postList.get(1)));
        pdList.stream().forEach(item-> System.out.println("pst.id = " + item.getId() + " bsaacid = " +item.getBsaAcid() ));
        Assert.assertEquals( pdList.stream().filter(item -> item.getBsaAcid().equals(mfoOut)||item.getBsaAcid().equals(mfoIn)).count(), 2);

        int cnt = baseEntityRepository.selectFirst("select count(1) from ibcb where IBACOU = ?", mfoOut).getInteger(0);
        Assert.assertEquals(1, cnt);
        cnt = baseEntityRepository.selectFirst("select count(1) from ibcb where IBACIN = ?", mfoIn).getInteger(0);
        Assert.assertEquals(1, cnt);
        cnt = baseEntityRepository.selectFirst("select count(*) from gl_acc where bsaacid in (?,?)", mfoOut, mfoIn).getInteger(0);
        Assert.assertEquals( 2, cnt );
    }
}
