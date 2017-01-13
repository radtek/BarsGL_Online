package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by ER18837 on 07.06.16.
 */
public enum BatchPostStep implements HasLabel{
    NOHAND("Пусто"), HAND1("Ввод"), HAND2("Авторизация"), HAND3("Подтверждение даты"), HAND4("Исполнено");

    private String label;

    BatchPostStep(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isNoneStep() {
        return this == NOHAND;
    }

    public boolean isInputStep() {
        return this == HAND1;
    }

    public boolean isControlStep() {
        return this == HAND2;
    }

    public boolean isConfirmStep() {
        return this == HAND3;
    }

    public boolean isCompleted() {
        return this == HAND4;
    }

}
