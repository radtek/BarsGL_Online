package ru.rbt.barsgl.ejb.controller.operday.task.md;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by Ivan Sevastyanov on 29.11.2018.
 */
public enum DismodOutState implements HasLabel {

    L("Загружается"), S("Сформирован"), E ("Ошибка");

    private final String label;

    DismodOutState(String name) {
        this.label = name;
    }

    @Override
    public String getLabel() {
        return label;
    }
}