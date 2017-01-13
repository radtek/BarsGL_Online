package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by akichigi on 11.03.15.
 */
public enum JobStartupType implements HasLabel {

    AUTO("Автоматический запуск"), MANUAL("Ручной запуск"), DISABLED("Отключён");

    private final String label;

    private JobStartupType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
