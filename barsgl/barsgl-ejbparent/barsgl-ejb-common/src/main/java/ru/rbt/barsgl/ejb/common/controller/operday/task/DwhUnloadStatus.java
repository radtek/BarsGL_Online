package ru.rbt.barsgl.ejb.common.controller.operday.task;

/**
 * Created by Ivan Sevastyanov
 */
public enum DwhUnloadStatus {
    /**
     * выгрузка стартовала
     */
    STARTED("0"),
    /**
     * выгрузка прошла успешно
     */
    SUCCEDED("1"),
    /**
     * выгрузка завершилась с ошибкой
     */
    ERROR("2"),
    /**
     * выгруженные данные обработаны
     */
    CONSUMED("4"),
    /**
     * выгрузка пропущена
     */
    SKIPPED("5");

    private String flag;

    DwhUnloadStatus(String flag) {
        this.flag = flag;
    }

    public String getFlag() {
        return flag;
    }
}
