package ru.rbt.barsgl.ejb.controller.operday.task;

/**
 * Created by Ivan Sevastyanov
 */
public enum DwhUnloadStatus {
    STARTED("0"), SUCCEDED("1"), ERROR("2"), SKIPPED("3");

    private String flag;

    DwhUnloadStatus(String flag) {
        this.flag = flag;
    }

    public String getFlag() {
        return flag;
    }
}
