package ru.rbt.barsgl.testjavadoc;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.awt.*;
import java.io.*;

/**
 * Created by ER18837 on 06.07.15.
 */
public class javadocExcel implements javadocOutFile{
    public int TO_PIXELS = 256;

    private boolean isExcel;
    private FileOutputStream outFile;

    private CellStyle classStyle;
    private CellStyle methodStyle;
    private CellStyle commentStyle;

    private XSSFWorkbook wb;
    private XSSFSheet sheet;
    private int rowNum;

    private String documentName;

    public javadocExcel() {
        isExcel = false;
    }

    public boolean init(String filePath, String fileName, String template){
        documentName = filePath + fileName + ".xlsx";
        try {
            outFile = new FileOutputStream(documentName);
        } catch (FileNotFoundException e) {
            TestJavadoc.outLog("Create Excel file error: " + e.getMessage());
            return false;
        }

        wb = new XSSFWorkbook();
        classStyle = createClassStyle(wb);
        methodStyle = createMethodStyle(wb);
        commentStyle = createCommentStyle(wb);

        sheet = createPackegeSheet(wb, "Javadoc");
        rowNum = 0;
        isExcel = true;
        return true;
    }

    @Override
    public void writeClass(String className, String comment) {
        if (!isExcel)
            return;
        rowNum = createCellClass(sheet, rowNum, className, comment);
    }

    @Override
    public void writeMethod(String methodName, String... params) {
        if (!isExcel)
            return;
        rowNum = createMethodRow(sheet, rowNum, methodName, params);
    }

    @Override
    public void close() {
        if (!isExcel)
            return;
        try {
            wb.write(outFile);
            outFile.close();
            TestJavadoc.outLog("Create Excel file: " + documentName);
        } catch (IOException e) {
            TestJavadoc.outLog("Write Excel file error: " + e.getMessage());
        }
    }

    private XSSFSheet createPackegeSheet(XSSFWorkbook wb, String sheetName) {
        XSSFSheet sheet = wb.createSheet(sheetName);
        sheet.setColumnWidth(0, 30 * TO_PIXELS);
        sheet.setColumnWidth(1, 60 * TO_PIXELS);
        sheet.setColumnWidth(2, 10 * TO_PIXELS);
        Row row = sheet.createRow(0);
        for(int i = 0; i < columnNames.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellStyle(methodStyle);
            cell.setCellValue(columnNames[i]);
        }
        return sheet;
    }

    private CellStyle createClassStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 12);
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        font.setItalic(true);
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new Color(196, 230, 241)));
        style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
        style.setAlignment(CellStyle.ALIGN_CENTER);
        return style;
    }

    private CellStyle createMethodStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setWrapText(true);
        Font font = wb.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style.setFont(font);
        return style;
    }

    private CellStyle createCommentStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setWrapText(true);
        return style;
    }

    private int createCellClass(XSSFSheet sheet, int r, String className, String comment) {
        Row row = sheet.createRow(++r);
        Cell cell = row.createCell(0);
        cell.setCellStyle(classStyle);
        cell.setCellValue(className);
        cell = row.createCell(1);
        cell.setCellStyle(classStyle);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 1, 2));
        cell.setCellValue(comment);
        return r;
    }

    private int createMethodRow(XSSFSheet sheet, int r, String methodName, String ... colValues) {
        Row row = sheet.createRow(++r);
        int i = 0;
        row.createCell(i++).setCellValue(methodName);
        for (String colValue : colValues) {
            row.createCell(i++).setCellValue(colValue);
        }
        row.getCell(0).setCellStyle(methodStyle);
        row.getCell(1).setCellStyle(commentStyle);
        row.getCell(2).setCellStyle(commentStyle);
        return r;
    }

}
