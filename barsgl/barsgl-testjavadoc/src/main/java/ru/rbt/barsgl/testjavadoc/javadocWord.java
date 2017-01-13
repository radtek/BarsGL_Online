package ru.rbt.barsgl.testjavadoc;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc;

import java.io.*;
import java.math.BigInteger;
import java.util.List;

/**
 * Created by ER18837 on 06.07.15.
 */
public class javadocWord implements javadocOutFile {
    public static String keyWord = "Регрессионное";
    private boolean isWord;
    private FileOutputStream docFile;

    private XWPFDocument doc = null;
    private XWPFTable table;
    private int colCount;

    private String documentName;

    public javadocWord() {
        isWord = false;
    }

    @Override
    public boolean init(String filePath, String fileName, String template) {
        try {
            if (!template.isEmpty()) {
                String templateName = filePath + template + ".docx";
                doc = new XWPFDocument(new FileInputStream(templateName));
            }
        } catch (IOException e) {
            TestJavadoc.outLog("Read Word template file error: " + e.getMessage());
        }
        if (null == doc) {
            doc = new XWPFDocument();
        }

        documentName = filePath + fileName + ".docx";
        try {
            docFile = new FileOutputStream(documentName);
        } catch (FileNotFoundException e) {
            TestJavadoc.outLog("Create Word file error: " + e.getMessage());
            return false;
        }

        colCount = columnNames.length;
        table = createTable(colCount);
        if (null == table) {
            return false;
        }

        isWord = true;
        return true;
    }

    @Override
    public void writeClass(String className, String comment) {
        if(!isWord)
            return;
        XWPFTableRow row = table.createRow();
        // Имя класса
        setCellText(row.getCell(0), className, true, true, false);

        // Комментарий - объединяем колонки
        XWPFTableCell cell = row.getCell(1);
        cell.getCTTc().addNewTcPr().addNewGridSpan().setVal(BigInteger.valueOf(colCount - 1));
        for (int i = colCount - 1; i > 1; i--) {
            row.getCell(i).getCTTc().newCursor().removeXml();   // убрать лишние ячейки
        }
        setCellText(cell, comment, true, true, false);
    }

    @Override
    public void writeMethod(String methodName, String... params) {
        if(!isWord)
            return;
        XWPFTableRow row = table.createRow();
        XWPFTableCell cell = row.getCell(0);
        setCellText(cell, methodName, false, true, false);
        int col = 1;
        for (String param : params) {
            cell = row.getCell(col++);
            setCellText(cell, param, false, false, false);
        }
        // попытка задать ширину колонки
//        row.getCell(colCount - 1).getCTTc().addNewTcPr().addNewTcW().setW(BigInteger.valueOf(100L));
    }

    @Override
    public void close() {
        if(!isWord)
            return;
        try {
            doc.write(docFile);
            docFile.flush();
            docFile.close();
            TestJavadoc.outLog("Create Word file: " + documentName);
        } catch (IOException e) {
            TestJavadoc.outLog("Write Word file error: " + e.getMessage());
        }
    }

    private XWPFTable createTable(int colCount) {
        XWPFTable table = null;
        List<XWPFParagraph> pars = doc.getParagraphs();
        for(int p = 0; p < pars.size() - 1; p++) {
            XWPFParagraph par = pars.get(p);
            String text = par.getText();
            if (text.contains(keyWord)) {
                XmlCursor cursor = null;
                cursor = pars.get(p+1).getCTP().newCursor();
                table = doc.insertNewTbl(cursor);
            }
        }
        if (null == table)
            table = doc.createTable();
        XWPFTableRow row = table.getRow(0);
        setCellText(row.getCell(0), columnNames[0], true, false, false);
        for (int i = 1; i < colCount; i++) {
            XWPFTableCell cell = row.addNewTableCell();
            setCellText(cell, columnNames[i], true, false, false);
        }
        // попытка задать ширину колонки
//        row.getCell(colCount - 1).getCTTc().addNewTcPr().addNewTcW().setW(BigInteger.valueOf(2000L));

        return table;
    }

    private void setCellText(XWPFTableCell cell, String text, boolean bold, boolean italic, boolean center) {
        cell.getCTTc().addNewTcPr().addNewVAlign().setVal(STVerticalJc.CENTER);
        ParagraphAlignment alignment = center ? ParagraphAlignment.CENTER : ParagraphAlignment.LEFT;
        XWPFParagraph para = cell.getParagraphs().get(0);
        // горизонтальное выравнивание
        para.setAlignment(alignment);
        para.setSpacingBefore(60);
        para.setSpacingAfter(60);

        XWPFRun run = para.createRun();
        // Шрифт Bold Italic
        run.setBold(bold);
        run.setItalic(italic);
        run.setText(text);
    }

}
