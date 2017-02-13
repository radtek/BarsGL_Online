package ru.rbt.barsgl.ejb.controller.excel;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.etl.BatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.integr.bg.ManualPostingController;
import ru.rbt.barsgl.ejb.repository.BatchPackageRepository;
import ru.rbt.barsgl.ejb.repository.BatchPostingRepository;
import ru.rbt.barsgl.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.job.InmemoryTask;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.ExceptionUtils;
import ru.rbt.barsgl.shared.enums.BatchPostAction;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.controller.excel.BatchProcessResult.BatchProcessDate.BT_NOW;
import static ru.rbt.barsgl.ejb.controller.excel.BatchProcessResult.BatchProcessDate.BT_PAST;
import static ru.rbt.barsgl.ejb.controller.excel.BatchProcessResult.BatchProcessStatus.*;
import static ru.rbt.barsgl.ejb.entity.etl.BatchPackage.PackageState.*;
import static ru.rbt.barsgl.audit.entity.AuditRecord.LogCode.Package;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.barsgl.shared.enums.BatchPostAction.CONFIRM_NOW;
import static ru.rbt.barsgl.shared.enums.BatchPostStatus.SIGNEDDATE;

/**
 * Created by Ivan Sevastyanov
 */
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
    private BeanManagedProcessor beanManagedProcessor;

    @Inject
    private OperdayController operdayController;

    @EJB
    private AuditController auditController;

    @EJB
    private AsyncProcessor asyncProcessor;

    @Override
    public void run(Map<String, Object> properties) throws Exception {
        execute ((Long) properties.get(ETL_PACKAGE_ID_PATH), CONFIRM_NOW, SIGNEDDATE, true);
    }

    public BatchProcessResult execute(Long idPackage, BatchPostAction action, BatchPostStatus oldStatus, boolean withCheck) throws Exception {
        BatchPackage loadedPackage = packageRepository.findById(BatchPackage.class, idPackage);
        return beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
            return processPackage(loadedPackage, action, oldStatus, withCheck);
        }), 60 * 60);
    }

    public BatchProcessResult processPackage(BatchPackage loadedPackage, BatchPostAction action, BatchPostStatus oldStatus, boolean withCheck) throws Exception {
        BatchProcessResult result = new BatchProcessResult(loadedPackage.getId());
        result.setProcessDate(!CONFIRM_NOW.equals(action) && BatchPostStatus.SIGNEDDATE.equals(oldStatus) ? BT_PAST : BT_NOW);
        try {
            // T0: устанавливаем статус пакету WORKING
            updatePackageWorking(loadedPackage, WORKING);
            logger.info(format("Обрабатывается пакет с ИД '%s' в транзакции: '%s'", loadedPackage.getId(), postingRepository.getTransactionKey()));

            // T0: читаем все провожки в пакете сортируем по ID
            // TODO выбирать только необработанные not COMPLETED
            List<BatchPosting> postings = packageRepository.getPostingsByPackage(loadedPackage.getId(), oldStatus);
            logger.info(format("Проводок '%s' в пакете с ИД '%s' в статусе '%s'", postings.size(), loadedPackage.getId(), oldStatus.name()));
            result.setTotalCount(postings.size());
            BatchProcessResult.BatchProcessStatus status = processPostings(postings, action, withCheck, result);

            // устанавливаем статус на пакете по результатам обработки
            packageRepository.updatePackageStateProcessed(loadedPackage,
                    status.equals(BT_COMPLETE) ? PROCESSED : WAITPROC,
                    operdayController.getSystemDateTime());
            result.setStatistics(packageRepository.getPackageStatistics(loadedPackage.getId()));
            logger.info(result.getProcessMessage());
            return result;
        } catch (Exception e) {
            auditController.error(Package, "Ошибка при обработке пакета ID = " + loadedPackage.getId(), loadedPackage, e);
            postingRepository.executeInNewTransaction(persistence ->
                packageRepository.updatePackageStateProcessed(loadedPackage, ERROR, operdayController.getSystemDateTime()));
            ;
            result.setErrorMessage(getErrorMessage(e));
            return result;
        }
    }

    private BatchProcessResult.BatchProcessStatus processPostings (List<BatchPosting> postings, BatchPostAction action, boolean withCheck, BatchProcessResult result) throws Exception {
        result.setStatus(BT_INITIAL);
        if (postings.size() == 0)
            return result.getStatus();
        boolean toContinue = true;
        for (BatchPosting posting : postings) {
            toContinue = postingRepository.executeInNewTransaction(persistence -> {
                ManualOperationWrapper wrapper = postingController.createStatusWrapper(posting);
                wrapper.setAction(action);
                try {
                    postingController.processMessage(posting, wrapper, withCheck);
                    result.incCompleteCount();
                } catch (Throwable e) {
                    String errMessage = wrapper.getErrorMessage();
                    if (isOperdayError(e)) {
                        result.setErrorMessage(errMessage);
                        logger.log(Level.WARNING, result.getMessage(), e);
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
            result.setStatus(toContinue ? BT_COMPLETE : BT_INTERRUPT);
        return result.getStatus();
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
            packageRepository.updatePackageStateProcessed(batchPackage, ERROR, operdayController.getSystemDateTime());
        } else {
            packageRepository.updatePackageStateProcessed(batchPackage, PROCESSED, operdayController.getSystemDateTime());
        }
    }

    private String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable, DataTruncation.class, SQLException.class, DefaultApplicationException.class, ValidationError.class);
    }

    public List<String> checkPackageMovement(BatchPackage loadedPackage, BatchPostStatus oldStatus) {
        try {
            // T0: устанавливаем статус пакету WORKING
            updatePackageWorking(loadedPackage, WORKING);
            logger.info(format("Проверка пакет с ИД '%s' в сервере движений: '%s'", loadedPackage.getId(), postingRepository.getTransactionKey()));

            // T0: читаем все провожки в пакете сортируем по ID
            // TODO выбирать только необработанные not COMPLETED
            List<BatchPosting> postings = packageRepository.getPostingsByPackage(loadedPackage.getId(), oldStatus);
            logger.info(format("Проводок '%s' в пакете с ИД '%s' в статусе '%s'", postings.size(), loadedPackage.getId(), oldStatus.name()));
            List<String> errorList = checkPostingMovement(postings);

            logger.info(format("Обработка пакета с ИД '%s' завершена. Ошибок по контролируемым счетам: %s",
                    loadedPackage.getId(), errorList.size()));
            return errorList;
        } catch (Exception e) {
            auditController.error(Package, "Ошибка при обработке пакета ID = " + loadedPackage.getId(), loadedPackage, e);
            List<String> errorList = new ArrayList<String>();
            errorList.add(e.getMessage());
            packageRepository.updatePackageStateProcessed(loadedPackage, ERROR, operdayController.getSystemDateTime());
            return errorList;
        }
    }

    private List<String> checkPostingMovement (List<BatchPosting> postings) throws Exception {
        List<String> errorList = new ArrayList<String>();
        for (BatchPosting posting : postings) {
            postingRepository.executeInNewTransaction(persistence -> {
                if (!isEmpty(posting.getMovementId()))
                    return null;        // уже было движение
                ManualOperationWrapper wrapper = postingController.createStatusWrapper(posting);
                try {
                    postingController.sendMovement(posting, wrapper);
                } catch (Throwable e) {
                    Throwable th =  ExceptionUtils.findException(e, ValidationError.class);
                    if (null == th) {
                        logger.log(Level.SEVERE, format("Error on processing of batch posting '%s'", posting.getId()), e);
                        postingRepository.executeInNewTransaction(persistence0 ->
                                postingRepository.updatePostingStatusError(posting.getId(), wrapper.getErrorMessage(), BatchPostStatus.ERRPROC, 1));
                    }
                }
                return null;
            });
        }
        return errorList;
    }

    public void updatePackageWorking(BatchPackage pkg, BatchPackage.PackageState state) throws Exception {
        int cnt = postingRepository.executeInNewTransaction(persistence -> packageRepository.updatePackageStateNew(pkg, state));
        if (cnt != 1) {
            throw new ValidationError(ErrorCode.PACKAGE_IS_WORKING, pkg.getId().toString(), state.name());
        }
    }

/*    public ManualOperationWrapper createOperationWrapper(BatchPosting posting) {
        ManualOperationWrapper wrapper = new ManualOperationWrapper();

        wrapper.setId(posting.getId());
        wrapper.setDealSrc(posting.getSourcePosting());
        wrapper.setDealId(posting.getDealId());
        wrapper.setSubdealId(posting.getSubDealId());
        wrapper.setPaymentRefernce(posting.getPaymentRefernce());
        wrapper.setPostDateStr(new SimpleDateFormat(wrapper.dateFormat).format(posting.getValueDate()));
        wrapper.setValueDateStr(wrapper.getPostDateStr());

        wrapper.setCurrencyCredit(posting.getCurrencyCredit().getCurrencyCode());
        wrapper.setCurrencyDebit(posting.getCurrencyDebit().getCurrencyCode());
        // TO: передаем пустой филиал, должен быть взят из счета
//        operation.setFilialCredit(posting.getFilialCredit());
//        operation.setFilialDebit(posting.getFilialDebit());

        wrapper.setAccountCredit(posting.getAccountCredit());
        wrapper.setAccountDebit(posting.getAccountDebit());

        wrapper.setAmountCredit(posting.getAmountCredit());
        wrapper.setAmountDebit(posting.getAmountDebit());
        wrapper.setAmountRu(posting.getAmountRu());

        wrapper.setRusNarrativeLong(posting.getRusNarrativeLong());
        wrapper.setNarrative(posting.getNarrative());
        wrapper.setDeptId(posting.getDeptId());
        wrapper.setProfitCenter(posting.getProfitCenter());
        wrapper.setCorrection(YesNo.Y.equals(posting.getIsCorrection()));
        wrapper.setInputMethod(InputMethod.F);
        return wrapper;
    }*/
}
