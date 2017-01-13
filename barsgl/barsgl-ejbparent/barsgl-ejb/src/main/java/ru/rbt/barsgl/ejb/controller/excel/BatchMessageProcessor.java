package ru.rbt.barsgl.ejb.controller.excel;

import java.io.File;
import java.util.Map;

/**
 * Created by ER18837 on 29.02.16.
 */
public interface BatchMessageProcessor {
    /**
     * Обработать батч
     * @param file - файл
     * @param params - параметры
     * @return результат
     * @throws Exception
     */
    String processMessage(File file, Map<String,String> params) throws Exception;
}
