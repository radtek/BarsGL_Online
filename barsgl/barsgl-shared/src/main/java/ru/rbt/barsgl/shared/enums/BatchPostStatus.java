package ru.rbt.barsgl.shared.enums;

import ru.rbt.barsgl.shared.HasLabel;

import static ru.rbt.barsgl.shared.enums.BatchPostStep.*;

/**
 * Created by ER18837 on 01.06.16.
 */
public enum BatchPostStatus implements HasLabel {
    // пусто
    NONE(HAND1, "Нет")
    // ввод
    ,INPUT(HAND1, "Ввод")
    // отказ в авторизации
    , REFUSE(HAND1, "Отказ")
    // на авторизацию
    , CONTROL(HAND2, "На подпись")
    // визуально проверено
    , SIGNEDVIEW(HAND2, "Проверено")
    // авторизовано
    , SIGNED(HAND2, "Подписано")
    // отказ в подтверждении даты
    , REFUSEDATE(HAND2, "Отказ даты")
    // ошибка обработки после авторизации
    , ERRPROC(HAND2, "Ошибка обработки")
    // ошибка сервиса движений после авторизации
    , ERRSRV(HAND2, "Ошибка сервиса")
    // отказ сервиса движений (нет средств) после авторизации
    , REFUSESRV(HAND2, "Отказ сервиса")
    // на подтверждение даты
    , WAITDATE(HAND3, "Подтвердить дату")
    // дата подтверждена
    , SIGNEDDATE(HAND3, "Дата подтверждена")
    // ошибка обработки после потверждения даты
    , ERRPROCDATE(HAND3, "Ошибка обработки даты")
    // обработана
    , COMPLETED(HAND4, "Обработано")
    // обработка начата, но не закончена
    , UNKNOWN(HAND4, "Неизвестен");

    private BatchPostStep step;
    private String label;

    BatchPostStatus(BatchPostStep step, String label) {
        this.step = step;
        this.label = label;
    }

    public BatchPostStep getStep() {
        return step;
    }

    @Override
    public String getLabel() {
        return label;
    }

}
