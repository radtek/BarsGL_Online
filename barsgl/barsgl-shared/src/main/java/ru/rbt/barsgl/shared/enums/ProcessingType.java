package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by er18837 on 06.08.2018.
 */
public enum ProcessingType implements HasLabel {

    ALL("Все", ""), PROCESSED("Обработанные", "Y"), UNPROCESSED ("Необработанные", "N");
    private String label;
    private String value;

    ProcessingType(String label, String value) {
        this.label = label;
        this.value = value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }
}
