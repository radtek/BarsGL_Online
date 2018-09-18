package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by er18837 on 06.08.2018.
 */
public enum Reg47422State implements HasLabel {

    PROC_DAT("Обработана с исключены счета 47422"),
    PROC_ACC("Обработана с заменой счета 47422 на счет 47416"),
    ERRPROC("Неизвестная ошибка обработки проводок, попадающих под условия склейки/замены"),
    SKIP_SRC("Совпали источники, проводки исключены из обработки"),
    WT47416("Не открыт счет 47416, проводки ожидают открытие счета"),
    CHANGE("Найдены изменения состояния проводок перед очередной обработкой"),
    LOAD("Устанавливается по умолчанию при вставке записи в регистр оперативного учета"),
    ;
    
    private String label;

    Reg47422State(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
