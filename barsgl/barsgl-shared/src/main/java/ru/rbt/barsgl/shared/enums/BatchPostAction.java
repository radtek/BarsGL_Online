package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by ER18837 on 09.06.16.
 */
public enum BatchPostAction implements HasLabel {
    // сохранить - шаг 1 (INPUT)
    SAVE("Сохранить")
    // сохранить и на подпись - шаг 1 (CONTROL)
    , SAVE_CONTROL("Сохранить, на подпись")
    // на подпись - шаг 1 (CONTROL)
    , CONTROL("На подпись")
    // удалить - шаг 1 (INVISIBLE)
    , DELETE("Удалить")
    // изменить - шаг 1 (INPUT), шаг 2 (CONTROL)
    , UPDATE("Изменить")
    // изменить и на подпись - шаг 1 (CONTROL)
    , UPDATE_CONTROL("Изменить, на подпись")
    // изменить и подписать - шаг 2 (SIGNED, WAITDATE)
    , UPDATE_SIGN("Изменить, подписать")
    // подписать - шаг 2 (SIGNED, WAITDATE)
    , SIGN("Подписать")
    // подтвердить архивную дату - шаг 3 (SIGNEDDATE)
    , CONFIRM("Подтвердить архивную дату")
    // подтвердить текущей датой - шаг 3 (SIGNEDDATE)
    , CONFIRM_NOW("Подтвердить текущую дату")
    // отказать - шаг 2 (REFUSE), 3 (REFUSEDATE)
    , REFUSE("Отказать")
    ;

    private String label;

    BatchPostAction(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
