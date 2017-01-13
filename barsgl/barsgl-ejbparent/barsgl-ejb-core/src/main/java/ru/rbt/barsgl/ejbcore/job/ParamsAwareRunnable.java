package ru.rbt.barsgl.ejbcore.job;

import java.io.Serializable;
import java.util.Properties;

/**
 * Created by Ivan Sevastyanov
 */
public interface ParamsAwareRunnable extends Serializable {
    /**
     * здесь выполняется "полезная работа"
     * @param jobName название запускающей задачи
     * @param properties параметры выполнения задачи
     */
    void run(String jobName, Properties properties) throws Exception;
}
