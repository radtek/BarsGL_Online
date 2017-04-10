package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.BackvalueJournalController;
import ru.rbt.barsgl.ejb.controller.excel.BatchProcessResult;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLManualOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.integr.oper.BatchPostingProcessor;
import ru.rbt.barsgl.ejb.integr.oper.ManualOperationProcessor;
import ru.rbt.barsgl.ejb.integr.oper.OrdinaryPostingProcessor;
import ru.rbt.barsgl.ejb.integr.pst.GLOperationProcessor;
import ru.rbt.barsgl.ejb.repository.*;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.JpaAccessCallback;
import ru.rbt.barsgl.ejbcore.repository.PropertiesRepository;
import ru.rbt.barsgl.ejbcore.util.StringUtils;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.ExceptionUtils;
import ru.rbt.barsgl.shared.enums.*;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.barsgl.ejb.controller.excel.BatchProcessResult.BatchProcessDate.BT_NOW;
import static ru.rbt.barsgl.ejb.controller.excel.BatchProcessResult.BatchProcessDate.BT_PAST;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.*;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.Package;
import static ru.rbt.barsgl.ejb.props.PropertyName.PD_CONCURENCY;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.substr;
import static ru.rbt.barsgl.ejbcore.validation.ValidationError.initSource;
import static ru.rbt.barsgl.shared.enums.BatchPackageState.*;
import static ru.rbt.barsgl.shared.enums.BatchPostAction.CONFIRM_NOW;
import static ru.rbt.barsgl.shared.enums.BatchPostStatus.ERRPROC;

/**
 * Created by ER18837 on 11.01.17.
 * фоновая обработка ручных / пакетных операций
 */
@Stateless
@LocalBean
public class ManualOperationController {
    private static final Logger log = Logger.getLogger(EtlPostingController.class);
    private static final String postingName = "GL_BATPST";
    public static final SimpleDateFormat timeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    @EJB
    private ManualPostingController postingController;

    @EJB
    private AuditController auditController;

    @EJB
    private EtlPostingController etlPostingController;

    @Inject
    private BatchPostingProcessor postingProcessor;

    @Inject
    private ManualOperationProcessor operationProcessor;

    @Inject
    private BatchPackageRepository packageRepository;

    @EJB
    private BatchPostingRepository postingRepository;

    @EJB
    private ManualOperationRepository operationRepository;

    @Inject
    private AppUserRepository userRepository;

    @Inject
    private GLAccountRepository accountRepository;

    @Inject
    private OrdinaryPostingProcessor ordinaryPostingProcessor;

    @Inject
    private OperdayController operdayController;

    @EJB
    private PdRepository pdRepository;

    @Inject
    private BackvalueJournalController journalController;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private AsyncProcessor asyncProcessor;

    @EJB
    private PropertiesRepository propertiesRepository;

    // ====================================================
    // Обработка сообщений

    public int processPostings(List<BatchPosting> postings) {
        int cntError = 0;
        try {
            cntError = asyncProcessPostings(postings);
        } catch (Exception e) {
            auditController.error(Package, "Ошибка при обработке ручных операций", null, e);
        } finally {
            // pseudo online localization in DIRECT mode only
            if (DIRECT == operdayController.getOperday().getPdMode()) {
                recalculateBackvalue("по ручным операциям: " + StringUtils.listToString(postings, ","));
            }
        }
        return cntError;
    }

