package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by akichigi on 03.08.16.
 */
public enum  BoolType implements HasLabel {
    Y("Да"), N("Нет");

    private String label;

    BoolType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
