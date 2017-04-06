package ru.rbt.barsgl.ejb.controller.lg;

/**
 * Created by Ivan Sevastyanov on 19.10.2016.
 */
@FunctionalInterface
public interface LongRunningWork {

    public boolean runWork() throws Exception;
}
