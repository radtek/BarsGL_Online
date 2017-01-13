package ru.rbt.barsgl.ejb.controller.operday.task.test;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.operday.PreCobStepController;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

import javax.ejb.EJB;
import java.util.Properties;

/**
 * Created by Ivan Sevastyanov on 24.06.2016.
 */
public class ProcessFansTestTask implements ParamsAwareRunnable {

    public static final String OPERDATE_KEY = "operday";

    @EJB
    private PreCobStepController cobStepController;

    @EJB
    private OperdayController operdayController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        java.sql.Date operday = new java.sql.Date(
                TaskUtils.getExecuteDate(OPERDATE_KEY, properties, operdayController.getOperday().getCurrentDate()).getTime());
        cobStepController.processFan(operday);
    }
}
