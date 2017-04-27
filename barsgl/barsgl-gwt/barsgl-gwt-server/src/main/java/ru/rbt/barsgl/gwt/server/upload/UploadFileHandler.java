package ru.rbt.barsgl.gwt.server.upload;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.poi.POIXMLException;
import ru.rbt.barsgl.ejb.controller.excel.BatchMessageProcessor;
import ru.rbt.barsgl.ejb.controller.excel.CardMessageProcessor;
import ru.rbt.barsgl.ejb.controller.excel.ParamsParserException;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejbcore.remote.ServerAccess;
import ru.rbt.barsgl.gwt.serverutil.GwtServerUtils;
import ru.rbt.shared.ExceptionUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static ru.rbt.barsgl.gwt.serverutil.GwtServerUtils.findServerAccess;

/**
 * Created by Ivan Sevastyanov
 */
public class UploadFileHandler extends HttpServlet {

    private static Logger log = Logger.getLogger(UploadFileHandler.class);

    private static final String UPLOAD_TYPE = "uploadtype";

    private ServerAccess localInvoker;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
        GwtServerUtils.setRequest(request);
        resp.setContentType("text/html;charset=Windows-1251");
        try {
            if (ServletFileUpload.isMultipartContent(request)) {
                FileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                List<FileItem> items = upload.parseRequest(request);
                Map<String, String> params = new HashMap<>();
                Iterator<FileItem> iterator = items.iterator();
                File file = null;
                while (iterator.hasNext()) {
                    FileItem item = (FileItem) iterator.next();
                    // processes only fields that are form fields
                    if (item.isFormField()) {
                        String fieldname = item.getFieldName();
                        String fieldvalue = item.getString();
                        params.put(fieldname, fieldvalue);
                    } else {
                        file = writeFile(item);
                    }
                }
                if (null == file) {
                    throw new IllegalArgumentException("Нет файлов для обработки");
                }

                String uploadType = params.get(UPLOAD_TYPE);
                Class<?> uploadProcessor = getUploadProcessor(uploadType);
                String result = localInvoker.invoke(uploadProcessor, "processMessage", file, params);
                resp.getWriter().append(result);

            } else {
                throw new RuntimeException("Request is not have multipart content");
            }

        } catch (Exception e) {
            String rusErr = "";
            if (ExceptionUtils.isExistsInException(e, ParamsParserException.class)) {
                ParamsParserException exception = ExceptionUtils.findException(e, ParamsParserException.class);
                if (exception.getMessage() == null || exception.getMessage().trim().isEmpty())
                    rusErr = "Ошибки формата данных при загрузке пакета из файла";
                else rusErr = exception.getMessage();
            }else if (ExceptionUtils.isExistsInException(e.getCause(), IllegalStateException.class)){
                rusErr = "Файл содержит формулы. Загрузка файла невозможна";
            }else if (ExceptionUtils.isExistsInException(e.getCause(), POIXMLException.class)){
                rusErr = "Неверный формат файла. Нужен файл в формате 'xlsx'";
            }
            if (!rusErr.isEmpty()){
                try {
                    localInvoker.invoke(AuditController.class, "warning",
                            AuditRecord.LogCode.BatchOperation, rusErr, null, null);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                resp.getWriter().print(rusErr);

            }else{
                try {
                    localInvoker.invoke(AuditController.class, "error",
                            AuditRecord.LogCode.BatchOperation, e.getMessage(), null, e);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace(resp.getWriter());
            }
        }
    }

    private Class<?> getUploadProcessor(String uploadType) throws Exception {
        if("Batch".equals(uploadType)) {
            return BatchMessageProcessor.class;
        } else if("Card".equals(uploadType)) {
            return CardMessageProcessor.class;
        } else {
            throw new Exception("Не определен тип обработчика " + uploadType + " для загруженного файла");
        }
    }

    private File writeFile(FileItem fileItem) throws Exception {
        String filename = Long.toString(System.currentTimeMillis());
        File file = File.createTempFile("card_" + filename, ".xlsx");
        try(OutputStream os = new FileOutputStream(file);) {
            IOUtils.copy(fileItem.getInputStream(), os);
        }
        return file;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        localInvoker = findServerAccess();
    }
}
