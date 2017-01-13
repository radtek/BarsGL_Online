package ru.rbt.barsgl.shared.enums;

/**
 * Created by ER18837 on 17.06.16.
 */
public enum MovementErrorTypes {
    // – бизнес-ошибка, получен ответ со статусом ‘ERROR’
    ERR_BUSINESS(2, "Ошибка счета (получен отрицательный ответ от сервиса движений)"),
    // – системная ошибка на стороне сервиса SYSERROR
    ERR_SERVICE(3, "Ошибка сервиса движений. Обратетесь к администратору"),
    // – исчерпан лимит ожидания
    ERR_TIMEOUT(4, "Нет ответа от сервиса движений. Обратетесь к администратору"),
    // - exception в процессе работы на стороне клиента
    ERR_REQUEST(5, "Ошибка запроса к сервису движений. Обратетесь к администратору");

    private int code;
    private String message;

    MovementErrorTypes(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

