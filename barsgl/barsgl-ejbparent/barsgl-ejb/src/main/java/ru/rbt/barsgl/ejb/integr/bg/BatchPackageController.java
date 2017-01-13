package ru.rbt.barsgl.ejb.integr.bg;

import ru.rbt.barsgl.ejb.controller.excel.BatchProcessResult;
import ru.rbt.barsgl.ejb.controller.excel.ProcessExcelPackageTask;
import ru.rbt.barsgl.ejb.entity.etl.BatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.integr.oper.BatchPostingProcessor;
import ru.rbt.barsgl.ejb.repository.BatchPackageRepository;
import ru.rbt.barsgl.ejb.repository.BatchPostingRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.Assert;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.BatchPostAction;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.barsgl.shared.enums.SecurityActionCode;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.*;

import static ru.rbt.barsgl.ejb.controller.excel.BatchProcessResult.BatchProcessStatus.*;
import static ru.rbt.barsgl.ejb.entity.etl.BatchPackage.PackageState;
import static ru.rbt.barsgl.ejb.entity.etl.BatchPackage.PackageState.*;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.BatchOperation;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.listToString;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.PACKAGE_IS_PROCESSED;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.PACKAGE_SAME_NOT_ALLOWED;
import static ru.rbt.barsgl.ejbcore.validation.ValidationError.initSource;
import static ru.rbt.barsgl.shared.ExceptionUtils.getErrorMessage;
import static ru.rbt.barsgl.shared.enums.BatchPostAction.CONFIRM;
import static ru.rbt.barsgl.shared.enums.BatchPostAction.CONFIRM_NOW;
import static ru.rbt.barsgl.shared.enums.BatchPostStatus.*;

/**
 * Created by ER18837 on 22.06.16.
 */
@Stateless
@LocalBean
public class BatchPackageController {

    @EJB
    private BatchPostingRepository postingRepository;

    @Inject
    private BatchPackageRepository packageRepository;

    @Inject
    private ManualPostingController manualPostingController;

    @Inject
    private BatchPostingProcessor postingProcessor;

    @Inject
    private UserContext userContext;

    @EJB
    private AuditController auditController;

    @Inject
    private ProcessExcelPackageTask excelPackageTask;

