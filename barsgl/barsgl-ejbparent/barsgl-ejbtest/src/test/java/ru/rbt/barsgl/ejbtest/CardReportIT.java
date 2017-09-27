package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.integr.bg.CardReportController;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.operation.CardReportWrapper;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by er18837 on 23.08.2017.
 */
public class CardReportIT extends AbstractRemoteIT{

    @Test
    public void testReportSqlOld() throws SQLException {
        Date dat = getOperday().getCurrentDate();
        CardReportWrapper wrapper = new CardReportWrapper();
        wrapper.setFilial("CHL");
        wrapper.setPostDateStr(new SimpleDateFormat(wrapper.getDateFormat()).format(dat));

        RpcRes_Base<CardReportWrapper> res = remoteAccess.invoke(CardReportController.class, "getCardReport", wrapper);
        System.out.println(res.getMessage());
        Assert.assertFalse(res.isError());
        String sql = res.getResult().getReportSql();
        Assert.assertNotNull(sql);
        System.out.println(sql);

        List<DataRecord> report = baseEntityRepository.select(sql);
        Assert.assertNotNull(report);
        // Assert.assertFalse(report.isEmpty());
        System.out.println(report.size());
    }
}
