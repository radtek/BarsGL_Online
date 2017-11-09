package ru.rbt.grid.gwt.server.rpc.asyncGrid;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejbcore.ClientSupportRepository;
import ru.rbt.barsgl.ejbcore.page.SqlPageSupport;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Field;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.server.rpc.AbstractGwtService;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.Export.ExcelExportHead;
import ru.rbt.barsgl.shared.NotAuthorizedUserException;
import ru.rbt.barsgl.shared.Repository;
import ru.rbt.barsgl.shared.SqlQueryTimeoutException;
import ru.rbt.barsgl.shared.column.XlsColumn;
import ru.rbt.barsgl.shared.column.XlsType;
import ru.rbt.barsgl.shared.criteria.OrderByColumn;
import ru.rbt.barsgl.shared.criteria.OrderByType;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.shared.ExceptionUtils;

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static ru.rbt.barsgl.gwt.core.utils.WhereClauseBuilder.filterCriteriaAdapter;

/**
 * Created by akichigi on 02.04.15.
 */
public class AsyncGridServiceImpl extends AbstractGwtService implements AsyncGridService {

    @Override
    public Integer getAsyncCount(Repository repository, String sql, List<FilterItem> filterCriteria) throws Throwable {
        try {
            return localInvoker.invoke(SqlPageSupport.class, "count", sql, repository, filterCriteriaAdapter(filterCriteria));
        } catch (Throwable t) {
            processException(t, sql, "Ошибка при запросе кол-ва записей для списка");
            return null;
        }
    }

    @Override
    public List<Row> getAsyncRows(Repository repository, String sql, Columns columns, int start, int pageSize, List<FilterItem> filterCriteria, List<SortItem> sortCriteria) throws Throwable {
        try {
            List<DataRecord> data = localInvoker.invoke(SqlPageSupport.class, "selectRows", sql, repository, filterCriteriaAdapter(filterCriteria), pageSize,
                    start + 1, sortCriteriaAdapter(sortCriteria));
            List<Row> result = new ArrayList<Row>();

            for(DataRecord r: data) {
                Row row = new Row();
                for( int i = 0; i < columns.getColumnCount(); i++){
                    if (columns.getColumnByIndex(i).getType() == Column.Type.LONG) {
                        row.addField(new Field(r.getLong(columns.getColumnByIndex(i).getName())));
                    } else if (columns.getColumnByIndex(i).getType() == Column.Type.INTEGER) {
                        row.addField(new Field(r.getInteger(columns.getColumnByIndex(i).getName())));
                    } else {
                        row.addField(new Field((Serializable) r.getObject(columns.getColumnByIndex(i).getName())));
                    }
                }

                result.add(row);
            }

            return result;
        } catch (Throwable t) {
            processException(t, sql, "Ошибка при запросе записей для списка");
            return null;
        }
    }

    // TODO это что за дублирование с selectOne !? слить этот г-код
    @Override
    public Row selectOne(Repository repository, String sql, Serializable[] params) throws Throwable {
        try{
            Object [] array = new Object[(params == null) ? 2 : params.length + 2];
            array[0] = sql;
            array[1] = repository;
            if(params != null && params.length  > 0)
                System.arraycopy(params, 0, array, 2, params.length);
            DataRecord record = localInvoker.invoke(ClientSupportRepository.class, "selectOne", array);
            //DataRecord record = localInvoker.invoke(ClientSupportRepository.class, "selectOne", sql, repository, (Object[])params /*new Object[]{11} */);
            Row row = new Row();
            for( int i = 0; i <  record.getColumnCount(); i++){
                row.addField(new Field((Serializable)record.getObject(i)));
            }
            return row;

        }catch (Throwable t){
            processException(t, sql, "Ошибка при запросе строки");
            return null;
        }
    }

    @Override
    public String export2Excel(Repository repository, String sql, Columns columns, List<FilterItem> filterCriteria, List<SortItem> sortCriteria, ExcelExportHead head) throws Throwable {
        return export2Excel(repository, sql, columns, filterCriteria, sortCriteria, head, false);
    }

