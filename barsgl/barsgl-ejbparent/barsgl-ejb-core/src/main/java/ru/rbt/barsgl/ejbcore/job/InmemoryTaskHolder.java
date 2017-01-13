package ru.rbt.barsgl.ejbcore.job;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by Ivan Sevastyanov
 */
class InmemoryTaskHolder implements Serializable {

    private final Class<? extends InmemoryTask> runnableClass;
    private final Map<String,Object> context;

    InmemoryTaskHolder(Class<? extends InmemoryTask> runnableClass, Map<String, Object> context) {
        this.runnableClass = runnableClass;
        this.context = context;
    }

    Class<? extends InmemoryTask> getRunnableClass() {
        return runnableClass;
    }

    Map<String, Object> getContext() {
        return context;
    }
}
