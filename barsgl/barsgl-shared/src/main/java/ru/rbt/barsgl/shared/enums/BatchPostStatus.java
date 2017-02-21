package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

import static ru.rbt.barsgl.shared.enums.BatchPostStep.*;

/**
 * Created by ER18837 on 01.06.16.
 */
public enum BatchPostStatus implements HasLabel {
    // пусто
    NONE(HAND1, "Нет", "состояние не определено %s")
    // ввод
    ,INPUT(HAND1, "Ввод", "загружен успешно %s")
    // отказ в авторизации
    , REFUSE(HAND1, "Отказ", "возвращен на доработку %s")
    // на авторизацию
    , CONTROL(HAND2, "На подпись", "передан на подпись %s")
    // визуально проверено
    , SIGNEDVIEW(HAND2, "Проверено", "визуально проверен %s")
    // запрос в сервис
    , WAITSRV(HAND2, "Запрос в сервис", "ожидает ответ от АБС %s")
    // положительный ответ от сервиса
    , OKSRV(HAND2, "Подтверждение от АБС", "получено подтверждение от АБС %s")
    // отказ сервиса движений (нет средств) после авторизации
    , REFUSESRV(HAND2, "Отказ от АБС", "получен отказ от АБС %s")
    // ошибка сервиса движений после авторизации
    , ERRSRV(HAND2, "Ошибка сервиса", "произошла ошибка при обращении к сервису движений %s")
    // нет ответа от сервиса движений после авторизации
    , TIMEOUTSRV(HAND2, "Нет ответа от сервиса", "нет ответа от сервиса движений %s")
    // нет ответа от сервиса движений после авторизации
    , ERROPERSRV(HAND2, "Ответ от сервиса после таймаута", "ответ от сервиса движений пришел после установки статуса 'TIMEOUTSRV' %s")
    // авторизовано
    , SIGNED(HAND2, "Подписано", "подписан %s и отправлен на обработку")
    // отказ в подтверждении даты
    , REFUSEDATE(HAND2, "Отказ даты", "возвращен без подтверждения даты проводки %s")
    // ошибка обработки после авторизации
    , ERRPROC(HAND2, "Ошибка обработки", "системная ошибка при создании проводок %s")
    // на подтверждение даты
    , WAITDATE(HAND3, "Подтвердить дату", "передан на подтверждение даты проводки %s")
    // дата подтверждена
    , SIGNEDDATE(HAND3, "Дата подтверждена", "подписан с подтверждением даты проводки %s,\nзапрос отправлен на обработку")
    // псевдостатус, только для вывода сообщения
    , CONFIRM(HAND3, "Дата подтверждена", "подтверждена дата проводки %s,\nзапрос отправлен на обработку")
    // ошибка обработки после потверждения даты
    , ERRPROCDATE(HAND3, "Ошибка обработки даты", "системная ошибка при создании проводок %s")
    // обработка начата, но не закончена
    , WORKING(HAND4, "Обработка", "обрабатывается %s")
    // обработана
    , COMPLETED(HAND4, "Обработан", "обработан %s")
    ;

    private BatchPostStep step;
    private String label;
    private String messageFormat;

    BatchPostStatus(BatchPostStep step, String label, String messageFormat) {
        this.step = step;
        this.label = label;
        this.messageFormat = messageFormat;
    }

    public BatchPostStep getStep() {
        return step;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public String getMessageFormat() {
        return messageFormat;
    }
}
