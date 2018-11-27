package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.BankCalendarDay;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLBackValueOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.ejbcore.mapping.YesNo;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;

import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.BUFFER;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.AUD;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;
import static ru.rbt.barsgl.ejbtest.BackValueOperationIT.createEtlPosting;
import static ru.rbt.barsgl.ejbtest.BackValueOperationIT.createEtlPostingNotSaved;
import static ru.rbt.barsgl.shared.enums.DealSource.ARMPRO;
import static ru.rbt.barsgl.shared.enums.DealSource.SECMOD;
import static ru.rbt.barsgl.shared.enums.OperState.CANC;
import static ru.rbt.barsgl.shared.enums.OperState.POST;
import static ru.rbt.barsgl.shared.enums.OperState.SOCANC;

/**
 * Created by er18837 on 26.11.2018.
 */
public class BackValueStornoIT extends AbstractTimerJobIT {

    @BeforeClass
    public static void beforeClass() {
        try {
            setOperday(DateUtils.parseDate("03.07.2018", "dd.MM.yyyy"), DateUtils.parseDate("02.07.2018", "dd.MM.yyyy"), ONLINE, OPEN, DIRECT);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        setBVparams();
    }

    @AfterClass
    public static void afterClass() {
        restoreOperday();
    }

    // заполнить параметры BackValue
    public static void setBVparams() {
        baseEntityRepository.executeNativeUpdate("delete from GL_BVPARM");
        baseEntityRepository.executeNativeUpdate("insert into GL_BVPARM (ID_SRC, BV_SHIFT, BVSTRN_INVISIBLE, DTB, DTE) values ('ARMPRO', 4, 'Y', '2017-01-01', null)");
        baseEntityRepository.executeNativeUpdate("insert into GL_BVPARM (ID_SRC, BV_SHIFT, BVSTRN_INVISIBLE, DTB, DTE) values ('SECMOD', 2, 'N', '2017-01-01', null)");

        baseEntityRepository.executeNativeUpdate("delete from GL_CRPRD");
//        baseEntityRepository.executeNativeUpdate("insert into GL_CRPRD (PRD_LDATE, PRD_CUTDATE) values ('2015-02-28', '2015-03-04')");
    }

    @Test
    public void testStrotnoInvisN() throws SQLException {
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20209810_0001%1");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20209810_0001%2");
        BigDecimal amt = new BigDecimal("123.45");
        BankCurrency currency = RUB;
        Date od = getOperday().getCurrentDate();
        Date vdate = getWorkDateBefore(od, 2);
        Date sdate = vdate;

        // SECMOD прямая
        EtlPosting pst = createEtlPosting(vdate, SECMOD.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);
        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());

        // сторно
        EtlPosting pstS = createStornoPosting(sdate, pst);
        GLOperation operationS = (GLOperation) postingController.processMessage(pstS);
        operationS = (GLOperation) baseEntityRepository.findById(operationS.getClass(), operationS.getId());

        Assert.assertTrue(operationS.isStorno());
        Assert.assertEquals(POST, operation.getState());
        Assert.assertEquals(POST, operationS.getState());
        Assert.assertEquals(operation.getId(), operationS.getStornoOperation().getId());
    }

    @Test
    public void testStrotnoInvisY() throws SQLException {
        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20209810_0001%1");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "20209810_0001%2");
        BigDecimal amt = new BigDecimal("123.45");
        BankCurrency currency = RUB;
        Date od = getOperday().getCurrentDate();
        Date vdate = getWorkDateBefore(od, 2);
        Date sdate = vdate;

        // ARMPRO прямая
        EtlPosting pst = createEtlPosting(vdate, ARMPRO.getLabel(), bsaDt, currency, amt, bsaCt, currency, amt);
        GLOperation operation = (GLOperation) postingController.processMessage(pst);

        // сторно
        EtlPosting pstS = createStornoPosting(sdate, pst);
        GLOperation operationS = (GLOperation) postingController.processMessage(pstS);
        operationS = (GLOperation) baseEntityRepository.findById(operationS.getClass(), operationS.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());

        Assert.assertTrue(operationS.isStorno());
        Assert.assertEquals(CANC, operation.getState());
        Assert.assertEquals(SOCANC, operationS.getState());
        Assert.assertEquals(operation.getId(), operationS.getStornoOperation().getId());
    }

    public EtlPosting createEtlPosting(Date valueDate, String src,
                                              String accountDebit, BankCurrency currencyDebit, BigDecimal amountDebit,
                                              String accountCredit, BankCurrency currencyCredit, BigDecimal amountCredit) {
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "BackValuePackage");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = createEtlPostingNotSaved(pkg, valueDate, src,
                accountDebit, currencyDebit, amountDebit,
                accountCredit, currencyCredit, amountCredit);

        return (EtlPosting) baseEntityRepository.save(pst);
    }

    public EtlPosting createStornoPosting(Date valueDateStorno, EtlPosting pst) {
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "BackValuePackage");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pstS = newPosting(stamp, pkg);
        // идентификаторы
        pstS.setSourcePosting(pst.getSourcePosting());
        pstS.setStornoReference(pst.getEventId());
        pstS.setDealId(pst.getDealId());
        pstS.setPaymentRefernce(pst.getPaymentRefernce());
        pstS.setStorno(YesNo.Y);
        // данные по вееру
        pstS.setFan(pst.getFan());
        pstS.setParentReference(pst.getParentReference());
        // данные проводки (инвертированы)
        pstS.setAccountDebit(pst.getAccountCredit());
        pstS.setCurrencyDebit(pst.getCurrencyCredit());
        pstS.setAmountDebit(pst.getAmountCredit());
        pstS.setAccountCredit(pst.getAccountDebit());
        pstS.setCurrencyCredit(pst.getCurrencyDebit());
        pstS.setAmountCredit(pst.getAmountDebit());

        pstS.setValueDate(valueDateStorno);
        return (EtlPosting) baseEntityRepository.save(pstS);
    }


    public static Date getWorkDateBefore(Date dateTo, int days, boolean withTech) {
        return remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", dateTo, days, withTech);
    }

    public static Date getWorkDateBefore(Date dateTo, int days) {
        return remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateBefore", dateTo, days, false);
    }
}
