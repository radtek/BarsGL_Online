package ru.rbt.barsgl.ejb.controller.cob;

/**
 * Created by ER18837 on 15.03.17.
 */
@FunctionalInterface
public interface CobRunningWork {
    public CobStepResult runWork() throws Exception;
}