    /**
     * Обработка запроса от интерфейса на обработку запроса на операцию
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> processPackageRq(ManualOperationWrapper wrapper) throws Exception {
        try {
            manualPostingController.checkOperdayOnline(wrapper.getErrorList());
            switch (wrapper.getAction()) {
                case CONTROL:           // на подпись - шаг 1 (CONTROL)
                    return forSignPackageRq(wrapper);
                case DELETE:            // удалить - шаг 1 (INVISIBLE)
                    return deletePackageRq(wrapper);
                case SIGN:              // подписать - шаг 2 (SIGNED, WAITDATE)
                    return authorizePackageRq(wrapper, BatchPostStep.HAND2);
                case CONFIRM:           // подтвердить прошлой датой - шаг 3 (SIGNEDDATE)
                    return confirmPackageRq(wrapper, BatchPostStep.HAND3);
                case CONFIRM_NOW:       // подтвердить текущей датой - шаг 3 (SIGNEDDATE)
                    return confirmPackageRq(wrapper, BatchPostStep.HAND3);
                case REFUSE:            // отказать - шаг 3 (REFUSEDATE)
                    return refusePackageRq(wrapper, BatchPostStatus.REFUSEDATE);
            }
            return new RpcRes_Base<ManualOperationWrapper>(
                    wrapper, true, "Неверное действие");
        } catch (Throwable e) {
            String msg = "Ошибка обработки пакета ID = " + wrapper.getPkgId();
            String errorMsg = wrapper.getErrorMessage();
            if (!isEmpty(errorMsg)) {
                auditController.error(BatchOperation, msg, null, errorMsg);
                return new RpcRes_Base<>(wrapper, true, errorMsg);
            } else { //           if (null == validationEx && ) { // null == defaultEx &&
                auditController.error(BatchOperation, msg, null, e);
                return new RpcRes_Base<>(wrapper, true, getErrorMessage(e, ValidationError.class));
            }
        }
    }

    private BatchPackage getPackageWithCheck(ManualOperationWrapper wrapper, PackageState ... packageState) throws SQLException {
        Long id = wrapper.getPkgId();
        final BatchPackage pkg = Optional.of(packageRepository.findById(BatchPackage.class, id))
                .orElseThrow(() -> new DefaultApplicationException("Не найден пакет с Id = " + id));
        Set<PackageState> states = new HashSet<PackageState>();
        if (packageState != null) {// !packageState.equals(pkg.getPackageState())) {
            states.addAll(Arrays.asList(packageState));
            if (!states.contains(pkg.getPackageState())) {
                ValidationError error = new ValidationError(ErrorCode.PACKAGE_BAD_STATUS, id.toString(), pkg.getPackageState().name(),
                        ERROR.equals(pkg.getPackageState()) ? "\nПакет содержит операции с ошибкой, обработка невозможна" : "");
                wrapper.getErrorList().addErrorDescription("", "", ValidationError.getErrorText(error.getMessage()), ValidationError.getErrorCode(error.getMessage()));
                throw new DefaultApplicationException(wrapper.getErrorMessage(), error);
            }
        }
        return pkg;
    }

    private void checkPostingsStatusNotError(Long packageId, BatchPostStatus ... enabledStatus) throws SQLException {
        String whereStatus = "";
        String whereStr = "";
        String errStr = "";
        if (enabledStatus.length != 0) {
            whereStatus = listToString(Arrays.asList(enabledStatus), "','");
            whereStr = " or STATE not in ('" + whereStatus + "')";
            errStr = " или запросы на операцию не в статусе " + whereStatus;
        }
        String sql = "select 1 from GL_BATPST where ID_PKG = ? and (VALUE(ECODE, 0) <> 0 or VALUE(EMSG, '') <> ''" + whereStr + ")";
        DataRecord res = postingRepository.selectFirst(sql, packageId);
        if (null != res) {
            throw new DefaultApplicationException("Пакет с ID = " + packageId + " содержит ошибки" + errStr);
        }
    }

    private void checkPostingsStatusNotIn(ManualOperationWrapper wrapper, BatchPostStatus ... enabledStatus) throws SQLException {
        Long id = wrapper.getPkgId();
        if (enabledStatus.length != 0) {
            String whereStatus = listToString(Arrays.asList(enabledStatus), "','");
            String sql = "select 1 from GL_BATPST where ID_PKG = ? and STATE in ('"
                    + listToString(Arrays.asList(enabledStatus), "','")  + "')";
            DataRecord res = postingRepository.selectFirst(sql, id);
            if (null != res) {
                throw new DefaultApplicationException("Пакет с ID = " + id
                        + " содержит запросы на операцию в статусе ('" + whereStatus + "')");
            }
        }
    }

    /**
     * Интерфейс: Передает запрос на операцию на подпись
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> forSignPackageRq(ManualOperationWrapper wrapper) throws Exception {
        BatchPackage pkg = getPackageWithCheck(wrapper, LOADED);
        BatchPosting posting0 = packageRepository.getOnePostingByPackage(pkg.getId(), INPUT);
        try {
            checkFilialPermission(wrapper.getPkgId(), wrapper.getUserId());
            postingProcessor.checkBackvaluePermission(posting0.getPostDate(), wrapper.getUserId());
        } catch (ValidationError e) {
            String msg = "Ошибка при передаче запроса на операцию на подпись";
            String errMessage = manualPostingController.addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.warning(BatchOperation, msg, new BatchPosting().getTableName(), wrapper.getId().toString(), e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
        checkPostingsStatusNotError(wrapper.getPkgId(), INPUT);
        boolean needHistory = createPackageHistory(pkg, BatchPostStep.HAND1, wrapper.getAction());
        return setPackageRqStatusControl(wrapper, pkg, needHistory);
    }

    private RpcRes_Base<ManualOperationWrapper> setPackageRqStatusControl(ManualOperationWrapper wrapper, BatchPackage pkg, boolean withChange) throws Exception {
        BatchPostStatus status = BatchPostStatus.CONTROL;
        BatchPackage.PackageState pkgStatus = ON_CONTROL;
        postingRepository.executeInNewTransaction(persistence -> {
            if (withChange) {
                packageRepository.updatePostingsStatusChange(pkg, userContext.getTimestamp(), userContext.getUserName(), status);
            } else {
                packageRepository.updatePostingsStatus(pkg, status);
            }
            packageRepository.updatePackageStateProcessed(pkg, pkgStatus, userContext.getTimestamp());
            return null;
        });
        wrapper.setStatus(status);
        String msg = "Пакет ID = " + wrapper.getPkgId() + " передан на подпись";
        auditController.info(BatchOperation, msg, new BatchPackage().getTableName(), wrapper.getPkgId().toString());
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    private RpcRes_Base<ManualOperationWrapper> setPackageRqStatusSigned(ManualOperationWrapper wrapper, BatchPackage pkg, BatchPostStatus oldStatus, BatchPostStatus newStatus  ) throws Exception {
        BatchProcessResult result = new BatchProcessResult(pkg.getId());
        postingRepository.executeInNewTransaction(persistence -> {
            String msg = "???";
            BatchPackage.PackageState pkgStatus = WORKING;
            BatchProcessResult.BatchProcessStatus resultStatus = BT_INITIAL;
            switch (newStatus) {
                case WAITDATE:
                    packageRepository.signedPostingsStatus(pkg, userContext.getTimestamp(), userContext.getUserName(), oldStatus, newStatus);
                    pkgStatus = ON_WAITDATE;
                    resultStatus = BT_WAITDATE;
                    break;
                case SIGNED:
                    packageRepository.signedPostingsStatus(pkg, userContext.getTimestamp(), userContext.getUserName(), oldStatus, newStatus);
                    pkgStatus = IS_SIGNED;
                    resultStatus = BT_SIGNED;
                    break;
                case SIGNEDDATE:
                    if (oldStatus.getStep().isControlStep()) {
                        packageRepository.signedConfirmPostingsStatus(pkg, userContext.getTimestamp(), userContext.getUserName(), oldStatus, newStatus);
                        resultStatus = BT_SIGNEDDATE;
                    } else {
                        packageRepository.confirmPostingsStatus(pkg, userContext.getTimestamp(), userContext.getUserName(), oldStatus, newStatus);
                        resultStatus = BT_CONFIRM;
                    }
                    pkgStatus = IS_SIGNEDDATE;
                    break;
                default:
                    Assert.isTrue(true, "Неверный статус");
            }
            packageRepository.updatePackageStateProcessed(pkg, pkgStatus, userContext.getTimestamp());
            result.setStatus(resultStatus);
            return msg;
        });
        wrapper.setStatus(newStatus);
        result.setStatistics(packageRepository.getPackageStatistics(pkg.getId()));
        String msg = result.getSignedMessage();
        wrapper.getErrorList().addErrorDescription("", "", msg, null);

        auditController.info(BatchOperation, msg, new BatchPackage().getTableName(), wrapper.getPkgId().toString());
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    /**
     * Интерфейс: Создает запрос на операцию без проверки прав (для тестов)
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> deletePackageRq(ManualOperationWrapper wrapper) throws Exception {
        BatchPackage pkg = getPackageWithCheck(wrapper, null);
        String userName = pkg.getUserName();
        postingRepository.executeInNewTransaction(persistence -> deletePackage(wrapper));
        String msg = "Пакет с ID = " + wrapper.getPkgId() + ", созданный пользователем '" + userName + "', удалён";
        auditController.info(BatchOperation, msg, new BatchPackage().getTableName(), wrapper.getPkgId().toString());
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    private BatchPackage deletePackage(ManualOperationWrapper wrapper) {
        try {
            BatchPackage pkg = getPackageWithCheck(wrapper, null);
            checkPostingsStatusNotIn(wrapper, COMPLETED, UNKNOWN, SIGNED, SIGNEDDATE);
            BatchPosting posting = packageRepository.getOnePostingByPackage(pkg.getId());
            if (postingProcessor.needHistory(posting, BatchPostStep.HAND1, wrapper.getAction())) {
                packageRepository.setPackageInvisible(pkg, userContext.getTimestamp(), userContext.getUserName()); // сделать пакет невидимым
                return packageRepository.findById(BatchPackage.class, pkg.getId());
            } else {
                packageRepository.deletePackage(pkg);   // удалить пакет
                return null;
            }

        } catch (Throwable e) {
            String msg = "Ошибка при удалении пакета, загруженного из файла";
            manualPostingController.addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.error(BatchOperation, msg, null, e);
            throw new DefaultApplicationException(msg, e);
        }
    }

    public RpcRes_Base<ManualOperationWrapper> authorizePackageRq(ManualOperationWrapper wrapper, BatchPostStep step) throws Exception {
        try {
            if (!step.isControlStep()) {
                throw new DefaultApplicationException("Неверный шаг оерации: " + step.name());
            }
            manualPostingController.checkProcessingAllowed(wrapper.getErrorList());
            BatchPackage pkg = getPackageWithCheck(wrapper, ON_CONTROL, WAITPROC);
            if (WAITPROC.equals(pkg.getPackageState())) {
                return reprocessPackageRq(wrapper, step);
            }
//            checkPostingsStatusNotError(wrapper.getPkgId()); //, CONTROL
            checkFilialPermission(wrapper.getPkgId(), wrapper.getUserId());
            BatchPostStatus oldStatus  = BatchPostStatus.CONTROL;
            BatchPosting posting0 = packageRepository.getOnePostingByPackage(pkg.getId(), oldStatus);
            if (null == posting0) {
                throw new ValidationError(PACKAGE_IS_PROCESSED, pkg.getId().toString(), oldStatus.name());
            }
            checkHand12Diff(posting0);
            excelPackageTask.updatePackageWorking(pkg, INPROGRESS);
            createPackageHistory(pkg, oldStatus.getStep(), wrapper.getAction());
            updatePostingsStatus(pkg, oldStatus, SIGNEDVIEW);
            oldStatus = SIGNEDVIEW;
            if (!YesNo.Y.equals(pkg.getMovementOff())) {
                excelPackageTask.checkPackageMovement(pkg, oldStatus);
            }
            Date operday = userContext.getCurrentDate();
            if (posting0.getPostDate().equals(operday)) {
                return authorizePackageRqInternal(wrapper, pkg, oldStatus, SIGNED);
            } else if (postingProcessor.checkActionEnable(wrapper.getUserId(), SecurityActionCode.OperHand3)) {
                return authorizePackageRqInternal(wrapper, pkg, oldStatus, SIGNEDDATE);
            } else {
                return setPackageRqStatusSigned(wrapper, pkg, oldStatus, WAITDATE);
            }
        } catch (ValidationError e) {
            String msg = "Ошибка при авторизации пакета, загруженного из файла";
            String errMessage = manualPostingController.addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.warning(BatchOperation, msg, null, e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
    }

    public RpcRes_Base<ManualOperationWrapper> confirmPackageRq(ManualOperationWrapper wrapper, BatchPostStep step) throws Exception {
        try {
            if (!step.isConfirmStep()) {
                throw new DefaultApplicationException("Неверный шаг оерации: " + step.name());
            }
            manualPostingController.checkProcessingAllowed(wrapper.getErrorList());
            BatchPackage pkg = getPackageWithCheck(wrapper, ON_WAITDATE, WAITPROC);
            if (WAITPROC.equals(pkg.getPackageState())) {
                return reprocessPackageRq(wrapper, step);
            }
//            checkPostingsStatusNotError(wrapper.getPkgId()); //, WAITDATE);
            checkFilialPermission(wrapper.getPkgId(), wrapper.getUserId());
            BatchPostStatus oldStatus = WAITDATE;
            BatchPosting posting0 = packageRepository.getOnePostingByPackage(pkg.getId(), oldStatus);
            if (null == posting0) {
                throw new ValidationError(PACKAGE_IS_PROCESSED, pkg.getId().toString(), oldStatus.name());
            }
            checkHand12Diff(posting0);
            excelPackageTask.updatePackageWorking(pkg, INPROGRESS);
            BatchPostStatus status = posting0.getStatus();
            createPackageHistory(pkg, status.getStep(), wrapper.getAction());
            if (CONFIRM_NOW.equals(wrapper.getAction())) {
                postingRepository.executeInNewTransaction(persistence -> {
                    packageRepository.setPackagePostingDate(pkg.getId(), userContext.getCurrentDate());
                    return null;
                });
            }
            return authorizePackageRqInternal(wrapper, pkg, oldStatus, SIGNEDDATE);
        } catch (ValidationError e) {
            String msg = "Ошибка при подтверждении даты пакета, загруженного из файла";
            String errMessage = manualPostingController.addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.warning(BatchOperation, msg, null, e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
    }

    public RpcRes_Base<ManualOperationWrapper> reprocessPackageRq(ManualOperationWrapper wrapper, BatchPostStep step) throws Exception {
        try {
            manualPostingController.checkProcessingAllowed(wrapper.getErrorList());
            BatchPackage pkg = getPackageWithCheck(wrapper, WAITPROC);
//            checkPostingsStatusNotError(wrapper.getPkgId()); //, CONTROL
            checkFilialPermission(wrapper.getPkgId(), wrapper.getUserId());
            BatchPostStatus oldStatus  = step.isControlStep() ? SIGNED : SIGNEDDATE;
            BatchPosting posting0 = packageRepository.getOnePostingByPackage(pkg.getId(), oldStatus);
            if (null == posting0) {
                throw new ValidationError(PACKAGE_IS_PROCESSED, pkg.getId().toString(), oldStatus.name());
            }
            excelPackageTask.updatePackageWorking(pkg, INPROGRESS);
            createPackageHistory(pkg, oldStatus.getStep(), wrapper.getAction());

            if (step.isConfirmStep()) {
                wrapper.setAction(userContext.getCurrentDate().equals(posting0.getPostDate()) ? CONFIRM_NOW : CONFIRM);
            }
            return reprocessPackageRqInternal(wrapper, pkg, oldStatus);
        } catch (ValidationError e) {
            String msg = "Ошибка при переобработке пакета, загруженного из файла";
            String errMessage = manualPostingController.addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.warning(BatchOperation, msg, null, e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
    }

    public RpcRes_Base<ManualOperationWrapper> authorizePackageRqInternal(ManualOperationWrapper wrapper, BatchPackage pkg, BatchPostStatus oldStatus, BatchPostStatus newStatus) throws Exception {
        auditController.info(BatchOperation, "Начало обработки пакета ID = " + wrapper.getPkgId(), "GL_BATPKG", wrapper.getPkgId().toString());
        setPackageRqStatusSigned(wrapper, pkg, oldStatus, newStatus);
        BatchProcessResult result = excelPackageTask.execute(wrapper.getPkgId(), wrapper.getAction(), newStatus, true);
        String msg = result.getProcessMessage();
        auditController.info(BatchOperation, msg, "GL_BATPKG", wrapper.getPkgId().toString());
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    public RpcRes_Base<ManualOperationWrapper> reprocessPackageRqInternal(ManualOperationWrapper wrapper, BatchPackage pkg, BatchPostStatus oldStatus) throws Exception {
        auditController.info(BatchOperation, "Начало повторной обработки пакета ID = " + wrapper.getPkgId(), "GL_BATPKG", wrapper.getPkgId().toString());
        BatchProcessResult result = excelPackageTask.execute(wrapper.getPkgId(), wrapper.getAction(), oldStatus, true);
        String msg = result.getProcessMessage();
        auditController.info(BatchOperation, msg, "GL_BATPKG", wrapper.getPkgId().toString());
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    public RpcRes_Base<ManualOperationWrapper> refusePackageRq(ManualOperationWrapper wrapper, BatchPostStatus status) throws Exception {
        BatchPostStatus status0 = WAITDATE;
        BatchPackage pkg = getPackageWithCheck(wrapper, ON_CONTROL, ON_WAITDATE);
//        checkPostingsStatusNotError(wrapper.getPkgId(), status0);
        String msg;
        int momementCount = 0;
        if ((momementCount = packageRepository.getMovementCount(pkg.getId())) > 0) {
            msg = "Нельзя вернуть пакет с ID = " + wrapper.getPkgId() + " на доработку,\n" +
                    "по нему есть операции (" + momementCount + ") с успешным запросом в сервис движений";
        } else {
            postingRepository.executeInNewTransaction(persistence -> {
                createPackageHistory(pkg, status0.getStep(), wrapper.getAction());
                packageRepository.refusePackageStatus(pkg, userContext.getTimestamp(), userContext.getUserName(), wrapper.getReasonOfDeny(), status);
                return null;
            });
            wrapper.setStatus(status);
            msg = "Пакет с ID = " + wrapper.getPkgId() + " возвращён на доработку";
        }
        auditController.info(BatchOperation, msg, new BatchPackage().getTableName(), wrapper.getPkgId().toString());
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    private boolean createPackageHistory(BatchPackage pkg, BatchPostStep step, BatchPostAction action) throws Exception {
        BatchPosting posting = packageRepository.getOnePostingByPackage(pkg.getId());
        boolean needHistory = postingProcessor.needHistory(posting, step, action);
        if (needHistory) {
            postingRepository.executeInNewTransaction(persistence -> packageRepository.createPackageHistory(pkg));
        }
        return needHistory;
    }

    public void checkFilialPermission(Long packageId, Long userId) throws Exception {
        if (!packageRepository.checkFilialPermission(packageId, userId)) {
            throw new ValidationError(ErrorCode.PACKAGE_FILIAL_NOT_ALLOWED);
        }
    }

    public void checkHand12Diff(BatchPosting posting) {
        BatchPostStep step = posting.getStatus().getStep();
        if (step.isControlStep()) {
            if (userContext.getUserName().equals(posting.getUserName())) {
                throw new ValidationError(PACKAGE_SAME_NOT_ALLOWED, posting.getPackageId().toString());
            }
        }
    }

    public void updatePostingsStatus(BatchPackage pkg, BatchPostStatus statusOld, BatchPostStatus statusNew) throws Exception {
        postingRepository.executeInNewTransaction(persistence -> packageRepository.updatePostingsStatus(pkg, statusOld, SIGNEDVIEW) );
    }


}
