package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by er18837 on 06.08.2018.
 */
public enum Exclude4496Status implements HasLabel {
    
    PROC("Успешно обработаны"),
    ERRPROC("Неизвестная ошибка обработки проводок, попадающих под условия склейки/замены"),
    ERRSRC("Совпали источники, проводки исключены из обработки"),
    WT47416("Не открыт счет 47416, проводки ожидают открытие счета"),
    WTSKIP("Не удовлетворяют условиям, ожидают обработки при следующем запуске задачи"),
    CHANG("Найдены изменения состояния проводок перед очередной обработкой"),
    NEW("Устанавливается по умолчанию при вставке записи в регистр оперативного учета"),
    ;
    
    private String label;

    Exclude4496Status(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
