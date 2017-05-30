package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Created by ER22317 on 04.12.2015.
 */
public class ExchAccountIT extends AbstractRemoteIT {

    @Test
    public void test() throws SQLException {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        EtlPackage pkg = newPackage(System.currentTimeMillis(), "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);


        EtlPosting pst = newPosting(System.currentTimeMillis(), pkg);
        Assert.assertNotNull(pst);

        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountKeyDebit("001;RUR;00401794;351020301;9;;0000000252;0001;47408;;;;K+TP;981495;");
        pst.setAccountKeyCredit("001;USD;00401794;501020501;9;;0000000251;0001;47407;;;;K+TP;981495;");
        pst.setAmountCredit(new BigDecimal("40000000.000"));
        pst.setAmountDebit(new BigDecimal("2454900000.000"));
        pst.setCurrencyCredit(BankCurrency.USD);
        pst.setCurrencyDebit(BankCurrency.RUB);
        pst.setSourcePosting(GLOperation.srcKondorPlus);
        pst.setDealId("981495");
        pst.setErrorCode(null);
        pst.setErrorMessage(null);
        pst.setAmountCreditRu(null);
        pst.setAmountDebitRu(null);


        baseEntityRepository.executeNativeUpdate("update gl_acc set dtc = null where dealid = ?","981495");
        baseEntityRepository.executeNativeUpdate("update accrln set drlnc = null where BSAACID in ('47408810320010000026', '47407840020010000025')");
        baseEntityRepository.executeNativeUpdate("update bsaacc set bsaacc = ? where ID in ('47408810320010000026', '47407840020010000025')",
                Utl4Tests.parseDate("2029-01-01", "yyyy-MM-dd"));

        pst = (EtlPosting) baseEntityRepository.save(pst);

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);

        Assert.assertNotNull(baseEntityRepository.selectOne("select bsaacid from excacrln where ccode = ? and ccy = ? and cash = ? and psav = ?", "0001", "USD", "N", "П"));

        Assert.assertNotNull(operation.getAccountExchange());

    }
}
