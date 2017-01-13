package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by ER18837 on 22.09.15.
 */
public enum UserRole implements HasLabel {

    ADMIN("Администратор"),
    ACCOUNT("Открытие счета"),
    OPERATION("Создание проводки"),
    ACC_OPER("Открытие счета и создание проводки"),
    GUEST("Просмотр");

    private String description;

    UserRole(String descr) {
        this.description = descr;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String getLabel() {
        return description;
    }
}
