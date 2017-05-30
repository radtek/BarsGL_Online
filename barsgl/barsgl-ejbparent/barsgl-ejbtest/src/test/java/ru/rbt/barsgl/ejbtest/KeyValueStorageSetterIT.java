package ru.rbt.barsgl.ejbtest;

import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.KeyValueStorageSetter;
import ru.rbt.barsgl.ejbcore.mapping.job.SingleActionJob;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;

import java.util.logging.Logger;

/**
 * Created by ER22228
 */
public class KeyValueStorageSetterIT extends AbstractTimerJobIT {

    public static final Logger logger = Logger.getLogger(KeyValueStorageSetterIT.class.getName());


    @Test
    public void testLocal() throws Exception {

        SingleActionJob job =
            SingleActionJobBuilder.create()
                .withClass(KeyValueStorageSetter.class)
                .withProps(
                    "operation=setTaskContinue\n" +
                        "key=AccountListTask\n" +
                        "value=false"
                )
                .build();
        jobService.executeJob(job);
    }
}
