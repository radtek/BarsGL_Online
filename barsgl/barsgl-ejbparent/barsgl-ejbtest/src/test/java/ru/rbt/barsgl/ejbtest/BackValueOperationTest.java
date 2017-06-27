package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLBackValueOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperationExt;
import ru.rbt.barsgl.ejb.integr.bg.EtlPostingController;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.enums.BackValuePostStatus;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Logger;

import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;

/**
 * Created by er18837 on 26.06.2017.
 */
public class BackValueOperationTest extends AbstractTimerJobTest {

    public static final Logger log = Logger.getLogger(BackValueOperationTest.class.getName());

    @Test
    public void testFindOperationExt() throws SQLException {
        /*DataRecord data = baseEntityRepository.selectFirst("select max(GLOID) from GL_OPEREXT");
        Long gloExt = data.getLong(0);

        baseEntityRepository.executeNativeUpdate("update GL_OPER set OPER_CLASS = 'BACK_VALUE' where GLOID = ?", gloExt);

        GLOperationExt operExt = (GLOperationExt) baseEntityRepository.findById(GLOperationExt.class, gloExt);
        Assert.assertNotNull(operExt);*/
        Long gloExt = 42260L;
        GLOperation oper = (GLOperation) baseEntityRepository.findById(GLOperation.class, gloExt);
        Assert.assertNotNull(oper);
        GLBackValueOperation operBV = (GLBackValueOperation) baseEntityRepository.findById(GLBackValueOperation.class, gloExt);
        Assert.assertNotNull(operBV);
    }

    @Test
    public void testCreatOperationExt() throws SQLException {

        String bsaDt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810%1");
        String bsaCt = Utl4Tests.findBsaacid(baseEntityRepository, getOperday(), "40817810%3");

        BigDecimal amt = new BigDecimal("98.76");

        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(getOperday().getCurrentDate());
        cal.add(Calendar.DATE, -7);
        Date valueDate = cal.getTime();
        EtlPosting pst = createEtlPosting(valueDate, GLOperation.srcKondorPlus, bsaDt, RUB, amt, bsaCt, RUB, amt);
        Assert.assertNotNull(pst);

        GLBackValueOperation operation = remoteAccess.invoke(EtlPostingController.class, "processBackValue", pst);
        Assert.assertNotNull(operation);
//        Assert.assertNotNull(operation.getOperExt());

        GLOperationExt operExt = (GLOperationExt) baseEntityRepository.findById(GLOperationExt.class, operation.getId());
        Assert.assertNotNull(operExt);

        Assert.assertEquals(BackValuePostStatus.CONTROL, operExt.getManualStatus());
        Assert.assertEquals(operation.getPostDate(), operExt.getPostdatePlan());

    }

    private EtlPosting createEtlPosting(Date valueDate, String src,
                                        String accountDebit, BankCurrency currencyDebit, BigDecimal amountDebit,
                                        String accountCredit, BankCurrency currencyCredit, BigDecimal amountCredit) {
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "BackValue");
        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = newPosting(stamp, pkg);
        pst.setValueDate(valueDate);
        pst.setSourcePosting(src);
        pst.setDealId("DEAL_" + stamp);

        pst.setAccountDebit(accountDebit);
        pst.setCurrencyDebit(currencyDebit);
        pst.setAmountDebit(amountDebit);

        pst.setAccountCredit(accountCredit);
        pst.setCurrencyCredit(currencyCredit);
        pst.setAmountCredit(amountCredit);

        return (EtlPosting) baseEntityRepository.save(pst);
    }

}
