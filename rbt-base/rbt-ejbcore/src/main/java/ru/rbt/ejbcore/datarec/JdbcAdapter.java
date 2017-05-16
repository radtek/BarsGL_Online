package ru.rbt.ejbcore.datarec;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Интерфейс, предоставляющий средства для работы с данными на уровне JDBC, заключающие в себе
 * особенности управления данными конкретной СУБД.<br/>
 * Например, в одной субд {@link boolean}-поля могут храниться как числовые значения (0 или 1),
 * а в другой СУБД &mdash; как строковые ('Y' или 'N'). Подобные особенности могут быть и у полей
 * других типов.
 */
public interface JdbcAdapter {

    /**
     * Устанавливает заданное значение у заданного параметра в SQL-запросе
     * @param ps Объект SQL-запроса
     * @param index Номер параметра, значение которого слезует установить (1, 2, ...)
     * @param value Значенеи параметра
     * @throws java.sql.SQLException В случае ошибки установления параметра
     */
    public void setParameterValue(PreparedStatement ps, int index, Object value) throws SQLException;

    /**
     * Считывает значение заданной колонки JDBC-выборки
     * @param rs Объект выборки
     * @param index Индекс колонки (1, 2, ...)
     * @return Значение, содержащееся в указанной колонке
     * @throws java.sql.SQLException В случае ошибки считывания данных
     */
    public Object getResultValue(ResultSet rs, int index) throws SQLException;

    /**
     * Считывает значение заданного OUT-параметра после вызова хранимой процедуры
     * @param cs Объект вызова хранимой процедуры
     * @param index Номер параметра (начиная с 1)
     * @param type Тип параметра согласно {@link java.sql.Types}
     * @return Выходное значение параметра
     * @throws java.sql.SQLException В случае ошибки считывания данных
     */
    public Object getResultValue(CallableStatement cs, int index, int type) throws SQLException;

}
