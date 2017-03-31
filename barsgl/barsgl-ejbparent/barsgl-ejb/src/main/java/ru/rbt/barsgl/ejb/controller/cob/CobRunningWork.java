package ru.rbt.barsgl.ejb.controller.cob;

import ru.rbt.barsgl.shared.enums.CobPhase;

/**
 * Created by ER18837 on 15.03.17.
 */
@FunctionalInterface
public interface CobRunningWork {
    public CobStepResult runWork(Long idCob, CobPhase phase) throws Exception;
}
