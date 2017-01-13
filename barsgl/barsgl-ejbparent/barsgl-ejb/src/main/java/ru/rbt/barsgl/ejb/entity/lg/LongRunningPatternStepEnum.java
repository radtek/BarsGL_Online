package ru.rbt.barsgl.ejb.entity.lg;

/**
 * Created by Ivan Sevastyanov on 19.10.2016.
 */
public enum LongRunningPatternStepEnum {
    SYNC_CHECK_RUN(1, LongRunningPatternEnum.PdSyncTask)
    , SYNC_WAIT_STOPPING(2, LongRunningPatternEnum.PdSyncTask)
    , SYNC_GLPD(3, LongRunningPatternEnum.PdSyncTask)
    , SYNC_MOVE_GLPDARCH(4, LongRunningPatternEnum.PdSyncTask)
    , SYNC_SWITCH_PDMODE(5, LongRunningPatternEnum.PdSyncTask)
    , SYNC_RECALC(6, LongRunningPatternEnum.PdSyncTask)
    , SYNC_ALLOW_PROCESSING(7, LongRunningPatternEnum.PdSyncTask);

    private long idStep;
    private LongRunningPatternEnum pdSyncTask;

    LongRunningPatternStepEnum(long idStep, LongRunningPatternEnum pdSyncTask) {
        this.idStep = idStep;
        this.pdSyncTask = pdSyncTask;
    }

    public long getIdStep() {
        return idStep;
    }

    public LongRunningPatternEnum getPdSyncTask() {
        return pdSyncTask;
    }

    @Override
    public String toString() {
        return "LongRunningPatternStepEnum{" +
                "idStep=" + idStep +
                ", pdSyncTask=" + pdSyncTask +
                '}';
    }
}
