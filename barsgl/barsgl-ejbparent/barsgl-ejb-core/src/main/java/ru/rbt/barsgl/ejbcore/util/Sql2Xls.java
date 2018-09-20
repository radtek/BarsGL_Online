package ru.rbt.barsgl.ejbcore.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import ru.rbt.barsgl.shared.Export.ExcelExportHead;
import ru.rbt.barsgl.shared.column.XlsColumn;
import ru.rbt.ejbcore.datarec.DataRecord;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by ER18837 on 16.02.16.
 */
public class Sql2Xls {

    public static final Logger logger = Logger.getLogger(Sql2Xls.class.getName());

    private final int koeffWidth = 35;
    private List<XlsColumn> columns = new ArrayList();
    private ExcelExportHead head = null;
    private List<DataRecord> dataRecords;

    public Sql2Xls(List<DataRecord> dataRecords) {
        this.dataRecords = dataRecords;
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

    public void process(OutputStream out) throws Exception {
        if(this.columns.isEmpty()) {
            throw new Exception("Колонки не заданы");
        } else {
            SXSSFWorkbook wb;
            SXSSFSheet curSheet;
            Row row;
            int rowNumber = head == null ? 0 : 5;
            wb = new SXSSFWorkbook(100);
            CreationHelper cHelper = wb.getCreationHelper();
            HashMap cellStyles = new HashMap();

            try {
                curSheet = wb.createSheet();
                row = curSheet.createRow(rowNumber);

                Cell cell;
                for(int e = 0; e < this.columns.size(); ++e) {
                    cell = row.createCell(e);
                    cell.setCellValue(columns.get(e).getCaption());
                }
                for(int e = 0; e < this.columns.size(); ++e) {
                    try {
                        if (this.columns.get(e).getWidth() > 10) {
                            curSheet.setColumnWidth(e, this.columns.get(e).getWidth() * koeffWidth);
                        } else {
                            curSheet.trackColumnForAutoSizing(e);
                            curSheet.autoSizeColumn(e, true);
                        }
                    } catch (Throwable t) {
                        logger.log(Level.SEVERE, "Error on autosizing column: " + e, t);
                    }
                }
                writeHead(curSheet);

                for(DataRecord record: dataRecords){
                    ++rowNumber;
                    row = curSheet.createRow(rowNumber);

                    for(int e = 0; e < this.columns.size(); ++e) {
                        XlsColumn column = columns.get(e);
                        String columnName = column.getName();

                        if (record.getObject(columnName) != null) {
                            cell = row.createCell(e);
                            CellStyle cellStyle;
                            switch(column.getType()) {
                                case STRING:
                                    cell.setCellValue(record.getString(columnName));
                                    break;
                                case BOOLEAN:
                                    cell.setCellValue(record.getBoolean(columnName));
                                    break;
                                case INTEGER:
                                    cell.setCellValue((double)record.getInteger(columnName));
                                case LONG:
                                    cell.setCellValue((double)record.getLong(columnName));
                                    break;
                                case DECIMAL:
                                    cell.setCellValue(record.getBigDecimal(columnName).doubleValue());
                                    if(!cellStyles.containsKey(Integer.valueOf(e))) {
                                        cellStyle = wb.createCellStyle();
                                        cellStyle.setDataFormat(cHelper.createDataFormat().getFormat(column.getFormat()));
                                        cellStyles.put(Integer.valueOf(e), cellStyle);
                                    }

                                    cell.setCellStyle((CellStyle)cellStyles.get(Integer.valueOf(e)));
                                    break;
                                case DATE:
                                    cell.setCellValue(record.getDate(columnName));
                                    if(!cellStyles.containsKey(Integer.valueOf(e))) {
                                        cellStyle = wb.createCellStyle();
                                        cellStyle.setDataFormat(cHelper.createDataFormat().getFormat(((XlsColumn)this.columns.get(e)).getFormat()));
                                        cellStyles.put(Integer.valueOf(e), cellStyle);
                                    }

                                    cell.setCellStyle((CellStyle)cellStyles.get(Integer.valueOf(e)));
                                    break;
                                case DATETIME:
                                    cell.setCellValue(record.getDate(columnName));
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
            }
        }
    }

    public List<XlsColumn> getColumns() {
        return this.columns;
    }

    public void setColumns(List<XlsColumn> columns) {
        this.columns = columns;
    }

    public void setHead(ExcelExportHead head) {
        this.head = head;
    }

    public static void main(String[] args) {
    }
}
