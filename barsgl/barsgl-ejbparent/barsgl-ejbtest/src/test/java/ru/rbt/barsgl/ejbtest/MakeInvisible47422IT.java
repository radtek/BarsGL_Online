package ru.rbt.barsgl.ejbtest;

import org.junit.Test;
import ru.rbt.barsgl.ejb.controller.operday.task.MakeInvisible47422Task;

import java.util.Properties;

/**
 * Created by er18837 on 06.09.2018.
 */
public class MakeInvisible47422IT extends AbstractRemoteIT {

    @Test
    public void testFirst() {
        Properties props = new Properties();
        props.setProperty("depth", "30");
//        props.setProperty("mode", "Glue");
        remoteAccess.invoke(MakeInvisible47422Task.class, "testExec", null, props);
    }
}
