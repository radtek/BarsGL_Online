package ru.rbt.barsgl.gwt.server.export;

import ru.rbt.ejbcore.DefaultApplicationException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;

/**
 * Created by ER18837 on 15.02.16.
 */
public class ExportFileHandler extends HttpServlet {
    private static final long serialVersionUID = 1L;

//    private static final Log log = LogFactory.getLog(Export.class);

    private File file;

    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String type = "";
        String fileName = request.getParameter("filename");

        if (fileName.contains("xlsx"))
            type = "xlsx";
        else if (fileName.contains("xls"))
            type = "xls";
        else if (fileName.contains("csv")) {
            type = "csv";
        }
        try {
            byte[] bytes = getFile(fileName);
            String newFileName = request.getParameter("newfilename");
            if (isEmpty(newFileName)) newFileName = fileName;
            sendFile(response, bytes, type, newFileName);
        } catch (IOException e) {
//            log.error("Ошибка экспорта файла", e);
        }
    }

    byte[] getFile(String filename) {
        byte[] bytes = (byte[]) null;
        try {
//        	String filePath = System.getProperty("java.io.tmpdir") + filename;
            file = new File(filename);
            if (!file.exists()) {
                throw new DefaultApplicationException("File not exists: '" + filename + "'", null);
            }
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(this.file);
                bytes = new byte[(int) this.file.length()];
                fis.read(bytes);
            } finally {
                if(fis != null) {
                    fis.close();
                }
            }
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
        return bytes;
    }

    void sendFile(HttpServletResponse response, byte[] bytes, String type, String newFileName)
            throws IOException {
        ServletOutputStream stream = null;
        stream = response.getOutputStream();

        if (type.equals("csv")) {
            response.setContentType("text/csv");
            response.setContentType("application/vnd.ms-excel");
        } else if (type.equals("xls")) {
            response.setContentType("application/vnd.ms-excel");
        }

        response.setHeader("Content-Disposition",
                "attachment; filename=" + newFileName); // + file.getName());
        response.addHeader("Content-Type", "application/octet-stream");
        response.setContentLength(bytes.length);
        stream.write(bytes);
        stream.close();
    }
}
