package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

import static ru.rbt.barsgl.shared.enums.BatchPostStep.*;

/**
 * Created by ER18837 on 16.01.17.
 */
public enum BatchPackageState implements HasLabel {
      UNDEFINED(NOHAND, "Не определено", "состояние не определено %s")
    , INPROGRESS(HAND1, "Загрузка", "загружается %s")
    , LOADED(HAND1, "Загружен", "загружен успешно %s")
    , ERROR(HAND1, "Ошибка", "загружен с ошибкой %s")
    , DELETED(HAND1, "Удален", "удален %s")
    , ON_CONTROL(HAND2, "На подпись", "передан на подпись %s")
    , IS_SIGNEDVIEW(HAND2, "Проверен", "визуально проверен %s")
    , ON_WAITSRV(HAND2, "Ожидание ответы от АБС", "ожидает ответы от АБС %s")
    , IS_SIGNED(HAND2, "Подписан", "подписан %s и отправлен на обработку")
    , IS_REFUSEDATE(HAND2, "Возвращен", "возвращен без подтверждения даты проводки %s")
    , IS_ERRSRV(HAND2, "Ошибка сервиса", "ошибка обращения к сервису движений. Обработка прервана %s")
    , ON_WAITDATE(HAND3, "Подтвердить дату", "передан на подтверждение даты проводки %s")
    , IS_CLICKDATE(HAND3, "Дата подтверждена предварительно", "подписан с подтверждением даты проводки %s \nпакет отправлен на обработку")
    , IS_SIGNEDDATE(HAND3, "Дата подтверждена", "подписан с подтверждением даты проводки %s \nпакет отправлен на обработку")
    , IS_CONFIRM(HAND3, "Дата подтверждена", "подтверждена дата проводки %s \nпакет отправлен на обработку")
    , IS_WORKING(HAND4, "Обрабатывается", "обрабатывается %s")
    , PROCESSED(HAND4, "Обработан", "обработан %s")
    , WAITPROC(HAND4, "Обработка прервана", "обработка прервана %s")
    ;

    private BatchPostStep step;
    private String label;
    private String messageFormat;

    BatchPackageState(BatchPostStep step, String label, String messageFormat) {
        this.step = step;
        this.label = label;
        this.messageFormat = messageFormat;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public BatchPostStep getStep() {
        return step;
    }

    public String getMessageFormat() {
        return messageFormat;
    }

    public static BatchPackageState getStateByPostingStatus(BatchPostStatus postStatus) {
        switch (postStatus) {
            case INPUT:         return LOADED;
            case CONTROL:       return ON_CONTROL;
            case SIGNEDVIEW:    return IS_SIGNEDVIEW;
            case SIGNED:        return IS_SIGNED;
            case REFUSEDATE:    return IS_REFUSEDATE;
            case WAITSRV:       return ON_WAITSRV;
            case OKSRV:         return ON_WAITSRV;
            case WAITDATE:      return ON_WAITDATE;
            case CLICKDATE:     return IS_CLICKDATE;
            case SIGNEDDATE:    return IS_SIGNEDDATE;
            case CONFIRM:       return IS_CONFIRM;
            case WORKING:       return IS_WORKING;
            case COMPLETED:     return PROCESSED;
            default:            return UNDEFINED;
        }
    }

}
