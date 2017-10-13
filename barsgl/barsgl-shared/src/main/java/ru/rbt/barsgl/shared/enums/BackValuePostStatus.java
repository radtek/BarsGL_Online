package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

/**
 * Created by er18837 on 26.06.2017.
 */
public enum BackValuePostStatus implements HasLabel {
    // пусто
    NONE("Нет", "состояние не определено %s")
    // на авторизацию
    , CONTROL("На подпись", "передана на подпись %s")
    // до выяснения
    , HOLD("До выяснения", "задержана до выяснения %s")
    // визуально проверено
    , SIGNEDVIEW("Проверено", "визуально проверена %s")
    // дата подтверждена
    , SIGNEDDATE("Подтверждена", "подтверждена дата проводки '%s',\nоперация отправлена на обработку")
    // обработана
    , COMPLETED("Обработана", "обработана %s")
    ;

    private String label;
    private String messageFormat;

    BackValuePostStatus(String label, String messageFormat) {
        this.label = label;
        this.messageFormat = messageFormat;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public String getMessageFormat() {
        return messageFormat;
    }
}

