package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by er18837 on 06.08.2018.
 */
public enum ProcessingType implements HasLabel {

    ALL("Все"), PROCESSED("Обработанные"), UNPROCESSED ("Необработанные");
    private String label;

    ProcessingType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

}
