package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by ER18837 on 09.12.15.
 */
public enum JobSchedulingType implements HasLabel {

    CALENDAR("Расписание"), INTERVAL("Интервал"), SINGLE("Однократно");

    private final String label;

    private JobSchedulingType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}

