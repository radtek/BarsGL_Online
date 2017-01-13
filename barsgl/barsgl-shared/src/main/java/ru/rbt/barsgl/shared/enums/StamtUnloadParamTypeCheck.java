package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by ER21006 on 19.01.2016.
 */
public enum StamtUnloadParamTypeCheck implements HasLabel {
//    EXCLUDE("Исключить")
//    ,INCLUDE("Включить")
    EXCLUDE("Exclude")
    ,INCLUDE("Include")
    ;

    private String label;

    StamtUnloadParamTypeCheck(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
