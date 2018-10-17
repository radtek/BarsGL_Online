package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by er18837 on 15.10.2018.
 */
public enum AccountBatchPackageState implements HasLabel {
    IS_LOAD("Пакет загружен"),
    ON_VALID("Начало валидации"),
    IS_VALID("Валидация завершена успешно"),
    ERROR("Валидация завершена с ошибкой"),
    ON_OPEN("Начало обработки"),
    PROC_ERR("При обрабоке были ошибки"),
    PROCESSED("Обработка завершена усрешно"),
    ;

    private String label;

    AccountBatchPackageState(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
