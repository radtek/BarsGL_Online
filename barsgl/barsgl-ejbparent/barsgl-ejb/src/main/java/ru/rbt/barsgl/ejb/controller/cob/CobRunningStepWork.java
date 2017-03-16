package ru.rbt.barsgl.ejb.controller.cob;

import ru.rbt.barsgl.shared.enums.CobStep;

/**
 * Created by ER18837 on 15.03.17.
 */
public class CobRunningStepWork {

    private final CobStep step;
    private final CobRunningWork work;

    public CobRunningStepWork(CobStep step, CobRunningWork work) {
        this.step = step;
        this.work = work;
    }

    public CobStep getStep() {
        return step;
    }

    public CobRunningWork getWork() {
        return work;
    }
}
