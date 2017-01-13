package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by ER18837 on 07.04.16.
 * статус допустимости обработки операций
 */
public enum ProcessingStatus implements HasLabel {
    REQUIRED("Запрос остановки", "STARTED"), STOPPED("Обработка остановлена", "REQUIRED"),
    ALLOWED("Обработка разрешена", "STOPPED"), STARTED("Обработка запущена", "ALLOWED");

    private final String name;
    private String dependsOn;

    ProcessingStatus(String name, String dependsOn) {
        this.name = name;
        this.dependsOn = dependsOn;
    }

    @Override
    public String getLabel() {
        return name;
    }

    public ProcessingStatus getDependsOn() {
        return ProcessingStatus.valueOf(dependsOn);
    }
}


