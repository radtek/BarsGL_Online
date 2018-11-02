package ru.rbt.barsgl.ejbcore.util;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Ivan Sevastyanov
 */
public class ExcelParser implements Iterator<List<Object>>, Closeable {

    private final InputStream stream;

    private Iterator<Row> rowIterator;

    private int from;

    private String [] params;

    private int columnCount;
    private int rowCount;

    public ExcelParser(InputStream stream) {
        this.stream = stream;
    }

    /**
     * Формируем списки параметров для вызова процедуры
     * @param from с какой строки начинаем парсить (0 - первая строка)
     * @return результат парсинга
     * @throws IOException
     */
    public List<List<Object>> parse(final int from, String ... params) throws IOException, InvalidFormatException {
        List<List<Object>> result = new ArrayList<>();
        Iterator<List<Object>> it = parseSafe(from, params);
        while (it.hasNext()) {
            List<Object> row = it.next();
            if(row.size() > 0) {
                result.add(row);
            }
        }
        return result;
    }

    public Iterator<List<Object>> parseSafe(final int from, String ... params) throws IOException, InvalidFormatException {
        this.from = from;
        this.params = params;
        XSSFWorkbook book = new XSSFWorkbook(stream);
        XSSFSheet sheet = book.getSheetAt(0);
        columnCount = (null != sheet.getRow(0)) ? sheet.getRow(0).getLastCellNum() : 0; // TODO ???
        rowCount = sheet.getLastRowNum() + 1;
        rowIterator = sheet.iterator();
        return this;
     }

    @Override
    public void close() {
        if (null != stream) {
            try {
                stream.close();
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean hasNext() {
        return rowIterator != null && rowIterator.hasNext();
    }

    @Override
    public List<Object> next() {
        List<Object> rowList = new ArrayList<>();
        Row row = rowIterator.next();
        boolean hasValue = false;
        if (from <= row.getRowNum()) {
            for (int col = 0; col < columnCount; col++) {
                Cell cell = row.getCell(col);
                if (null == cell) {
                    rowList.add(null);
                } else {
                    Object cellValue = null;
                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_STRING:
                            cellValue = cell.getStringCellValue();
                            break;
                        case Cell.CELL_TYPE_NUMERIC:
                            if (HSSFDateUtil.isCellDateFormatted(cell)) {
                                cellValue = cell.getDateCellValue();
                            } else {
                                cellValue = cell.getNumericCellValue();
                            }
                            break;
                        case Cell.CELL_TYPE_BOOLEAN:
                            cellValue = cell.getBooleanCellValue();
                            break;
                        default:
                            cellValue = cell.getStringCellValue();
                    }
                    rowList.add(cellValue);
                    hasValue |= !cellValue.toString().trim().isEmpty();
                }
            }
            if (hasValue) {
                for (String param : params) {
                    rowList.add(param);
                }
                return rowList;
            }
            return Collections.emptyList();
        } else if(hasNext()) {
            return next();
        } else {
            return rowList;
        }
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getColumnCount() {
        return columnCount;
    }
}