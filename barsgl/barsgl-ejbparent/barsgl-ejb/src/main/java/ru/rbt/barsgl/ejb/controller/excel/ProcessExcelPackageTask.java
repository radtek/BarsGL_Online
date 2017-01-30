package ru.rbt.barsgl.ejb.controller.excel;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.etl.BatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.sec.AuditRecord;
import ru.rbt.barsgl.ejb.integr.bg.ManualOperationController;
import ru.rbt.barsgl.ejb.integr.bg.ManualPostingController;
import ru.rbt.barsgl.ejb.repository.BatchPackageRepository;
import ru.rbt.barsgl.ejb.repository.BatchPostingRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.JpaAccessCallback;
import ru.rbt.barsgl.ejbcore.job.InmemoryTask;
import ru.rbt.barsgl.ejbcore.repository.PropertiesRepository;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.ExceptionUtils;
import ru.rbt.barsgl.shared.enums.BatchPackageState;
import ru.rbt.barsgl.shared.enums.BatchPostAction;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.controller.excel.BatchProcessResult.BatchProcessDate.BT_NOW;
import static ru.rbt.barsgl.ejb.controller.excel.BatchProcessResult.BatchProcessDate.BT_PAST;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.BatchOperation;
import static ru.rbt.barsgl.ejb.props.PropertyName.PD_CONCURENCY;
import static ru.rbt.barsgl.shared.enums.BatchPackageState.ERROR;
import static ru.rbt.barsgl.shared.enums.BatchPackageState.PROCESSED;
import static ru.rbt.barsgl.shared.enums.BatchPostAction.CONFIRM_NOW;
import static ru.rbt.barsgl.shared.enums.BatchPostStatus.*;

/**
 * Created by Ivan Sevastyanov
 */
@Deprecated
public class ProcessExcelPackageTask implements InmemoryTask {
    private static final Logger logger = Logger.getLogger(ProcessExcelPackageTask.class.getName());

    public static final String ETL_PACKAGE_ID_PATH = "id_package";

    @Inject
    private BatchPackageRepository packageRepository;

    @EJB
    private BatchPostingRepository postingRepository;

    @EJB
    private ManualPostingController postingController;

    @EJB
    private ManualOperationController operationController;

    @Inject
    private OperdayController operdayController;

    @EJB
    private AuditController auditController;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private AsyncProcessor asyncProcessor;

    @EJB
    private PropertiesRepository propertiesRepository;

    @Override
    public void run(Map<String, Object> properties) throws Exception {
        execute ((Long) properties.get(ETL_PACKAGE_ID_PATH), CONFIRM_NOW, SIGNEDDATE, true);
    }

