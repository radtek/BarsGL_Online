package ru.rbt.ejbcore.repository;

import ru.rbt.ejbcore.datarec.DBParams;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.BaseEntity;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Created by Ivan Sevastyanov
 */
public interface BaseEntityRepository<T extends BaseEntity, K extends Serializable> {

    <E> E selectFirst(Class<E> clazz, String jpaQuery, Object... params);

    <E> E selectOne(Class<E> clazz, String jpaQuery, Object... params);

    /**
     * Возвращает {@link ru.rbt.ejbcore.datarec.DataRecord} или <code>null</code> если ничего не найдено
     * @param sqlString выражение SQL
     * @param params перечисление параметров
     * @return результат в виде {@link ru.rbt.ejbcore.datarec.DataRecord}
     * @throws SQLException ошибка БД пробрасывается как есть
     */
    DataRecord selectFirst(String sqlString, Object... params) throws SQLException;

    DataRecord selectOne(String sqlString, Object... params) throws SQLException;

    <E> List<E> select(Class<E> clazz, String jpaQuery, Object... params);

    List<DataRecord> select(String sqlString, Object ... params) throws SQLException;

    T save(T entity);

    T update(T entity);

    void remove(T entity);

    T refresh(T entity);

    /**
     * Обновляем сущность
     * @param entity сущность
     * @param force если true - принудительно, минуя кэш сессии
     * @return обновленная сущность
     */
    T refresh(T entity, boolean force);

    T findById(Class<T> clazz, K primaryKey);

    int executeUpdate(String jpQuery, Object... params);

    int executeNativeUpdate(String nativeSQL, Object... params);

    Long nextId(String sequenceName);

    /**
     * адаптировано под DB2 по такое:
     * <code>CREATE SEQUENCE SEQ_GL_AUT AS INTEGER START WITH 1000 NOCACHE;</code>
     * @param sequenceName название последовательности
     * @return next integer value
     */
    Integer nextIntegerId(String sequenceName) throws SQLException;

    /**
     * Получаем "замапленный" объект по SQL запросу
     * @param entityClass "замапленный" класс
     * @param sqlQuery SQL запрос
     * @param max максимальное кол-во строк
     * @param params параметры
     * @param <E> приведенный тип
     * @return список "замапленных" объектов
     * @return
     */
    <E> List<E> findNative(Class<E> entityClass, String sqlQuery, int max, Object... params);

    <E> List<E> selectHinted(Class<E> clazz, String jpaQuery
            , Object[] params, Map<String, String> hints);

    List<DataRecord> selectMaxRows(String sqlString, int maxRows, Object[] params) throws SQLException;

    DBParams executeCallable(String sql, DBParams params) throws SQLException;

}