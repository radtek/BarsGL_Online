package ru.rbt.barsgl.ejb.controller.operday.task.dem;

import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

import javax.ejb.EJB;
import java.util.Properties;

/**
 * Created by Ivan Sevastyanov on 22.01.2016.
 * выгрузка остатков по запросу
 */
public class UniAccountBalanceUnloadTask implements ParamsAwareRunnable {

    @EJB
    private UniAccountBalanceUnloadTaskSupport taskSupport;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        taskSupport.execute(properties);
    }

}
