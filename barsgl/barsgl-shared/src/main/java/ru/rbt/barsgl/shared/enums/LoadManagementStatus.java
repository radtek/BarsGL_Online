package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by SotnikovAV on 02.11.2016.
 */
public enum LoadManagementStatus implements HasLabel {

    None(""),
    Assigned("Назначено"),
    Approved("Согласовано"),
    Executed("На выполнение"),
    Execution("Выполняется"),
    Error("Ошибка"),
    Done("Завершено");


    private String label;

    LoadManagementStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
