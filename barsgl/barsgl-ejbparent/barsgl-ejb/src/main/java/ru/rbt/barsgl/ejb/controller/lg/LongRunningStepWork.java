package ru.rbt.barsgl.ejb.controller.lg;

import ru.rbt.barsgl.ejb.entity.lg.LongRunningPatternStepEnum;

/**
 * Created by Ivan Sevastyanov on 19.10.2016.
 */
public class LongRunningStepWork {

    private final LongRunningPatternStepEnum step;
    private final LongRunningWork work;

    public LongRunningStepWork(LongRunningPatternStepEnum step, LongRunningWork work) {
        this.step = step;
        this.work = work;
    }

    public LongRunningPatternStepEnum getStep() {
        return step;
    }

    public LongRunningWork getWork() {
        return work;
    }
}