    /**
     * обработка пакета
     * @param action
     * @param oldStatus
     * @param withCheck
     * @return
     * @throws Exception
     */
    public BatchProcessResult processPackage(Long idPackage , BatchPostAction action, BatchPostStatus oldStatus, boolean withCheck) throws Exception {
//        BatchPackageState pkgState = BatchPackageState.getStateByPostingStatus(oldStatus);
        BatchPackageState pkgState = IS_WORKING;
        BatchProcessResult result = new BatchProcessResult(idPackage);
        result.setPackageState(pkgState);
        result.setProcessDate(BatchPostStatus.SIGNED.equals(oldStatus) || CONFIRM_NOW.equals(action) ? BT_NOW : BT_PAST);
        try {
            // устанавливаем статус пакету IS_WORKING
            updatePackageWorking(idPackage, pkgState);
            log.info(format("Обрабатывается пакет с ИД '%s' в транзакции: '%s'", idPackage, postingRepository.getTransactionKey()));

            // T0: читаем все провожки в пакете сортируем по ID
            // TODO выбирать только необработанные not COMPLETED
            List<BatchPosting> postings = packageRepository.getPostingsByPackageWithStatus(idPackage, oldStatus);
            log.info(format("Проводок '%s' в пакете с ИД '%s' в статусе '%s'", postings.size(), idPackage, oldStatus.name()));
            result.setTotalCount(postings.size());
            int errCount = asyncProcessPostings(postings);

            // устанавливаем статус на пакете по результатам обработки
            pkgState = PROCESSED;
            packageRepository.updatePackageStateProcessed(idPackage, pkgState, operdayController.getSystemDateTime());
            result.setPackageState(pkgState);
            result.setPackageStatistics(packageRepository.getPackageStatistics(idPackage), true);
            auditController.info(BatchOperation, result.getPackageProcessMessage());
            return result;
            // устанавливаем статус на пакете по результатам обработки
        } catch (Exception e) {
            auditController.error(BatchOperation, "Ошибка при обработке пакета ID = " + idPackage, "GL_BATPKG", idPackage.toString(), e);
            postingRepository.executeInNewTransaction(persistence ->
                    packageRepository.updatePackageStateProcessed(idPackage, ERROR, operdayController.getSystemDateTime()));
            result.setPackageStatistics(packageRepository.getPackageStatistics(idPackage), true);
            result.setErrorMessage(getErrorMessage(e));
            return result;
        } finally {
            // pseudo online localization in DIRECT mode only
            if (DIRECT == operdayController.getOperday().getPdMode()) {
                recalculateBackvalue("по пакету Excel " + idPackage);
            }
        }
    }

    /**
     * обработка ручных запросов со статусом SIGNED, SIGNEDDAGE
     * @return
     * @throws Exception
     */
    private int asyncProcessPostings(List<BatchPosting> postings) throws Exception {
        final int[] errorCount = {0};
        List<JpaAccessCallback<GLOperation>> callbacks = postings.stream().map(
                posting -> (JpaAccessCallback<GLOperation>) persistence ->
                        beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence1, connection) -> {
                            BatchPostStep step = posting.getStatus().getStep();
                            try {
                                return processPosting(posting, false);
                            } catch (Throwable e) {
                                log.error(format("Error on processing of batch posting '%s'", posting.getId()), e);
                                String errMessage = getErrorMessage(e);
                                errorCount[0]++;
                                postingRepository.executeInNewTransaction(persistence0 ->
                                        postingRepository.updatePostingStatusError(posting.getId(), errMessage,
                                                step.isControlStep() ? BatchPostStatus.ERRPROC : BatchPostStatus.ERRPROCDATE, 1));
                                return null;
                            }
                        })
        ).collect(Collectors.toList());
        asyncProcessor.asyncProcessPooled(callbacks, propertiesRepository
                .getNumber(PD_CONCURENCY.getName()).intValue(), 1, TimeUnit.HOURS);
        return errorCount[0];
    }

