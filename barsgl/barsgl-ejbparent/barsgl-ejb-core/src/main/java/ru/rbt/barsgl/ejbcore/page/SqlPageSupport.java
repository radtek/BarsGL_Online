package ru.rbt.barsgl.ejbcore.page;

import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.shared.column.XlsColumn;
import ru.rbt.barsgl.shared.criteria.Criterion;
import ru.rbt.barsgl.shared.criteria.OrderByColumn;
import ru.rbt.barsgl.shared.enums.Repository;

import java.util.List;

/**
 * Created by Ivan Sevastyanov
 */
public interface SqlPageSupport {

    /**
     * Получаем строки по <code>nativeSql</code> страницей размером <code>pageSize</code> номер страницы <code>pageNumber</code>, используя сортировку <code>orderBy</code>
     * @param nativeSql запрос
     * @param criterion критерии фильтрации
     * @param pageSize размер страницы
     * @param pageNumber начиная с
     * @param orderBy сортировка
     * @return список строк завернутых в DataRecord
     */
    List<DataRecord> select(String nativeSql, Criterion<?> criterion, int pageSize, int pageNumber, OrderByColumn orderBy);

    /**
     * Получаем строки по <code>nativeSql</code> страницей размером <code>pageSize</code> номер страницы <code>pageNumber</code>, используя сортировку <code>orderBy</code>
     * @param nativeSql запрос
     * @param criterion критерии фильтрации
     * @param pageSize размер страницы
     * @param pageNumber начиная с
     * @param orderBy сортировка
     * @return список строк завернутых в DataRecord
     */
    List<DataRecord> select(final String nativeSql, Repository rep, Criterion<?> criterion, int pageSize, int pageNumber, OrderByColumn orderBy);

    /**
     * Получаем строки по <code>nativeSql</code> страницей размером <code>pageSize</code> начиная с <code>startWith</code>, используя сортировку <code>orderBy</code>  
     * @param nativeSql запрос
     * @param criterion критерии фильтрации
     * @param pageSize размер страницы
     * @param startWith начиная с 
     * @param orderBy сортировка
     * @return список строк завернутых в DataRecord
     */
    List<DataRecord> selectRows(String nativeSql, Criterion<?> criterion, int pageSize, int startWith, OrderByColumn orderBy);

    /**
     * Получаем строки по <code>nativeSql</code> страницей размером <code>pageSize</code> начиная с <code>startWith</code>, используя сортировку <code>orderBy</code>
     * @param nativeSql запрос
     * @param criterion критерии фильтрации
     * @param pageSize размер страницы
     * @param startWith начиная с
     * @param orderBy сортировка
     * @return список строк завернутых в DataRecord
     */
    List<DataRecord> selectRows(String nativeSql, Repository repository, Criterion<?> criterion, int pageSize, int startWith, OrderByColumn orderBy);

    /**
     * Кол-во строк с учетом критерий фильтрации
     * @param nativeSql запрос
     * @param criterion критерии
     * @return количество
     */
    int count(String nativeSql, Criterion<?> criterion);

    /**
     * Кол-во строк с учетом критерий фильтрации
     * @param nativeSql запрос
     * @param criterion критерии
     * @return количество
     */
    int count(String nativeSql, Repository repository, Criterion<?> criterion);

    /**
     *
     * @param nativeSql запрос
     * @param xlsColumns колоки
     * @param criterion критерии
     * @param pageSize размер страницы
     * @param startWith начиная с
     * @param orderBy сортировка
     * @return имя файла Excel с выгрузкой
     * @throws Exception
     */
    String export2Excel(String nativeSql, List<XlsColumn> xlsColumns, Criterion<?> criterion, int pageSize, int startWith, OrderByColumn orderBy) throws Exception;

    /**
     *
     * @param nativeSql запрос
     * @param xlsColumns колоки
     * @param criterion критерии
     * @param pageSize размер страницы
     * @param startWith начиная с
     * @param orderBy сортировка
     * @return имя файла Excel с выгрузкой
     * @throws Exception
     */
    String export2Excel(String nativeSql, Repository repository, List<XlsColumn> xlsColumns, Criterion<?> criterion, int pageSize, int startWith, OrderByColumn orderBy) throws Exception;

}
