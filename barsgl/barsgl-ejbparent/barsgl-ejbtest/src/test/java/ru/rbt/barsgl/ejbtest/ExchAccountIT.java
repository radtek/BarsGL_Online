package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejbtest.utl.Utl4Tests;
import ru.rbt.barsgl.shared.enums.DealSource;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Created by ER22317 on 04.12.2015.
 */
public class ExchAccountIT extends AbstractRemoteIT {

    @Test
    public void testCreateAccountOnKeysDebit() throws SQLException {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        EtlPackage pkg = newPackage(System.currentTimeMillis(), "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);


        EtlPosting pst = newPosting(System.currentTimeMillis(), pkg);
        Assert.assertNotNull(pst);

        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountKeyDebit("001;RUR;00000018;771020201;;;PL00000002;0001;70606;22101;7903;01;K+TP;;");
        pst.setAccountKeyCredit("001;USD;00201475;351020301;10;;0000000013;0001;47408;;1876;01;K+TP;976429;");
        pst.setAmountDebit(new BigDecimal("245490.000"));
        pst.setAmountDebitRu(new BigDecimal("245490.000"));
        pst.setAmountCredit(new BigDecimal("0.000"));
        pst.setAmountCreditRu(new BigDecimal("245490.000"));
        pst.setCurrencyCredit(BankCurrency.USD);
        pst.setCurrencyDebit(BankCurrency.RUB);
        pst.setSourcePosting(DealSource.KondorPlus.getLabel());
        pst.setDealId("981495");
        pst.setErrorCode(null);
        pst.setErrorMessage(null);

        pst = (EtlPosting) baseEntityRepository.save(pst);

        deleteExAccount("70606810480014620140", "0001", "USD", "N", "А");

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);

        String bsaacid = checkExAccount("0001", "USD", "N", "А", operation, GLOperation.OperSide.D);
    }

    @Test
    public void testCreateAccountOnKeysCredit() throws SQLException {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        EtlPackage pkg = newPackage(System.currentTimeMillis(), "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);


        EtlPosting pst = newPosting(System.currentTimeMillis(), pkg);
        Assert.assertNotNull(pst);

        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountKeyDebit("001;USD;00200428;501020501;10;;0000000153;0001;47407;;4498;01;K+TP;978651;");
        pst.setAccountKeyCredit("001;RUR;00000018;671020201;;;PL00000001;0001;70601;12201;7904;01;K+TP;;");
        pst.setAmountDebit(new BigDecimal("0.000"));
        pst.setAmountDebitRu(new BigDecimal("245490.000"));
        pst.setAmountCredit(new BigDecimal("245490.000"));
        pst.setAmountCreditRu(new BigDecimal("245490.000"));
        pst.setCurrencyCredit(BankCurrency.RUB);
        pst.setCurrencyDebit(BankCurrency.USD);
        pst.setSourcePosting(DealSource.KondorPlus.getLabel());
        pst.setDealId("981496");
        pst.setErrorCode(null);
        pst.setErrorMessage(null);

        pst = (EtlPosting) baseEntityRepository.save(pst);

        deleteExAccount("70601810000012201001", "0001", "USD", "N", "П");

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);

        String bsaacid = checkExAccount("0001", "USD", "N", "П", operation, GLOperation.OperSide.C);
    }

    @Test
    public void testCreateAccountOnExchange() throws SQLException {
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
        EtlPackage pkg = newPackage(System.currentTimeMillis(), "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);


        EtlPosting pst = newPosting(System.currentTimeMillis(), pkg);
        Assert.assertNotNull(pst);

        pst.setValueDate(getOperday().getCurrentDate());

        pst.setAccountKeyDebit("001;RUR;00401794;351020301;9;;0000000252;0001;47408;;;;K+TP;981495;");
        pst.setAccountKeyCredit("001;USD;00401794;501020501;9;;0000000251;0001;47407;;;;K+TP;981495;");
        pst.setAmountCredit(new BigDecimal("4000.000"));
        pst.setAmountDebit(new BigDecimal("245490.000"));
        pst.setCurrencyCredit(BankCurrency.USD);
        pst.setCurrencyDebit(BankCurrency.RUB);
        pst.setSourcePosting(DealSource.KondorPlus.getLabel());
        pst.setDealId("981495");
        pst.setErrorCode(null);
        pst.setErrorMessage(null);
        pst.setAmountCreditRu(null);
        pst.setAmountDebitRu(null);

        pst = (EtlPosting) baseEntityRepository.save(pst);

        // 70601810380012620140 70601810880012620640 ???
//        baseEntityRepository.executeNativeUpdate("update gl_acc set dtc = null where dealid = ?","981495");

        deleteExAccount("70601810380012620140", "0001", "USD", "N", "П");

        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);

        String bsaacid = checkExAccount("0001", "USD", "N", "П", operation, GLOperation.OperSide.N);

        Assert.assertEquals(bsaacid, operation.getAccountExchange());

    }

    private void deleteExAccount(String bsaacid, String ccode, String ccy, String cash, String psav ) throws SQLException {
        baseEntityRepository.executeNativeUpdate("delete from GL_ACC where BSAACID = ?", bsaacid);
        baseEntityRepository.executeNativeUpdate("delete from EXCACRLN where BSAACID = ?", bsaacid);

        DataRecord data = baseEntityRepository.selectFirst("select BSAACID from EXCACRLN where ccode = ? and ccy = ? and cash = ? and psav = ?",
                ccode, ccy, cash, psav);
        if (null == data)
            return;
        baseEntityRepository.executeNativeUpdate("delete from GL_ACC where BSAACID = ?", data.getString(0));
        baseEntityRepository.executeNativeUpdate("delete from EXCACRLN where BSAACID = ?", data.getString(0));
    }

    private String checkExAccount(String ccode, String ccy, String cash, String psav, GLOperation operation, GLOperation.OperSide operSide) throws SQLException {
        DataRecord data = baseEntityRepository.selectFirst("select bsaacid from excacrln where ccode = ? and ccy = ? and cash = ? and psav = ?",
                ccode, ccy, cash, psav);
        Assert.assertNotNull("Не создан счет курсовой разницы в EXCACRLN", data);
        String bsaacid = data.getString(0);
        DataRecord account = baseEntityRepository.selectFirst("select * from GL_ACC a where a.bsaAcid = ?", bsaacid);
        Assert.assertNotNull("Не создан счет курсовой разницы в GL_ACC", account);
        Assert.assertEquals(operation.getId(), account.getLong("GLOID"));
        Assert.assertEquals(operSide.name(), account.getString("GLO_DC"));
        return account.getString("BSAACID");
    }
}
