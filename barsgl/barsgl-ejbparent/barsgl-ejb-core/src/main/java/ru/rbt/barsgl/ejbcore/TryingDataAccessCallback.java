package ru.rbt.barsgl.ejbcore;

import java.io.Serializable;
import java.sql.Connection;

/**
 * Created by Ivan Sevastyanov on 01.12.2016.
 */
@FunctionalInterface
public interface TryingDataAccessCallback<T> extends Serializable {
    /**
     * выполняем работу транзакционного используя текущий счетчик попыток
     * @param connection соединение
     * @param currentAttempt номер попытки
     * @return результат
     * @throws Exception
     */
    T call(Connection connection, int currentAttempt) throws Exception;
}
