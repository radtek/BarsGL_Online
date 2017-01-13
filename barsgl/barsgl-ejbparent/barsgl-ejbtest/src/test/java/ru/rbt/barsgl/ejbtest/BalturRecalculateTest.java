package ru.rbt.barsgl.ejbtest;

import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by Ivan Sevastyanov on 12.08.2016.
 * Пересчет остатков по оборотам.
 * Выполняется по счетам для которых было изменение PD
 * во врем вероятного отключения триггеров по пересчету остатков
 */
public class BalturRecalculateTest extends AbstractRemoteTest {

    public static final Logger log = Logger.getLogger(BalturRecalculateTest.class.getName());

    @Test public void test() throws SQLException {
        List<DataRecord> pdsCt = baseEntityRepository.selectMaxRows("select * from pd where amntbc > 0 and bsaacid <> '' and acid <> '' and invisible <> '1'", 10, new Object[]{});
        Assert.assertEquals(10, pdsCt.size());
        String bsaacid = pdsCt.get(0).getString("bsaacid");
        String acid = pdsCt.get(0).getString("acid");

        List<DataRecord> pdsDt = baseEntityRepository.selectMaxRows("select * from pd where amntbc < 0 and bsaacid <> '' and acid <> '' and invisible <> '1'", 10, new Object[]{});

        log.info("deleted: " + baseEntityRepository.executeNativeUpdate("delete from baltur where bsaacid = ? and acid = ?", bsaacid, acid));
        List<DataRecord> allpds = new ArrayList<>();
        allpds.addAll(pdsCt); allpds.addAll(pdsDt);
        allpds.forEach(r -> {
            baseEntityRepository.executeNativeUpdate("update pd set amntbc = 0, amnt = 0 where id = ?", r.getLong("id"));
        });


    }
}
