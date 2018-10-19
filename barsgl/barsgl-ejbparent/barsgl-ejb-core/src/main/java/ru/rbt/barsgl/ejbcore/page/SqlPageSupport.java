package ru.rbt.barsgl.ejbcore.page;

import ru.rbt.barsgl.shared.Export.ExcelExportHead;
import ru.rbt.barsgl.shared.Repository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.column.XlsColumn;
import ru.rbt.barsgl.shared.criteria.Criterion;
import ru.rbt.barsgl.shared.criteria.OrderByColumn;
import ru.rbt.ejbcore.datarec.DataRecord;

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
    List<DataRecord> selectRows(String nativeSql, Criterion<?> criterion, int pageSize, int startWith, List<OrderByColumn> orderBy);

    /**
     * Получаем строки по <code>nativeSql</code> страницей размером <code>pageSize</code> начиная с <code>startWith</code>, используя сортировку <code>orderBy</code>
     * @param nativeSql запрос
     * @param criterion критерии фильтрации
     * @param pageSize размер страницы
     * @param startWith начиная с
     * @param orderBy сортировка
     * @return список строк завернутых в DataRecord
     */
    List<DataRecord> selectRows(String nativeSql, Repository repository, Criterion<?> criterion, int pageSize, int startWith, List<OrderByColumn> orderBy);

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
     * @param head заголовок
     * @return имя файла Excel с выгрузкой
     * @throws Exception
     */
    String export2Excel(String nativeSql, List<XlsColumn> xlsColumns, Criterion<?> criterion, int pageSize, int startWith, OrderByColumn orderBy, ExcelExportHead head) throws Exception;

    /**
     *
     * @param nativeSql запрос
     * @param xlsColumns колоки
     * @param criterion критерии
     * @param pageSize размер страницы
     * @param startWith начиная с
     * @param orderBy сортировка
     * @param head заголовок
     * @return имя файла Excel с выгрузкой
     * @throws Exception
     */
    String export2Excel(String nativeSql, Repository repository, List<XlsColumn> xlsColumns, Criterion<?> criterion, int pageSize, int startWith, OrderByColumn orderBy, ExcelExportHead head) throws Exception;

    String export2Excel(String nativeSql, Repository repository, List<XlsColumn> xlsColumns, Criterion<?> criterion, int pageSize, int startWith, OrderByColumn orderBy, ExcelExportHead head, boolean allrows) throws Exception;

    String export2ExcelSort(String nativeSql, Repository repository, List<XlsColumn> xlsColumns, Criterion<?> criterion, int pageSize, int startWith, List<OrderByColumn> orderBy, ExcelExportHead head, boolean allrows) throws Exception;

    RpcRes_Base<Boolean> export2ExcelExists(String nativeSql, Repository repository, Criterion<?> criterion) throws Exception;
}
