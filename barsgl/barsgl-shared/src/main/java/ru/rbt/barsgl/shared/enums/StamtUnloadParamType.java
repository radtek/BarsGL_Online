package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by ER21006 on 19.01.2016.
 */
public enum StamtUnloadParamType implements HasLabel {

    A("Счет"), B("БС2");

    private String label;

    StamtUnloadParamType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }


}
