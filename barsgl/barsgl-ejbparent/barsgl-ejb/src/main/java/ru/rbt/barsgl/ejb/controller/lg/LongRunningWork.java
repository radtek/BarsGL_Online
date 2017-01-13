package ru.rbt.barsgl.ejb.controller.lg;

import ru.rbt.barsgl.ejb.entity.task.JobHistory;

/**
 * Created by Ivan Sevastyanov on 19.10.2016.
 */
@FunctionalInterface
public interface LongRunningWork {

    public boolean runWork() throws Exception;
}
