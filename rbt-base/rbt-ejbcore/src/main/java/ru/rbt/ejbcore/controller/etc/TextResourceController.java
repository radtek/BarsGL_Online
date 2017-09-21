package ru.rbt.ejbcore.controller.etc;

import java.io.*;

/**
 * Created by ER21006 on 03.02.2016.
 */
public class TextResourceController {

    /**
     * Находим в основном SQL скрипты как ресурсы
     * @param resourceName
     * @return
     * @throws IOException
     */
    public String getContent(String resourceName) throws IOException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)
             ; InputStreamReader reader = new InputStreamReader(is)
             ; BufferedReader br = new BufferedReader(reader)
             ; StringWriter sw = new StringWriter()
             ; BufferedWriter builder = new BufferedWriter(sw)){
            String line;
            if (null != is) {
                while ((line = br.readLine()) != null) {
                    builder.write(line); builder.newLine();
                }
                builder.flush();
                return sw.toString();
            }
            throw new IOException(String.format("Resource '%s' is unknown", resourceName));
        }
    }
}
