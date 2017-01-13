package ru.rbt.barsgl.ejb.integr.fan;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by ER18837 on 10.04.15.
 * Базовый класс для обработчика веерных операций проводок в режиме precob
 */
public abstract class FanOperationProcessor {

    @Inject
    private OperdayController operdayController;

    public abstract boolean isSupported(GLOperation operation);

    public abstract GLOperation.OperType getOperationType();

    public abstract void setSpecificParameters(GLOperation operation) throws Exception;

    public abstract void addPosting(GLOperation operation, List<GLPosting> postList) throws Exception;

    public final boolean isStornoOneday(GLOperation operation) {
        return (operation.isStorno())                                                           // сторно
                && operation.stornoOneday(operdayController.getOperday().getCurrentDate());     // обе операции в опердень
    }

    public final boolean isStornoBackvalue(GLOperation operation) {
        return (operation.isStorno())                                                           // сторно
                && !operation.stornoOneday(operdayController.getOperday().getCurrentDate());    // хотя бы одна операция в другой день
    }

    public void resolveOperationReference(GLOperation operation) throws Exception {
    }

    public void resolvePostingReference(GLOperation operation, List<GLPosting> postList) {
    }
}
