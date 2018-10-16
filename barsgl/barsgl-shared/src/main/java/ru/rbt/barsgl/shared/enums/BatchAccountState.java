package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by er18837 on 16.10.2018.
 */
public enum BatchAccountState implements HasLabel {
    LOAD("Загружен"),
    VALID("Проверен"),
    ERRCHK("Ошибка валидации"),
    ERRPROC("Ошибка при открытии счета"),
    COMPLETED("Обработка завершена"),
    ;

    private String label;

    BatchAccountState(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
