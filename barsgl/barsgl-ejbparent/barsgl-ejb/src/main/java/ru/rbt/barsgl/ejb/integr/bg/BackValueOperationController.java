package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.BackvalueJournalController;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLBackValueOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperationExt;
import ru.rbt.barsgl.ejb.integr.oper.OrdinaryPostingProcessor;
import ru.rbt.barsgl.ejb.integr.pst.GLOperationProcessor;
import ru.rbt.barsgl.ejb.repository.BackValueOperationRepository;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.shared.enums.BackValuePostStatus;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.JpaAccessCallback;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.ExceptionUtils;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.*;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.barsgl.ejb.props.PropertyName.PD_CONCURENCY;
import static ru.rbt.barsgl.shared.enums.BatchPostStatus.ERRPROC;
import static ru.rbt.barsgl.shared.enums.OperState.BLOAD;
import static ru.rbt.barsgl.shared.enums.OperState.ERCHK;
import static ru.rbt.barsgl.shared.enums.OperState.ERPROC;
import static ru.rbt.ejbcore.validation.ValidationError.initSource;

/**
 * Created by er18837 on 05.07.2017.
 */
@Stateless
@LocalBean
public class BackValueOperationController {
    private static final Logger log = Logger.getLogger(BackValueOperationController.class);

    @EJB
    private EtlPostingController etlPostingController;

    @EJB
    private AuditController auditController;

    @Inject
    private OrdinaryPostingProcessor ordinaryPostingProcessor;

    @EJB
    BackValueOperationRepository operationRepository;

    @Inject
    private OperdayController operdayController;

    public GLBackValueOperation processOperation(GLBackValueOperation operation) throws Exception {
        String msgCommon = format(" АЕ: '%s', ID_PST: '%s'", operation.getEtlPostingRef(), operation.getAePostingId());
        if (operation.getState() == BLOAD ) {
            GLOperationExt operationExt = operation.getOperExt();
            if (null == operationExt) {
                auditController.error(BackValueOperation, format("Неверная операция BackValue в статусе '%s', ID '%s' - нет записи в таблице GL_OPEREXT"
                        , operation.getState(), operation.getId()), operation, "");
                return operation;
            }
            auditController.info(Operation, "Начало обработки BackValue" + msgCommon, operation);
            try {
                if (!operationExt.getPostDatePlan().equals(operation.getPostDate())) {
                    // была изменена дата проводки - пересчитать параметры
                    operation = (GLBackValueOperation) etlPostingController.setDateParameters(ordinaryPostingProcessor, operation);
                }
            } catch (Throwable e) {
                String msg = "Ошибка при заполнения данных BackValue операции (зависящих от даты) по проводке";
                etlPostingController.operationErrorMessage(e, msg + msgCommon, operation, ERCHK, initSource());
                return operation;
            }
            try {
//                if (!etlPostingController.refillAccounts(operation)) {  // TODO надо ?
//                    return operation;
//                }
                operation = refreshOperationForcibly(operation);  // TODO надо ?
                if (etlPostingController.processOperation(operation, false)) {
                    operationRepository.updateManualStatus(operation.getId(), BackValuePostStatus.COMPLETED);
                    auditController.info(Operation,
                            format("Успешное завершение обработки BackValue операции '%s'", operation.getId())
                            , operation);
                    return operation;
                } else {
                    auditController.error(Operation, format("Не выполнена обработка BackValue операции '%s'"
                            , operation.getId()), operation, "");
                    return operation;
                }
            } catch (Throwable e) {
                String msg = "Ошибка при обработки BackValue операции (зависящих от даты) по проводке";
                etlPostingController.operationErrorMessage(e, msg + msgCommon, operation, ERCHK, initSource());
                return operation;
            }
        } else {
            auditController.warning(Operation, format("Попытка обработки BackValue операции в статусе '%s', ID '%s'"
                    , operation.getState(), operation.getId()), operation, "");
            return operation;
        }
    }

    private GLBackValueOperation refreshOperationForcibly(GLBackValueOperation operation) {
        try {
            return operationRepository.executeInNewTransaction(persistence -> operationRepository.refresh(operation, true));
        } catch (Exception e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class,
                PersistenceException.class);
    }

}

