package ru.rbt.barsgl.ejbcore.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import ru.rbt.barsgl.shared.Export.ExcelExportHead;
import ru.rbt.barsgl.shared.column.XlsColumn;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ER18837 on 16.02.16.
 */
public class Sql2Xls {
    private final String query;
    private final ArrayList<Object> params;
    private List<XlsColumn> columns = new ArrayList();
    private ExcelExportHead head = null;
    private List<Row> headRows = new ArrayList();

    public Sql2Xls(String query, ArrayList<Object> params) {
        this.query = query;
        this.params = params;
    }

    private void writeHead(Sheet sheet){
        if (head == null) return;
        int rowNumber = 0;

        Row row;
        Cell cell;
        row = sheet.createRow(rowNumber++);
        cell = row.createCell(0);
        cell.setCellValue("Форма выгрузки: " + head.getFormTitle());

        row = sheet.createRow(rowNumber++);
        cell = row.createCell(0);
        cell.setCellValue("Пользователь: " + head.getUser());

        row = sheet.createRow(rowNumber++);
        cell = row.createCell(0);
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        cell.setCellValue("Дата выгрузки: " + df.format(new Date()));

        row = sheet.createRow(rowNumber++);
        cell = row.createCell(0);
        cell.setCellValue("Условия фильтра: " + head.getFilter());
    }

    public void process(OutputStream out, Connection connection) throws Exception {
        if(this.columns.isEmpty()) {
            throw new Exception("Колонки не заданы");
        } else {
            SXSSFWorkbook wb;
            Sheet curSheet;
            PreparedStatement statement = null;
            ResultSet rs = null;
            Row row;
            int rowNumber = head == null ? 0 : 5;
            wb = new SXSSFWorkbook(100);
            CreationHelper cHelper = wb.getCreationHelper();
            HashMap cellStyles = new HashMap();

            try {
                curSheet = wb.createSheet();

                statement = connection.prepareStatement(this.query);
                bindParameters(statement, this.params);
                rs = statement.executeQuery();
                row = curSheet.createRow(rowNumber);

                Cell cell;
                for(int e = 0; e < this.columns.size(); ++e) {
                    cell = row.createCell(e);
                    cell.setCellValue(columns.get(e).getCaption());
                }
                for(int e = 0; e < this.columns.size(); ++e) {
                    curSheet.autoSizeColumn(e, true);
                }
                writeHead(curSheet);

                while(rs.next()) {
                    ++rowNumber;
                    row = curSheet.createRow(rowNumber);

                    for(int e = 0; e < this.columns.size(); ++e) {
                        XlsColumn column = columns.get(e);
                        String columnName = column.getName();
                        if(rs.getObject(columnName) != null) {
                            cell = row.createCell(e);
                            CellStyle cellStyle;
                            switch(column.getType()) {
                                case STRING:
                                    cell.setCellValue(rs.getString(columnName));
                                    break;
                                case BOOLEAN:
                                    cell.setCellValue(rs.getBoolean(columnName));
                                    break;
                                case INTEGER:
                                    cell.setCellValue((double)rs.getInt(columnName));
                                case LONG:
                                    cell.setCellValue((double)rs.getLong(columnName));
                                    break;
                                case DECIMAL:
                                    cell.setCellValue(rs.getBigDecimal(columnName).doubleValue());
                                    if(!cellStyles.containsKey(Integer.valueOf(e))) {
                                        cellStyle = wb.createCellStyle();
                                        cellStyle.setDataFormat(cHelper.createDataFormat().getFormat(column.getFormat()));
                                        cellStyles.put(Integer.valueOf(e), cellStyle);
                                    }

                                    cell.setCellStyle((CellStyle)cellStyles.get(Integer.valueOf(e)));
                                    break;
                                case DATE:
                                    cell.setCellValue(rs.getDate(columnName));
                                    if(!cellStyles.containsKey(Integer.valueOf(e))) {
                                        cellStyle = wb.createCellStyle();
                                        cellStyle.setDataFormat(cHelper.createDataFormat().getFormat(((XlsColumn)this.columns.get(e)).getFormat()));
                                        cellStyles.put(Integer.valueOf(e), cellStyle);
                                    }

                                    cell.setCellStyle((CellStyle)cellStyles.get(Integer.valueOf(e)));
                                    break;
                                case DATETIME:
                                    cell.setCellValue(rs.getTimestamp(columnName));
                                    if(!cellStyles.containsKey(Integer.valueOf(e))) {
                                        cellStyle = wb.createCellStyle();
                                        cellStyle.setDataFormat(cHelper.createDataFormat().getFormat(((XlsColumn)this.columns.get(e)).getFormat()));
                                        cellStyles.put(Integer.valueOf(e), cellStyle);
                                    }

                                    cell.setCellStyle((CellStyle)cellStyles.get(Integer.valueOf(e)));
                                    break;
                            }
                        }
                    }
                }

                wb.write(out);
            } catch (Exception var24) {
                var24.printStackTrace();
                throw new Exception(var24.getMessage(),var24);
            } catch (Throwable t) {
                t.printStackTrace();
                throw new Exception(t.getMessage(),t);
            } finally {
//                wb.dispose();
                try {
                    if(statement != null) {
                        statement.close();
                    }
                } catch (SQLException var23) {
                    ;
                }

                try {
                    if(rs != null) {
                        rs.close();
                    }
                } catch (SQLException var22) {
                    ;
                }

            }
        }
    }

    public List<XlsColumn> getColumns() {
        return this.columns;
    }

    public void setColumns(List<XlsColumn> columns) {
        this.columns = columns;
    }

    private static void bindParameters(PreparedStatement statement, ArrayList<Object> params) throws SQLException {
        if(params != null) {
            for(int i = 0; i < params.size(); ++i) {
                Object param = params.get(i);
                if(param == null) {
                    statement.setObject(i + 1, (Object)null);
                } else if(param instanceof String) {
                    statement.setString(i + 1, (String)param);
                } else if(param instanceof Date) {
                    statement.setDate(i + 1, new java.sql.Date(((Date)param).getTime()));
                } else if(param instanceof Long) {
                    statement.setLong(i + 1, ((Long)param).longValue());
                } else if(param instanceof BigDecimal) {
                    statement.setBigDecimal(i + 1, (BigDecimal)param);
                } else if(param instanceof Integer) {
                    statement.setInt(i + 1, ((Integer)param).intValue());
                } else {
                    statement.setObject(i + 1, param);
                }
            }
        }
    }

    public void setHead(ExcelExportHead head) {
        this.head = head;
    }

    public static void main(String[] args) {
    }
}
