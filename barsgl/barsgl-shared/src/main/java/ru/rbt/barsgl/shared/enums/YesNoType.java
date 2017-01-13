package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by akichigi on 30.03.16.
 */
public enum YesNoType implements HasLabel {
    Yes("Да"), No("Нет");

    private String label;

    YesNoType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
