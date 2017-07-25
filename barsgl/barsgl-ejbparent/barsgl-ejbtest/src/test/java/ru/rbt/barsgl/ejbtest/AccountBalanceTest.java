package ru.rbt.barsgl.ejbtest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.CheckAccountWrapper;
import ru.rbt.barsgl.ejb.integr.bg.ManualOperationController;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by er23851 on 24.07.2017.
 */
public class AccountBalanceTest extends AbstractRemoteTest {


    @Test
    public void testCheckRedSaldoAccountBalance()
    {
        Date currentDate = new Date();
        CheckAccountWrapper wrapper = new CheckAccountWrapper();
        wrapper.setBsaAcid("30424810700014634892");
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
        wrapper.setDateOperStr(df.format(DateUtils.addDays(currentDate, -30)));
        wrapper.setAmount(BigDecimal.valueOf(3242720000L));

        RpcRes_Base<CheckAccountWrapper> res =  remoteAccess.invoke(ManualOperationController.class,"checkAmountBalance",wrapper);


        Assert.assertTrue(res.isError());

    }

    @Test
    public void testCheckNoRedSaldoAccountBalance()
    {
        Date currentDate = new Date();
        CheckAccountWrapper wrapper = new CheckAccountWrapper();
        wrapper.setBsaAcid("30424810700014634892");
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
        wrapper.setDateOperStr(df.format(DateUtils.addDays(currentDate, -30)));
        wrapper.setAmount(BigDecimal.valueOf(10000L));

        RpcRes_Base<CheckAccountWrapper> res =  remoteAccess.invoke(ManualOperationController.class,"checkAmountBalance",wrapper);


        Assert.assertFalse(res.isError());

    }

}
