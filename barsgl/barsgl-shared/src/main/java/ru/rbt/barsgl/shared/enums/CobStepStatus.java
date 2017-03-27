package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by ER18837 on 10.03.17.
 */
public enum CobStepStatus implements HasLabel {
    NotStart("Не начался"),
    Running("Выполняется"),
    Success("Завершен успешно"),
    Error("Завершен с ошибкой"),
    Halt("Завершен с критической ошибкой"),
    Skipped("Пропущен");

    private String label;
    CobStepStatus(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
