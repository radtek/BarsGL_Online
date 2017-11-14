package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * режим доступа
 */
public enum AccessMode implements HasLabel {
    FULL("Полный")
    , LIMIT("Ограниченный");

    private final String name;

    AccessMode(String name) {
        this.name = name;
    }

    @Override
    public String getLabel() {
        return name;
    }

    public static AccessMode switchMode(AccessMode from) {
        return from == FULL ? LIMIT : FULL;
    }
}