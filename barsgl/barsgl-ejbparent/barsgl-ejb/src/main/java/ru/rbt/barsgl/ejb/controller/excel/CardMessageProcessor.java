package ru.rbt.barsgl.ejb.controller.excel;

import java.io.File;
import java.util.Map;

/**
 * Created by ER22317 on 22.09.2016.
 */
public interface CardMessageProcessor {
    String processMessage(File file, Map<String,String> params) throws Exception;
}