/*
    public void checkOperdayOnline() {
        if (Operday.OperdayPhase.ONLINE != operdayController.getOperday().getPhase()) {
            throw new DefaultApplicationException("Ошибка при создании операции",
                    new ValidationError(ErrorCode.OPERDAY_NOT_ONLINE, operdayController.getOperday().getPhase().name()));
        }
    }

    public void checkProcessingAllowed() {
        if (!operdayController.isProcessingAllowed()) {
            throw new DefaultApplicationException("Ошибка при создании операции",
                    new ValidationError(ErrorCode.OPERDAY_IN_SYNCHRO));
        }
    }
*/

    public GLManualOperation processPosting(BatchPosting posting0, boolean withCheck) {
        // проверка статуса опердня
/*
        if (withCheck) {
            checkOperdayOnline();
            checkProcessingAllowed();
        }
*/
        String oper = " операции по запросу ID = " + posting0.getId();
        auditController.info(ManualOperation, "Начало обработки" + oper, posting0);
        try {
            BatchPostStep step = posting0.getStatus().getStep();
            // устанавливаем статус обработки с проверкой, что еще не было обработки
            try {
                postingController.updatePostingStatusNew(posting0, BatchPostStatus.WORKING, null);    // TODO надо ???
            } catch (Throwable e) {
                throw new DefaultApplicationException(logPostingError(e, "Ошибка при обработке" + oper, posting0, ERRPROC, 1));
            }
            BatchPosting posting = postingRepository.findById(posting0.getId());

            GLManualOperation operation0;
            try {
                operation0 = operationRepository.executeInNewTransaction(persistence1 -> createOperation(posting));
            } catch (Throwable e) {
                throw new DefaultApplicationException(logPostingError(e, "Ошибка при создании" + oper, posting, ERRPROC, 1));
            }

            final Long operationId = operation0.getId();
            try {
                final GLManualOperation enrichedOperation = operationRepository.findById(GLManualOperation.class, operationId);
                operationRepository.executeInNewTransaction(persistence1 -> enrichmentOperation(enrichedOperation));
            } catch (Throwable e) {
                throw new DefaultApplicationException(
                        logOperationError(e, "Ошибка при заполнения данных" + oper, operation0, OperState.ERCHK));
            }

            try {
                GLManualOperation resultOperation = operationRepository.executeInNewTransaction(persistence1 -> {
                    final GLManualOperation processedOperation = operationRepository.findById(GLManualOperation.class, operationId);
                    if (processOperation(processedOperation)) {
                        auditController.info(ManualOperation, "Успешное завершение обработки"  + oper, processedOperation);
                    }
                    postingRepository.updatePostingStatusSuccess(posting.getId(), processedOperation);
                    return processedOperation;
                });
                return resultOperation;
            } catch (Throwable e) {
                throw new DefaultApplicationException(
                        logOperationError(e, "Ошибка при обработке данных" + oper, operation0, OperState.ERCHK));
            }
        } catch (DefaultApplicationException e) {
            auditController.error(ManualOperation, "Системная ошибка при обработке"  + oper, posting0, e);
            operationRepository.setRollbackOnly();
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Создает операцию по набору данных, введенных с экрана
     * Заполняет дополнительные параметры операции
     * @return
     * @throws Exception
     */
    private GLManualOperation createOperation(BatchPosting posting) throws Exception {
        List<ValidationError> errors = operationProcessor.validate(posting, new ValidationContext());

        if (errors.isEmpty()) {
            GLManualOperation operation = operationProcessor.createOperation(posting);       // создать операцию
            operation.setState(OperState.LOAD);

            return operationRepository.save(operation);     // сохранить операцию
        } else {
            throw new DefaultApplicationException(postingProcessor.validationErrorsToString(errors, null));
        }
    }

    /**
     * Заполняет дополнительные параметры операции
     * @param operation
     * @throws Exception
     */
    private GLManualOperation enrichmentOperation(GLManualOperation operation) throws Exception {
        ordinaryPostingProcessor.enrichment(operation);                                  // обогащение операции
        return (GLManualOperation)operationRepository.update(operation);
    }

    /**
     * Обрабатывает входящую операцию, введенную с экрана
     * @param operation         - операция
     * @return                  - true, если опеарция обработана, false - WTAC или ERCHK
     */
    private boolean processOperation(GLManualOperation operation) throws Exception {
        final Long operationId = operation.getId();
        GLOperationProcessor operationProcessor = etlPostingController.findOperationProcessor(operation);
        String msgCommon = format(" операции ID = %s", operation.getId());
        boolean toContinue = true;
/*
      // TODO исключаю как лишнее действие
        try {
            toContinue = validateOperation(operationProcessor, operation);
        } catch ( Throwable e ) {
            if (!(e instanceof DefaultApplicationException)) {
                String msg = "Ошибка валидации данных" + msgCommon;
                logOperationError(e, msg, operation, OperState.ERCHK);
            }
            throw new DefaultApplicationException(e.getMessage(), e);
        }
*/
        if ( toContinue ) {
            try {
                operationRepository.executeInNewTransaction(persistence -> updateOperation(operationProcessor, operation));
            } catch ( Throwable e ) {
                logOperationError(e, "Ошибка заполнения данных" + msgCommon, operation, OperState.ERCHK);
                throw new DefaultApplicationException(e.getMessage(), e);
            }
            try {
                finalOperation(operationProcessor, operation);
            } catch ( Throwable e ) {
                logOperationError(e, "Ошибка обработки" + msgCommon, operation, OperState.ERPOST);
                throw new DefaultApplicationException(e.getMessage(), e);
            }
        }
        return true;
    }

    private boolean validateOperation(GLOperationProcessor operationProcessor, GLManualOperation operation) {
        List<ValidationError> errors = operationProcessor.validate(operation, new ValidationContext());
        boolean toContinue = errors.isEmpty();
        if (!toContinue) {
            String msg = format("Ошибка валидации операции '%s'", operation.getId());
            String err = operationProcessor.validationErrorsToString(errors);
            auditController.error(ManualOperation, msg, operation, err);
            throw new DefaultApplicationException(err);
        }
        return toContinue;
    }

    private GLManualOperation updateOperation(GLOperationProcessor operationProcessor, GLManualOperation operation) throws Exception {
        operation.setProcDate(operdayController.getOperday().getCurrentDate());
        operation.setPstScheme(operationProcessor.getOperationType());              // определить схему операции
        operationProcessor.resolveOperationReference(operation);
        operationProcessor.setSpecificParameters(operation);
        return operationRepository.update(operation);
    }

    private void finalOperation(GLOperationProcessor operationProcessor, GLManualOperation operation) throws Exception {
        List<GLPosting> pstList = operationRepository
                .executeInNewTransaction(persistence -> operationProcessor.createPosting(operation));      // обработать операцию
        if (!pstList.isEmpty()) {                                                   // создать проводки
            operationRepository.executeInNewTransaction(persistence -> {
                operationProcessor.resolvePostingReference(operation, pstList);
                return null;
            });
            pdRepository.processPosting(pstList, operationProcessor.getSuccessStatus());                             // обработать / записать проводки
        }
    }

    public String logPostingError(Throwable e, String msg, BatchPosting posting,
                                  BatchPostStatus status, int errorCode) {
        String errMessage = getErrorMessage(e);
        try {
            // TODO здесь падает при обработке пакета!!
            operationRepository.executeInNewTransaction(persistence -> postingRepository.updatePostingStatusError(posting.getId(), errMessage,
                    postingController.getStepStatus(posting.getStatus().getStep(), status), errorCode));
        } catch (Throwable e1) {
            return msg + "\n" + e.getMessage();
        }
        auditController.error(BatchOperation, msg, posting, e);
        return errMessage;
    }

    private String logOperationError(Throwable e, String msg, GLOperation operation, OperState state) {
        log.error(msg, e);
        final String errMessage = format("%s '%s': %s. Обнаружена: %s\n'", msg, operation.getId(), getErrorMessage(e), initSource());
        log.error(errMessage, e);
        try {
            operationRepository.executeInNewTransaction(persistence -> {
                operationRepository.updateOperationStatusError(operation, state, substr(errMessage, 4000));
                return null;
            });
        } catch (Exception e1) {
            return errMessage + "\n" + e.getMessage();
        }
        auditController.error(ManualOperation, msg, operation, e);
        return errMessage;

    }

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class,
                PersistenceException.class);
    }

    public void updatePackageWorking(Long idPackage, BatchPackageState state) throws Exception {
        int cnt = postingRepository.executeInNewTransaction(persistence -> packageRepository.updatePackageStateNew(idPackage, state));
        if (cnt != 1) {
            throw new ValidationError(ErrorCode.PACKAGE_IS_WORKING, idPackage.toString(), state.name());
        }
    }

    /**
     * локализация и пересчет по журналу сформированному пакетом
     * @throws Exception
     */
    private void recalculateBackvalue(String ident) {
        try {
            log.info("Начало пересчета/локализации " + ident);
            beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) ->
            {journalController.recalculateBackvalueJournal(); return null;});
            log.info("Успешное окончание пересчета/локализации " + ident);
        } catch (Exception e) {
            auditController.error(Task, "Ошибка при пересчете остатков БС2/ локализации " + ident +
                    "\nЗаписи не прошедшие пересчет/локализацию в таблице GL_BVJRNL.STATE = 'ERROR'", null, e);
        }
    }


}
