/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2016
 * Financial Board Project
 */
package ru.rbt.barsgl.ejbtest;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.AccountQueryProcessor;
import ru.rbt.barsgl.ejbcore.AccountQueryRepository;
import static ru.rbt.barsgl.ejbtest.AbstractRemoteTest.remoteAccess;

/**
 *
 * @author Andrew Samsonov
 */
public class AccountQueryProcessorTest extends AbstractRemoteTest {

    private static final Logger logger = Logger.getLogger(AccountQueryProcessorTest.class.getName());

    @Test
    public void process() throws Exception {
        Map<String, String> currencyMap = new HashMap<>();
        Map<String, Integer> currencyNBDPMap = new HashMap<>();
        String testRequest = IOUtils.toString(this.getClass().getResourceAsStream("/AccountQueryProcessorTest.xml"), "UTF-8");
        Assert.assertNotNull(testRequest);        

        remoteAccess.invoke(AccountQueryRepository.class, "loadCurrency", currencyMap, currencyNBDPMap);
        
        String value = remoteAccess.invoke(AccountQueryProcessor.class, "process", testRequest, currencyMap, currencyNBDPMap, 0L, true);
        
        logger.log(Level.INFO, "Return value: {0}", value);
        Assert.assertNotNull(value);
    }
}
