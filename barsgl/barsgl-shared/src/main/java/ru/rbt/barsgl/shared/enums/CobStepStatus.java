package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by ER18837 on 10.03.17.
 */
public enum CobStepStatus implements HasLabel {
    Step_NotStart("Не начался"),
    Step_Running("Выполняется"),
    Step_Success("Завершен успешно"),
    Step_Error("Завершен с ошибкой");

    private String label;
    CobStepStatus(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
