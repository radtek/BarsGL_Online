package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by akichigi on 03.06.16.
 */
public enum InvisibleType implements HasLabel{
    N("Видимое"),
    U("Удален пользователем"),
    S("Удален системой"),
    H("История");

    private final String label;

    InvisibleType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
