package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.util.HSSFColor;
import ru.rbt.barsgl.ejb.controller.cob.CobStepResult;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.oper.IncomingPostingProcessor;
import ru.rbt.barsgl.ejb.integr.pst.GLOperationProcessor;
import ru.rbt.barsgl.shared.enums.CobStepStatus;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.mapping.YesNo;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Operation;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperClass.AUTOMATIC;
import static ru.rbt.barsgl.shared.enums.OperState.*;
import static ru.rbt.ejbcore.validation.ValidationError.initSource;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless
@LocalBean
public class EtlPostingController extends AbstractEtlPostingController { //} implements EtlMessageController<EtlPosting, GLOperation> {

    private static final Logger log = Logger.getLogger(EtlPostingController.class);

    /**
     * Обрабатывает входящую проводку
     */
    @Override
    public GLOperation processMessage(EtlPosting posting) {
        String msgCommon = format(" АЕ: '%s', ID_PST: '%s'", posting.getId(), posting.getAePostingId());
        log.info("Log test: обработка проводки" + msgCommon);
        if (posting.getErrorCode() != null) { // && posting.getErrorCode() == 0) {  // Обрабатываем только совсем необработанные
            auditController.info(Operation, "Проводка АЕ уже была обработана" + msgCommon, posting);
            return null;
        }
        auditController.info(Operation, "Начало обработки проводки" + msgCommon, posting);
        try {
            ordinaryPostingProcessor.calculateOperationClass(posting);
            IncomingPostingProcessor etlPostingProcessor = findPostingProcessor(posting);      // найти процессор сообщеиня
            GLOperation operation;
            try {
                    operation = createOperation(etlPostingProcessor, posting);
                    if (null == operation) {
                    return null;            // ошибки валидации
                }
                etlPostingRepository.updatePostingStateSuccess(posting);
            } catch (Throwable e) {
                String msg = "Ошибка при создании операции по проводке";
                postingErrorMessage(e, msg + msgCommon, posting, initSource());
                return null;
            }

            try {
                operation = enrichmentOperation(etlPostingProcessor, operation);
            } catch (Throwable e) {
                String msg = "Ошибка при заполнения данных операции по проводке";
                operationErrorMessage(e, msg + msgCommon, operation, ERCHK, initSource());
                return operation;
            }

            try {
                GLOperationProcessor processor = simpleOperationProcessor;  // findOperationProcessor(operation); ??
                operation = fillAccount(processor, operation, GLOperation.OperSide.D);
                operation = fillAccount(processor, operation, GLOperation.OperSide.C);
            } catch (Throwable e) {
                String msg = "Ошибка при поиске (создании) счетов по проводке";
                operationErrorMessage(e, msg + msgCommon, operation, ERCHK, initSource());
                return operation;
            }

            operation.setBackValueParameters(posting.getBackValueParameters());
            if (processOperation(operation, true)) {
                auditController.info(Operation, "Успешное завершение обработки проводки" + msgCommon, operation);
            }
            return operation;
        } catch (Exception e) {
            String msg = "Нераспознанная ошибка при обработке проводки";
            auditController.error(Operation, msg + msgCommon, posting, e);
            errorController.error(msg + msgCommon, posting, e);
            context.setRollbackOnly();
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Обрабатывает входящую операцию, с отдельной веткой установки статуса WTAC/ERCHK
     * @param operation         операция
     * @param isWtacPreStage    true = первая обработка (пропустить WTAC)
     * @return                  true, если опеарция обработана, false - WTAC или ERCHK
     */
    private boolean processOperation(GLOperation operation, boolean isWtacPreStage) throws Exception {
        String msgCommon = format(" операции: '%s' ID_PST: '%s'", operation.getId(), operation.getAePostingId());
        boolean toContinue;
        GLOperationProcessor operationProcessor;
        try {
            preProcessOperation(operation);
            operationProcessor = findOperationProcessor(operation);
            toContinue = validateOperation(operationProcessor, operation, isWtacPreStage);
        } catch ( Throwable e ) {
            String msg = "Ошибка валидации данных";
            operationErrorMessage(e, msg + msgCommon, operation, ERCHK, initSource());
            return false;
        }
        if ( toContinue ) {
            try {
                updateOperation(operationProcessor, operation);
            } catch ( Throwable e ) {
                String msg = "Ошибка заполнения данных";
                operationErrorMessage(e, msg + msgCommon, operation, ERCHK, initSource());
                return false;
            }
            if (operation.isBackValue()) {          // OPER_CALSS == BV_MANUAL
                 if( isWtacPreStage ) {             // первый раз создаем расширенную операцию и выходим на ручную обработку
                     createOperationExt(operationProcessor, operation, BLOAD);
                     return false;
                 }
                 else {                             // больше сюда попадать не должны
                     auditController.warning(Operation, "Повторная обработка BackValue операции как AUTOMATIC");
/*
                     String msg = "Ошибка обработки операции BackValue";
                     operationErrorMessage(new DefaultApplicationException("Неверный процесс обработки: BackValue операции не должны здесь обрабатываться"),
                             msg + msgCommon, operation, ERPROC, initSource());
                     return false;
*/
                 }
            }
            try {
                finalOperation(operationProcessor, operation);
                return true;
            } catch ( Throwable e ) {
                String msg = "Ошибка обработки";
                operationErrorMessage(e, msg + msgCommon, operation, ERPOST, initSource());
            }
        }
        return false;
    }

    public boolean reprocessOperation(GLOperation operation, String reason) throws Exception {
        if (operation.getState() != POST) {
            if (!refillAccounts(operation)) {
                return false;
            }
            operation = refreshOperationForcibly(operation);
            if (processOperation(operation, false)) {
                auditController.info(Operation,
                        format("Успешное завершение повторной обработки операции '%s'. Причина '%s'.", operation.getId(), reason)
                        , operation);
                return true;
            } else {
                auditController.error(Operation, format("Ошибка повторной обработки операции '%s'. Причина '%s'."
                        , operation.getId(), reason), operation, "");
                return false;
            }
        } else {
            auditController.warning(Operation, format("Попытка повторной обработки операции в статусе '%s', ID '%s'. Причина '%s'."
                    , OperState.POST, operation.getId(), reason), operation, "");
            return false;
        }
    }

    /**
     * Повторная обработка опреаций со статусом WTAC
     * @param prevdate первая дата валютирования
     * @return операции обработанные с ошибками
     */
    public List<GLOperation> reprocessWtacOperations(Date prevdate) throws Exception {
        List<GLOperation> res = new ArrayList<GLOperation>();
        //TODO Golomin Предполагаемое количество операци - небольшое, если будет критическим, переписать на JDBC
        List<GLOperation> operations = operationRepository.select(GLOperation.class,
                "FROM GLOperation g WHERE g.state = ?1 AND g.currentDate = ?2 ORDER BY g.id", OperState.WTAC, prevdate);
        if (operations.size() > 0) {
            auditController.info(Operation, format("Найдено %d отложенных операций", operations.size()));
            for (GLOperation operation : operations) {
                if (!reprocessOperation(operation, "Обработка отложенных (WTAC) операций")) {
                    res.add(operation);
                }
            }
        } else {
            auditController.info(Operation, "Не найдено отложенных операций");
        }
        return res;
    }

    /**
     * Повторная обработка сторно
     * @param date1 первая дата валютирования
     * @param date2 вторая дата валютирования
     * @return false в случае ошибок иначе true
     */
    public int[] reprocessErckStornoToday(Date date1, Date date2) throws Exception {
        int cnt = 0;
        // TODO убедиться, что в выборку попадают только автоматические операции (OPER_CLASS = AUTOMATIC)
        List<GLOperation> operations = operationRepository.select(GLOperation.class,
                "FROM GLOperation g WHERE g.state = ?1 AND g.storno = ?2 AND g.operClass = ?3 AND g.valueDate IN (?4 , ?5) ORDER BY g.id"
                , ERCHK, YesNo.Y, AUTOMATIC, date1, date2);
        if (operations.size() > 0) {
            auditController.info(Operation, format("Найдено %d отложенных СТОРНО операций", operations.size()));
            for (GLOperation operation : operations) {
                if (reprocessOperation(operation, "Повторная обработка СТОРНО операций (ERCHK)")) {
                    cnt++;
                }
            }
            return new int[]{operations.size(), cnt};
        } else {
            return new int[]{0, 0};
        }
    }

}
