package ru.rbt.barsgl.ejbtest;

import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.LoadBranchDictTask;
import ru.rbt.barsgl.ejbtest.utl.SingleActionJobBuilder;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by er22317 on 09.02.2018.
 */
public class LoadBranchDictTest extends AbstractRemoteIT {
    private static final Logger log = Logger.getLogger(FanNdsPostingIT.class.getName());

    @Test
    public void test() throws Exception {

        Properties props = new Properties();
        jobService.executeJob(SingleActionJobBuilder.create().withClass(LoadBranchDictTask.class).withProps(props).build());

    }
}