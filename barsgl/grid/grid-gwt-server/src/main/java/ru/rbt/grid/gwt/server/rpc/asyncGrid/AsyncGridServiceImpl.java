package ru.rbt.grid.gwt.server.rpc.asyncGrid;

import ru.rbt.barsgl.ejbcore.ClientSupportRepository;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.page.SqlPageSupport;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Field;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.gwt.core.server.rpc.AbstractGwtService;
import ru.rbt.barsgl.shared.Export.ExcelExportHead;
import ru.rbt.barsgl.shared.column.XlsColumn;
import ru.rbt.barsgl.shared.column.XlsType;
import ru.rbt.barsgl.shared.criteria.*;
import ru.rbt.shared.enums.Repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static ru.rbt.barsgl.gwt.core.utils.WhereClauseBuilder.filterCriteriaAdapter;

/**
 * Created by akichigi on 02.04.15.
 */
public class AsyncGridServiceImpl extends AbstractGwtService implements AsyncGridService {

    @Override
    public Integer getAsyncCount(Repository repository, String sql, List<FilterItem> filterCriteria) throws Exception {
//    	return 1000;
        return localInvoker.invoke(SqlPageSupport.class, "count", sql, repository, filterCriteriaAdapter(filterCriteria));
    }

    @Override
    public List<Row> getAsyncRows(Repository repository, String sql, Columns columns, int start, int pageSize, List<FilterItem> filterCriteria, List<SortItem> sortCriteria) throws Exception {
        List<DataRecord> data = localInvoker.invoke(SqlPageSupport.class, "selectRows", sql, repository, filterCriteriaAdapter(filterCriteria), pageSize,
                start + 1, sortCriteriaAdapter(sortCriteria));
        List<Row> result = new ArrayList<Row>();

        for(DataRecord r: data) {
            Row row = new Row();
            for( int i = 0; i < columns.getColumnCount(); i++){
                row.addField(new Field((Serializable) r.getObject(columns.getColumnByIndex(i).getName())));
            }

            result.add(row);
        }

        return result;
    }

    @Override
    public Row selectFirst(String sql, Serializable[] params) throws Exception {
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

        }catch (Exception ex){
            throw new RuntimeException(ex);
        }

    }


    @Override
    public Row selectOne(Repository repository, String sql, Serializable[] params) throws Exception {
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

        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String export2Excel(Repository repository, String sql, Columns columns, List<FilterItem> filterCriteria, List<SortItem> sortCriteria, ExcelExportHead head) throws Exception {
        List<XlsColumn> xlsColumns = new ArrayList<XlsColumn>();
        for (int i = 0; i < columns.getColumnCount(); i++) {
            Column column = columns.getColumnByIndex(i);
            if (column.isVisible() && column.getWidth() > 0)
                xlsColumns.add(new XlsColumn(column.getName(), XlsType.getType(column.getType().toString()), column.getCaption(), column.getFormat()));
        }

        String fileName = localInvoker.invoke(SqlPageSupport.class, "export2Excel", sql, repository, xlsColumns,
                filterCriteriaAdapter(filterCriteria), 0, 0, sortCriteriaAdapter(sortCriteria), head);

        return fileName;
    }

    @Override
    public Integer getAsyncCount(String sql, List<FilterItem> filterCriteria) throws Exception {
        return localInvoker.invoke(SqlPageSupport.class, "count", sql, filterCriteriaAdapter(filterCriteria));
    }

    @Override
    public List<Row> getAsyncRows(String sql, Columns columns, int start, int pageSize, List<FilterItem> filterCriteria, List<SortItem> sortCriteria) throws Exception {
        List<DataRecord> data = localInvoker.invoke(SqlPageSupport.class, "selectRows", sql, filterCriteriaAdapter(filterCriteria), pageSize,
                                                    start + 1, sortCriteriaAdapter(sortCriteria));
        List<Row> result = new ArrayList<Row>();

        for(DataRecord r: data) {
            Row row = new Row();
            for( int i = 0; i < columns.getColumnCount(); i++){
                row.addField(new Field((Serializable) r.getObject(columns.getColumnByIndex(i).getName())));
            }

            result.add(row);
        }

        return result;
    }

    @Override
    public Row selectOne(String sql, Serializable[] params) throws Exception {
        try{
            Object [] array = new Object[(params == null) ? 1 : params.length + 1];
            array[0] = sql;
            if(params != null && params.length  > 0)
                System.arraycopy(params, 0, array, 1, params.length);
            DataRecord record = localInvoker.invoke(ClientSupportRepository.class, "selectOne", array);
            Row row = new Row();
            for( int i = 0; i <  record.getColumnCount(); i++){
                row.addField(new Field((Serializable)record.getObject(i)));
            }
            return row;

        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

/*
    public static Criterion filterCriteriaAdapter(List<FilterItem> filterCriteria){
        if (filterCriteria == null || filterCriteria.isEmpty()) return null;

        List<Criterion> list = new ArrayList<Criterion>();

        for (FilterItem item: filterCriteria){
           Operator operator;
           Serializable value = item.getSqlValue();
            switch (item.getCriteria()){
                case GE:
                    operator = Operator.GE;
                    break;
                case GT:
                    operator = Operator.GT;
                    break;
                case LT:
                    operator = Operator.LT;
                    break;
                case LE:
                    operator = Operator.LE;
                    break;
                case NE:
                    operator = Operator.NE;
                    break;
                case HAVE:
                    operator = Operator.LIKE;
                    value = "%" + value + "%";
                    break;
                case START_WITH:
                    operator = Operator.LIKE;
                    value = value + "%";
                    break;
                case LIKE:
                    operator = Operator.LIKE;
                    break;
                case IS_NULL:
                    operator = Operator.IS_NULL;
                    break;
                case NOT_NULL:
                    operator = Operator.NOT_NULL;
                    break;
                case IS_EMPTY:
                    operator = Operator.EQ;
                    value = "";
                    break;
                case NOT_EMPTY:
                    operator = Operator.NE;
                    value = "";
                    break;
                default:
                    operator = Operator.EQ;
            }
            list.add(CriterionColumn.createCriterion(item.getSqlName(), operator, value));
        }
        return new Criteria(CriteriaLogic.AND, list);
    }
*/

    private OrderByColumn sortCriteriaAdapter(List<SortItem> sortCriteria){
        if (sortCriteria == null || sortCriteria.isEmpty()) return null;
        SortItem item = sortCriteria.get(0);
        return new OrderByColumn(item.getName(), item.getType() == Column.Sort.ASC ? OrderByType.ASC : OrderByType.DESC);
    }

    @Override
    public String export2Excel(String sql, Columns columns, List<FilterItem> filterCriteria, List<SortItem> sortCriteria, ExcelExportHead head) throws Exception {
        List<XlsColumn> xlsColumns = new ArrayList<XlsColumn>();
        for (int i = 0; i < columns.getColumnCount(); i++) {
            Column column = columns.getColumnByIndex(i);
            if (column.isVisible() && column.getWidth() > 0)
                xlsColumns.add(new XlsColumn(column.getName(), XlsType.getType(column.getType().toString()), column.getCaption(), column.getFormat()));
        }

        String fileName = localInvoker.invoke(SqlPageSupport.class, "export2Excel", sql, xlsColumns,
                filterCriteriaAdapter(filterCriteria), 0, 0, sortCriteriaAdapter(sortCriteria), head);

      return fileName;
    }

    public void Debug(String msg) {
        System.out.println(msg);
    }

}
