package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by ER18837 on 19.04.16.
 */
public enum OperType implements HasLabel {
    S("Simple", 1),
    M("Mfo", 2),
    E("Exch", 3),
    ME("MfoExch", 4),
    F("Fan", 5),
    ST("Storno", 6);

    private final String name;
    private int owerCode;

    private OperType(String name, int owerCode) {
        this.name = name;
        this.owerCode = owerCode;
    }

    public String getName() {
        return name;
    }

    public int getOwerCode() {
        return owerCode;
    }

    @Override
    public String getLabel() {
        return name;
    }
}

