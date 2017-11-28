/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2016
 */
package ru.rbt.barsgl.ejbtest;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.AccountQueryProcessor;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Andrew Samsonov
 */
public class AccountQueryProcessorIT extends AbstractRemoteIT {

    private static final Logger logger = Logger.getLogger(AccountQueryProcessorIT.class.getName());

    @Test
    public void process() throws Exception {
        Map<String, String> currencyMap = new HashMap<>();
        Map<String, Integer> currencyNBDPMap = new HashMap<>();
        String testRequest = IOUtils.toString(this.getClass().getResourceAsStream("/AccountQueryProcessorTest.xml"), "UTF-8");
        Assert.assertNotNull(testRequest);        

        List<DataRecord> dataRecords = baseEntityRepository.selectMaxRows("SELECT GLCCY,CBCCY,NBDP FROM CURRENCY", Integer.MAX_VALUE, null);
        dataRecords.stream().forEach(item -> {
            currencyMap.put(item.getString("CBCCY"), item.getString("GLCCY"));
            currencyNBDPMap.put(item.getString("CBCCY"), item.getInteger("NBDP"));
        });
        
        String value = remoteAccess.invoke(AccountQueryProcessor.class, "process", testRequest, currencyMap, currencyNBDPMap, 0L, true);

        logger.log(Level.INFO, "Return value: {0}", value);
        Assert.assertNotNull(value);
    }
}