    public BatchProcessResult execute(Long idPackage, BatchPostAction action, BatchPostStatus oldStatus, boolean withCheck) throws Exception {
        BatchPackage loadedPackage = packageRepository.findById(idPackage);
        return beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
            return processPackage(loadedPackage, action, oldStatus, withCheck);
        }), 60 * 60);
    }

    public BatchProcessResult processPackage(BatchPackage loadedPackage, BatchPostAction action, BatchPostStatus oldStatus, boolean withCheck) throws Exception {
        Long idPackage = loadedPackage.getId();
        BatchPackageState pkgState = loadedPackage.getPackageState();     //  status.equals(BT_COMPLETE) ? PROCESSED : WAITPROC,
        BatchProcessResult result = new BatchProcessResult(idPackage, oldStatus);
        result.setProcessDate(BatchPostStatus.SIGNED.equals(oldStatus) || CONFIRM_NOW.equals(action) ? BT_NOW : BT_PAST);
        try {
            // T0: устанавливаем статус пакету IS_WORKING
            pkgState = PROCESSED;     //  status.equals(BT_COMPLETE) ? PROCESSED : WAITPROC,
            updatePackageWorking(idPackage, pkgState);
            logger.info(format("Обрабатывается пакет с ИД '%s' в транзакции: '%s'", idPackage, postingRepository.getTransactionKey()));

            // T0: читаем все провожки в пакете сортируем по ID
            // TODO выбирать только необработанные not COMPLETED
            List<BatchPosting> postings = packageRepository.getPostingsByPackage(idPackage, oldStatus);
            logger.info(format("Проводок '%s' в пакете с ИД '%s' в статусе '%s'", postings.size(), idPackage, oldStatus.name()));
            result.setTotalCount(postings.size());
            asyncProcessPostings(postings, action, withCheck, result);

            // устанавливаем статус на пакете по результатам обработки
            pkgState = PROCESSED;     //  status.equals(BT_COMPLETE) ? PROCESSED : WAITPROC,
            packageRepository.updatePackageStateProcessed(idPackage, pkgState,
                    operdayController.getSystemDateTime());
            result.setPackageStatistics(packageRepository.getPackageStatistics(idPackage), true);
            logger.info(result.getPackageProcessMessage());
            return result;
        } catch (Exception e) {
            auditController.error(BatchOperation, "Ошибка при обработке пакета ID = " + idPackage, loadedPackage, e);
            postingRepository.executeInNewTransaction(persistence ->
                packageRepository.updatePackageStateProcessed(idPackage, ERROR, operdayController.getSystemDateTime()));
            ;
            result.setPackageStatistics(packageRepository.getPackageStatistics(idPackage), true);
            result.setErrorMessage(getErrorMessage(e));
            return result;
        }
    }

    private BatchPackageState processPostings (List<BatchPosting> postings, BatchPostAction action, boolean withCheck, BatchProcessResult result) throws Exception {
        if (postings.size() == 0)
            return result.getPackageState();
        boolean toContinue = true;
        BatchPostStep step = postings.get(0).getStatus().getStep();
        for (BatchPosting posting : postings) {
            toContinue = postingRepository.executeInNewTransaction(persistence -> {
                ManualOperationWrapper wrapper = postingController.createStatusWrapper(posting);
                wrapper.setAction(action);
                try {
                    operationController.processPosting(posting, withCheck);
                    result.incCompleteCount();
                } catch (Throwable e) {
                    String errMessage = wrapper.getErrorMessage();
                    if (isOperdayError(e)) {
                        result.setErrorMessage(errMessage);
                        logger.log(Level.WARNING, result.getPackageProcessMessage(), e);
                        auditController.warning(AuditRecord.LogCode.BatchOperation, errMessage, posting, e );
                        return false;
                    } else {
                        result.incErrorCount();
                        logger.log(Level.SEVERE, format("Error on processing of batch posting '%s'", posting.getId()), e);
                        postingRepository.executeInNewTransaction(persistence0 ->
                                postingRepository.updatePostingStatusError(posting.getId(), errMessage, BatchPostStatus.ERRPROC, 1));
                    }
                }
                return true;
            });
            if (!toContinue)
                break;
        }
        if (result.getCompleteCount() > 0)
            result.setStatus(toContinue ? COMPLETED : WORKING);
        return result.getPackageState();
    }

    private BatchPackageState asyncProcessPostings (List<BatchPosting> postings, BatchPostAction action, boolean withCheck, BatchProcessResult result) throws Exception {
        // TODO не нужен result
        if (postings.size() == 0)
            return result.getPackageState();
        List<JpaAccessCallback<GLOperation>> callbacks = postings.stream().map(
                posting -> (JpaAccessCallback<GLOperation>) persistence ->
                        beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence1, connection) -> {
                            ManualOperationWrapper wrapper = postingController.createStatusWrapper(posting);
                            wrapper.setAction(action);
                            try {
                                GLOperation operation = operationController.processPosting(posting, withCheck);
                                result.incCompleteCount();
                                return operation;
                            } catch (Throwable e) {
                                String errMessage = wrapper.getErrorMessage();
                                if (isOperdayError(e)) {
                                    result.setErrorMessage(errMessage);
                                    logger.log(Level.WARNING, result.getPackageProcessMessage(), e);
                                    auditController.warning(AuditRecord.LogCode.BatchOperation, errMessage, posting, e );
                                    return null;
                                } else {
                                    result.incErrorCount();
                                    logger.log(Level.SEVERE, format("Error on processing of batch posting '%s'", posting.getId()), e);
                                    postingRepository.executeInNewTransaction(persistence0 ->
                                            postingRepository.updatePostingStatusError(posting.getId(), errMessage, BatchPostStatus.ERRPROC, 1));
                                    return null;
                                }
                            }
                        })
        ).collect(Collectors.toList());
        asyncProcessor.asyncProcessPooled(callbacks, propertiesRepository
                .getNumber(PD_CONCURENCY.getName()).intValue(), 1, TimeUnit.HOURS);
        result.setStatus(COMPLETED);
        return result.getPackageState();
    }


    private boolean isOperdayError(Throwable e) {
        ValidationError valError = ExceptionUtils.findException(e, ValidationError.class);
        if (null == valError)
            return false;
        else
            return valError.getCode().equals(ErrorCode.OPERDAY_NOT_ONLINE)
                    || valError.getCode().equals(ErrorCode.OPERDAY_IN_SYNCHRO);
    }

    private void setPackageState(BatchPackage batchPackage) {
        Long cntErrorPosting = packageRepository.selectOne(Long.class
                , "select count(p) cnt from BatchPosting p where p.packageId = ?1 and p.errorCode <> '0'"
                , batchPackage.getId());
        if (0 < cntErrorPosting) {
            packageRepository.updatePackageStateProcessed(batchPackage.getId(), ERROR, operdayController.getSystemDateTime());
        } else {
            packageRepository.updatePackageStateProcessed(batchPackage.getId(), PROCESSED, operdayController.getSystemDateTime());
        }
    }

    private String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable, DataTruncation.class, SQLException.class, DefaultApplicationException.class, ValidationError.class);
    }

    public void updatePackageWorking(Long idPackage, BatchPackageState state) throws Exception {
        int cnt = postingRepository.executeInNewTransaction(persistence -> packageRepository.updatePackageStateNew(idPackage, state));
        if (cnt != 1) {
            throw new ValidationError(ErrorCode.PACKAGE_IS_WORKING, idPackage.toString(), state.name());
        }
    }

}
