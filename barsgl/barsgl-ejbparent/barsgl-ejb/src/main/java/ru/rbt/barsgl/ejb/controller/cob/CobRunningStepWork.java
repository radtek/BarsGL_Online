package ru.rbt.barsgl.ejb.controller.cob;

import ru.rbt.barsgl.shared.enums.CobPhase;

/**
 * Created by ER18837 on 15.03.17.
 */
public class CobRunningStepWork {

    private final CobPhase phase;
    private final CobRunningWork work;

    public CobRunningStepWork(CobPhase phase, CobRunningWork work) {
        this.phase = phase;
        this.work = work;
    }

    public CobPhase getPhase() {
        return phase;
    }

    public CobRunningWork getWork() {
        return work;
    }
}
