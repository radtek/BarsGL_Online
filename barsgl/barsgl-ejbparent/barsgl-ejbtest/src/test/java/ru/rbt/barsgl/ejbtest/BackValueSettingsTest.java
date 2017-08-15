package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.entity.dict.BVSourceDeal;
import ru.rbt.barsgl.ejb.entity.dict.BVSourceDealId;
import ru.rbt.barsgl.ejb.entity.dict.ClosedReportPeriod;
import ru.rbt.barsgl.ejb.integr.dict.ManualDictionaryService;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.BVSourceDealWrapper;
import ru.rbt.barsgl.shared.dict.ClosedReportPeriodWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.DealSource;

import java.text.SimpleDateFormat;
import java.util.Date;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;

/**
 * Created by er18837 on 15.08.2017.
 */
public class BackValueSettingsTest extends AbstractTimerJobTest {

    @Test
    public void testClosedReportPeriod(){
        Operday operday = getOperday();
        Date lastDate = operday.getLastWorkingDay();
        Date cutDate = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateAfter", operday.getCurrentDate(), false);
        baseEntityRepository.executeUpdate("delete from ClosedReportPeriod p where p.lastDate = ?1", lastDate);

        ClosedReportPeriodWrapper wrapper = new ClosedReportPeriodWrapper();
        SimpleDateFormat dateFormat = new SimpleDateFormat(wrapper.getDateFormat());
        wrapper.setLastDateStr(dateFormat.format(lastDate));
        wrapper.setCutDateStr(dateFormat.format(cutDate));

        RpcRes_Base<ClosedReportPeriodWrapper> res = remoteAccess.invoke(ManualDictionaryService.class, "saveClosedReportPeriod", wrapper, FormAction.CREATE);
        if (!isEmpty(res.getMessage()))
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());

        ClosedReportPeriod period = (ClosedReportPeriod) baseEntityRepository.findById(ClosedReportPeriod.class, lastDate);
        Assert.assertNotNull(period);
        Assert.assertEquals(cutDate, period.getCutDate());
        Assert.assertNotNull(period.getCreateTimestamp());

        Date nextDate = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateAfter", cutDate, false);
        wrapper.setCutDateStr(dateFormat.format(nextDate));

        res = remoteAccess.invoke(ManualDictionaryService.class, "saveClosedReportPeriod", wrapper, FormAction.UPDATE);
        if (!isEmpty(res.getMessage()))
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());

        period = (ClosedReportPeriod) baseEntityRepository.findById(ClosedReportPeriod.class, lastDate);
        Assert.assertNotNull(period);
        Assert.assertEquals(nextDate, period.getCutDate());
        Assert.assertNotNull(period.getCreateTimestamp());

        res = remoteAccess.invoke(ManualDictionaryService.class, "saveClosedReportPeriod", wrapper, FormAction.DELETE);
        if (!isEmpty(res.getMessage()))
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());

        period = (ClosedReportPeriod) baseEntityRepository.findById(ClosedReportPeriod.class, lastDate);
        Assert.assertNull(period);
    }

    @Test
    public void testBVSourceDeal() {
        Operday operday = getOperday();
        Date startDate = remoteAccess.invoke(BankCalendarDayRepository.class, "getWorkDateAfter", operday.getCurrentDate(), false);
        String src = DealSource.PaymentHub.getLabel();

        BVSourceDealWrapper wrapper = new BVSourceDealWrapper();
        SimpleDateFormat dateFormat = new SimpleDateFormat(wrapper.getDateFormat());
        wrapper.setSourceDeal(src);
        wrapper.setDepth(3);
        wrapper.setStartDateStr(dateFormat.format(startDate));

        BVSourceDealId id = new BVSourceDealId(src, startDate);
        baseEntityRepository.executeUpdate("delete from BVSourceDeal d where d.id = ?1", id);

        RpcRes_Base<ClosedReportPeriodWrapper> res = remoteAccess.invoke(ManualDictionaryService.class, "saveBVSourceDeal", wrapper, FormAction.CREATE);
        if (!isEmpty(res.getMessage()))
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());

        BVSourceDeal sourceDeal = (BVSourceDeal) baseEntityRepository.findById(BVSourceDeal.class, id);
        Assert.assertNotNull(sourceDeal);
        Assert.assertEquals(3, (int)sourceDeal.getShift());
        Assert.assertNull(sourceDeal.getEndDate());
        Assert.assertNotNull(sourceDeal.getCreateTimestamp());

        wrapper.setDepth(5);

        res = remoteAccess.invoke(ManualDictionaryService.class, "saveBVSourceDeal", wrapper, FormAction.UPDATE);
        if (!isEmpty(res.getMessage()))
            System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());

        sourceDeal = (BVSourceDeal) baseEntityRepository.findById(BVSourceDeal.class, id);
        Assert.assertNotNull(sourceDeal);
        Assert.assertEquals(5, (int)sourceDeal.getShift());

    }

}
