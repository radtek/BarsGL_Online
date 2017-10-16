package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by er18837 on 07.07.2017.
 */
public enum BackValueAction implements HasLabel {
    SIGN("Подтвердить дату"),
    TO_HOLD("Задержать до выяснения"),
    EDIT_DATE("Изменить дату проводки"),
    STAT("Статистика")
    ;

    BackValueAction(String label) {
        this.label = label;
    }

    private String label;

    @Override
    public String getLabel() {
        return label;
    }

}
