package ru.rbt.barsgl.ejb.controller.excel;

import ru.rbt.ejbcore.mapping.BaseEntity;

import java.util.List;

/**
 * Created by Ivan Sevastyanov
 */
public interface InputMessageProcessor <To extends BaseEntity> {
    void processMessage(List<List<Object>> params) throws Exception;
    To buildPackage(List<List<Object>> params) throws ParamsParserException;
}
