package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.bg.fan.FanProcessorStorage;
import ru.rbt.barsgl.ejb.integr.fan.FanOperationProcessor;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejb.security.GLErrorController;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.Assert;
import ru.rbt.barsgl.shared.ExceptionUtils;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.List;

import static com.google.common.collect.Iterables.find;
import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperSide.C;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperSide.D;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.FanOperation;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.substr;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.*;


/**
 * Created by ER18837 on 08.04.15.
 */
public abstract class FanOperationController implements GLOperationController <String, GLOperation> {

    protected static final Logger log = Logger.getLogger(EtlPostingController.class);

    @EJB
    private GLOperationRepository glOperationRepository;

    @Inject
    private OperdayController operdayController;

    @EJB
    private FanProcessorStorage processorStorage;

    @EJB
    private AuditController auditController;

    @EJB
    private GLErrorController errorController;

    public abstract YesNo getStorno();

    public abstract boolean isWtacEnabled();

    protected List<GLOperation> getFanOperations(String parentReference, YesNo storno) {
        List<GLOperation> operList = glOperationRepository.getFanOperationByRef(parentReference, storno);
        Assert.isTrue(!operList.isEmpty(), format("Не найдены веерные операции для PAR_RF = %s ", parentReference));
        if (operList.size() < 2) {
            throw new ValidationError(FAN_IS_ONLY_ONE, parentReference);
        }
        return operList;
    }

    protected GLOperation getMainOperation(List<GLOperation> operList, String parentReference) {
        GLOperation mainOperation = null;
        for( GLOperation oper : operList) {
            if ( oper.getPref().equalsIgnoreCase(parentReference) ) {
                if (null != mainOperation ) {
                    throw new ValidationError(FAN_PARENT_NOT_SINGLE, parentReference);
                }
                mainOperation = oper;
            }
        }
        if (null == mainOperation) {
            throw new ValidationError(FAN_PARENT_NOT_EXISTS, parentReference);
        }
        return mainOperation;
    }

    protected GLOperation.OperSide defineFpSide(List<GLOperation> operList, GLOperation mainOperation) {
        String accDebit = mainOperation.getAccountDebit();
        String accCredit = mainOperation.getAccountCredit();
        boolean sideDebit = (null == find(operList, oper -> !oper.getAccountDebit().equals(accDebit), null));
        boolean sideCredit = (null == find(operList, oper -> !oper.getAccountCredit().equals(accCredit), null));
        if ( (sideDebit && sideCredit) || (!sideDebit && !sideCredit) ) {
            throw new ValidationError(FAN_SIDE_NOT_DEFINED, mainOperation.getParentReference());
        }
        return sideDebit ? C : D;
    }

    protected void setFanParameters(final GLOperation operation, GLOperation mainOperation,
                                    GLOperation.OperSide fpSide, BigDecimal amt, BigDecimal amtru) throws Exception {
            operation.setParentOperation(mainOperation);
            operation.setFpSide(fpSide);
            if (operation.equals(mainOperation)) {
                operation.setFbSide(fpSide == D ? C : D);
/*
                DataRecord amounts = glOperationRepository.getFanAmounts(mainOperation);
                if (null == amounts) {
                    throw new ValidationError(FAN_AMOUNT_NOT_DEFINED, mainOperation.getParentReference());
                }
*/
                operation.setAmountFan(amt);
                operation.setAmountFanRu(amtru);
            }
    }

    protected GLOperation updateFanParameters(final GLOperation operation, GLOperation mainOperation,
                                              GLOperation.OperSide fpSide, BigDecimal amt, BigDecimal amtru) throws Exception {
        return glOperationRepository.executeInNewTransaction(persistence -> {
            GLOperation operationTx = glOperationRepository.findById(GLOperation.class, operation.getId());
            setFanParameters(operationTx, mainOperation, fpSide, amt, amtru);
            return glOperationRepository.update(operationTx);
        });
    }

    protected GLOperation updateOperation(FanOperationProcessor operationProcessor, final GLOperation operation) throws Exception {
        return glOperationRepository.executeInNewTransaction(persistence -> {
            // обновляем объект в своей транзакции
            GLOperation operationTx = glOperationRepository.findById(GLOperation.class, operation.getId());
            operationTx.setProcDate(operdayController.getOperday().getCurrentDate());
            operationTx.setPstScheme(operationProcessor.getOperationType());
            operationProcessor.resolveOperationReference(operationTx);
            operationProcessor.setSpecificParameters(operationTx);
            return glOperationRepository.update(operationTx);
        });
    }

    protected FanOperationProcessor findOperationProcessor(GLOperation operation) throws Exception {
        return processorStorage.findOperationProcessor(operation);
    }

    protected void operationErrorMessage(Throwable e, String msg, GLOperation operation, OperState state, String source) {
        try {
            auditController.error(FanOperation, msg, operation, e);
            glOperationRepository.executeInNewTransaction(persistence -> {
                final String errorMessage = format("%s: \n%s Обнаружена: %s", msg, getErrorMessage(e), source);
                log.error(errorMessage, e);
                glOperationRepository.updateOperationStatusError(operation, state, errorMessage);
                return null;
            });
            errorController.error(msg, operation, e);
        } catch (Exception e1) {
            throw new DefaultApplicationException(e.getMessage(), e1);
        }
    }

    protected void operationFanErrorMessage(Throwable e, String msg, List<GLOperation> operList, String parentRef, YesNo storno, OperState state, String source) {
        try {
            final List<GLOperation> opList = (null != operList) ? operList :
                    glOperationRepository.getFanOperationByRef(parentRef, storno);
            auditController.error(FanOperation, msg, (null != opList) ? opList.get(0) : null, e);   // TODO
            glOperationRepository.executeInNewTransaction(persistence -> {
                final String errorMessage = format("%s: \n%s Обнаружена: %s\n'", msg, getErrorMessage(e), source);
                log.error(errorMessage, e);
                glOperationRepository.updateOperationFanStatusError(parentRef, storno, state, substr(errorMessage, 4000));
                return null;
            });
            for (GLOperation operation : opList) {
                errorController.error(msg, operation, e);
            }
        } catch (Exception e1) {
            throw new DefaultApplicationException(e.getMessage(), e1);
        }
    }

    protected String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, DefaultApplicationException.class);
    }

}
