package ru.rbt.barsgl.ejb.entity.lg;

/**
 * Created by Ivan Sevastyanov on 19.10.2016.
 */
public enum LongRunningPatternEnum {

    PdSyncTask(1);

    private long idPattern;

    LongRunningPatternEnum(long idPattern) {
        this.idPattern = idPattern;
    }
}
