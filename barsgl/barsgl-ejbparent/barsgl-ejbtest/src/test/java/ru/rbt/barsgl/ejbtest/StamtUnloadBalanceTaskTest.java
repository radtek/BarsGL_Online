package ru.rbt.barsgl.ejbtest;

import com.ibm.jms.JMSTextMessage;
import com.ibm.mq.jms.*;
import com.ibm.msg.client.wmq.WMQConstants;
import org.junit.Ignore;
import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.AccountDetailsNotifyTask;
import ru.rbt.barsgl.ejb.controller.operday.task.srvacc.AccountDetailsNotifyProcessor;
import ru.rbt.barsgl.ejb.controller.operday.task.stamt.StamtUnloadBalanceStep2Task;
import ru.rbt.barsgl.ejb.entity.acc.AcDNJournal;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;

import javax.jms.JMSException;
import javax.jms.Session;
import java.util.logging.Logger;

import static junit.framework.TestCase.assertTrue;

/**
 * Created by ER22228
 */
public class StamtUnloadBalanceTaskTest extends AbstractTimerJobTest {

    public static final Logger logger = Logger.getLogger(StamtUnloadBalanceTaskTest.class.getName());

    @Test
    @Ignore
    public void testRun() throws Exception {
        SingleActionJob job =
            SingleActionJobBuilder.create()
                .withClass(StamtUnloadBalanceStep2Task.class)
                .withProps("operday=01.10.2015")
                .build();
        jobService.executeJob(job);
    }
}