    @Override
    public String export2Excel(Repository repository, String sql, Columns columns, List<FilterItem> filterCriteria, List<SortItem> sortCriteria, ExcelExportHead head, boolean allrows) throws Throwable {
        try {
            List<XlsColumn> xlsColumns = new ArrayList<XlsColumn>();
            for (int i = 0; i < columns.getColumnCount(); i++) {
                Column column = columns.getColumnByIndex(i);
                if (column.isVisible() && column.getWidth() > 0)
                    xlsColumns.add(new XlsColumn(column.getName(), XlsType.getType(column.getType().toString()), column.getCaption(), column.getFormat()));
            }
            String fileName = localInvoker.invoke(SqlPageSupport.class, "export2Excel", sql, repository, xlsColumns,
                    filterCriteriaAdapter(filterCriteria), 0, 0, sortCriteriaAdapter(sortCriteria), head, allrows);

            return fileName;
        } catch (Throwable t) {
            processException(t, sql, "Ошибка при экспорте в Excel");
            return null;
        }
    }

    @Override
    public Integer getAsyncCount(String sql, List<FilterItem> filterCriteria) throws Throwable {
        return getAsyncCount(Repository.BARSGL, sql, filterCriteria);
    }

    @Override
    public List<Row> getAsyncRows(String sql, Columns columns, int start, int pageSize, List<FilterItem> filterCriteria, List<SortItem> sortCriteria) throws Throwable {
        return getAsyncRows(Repository.BARSGL, sql, columns, start, pageSize, filterCriteria, sortCriteria);
    }

    @Override
    public Row selectFirst(String sql, Serializable[] params) throws Throwable {
        try{
            Object [] array = new Object[(params == null) ? 1 : params.length + 1];
            array[0] = sql;
            if(params != null && params.length  > 0)
                System.arraycopy(params, 0, array, 1, params.length);
            DataRecord record = localInvoker.invoke(ClientSupportRepository.class, "selectFirst", array);
            Row row = new Row();
            if (record != null) {
                for (int i = 0; i < record.getColumnCount(); i++) {
                    row.addField(new Field((Serializable) record.getObject(i)));
                }
            }
            return row;

        } catch (Throwable t) {
            processException(t, sql, "Ошибка при запросе строки");
            return null;
        }
    }

    @Override
    public Row selectOne(String sql, Serializable[] params) throws Throwable {
        try{
            Object [] array = new Object[(params == null) ? 1 : params.length + 1];
            array[0] = sql;
            if(params != null && params.length  > 0)
                System.arraycopy(params, 0, array, 1, params.length);
            DataRecord record = localInvoker.invoke(ClientSupportRepository.class, "selectOne", array /*new Object[]{11} */);
            Row row = new Row();
            for( int i = 0; i <  record.getColumnCount(); i++){
                row.addField(new Field((Serializable)record.getObject(i)));
            }
            return row;

        }catch (Throwable t){
            processException(t, sql, "Ошибка при запросе строки");
            return null;
        }
    }

    private OrderByColumn sortCriteriaAdapter(List<SortItem> sortCriteria){
        if (sortCriteria == null || sortCriteria.isEmpty()) return null;
        SortItem item = sortCriteria.get(0);
        return new OrderByColumn(item.getName(), item.getType() == Column.Sort.ASC ? OrderByType.ASC : OrderByType.DESC);
    }

    @Override
    public String export2Excel(String sql, Columns columns, List<FilterItem> filterCriteria, List<SortItem> sortCriteria, ExcelExportHead head) throws Throwable {
        return export2Excel(Repository.BARSGL, sql, columns, filterCriteria, sortCriteria, head);
    }

    public void Debug(String msg) {
        System.out.println(msg);
    }

    public void processException(Throwable t, String sql, String message) throws Throwable {
        TimeoutException toe = ExceptionUtils.findException(t, TimeoutException.class);
        if (toe != null) {
            localInvoker.invoke(AuditController.class, "warning", AuditRecord.LogCode.User, message + ": " + sql, null, t);
            throw new SqlQueryTimeoutException(toe);
        } else {
            localInvoker.invoke(AuditController.class, "error", AuditRecord.LogCode.User, message + ": " + sql, null, t);

            NotAuthorizedUserException naue = ExceptionUtils.findException(t, NotAuthorizedUserException.class);
            if (naue != null) throw new NotAuthorizedUserException();


            SQLSyntaxErrorException ssee = ExceptionUtils.findException(t, SQLSyntaxErrorException.class);
            if (ssee != null) throw new Exception(ssee);

            SQLException ex = ExceptionUtils.getSqlTimeoutException(t);
            if( null != ex )
                throw new SqlQueryTimeoutException(ex);
            else
                throw new RuntimeException(t);
        }
    }

}
