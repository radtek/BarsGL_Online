package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeysBuilder;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Created by ER22317 on 04.12.2015.
 */
public class ExchAccountTest extends AbstractRemoteTest {

    @Test
    public void test() throws SQLException {
//        EtlPosting pst = (EtlPosting) baseEntityRepository.findById(EtlPosting.class, 1L);
        updateOperday(Operday.OperdayPhase.ONLINE, Operday.LastWorkdayStatus.OPEN);
//        EtlPackage pkg = newPackage(System.currentTimeMillis(), "SIMPLE");
//        Assert.assertTrue(pkg.getId() > 0);

        EtlPosting pst = (EtlPosting)baseEntityRepository.findById(EtlPosting.class, Long.valueOf(673));

//        EtlPosting pst = newPosting(System.currentTimeMillis(), pkg);
//        pst.setValueDate(getOperday().getCurrentDate());

//        String accCt = Optional.ofNullable(baseEntityRepository.selectFirst("select id from bsaacid where id like '40807%'"))
//                .orElseThrow(()->new RuntimeException("Null account")).getString("id");
//        pst.setAccountCredit("40817036200012959997");
//        pst.setAccountDebit("40817036250010000018");
//        pst.setAccountCredit("");
//        pst.setAccountDebit("");

//        AccountKeys keysDt = AccountKeysBuilder.create()
//                .withAcc2("47023")
//                .withCurrency("810")
//                .withCompanyCode("8237")
//                .withPlCode("12333")
//                .build();
//
//        AccountKeys keysCt = AccountKeysBuilder.create()
//                .withAcc2("47023")
//                .withCurrency("810")
//                .withCompanyCode("8237")
//                .withPlCode("12333")
//                .build();
//        AccountKeys keysDt = new AccountKeys("001;RUR;00401794;351020301;9;;0000000252;0001;47408;;1876;01;K+TP;981495;");
//        AccountKeys keysCt = new AccountKeys("001;USD;00401794;501020501;9;;0000000251;0001;47407;;4498;01;K+TP;981495;");
//        pst.setAccountKeyDebit(keysDt.toString());
//        pst.setAccountKeyCredit(keysCt.toString());

        //along test
//        pst.setAccountKeyDebit("001;RUR;00401794;351020301;9;;0000000252;0001;47408;;1876;01;K+TP;981495;");
//        pst.setAccountKeyCredit("001;USD;00401794;501020501;9;;0000000251;0001;47407;;4498;01;K+TP;981495;");
//        pst.setAmountCredit(new BigDecimal("40000000.000"));
//        pst.setAmountDebit(new BigDecimal("2454900000.000"));
//        pst.setCurrencyCredit(BankCurrency.USD);
//        pst.setCurrencyDebit(BankCurrency.RUB);
//        pst.setSourcePosting(KondorPlus.getLabel());
//        pst.setDealId("981495");

//        System.out.println("excacRlnRepository.findForPlcode7903");
        Assert.assertNotNull(pst);
//        System.out.println("pst.getErrorCode() = "+pst.getErrorCode());
        pst.setErrorCode(null);
        pst.setErrorMessage(null);
        pst.setAmountCreditRu(null);
        GLOperation operation = (GLOperation) postingController.processMessage(pst);
        Assert.assertNotNull(operation);
//        System.out.println("isExchangeDifferenceA() = "+operation.isExchangeDifferenceA());
        Assert.assertEquals("823746823492384692", operation.getAccountExchange());

    }
}
