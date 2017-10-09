package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLBackValueOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperationExt;
import ru.rbt.barsgl.ejb.integr.pst.GLOperationProcessor;
import ru.rbt.barsgl.shared.enums.BackValuePostStatus;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.mapping.YesNo;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.*;
import static ru.rbt.barsgl.ejb.entity.gl.GLOperation.OperClass.AUTOMATIC;
import static ru.rbt.barsgl.shared.enums.DealSource.KondorPlus;
import static ru.rbt.barsgl.shared.enums.OperState.*;
import static ru.rbt.ejbcore.validation.ValidationError.initSource;

/**
 * Created by er18837 on 05.07.2017.
 */
@Stateless
@LocalBean
public class BackValueOperationController extends AbstractEtlPostingController{
    private static final Logger log = Logger.getLogger(BackValueOperationController.class);

    @Inject
    EtlPostingController etlPostingController;

    @Override
    public GLOperation processMessage(EtlPosting etlMessage) {
        return null;
    }

    /**
     * Обработка операции после авторизиции и подтверждения / изменения даты
     * @return
     * @throws Exception
     */
    public Boolean processBackValueOperation(GLBackValueOperation operation) throws Exception {
        operation = refreshOperationForcibly(operation);
        String msgCommon = format(" BackValue операции: '%d', ID_PST: '%s'", operation.getId(), operation.getAePostingId());
        if (operation.getState() != POST ) {
            GLOperationExt operationExt = operation.getOperExt();
            if (null == operationExt) {
                auditController.error(BackValueOperation, format("Неверная операция BackValue в статусе '%s', ID '%s' - нет записи в таблице GL_OPEREXT"
                        , operation.getState(), operation.getId()), operation, "");
                return false;
            }
            auditController.info(Operation, "Начало обработки" + msgCommon, operation);
            try {
                if (!operationExt.getPostDatePlan().equals(operation.getPostDate())) {
                    // была изменена дата проводки - пересчитать параметры
                    operation = (GLBackValueOperation) setDateParameters(ordinaryPostingProcessor, operation);
                }
            } catch (Throwable e) {
                operationErrorMessage(e, "Ошибка при заполнения данных (зависящих от даты) по проводке" + msgCommon,
                        operation, BERCHK, initSource());
                return false;
            }
            try {
                if (processOperation(operation)) {
                    bvOperationRepository.updateManualStatus(operation.getId(), BackValuePostStatus.COMPLETED);
                    auditController.info(Operation, "Успешное завершение обработки" + msgCommon, operation);
                    return true;
                } else {
                    auditController.error(Operation, "Не выполнена обработка" + msgCommon, operation, "");
                    return false;
                }
            } catch (Throwable e) {
                operationErrorMessage(e, "Ошибка при обработкe" + msgCommon, operation, BERCHK, initSource());
                return false;
            }
        } else {
            auditController.warning(Operation, format("Попытка обработки %s в статусе '%s'"
                    , msgCommon, operation.getState()), operation, "");
            return false;
        }
    }

