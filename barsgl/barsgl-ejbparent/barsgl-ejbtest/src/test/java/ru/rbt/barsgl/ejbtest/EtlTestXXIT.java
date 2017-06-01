package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.mapping.YesNo;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by ER22317 on 09.03.2017.
 */
public class EtlTestXXIT extends AbstractTimerJobIT {

    @Test
    public void test01() throws Exception {
        long stamp = System.currentTimeMillis();

        EtlPackage pkg = newPackage(stamp, "SIMPLE");
        Assert.assertTrue(pkg.getId() > 0);
        pkg = (EtlPackage) baseEntityRepository.findById(pkg.getClass(), pkg.getId());
        pkg.setPackageState(EtlPackage.PackageState.LOADED);
        pkg.setAccountCnt(1);
        pkg.setPostingCnt(1);
        pkg = (EtlPackage) baseEntityRepository.update(pkg);

        EtlPosting pst1 = newPosting(stamp, pkg);
        pst1.setAePostingId("134433218");
        pst1.setEventId("ECqbn0715855");
        pst1.setDealId("RCqbn0782986");
        pst1.setValueDate(getOperday().getCurrentDate());
        pst1.setOperationTimestamp(new Date());
        pst1.setNarrative("CD02 CD02 qbn14W601");
        pst1.setRusNarrativeLong("RusNarrativeLong");
        pst1.setRusNarrativeShort("RusNarrativeShort");
        pst1.setStorno(YesNo.N);
        pst1.setAmountCredit(new BigDecimal("167.800"));
        pst1.setAmountDebit(new BigDecimal("167.800"));
        pst1.setCurrencyCredit(BankCurrency.RUB);
        pst1.setCurrencyDebit(BankCurrency.RUB);
        pst1.setSourcePosting("CAS_MRC");
        pst1.setFan(YesNo.Y);
        pst1.setParentReference("ECqbn0715855");
        pst1.setAccountKeyDebit("045;RUR;00620451;358010501;;;0000595563;0045;47423;;1897;01;CAS_MRC;;");
        pst1.setAccountKeyCredit("045;RUR;00000451;503020114;;;XX00001255;0045;60309;;;;CAS_MRC;;");
        pst1 = (EtlPosting) baseEntityRepository.save(pst1);

        GLOperation operation = (GLOperation) postingController.processMessage(pst1);

        Assert.assertNotNull(operation);
        Assert.assertTrue(0 < operation.getId());
        operation = (GLOperation) baseEntityRepository.findById(operation.getClass(), operation.getId());
        Assert.assertEquals(OperState.LOAD, operation.getState());
    }

}
