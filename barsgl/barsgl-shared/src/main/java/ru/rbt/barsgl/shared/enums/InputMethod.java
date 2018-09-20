package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by ER18837 on 09.03.16.
 */
public enum InputMethod implements HasLabel {
    AE("АЕ"), M("Manual"), F("File"), AE_GL("AE_Glued");

    private final String label;

    InputMethod(String label) {
        this.label = label;

    }

    @Override
    public String getLabel() {
        return label;
    }

};

