package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
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
    String mfoOut = "30301810500160000001";
    String mfoIn = "30302810100010000016";

    @BeforeClass
    public static void beforeClass() throws ParseException {
        Date operday = DateUtils.parseDate("2017-11-01", "yyyy-MM-dd");
        setOperday(operday, DateUtils.addDays(operday, -1), ONLINE, OPEN, pdMode);
//        baseEntityRepository.executeNativeUpdate("delete from ibcb where IBACOU = '"+mfoOut+"' or IBACIN = '"+mfoIn+"'");
//        baseEntityRepository.executeNativeUpdate("delete from gl_acc where bsaacid in ('"+mfoOut+"','"+mfoIn+"')");
    }

    @Test
    public void test()throws ParseException {
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(getOperday().getCurrentDate());
        pst.setAccountCredit("40802810500012433881");
        pst.setAccountDebit("40802810700164226099");
        pst.setAmountCredit(new BigDecimal("1200"));
        pst.setAmountDebit(pst.getAmountCredit());
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setCurrencyDebit(pst.getCurrencyCredit());
        pst.setSourcePosting(KondorPlus.getLabel());
        pst.setDealId("123");

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.POST, operation.getState());
        Assert.assertEquals(getOperday().getCurrentDate(), operation.getCurrentDate());

        List<GLPosting> postList = getPostings(operation);
        Assert.assertNotNull(postList);
        Assert.assertEquals(postList.size(), 2);

        List<Pd> pdList = getPostingPd(postList.get(0));
        pdList.addAll(getPostingPd(postList.get(1)));
        Assert.assertEquals( pdList.stream().filter(item -> item.getBsaAcid().equals(mfoOut)||item.getBsaAcid().equals(mfoIn)).count(), 2);
    }
}