    /**
     * Обрабатывает операцию BackValue - либо после авторизации, либо повторно
     * @param operation         операция
     * @return                  true, если опеарция обработана, false - WTAC или ERCHK
     */
    private boolean processOperation(GLBackValueOperation operation) throws Exception {
        // TODO при вызове из reprocessWtacOperations надо сначала обновить procDate, filialDebit, filialCredit
        String msgCommon = format(" операции: '%s' ID_PST: '%s'", operation.getId(), operation.getAePostingId());
        boolean toContinue;
        boolean isWtacPreStage = false;
        GLOperationProcessor operationProcessor;
        try {
            preProcessOperation(operation);
            operationProcessor = findOperationProcessor(operation);
            toContinue = validateOperation(operationProcessor, operation, isWtacPreStage);
        } catch ( Throwable e ) {
            String msg = "Ошибка валидации данных";
            operationErrorMessage(e, msg + msgCommon, operation, BERCHK, initSource());
            return false;
        }
        if ( toContinue ) {
            try {
                updateOperation(operationProcessor, operation);
            } catch ( Throwable e ) {
                String msg = "Ошибка заполнения данных";
                operationErrorMessage(e, msg + msgCommon, operation, BERCHK, initSource());
                return false;
            }
            if (BWTAC.equals(operation.getState())) {
                // изменяем статус, чтобы операция обработалась в общем потоке
                operationRepository.updateOperationStatusSuccess(operation, BLOAD);
                return true;
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

    /**
     * повторная
     * @param operation
     * @param reason
     * @return
     * @throws Exception
     */
    private boolean reprocessOperation(GLBackValueOperation operation, String reason) throws Exception {
        if (operation.getState() != POST) {
            if (!refillAccounts(operation)) {
                return false;
            }
            operation = refreshOperationForcibly(operation);
            if (processOperation(operation)) {
                auditController.info(Operation,
                        format("Успешное завершение повторной обработки операции BackValue '%s'. Причина '%s'.", operation.getId(), reason)
                        , operation);
                return true;
            } else {
                auditController.error(Operation, format("Ошибка повторной обработки операции BackValue '%s'. Причина '%s'."
                        , operation.getId(), reason), operation, "");
                return false;
            }
        } else {
            auditController.warning(Operation, format("Попытка повторной обработки операции BackValue в статусе '%s', ID '%s'. Причина '%s'."
                    , OperState.POST, operation.getId(), reason), operation, "");
            return false;
        }
    }

    /**
     * Повторная обработка опреаций со статусом WTAC
     * @param prevdate день создания операции (предыдущий ОД)
     * @return количество операций обработанных с ошибками
     */
    public int reprocessWtacBackValue(Date prevdate) throws Exception {
        List<GLBackValueOperation> operations = bvOperationRepository.select(GLBackValueOperation.class,
                // берем все операции раньше текущего опердня
                "FROM GLBackValueOperation g WHERE g.state = ?1 AND g.currentDate = ?2 ORDER BY g.id", OperState.BWTAC, prevdate);
        int res = 0;
        if (operations.size() > 0) {
            auditController.info(Operation, format("Найдено %d отложенных BackValue операций", operations.size()));
            for (GLBackValueOperation operation : operations) {
                if (!reprocessOperation(operation, "Обработка отложенных (BWTAC) операций"))
                    res++;
            }
            return res;
        } else {
            auditController.info(Operation, "Не найдено отложенных BackValue операций");
        }
        return res;
    }

    /**
     * Повторная обработка опреаций со статусом WTAC по списку
     * @return количество операций обработанных с ошибками
     */
    public int reprocessWtacBackValue(List<Long> idList) throws Exception {
        int res = 0;
        if (idList.size() > 0) {
            auditController.info(Operation, format("Переобработка %d отложенных BackValue операций", idList.size()));
            for (Long id : idList) {
                GLBackValueOperation operation = bvOperationRepository.findById(GLBackValueOperation.class, id);
                if (!reprocessOperation(operation, "Переобработка ошибочных отложенных (BERWTAC) операций"))
                    res++;
            }
            return res;
        } else {
            auditController.info(Operation, "Не найдено отложенных BackValue операций");
        }
        return res;
    }

    /**
     * Повторная обработка сторно, которые должны были выйти на ручную обработку
     * @param prevdate предыдущий ОД
     * @param curdate день создания операции (текущий ОД)
     * @return false в случае ошибок иначе true
     */
    public int reprocessErckStornoBvMnl(Date prevdate, Date curdate) throws Exception {
        int cnt = 0;
        // TODO убедиться, что в выборку попадают только BackBalue операции (OPER_CLASS = BV_MANUAL)
        // TODO среди них могут быть не дошедшие до авторизации и авторизованные - те, что упали после авторизации, пееробрабатывать не надо ???
        List<GLBackValueOperation> operations = bvOperationRepository.select(GLBackValueOperation.class,
                "FROM GLBackValueOperation g WHERE g.state = ?1 AND g.storno = ?2 AND g.currentDate = ?3 ORDER BY g.id"
                , ERCHK, YesNo.Y, curdate);
        if (operations.size() > 0) {
            auditController.info(Operation, format("Найдено %d отложенных СТОРНО операций BV_MANUAL", operations.size()));
            for (GLBackValueOperation operation: operations ) {
                // дата проводки в прошлом дне - пересчитать параметры
                operation = refreshOperationForcibly(operation);
                operation.setPostDate(curdate);
                setDateParameters(ordinaryPostingProcessor, operation);
                if (reprocessOperation(operation, "Повторная обработка СТОРНО операций BV_MANUAL (ERCHK)")) {
                    cnt++;
                }
            }
            return cnt;
        } else {
            return 0;
        }
    }

    /**
     * Повторная обработка сторно BackValue, которые должны были быть обработаны автоматически
     * @param prevdate предыдущий ОД
     * @param curdate день создания операции (текущий ОД)
     * @return false в случае ошибок иначе true
     */
    public int reprocessErckStornoBvAuto(Date prevdate, Date curdate) throws Exception {
        int cnt = 0;
        List<GLOperation> operations = operationRepository.select(GLOperation.class,
                "FROM GLOperation g WHERE g.state = ?1 AND g.storno = ?2 AND g.operClass = ?3 AND g.currentDate = ?4" +
                        " AND g.valueDate < ?5 ORDER BY g.id"
                , ERCHK, YesNo.Y, AUTOMATIC, curdate, prevdate);
        if (operations.size() > 0) {
            auditController.info(Operation, format("Найдено %d отложенных СТОРНО операций BackValue AUTOMATIC", operations.size()));
            for (GLOperation operation: operations ) {
                // дата проводки в прошлом дне - пересчитать параметры (кроме нестандартных операций)
                if (!operation.isNonStandard()) {
                    operation.setPostDate(curdate);
                    setDateParameters(ordinaryPostingProcessor, operation);
                }
                if (etlPostingController.reprocessOperation(operation, "Повторная обработка СТОРНО операций (ERCHK)")) {
                    cnt++;
                }
            }
            return cnt;
        } else {
            return 0;
        }
    }

    public GLBackValueOperation refreshOperationForcibly(GLBackValueOperation operation) {
        try {
            return operationRepository.executeInNewTransaction(persistence -> bvOperationRepository.refresh(operation, true));
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

}

