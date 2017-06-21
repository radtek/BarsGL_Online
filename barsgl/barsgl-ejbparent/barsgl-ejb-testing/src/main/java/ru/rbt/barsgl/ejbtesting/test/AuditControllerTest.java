package ru.rbt.barsgl.ejbtesting.test;

import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.shared.Assert;

import static ru.rbt.audit.entity.AuditRecord.LogCode.Task;

/**
 * Created by ER18837 on 10.06.15.
 */
@Stateless
public class AuditControllerTest {

    @EJB
    private GLOperationRepository operationRepository;

    @EJB
    private AuditController auditController;

    public void testLog(AuditRecord.LogLevel logLevel, String message, Long gloid) throws InterruptedException {
        GLOperation operation;
        try {
            operation = operationRepository.selectFirst(GLOperation.class, "from GLOperation o where o.id = ?1", gloid);
            Assert.notNull(operation, "Не найдена операция GLOID = " + gloid);
            try {
                auditController.info(AuditRecord.LogCode.Operation, message + ": before divide error", operation);
                long e = 5 / 0;
            } catch (Throwable e) {
                Thread.sleep(100);
                auditController.error(AuditRecord.LogCode.Operation, message, operation, e);
            }
            try {
                Thread.sleep(100);
                auditController.info(AuditRecord.LogCode.Operation, message + ": before BalanceChapter error: " + message, operation);
                operationRepository.getBalanceChapter("");
            } catch (Throwable e) {
                Thread.sleep(100);
                auditController.warning(AuditRecord.LogCode.Operation, message, operation, e);
            }
            try {
                Thread.sleep(100);
                auditController.info(AuditRecord.LogCode.Operation, message + ": before ValidationError: " + message, operation);
                throw new ValidationError(ErrorCode.CURRENCY_CODE_NOT_EXISTS, "LOL");
            } catch (Throwable e) {
                Thread.sleep(100);
                auditController.error(AuditRecord.LogCode.Operation, message, operation, e);
            }
            try {
                Thread.sleep(100);
                auditController.info(AuditRecord.LogCode.Operation, message + ": before FanAmounts error: " + message, operation);
                operationRepository.executeInNewTransaction(persistence -> {
                    operationRepository.getFanAmounts(operation);
                    return operation;
                });
            } catch (Throwable e) {
                Thread.sleep(100);
                auditController.error(AuditRecord.LogCode.Operation, message, operation, e);
            }
            try {
                Thread.sleep(100);
                auditController.info(AuditRecord.LogCode.Operation, message + ": before FanAmounts error: " + message, operation);
                operationRepository.executeInNewTransaction(persistence -> {
                    throw new ValidationError(ErrorCode.CURRENCY_CODE_NOT_EXISTS, "SOS");
                });
            } catch (Throwable e) {
                Thread.sleep(100);
                auditController.error(AuditRecord.LogCode.Operation, message, operation, e);
            }
        } catch (Exception e) {
            Thread.sleep(100);
            auditController.error(AuditRecord.LogCode.Operation, message, null, e);
        }
    }

    public void testLog2() {
        auditController.info(Task, "Start transaction");
        try {
            operationRepository.executeNativeUpdate("update blabla set bla='LOL' where lala = 'DUMMY'");
        } catch (Exception e) {
            operationRepository.setRollbackOnly();
            auditController.error(Task, "Error on testing update", null, e);
        }
    }

}
