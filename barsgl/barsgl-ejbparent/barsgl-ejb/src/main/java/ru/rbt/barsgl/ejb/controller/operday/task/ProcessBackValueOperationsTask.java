package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.BackvalueJournalController;
import ru.rbt.barsgl.ejb.entity.gl.GLBackValueOperation;
import ru.rbt.barsgl.ejb.integr.bg.BackValueOperationController;
import ru.rbt.barsgl.ejb.integr.bg.EtlPostingController;
import ru.rbt.barsgl.ejb.integr.oper.OrdinaryPostingProcessor;
import ru.rbt.barsgl.ejb.repository.BackValueOperationRepository;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.JpaAccessCallback;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.ExceptionUtils;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.*;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Package;
import static ru.rbt.barsgl.ejb.props.PropertyName.PD_CONCURENCY;
import static ru.rbt.barsgl.shared.enums.OperState.ERPROC;

/**
 * Created by er18837 on 05.07.2017.
 */
public class ProcessBackValueOperationsTask implements ParamsAwareRunnable {
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ProcessBackValueOperationsTask.class);
    private static final Integer MANUAL_COUNT = 1000;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private BackValueOperationController operationController;

    @EJB
    private EtlPostingController etlPostingController;

    @EJB
    private AsyncProcessor asyncProcessor;

    @EJB
    private PropertiesRepository propertiesRepository;

    @Inject
    private OrdinaryPostingProcessor ordinaryPostingProcessor;

    @Inject
    private BackvalueJournalController journalController;

    @EJB
    private AuditController auditController;

    @Inject
    private OperdayController operdayController;

    @Inject
    BackValueOperationRepository operationRepository;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        executeWork();
    }

    public void executeWork() throws Exception {
        executeWork(MANUAL_COUNT);
    }

    /**
     * обработка
     * @throws Exception
     */
    public void executeWork(int mnlPostingCount) throws Exception {
        if (checkOperdayStatus()) {
            // обработка ручных операций
            processBackValueOperations(mnlPostingCount);
        }
    }

    public boolean checkOperdayStatus() {
        Operday.OperdayPhase phase = operdayController.getOperday().getPhase();
        boolean allowed = operdayController.isProcessingAllowed();
        if (ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE != phase || !allowed) {
            auditController.error(BatchOperation, "Нельзя запустить задачу обработки BackValue операций", null,
                    String.format("Опердень в статусе %s, обработка %s", phase.name(), allowed ? "разрешена" : "запрещена"));
            return false;
        }
        return true;
    }

    public void processBackValueOperations(int operCount) throws Exception {
        beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
            // T0: читаем операции в SIGNEDDATE с UR чтоб исключить блокировку сортируем по дате берем максимально по умолчанию 10 (параметр)
            Date curdate = operdayController.getOperday().getCurrentDate();
            long cnt = operationRepository.getCountOperationsForProcessing(curdate);
            String msg = format("Кол-во авторизованных операций BackValue для обработки '%s' в режиме 'UR' за дату '%s'"
                    , cnt, dateUtils.onlyDateString(curdate));
            log.info(msg);
            if (cnt > 0) {
                List<GLBackValueOperation> operations = operationRepository.getOperationsForProcessing(operCount, curdate);
                auditController.info(BackValueOperation, msg);
                int cntError = 0;
                try {
                    cntError = asyncProcessOperations(operations);
                } catch (Exception e) {
                    auditController.error(Package, "Ошибка при обработке операций BackValue", null, e);
                }
                return cntError;
            }
            return null;
        }), 60 * 60);
    }

    /**
     * обработка ручных запросов со статусом SIGNED, SIGNEDDAGE
     * @return
     * @throws Exception
     */
    private int asyncProcessOperations(List<GLBackValueOperation> operations) throws Exception {
        final int[] errorCount = {0};
        List<JpaAccessCallback<Boolean>> callbacks = operations.stream().map(
                operation -> (JpaAccessCallback<Boolean>) persistence ->
                        beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence1, connection) -> {
                            try {
                                return operationController.processBackValueOperation(operation);
                            } catch (Throwable e) {
                                log.error(format("Error on processing of BackValue operation '%s'", operation.getId()), e);
                                String errMessage = getErrorMessage(e);
                                errorCount[0]++;
                                // TODO вроде лишнее
                                operationRepository.executeInNewTransaction(persistence0 -> {
                                    operationRepository.updateOperationStatusError(operation, ERPROC, errMessage);
                                    return null;
                                });
                                return null;
                            }
                        })
        ).collect(Collectors.toList());
        asyncProcessor.asyncProcessPooled(callbacks, propertiesRepository
                .getNumber(PD_CONCURENCY.getName()).intValue(), 1, TimeUnit.HOURS);
        return errorCount[0];
    }

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class,
                PersistenceException.class);
    }
}
