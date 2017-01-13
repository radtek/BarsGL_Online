package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by SotnikovAV on 02.11.2016.
 */
public enum LoadManagementAction implements HasLabel {
    None(""),
    Restart("Перезапустить"),
    SetError("Установить ошибку"),
    SetOK("Пропустить");

    private String label;

    LoadManagementAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
