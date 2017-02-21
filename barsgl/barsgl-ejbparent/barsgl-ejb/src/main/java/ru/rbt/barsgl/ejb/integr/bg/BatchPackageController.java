package ru.rbt.barsgl.ejb.integr.bg;

import ru.rbt.barsgl.ejb.controller.excel.BatchProcessResult;
import ru.rbt.barsgl.ejb.entity.etl.BatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.integr.oper.BatchPostingProcessor;
import ru.rbt.barsgl.ejb.integr.oper.MovementCreateProcessor;
import ru.rbt.barsgl.ejb.integr.struct.MovementCreateData;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.barsgl.ejb.repository.BatchPackageRepository;
import ru.rbt.barsgl.ejb.repository.BatchPostingRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.ejbcore.repository.PropertiesRepository;
import ru.rbt.barsgl.ejbcore.util.StringUtils;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.Assert;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.BatchPackageState;
import ru.rbt.barsgl.shared.enums.BatchPostAction;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.BatchPostStep;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.*;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.controller.excel.BatchProcessResult.BatchProcessDate.*;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.BatchOperation;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.listToString;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.*;
import static ru.rbt.barsgl.ejbcore.validation.ValidationError.initSource;
import static ru.rbt.barsgl.shared.enums.BatchPackageState.*;
import static ru.rbt.barsgl.shared.enums.BatchPostAction.CONFIRM_NOW;
import static ru.rbt.barsgl.shared.enums.BatchPostStatus.*;

/**
 * Created by ER18837 on 22.06.16.
 */
@Stateless
@LocalBean
public class BatchPackageController {
    private final long MAX_ROWS = 1000;

    @EJB
    private BatchPostingRepository postingRepository;

    @Inject
    private BatchPackageRepository packageRepository;

    @Inject
    private ManualPostingController postingController;

    @Inject
    private BatchPostingProcessor postingProcessor;

    @Inject
    private UserContext userContext;

    @EJB
    private AuditController auditController;

    @Inject
    private MovementCreateProcessor movementProcessor;

    @EJB
    private PropertiesRepository propertiesRepository;

    public int getMaxRowsExcel() {
        return (int)(long)propertiesRepository.getNumberDef(PropertyName.BATPKG_MAXROWS.getName(), MAX_ROWS);
    }

    /**
     * Обработка запроса от интерфейса на обработку запроса на операцию
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> processPackageRq(ManualOperationWrapper wrapper) throws Exception {
        try {
//            postingController.checkOperdayOnline(wrapper.getErrorList());
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
                case STATISTICS:            // статистика
                    return statisticsPackageRq(wrapper);
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
                return new RpcRes_Base<>(wrapper, true, msg + "\n" +  postingController.getErrorMessage(e));
            }
        }
    }

    private BatchPackage getPackageWithCheck(ManualOperationWrapper wrapper, BatchPackageState ... packageState) throws SQLException {
        Long id = wrapper.getPkgId();
        final BatchPackage pkg = packageRepository.findById(id);
        if (null == pkg ) {
                throw new DefaultApplicationException("Не найден пакет с Id = " + id + ". Обновите информацию");
        }
        Set<BatchPackageState> states = new HashSet<BatchPackageState>();
        if (packageState != null) {// !packageState.equals(pkg.getPackageState())) {
            states.addAll(Arrays.asList(packageState));
            if (!states.contains(pkg.getPackageState())) {
                ValidationError error = new ValidationError(ErrorCode.PACKAGE_BAD_STATUS, id.toString(),
                        wrapper.getAction().getLabel(), pkg.getPackageState().name(), pkg.getPackageState().getLabel());
//                        ERROR.equals(pkg.getPackageState()) ? "\nПакет содержит операции с ошибкой, обработка невозможна" : "");
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

    public RpcRes_Base<ManualOperationWrapper> statisticsPackageRq(ManualOperationWrapper wrapper) throws Exception {
        Long pkgId = wrapper.getPkgId();
        BatchProcessResult result = new BatchProcessResult(pkgId);
        result.setPackageStatistics(packageRepository.getPackageStatistics(pkgId), true);
        String msg = result.getPackageProcessMessage();
        wrapper.getErrorList().addErrorDescription("", "", msg, null);
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    /**
     * Интерфейс: Передает запрос на операцию на подпись
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> forSignPackageRq(ManualOperationWrapper wrapper) throws Exception {
        BatchPackage pkg = getPackageWithCheck(wrapper, LOADED);
        BatchPosting posting0 = postingRepository.getOnePostingByPackageWithStatus(pkg.getId(), INPUT);
        try {
            checkFilialPermission(wrapper.getPkgId(), wrapper.getUserId());
            postingProcessor.checkBackvaluePermission(posting0.getPostDate(), wrapper.getUserId());
        } catch (ValidationError e) {
            String msg = "Ошибка при передаче запроса на операцию на подпись";
            String errMessage = postingController.addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.warning(BatchOperation, msg, new BatchPosting().getTableName(), wrapper.getId().toString(), e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
        checkPostingsStatusNotError(wrapper.getPkgId(), INPUT);
        boolean needHistory = createPackageHistory(pkg, BatchPostStep.HAND1, wrapper.getAction());
        return setPackageRqStatusControl(wrapper, pkg, needHistory);
    }

    private RpcRes_Base<ManualOperationWrapper> setPackageRqStatusControl(ManualOperationWrapper wrapper, BatchPackage pkg, boolean withChange) throws Exception {
        Long pkgId = pkg.getId();
        BatchPostStatus status = BatchPostStatus.CONTROL;
        String statusIn = StringUtils.arrayToString(new BatchPostStatus[]{INPUT}, ",", "'");
        BatchPackageState pkgStatus = ON_CONTROL;
        postingRepository.executeInNewTransaction(persistence -> {
            if (withChange) {
                packageRepository.updatePostingsStatusChange(pkgId, userContext.getTimestamp(), userContext.getUserName(), status, statusIn);
            } else {
                packageRepository.updatePostingsStatus(pkgId, status, statusIn);
            }
            BatchPackageState pkgStatusOld = pkg.getPackageState();
            int cnt = packageRepository.updatePackageState(pkg.getId(), pkgStatus, pkgStatusOld);
            if (0 == cnt)
                throw  new ValidationError(PACKAGE_STATUS_WRONG, pkgStatusOld.name(), pkgStatusOld.getLabel());
            return null;
        });
        wrapper.setStatus(status);
        String msg = "Пакет ID = " + wrapper.getPkgId() + " передан на подпись";
        auditController.info(BatchOperation, msg, new BatchPackage().getTableName(), wrapper.getPkgId().toString());
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    public RpcRes_Base<ManualOperationWrapper> setPackageRqStatusSigned(ManualOperationWrapper wrapper, String userName, BatchPackage pkg, BatchPackageState pkgStateOld,
                                                                        BatchPostStatus newStatus, BatchPostStatus logStatus, BatchPostStatus ... oldStats) throws Exception {
        Assert.notNull(oldStats, "Текущий статус пустой!");
        Long pkgId = pkg.getId();
        BatchPostStep step = oldStats[0].getStep();
        BatchProcessResult result = new BatchProcessResult(pkg.getId(), newStatus);
        postingRepository.executeInNewTransaction(persistence -> {
            String statusIn = StringUtils.arrayToString(oldStats, ",", "'");
            BatchPostAction action = wrapper.getAction();
            int cnt = 0;
            Date timestamp = userContext.getTimestamp();
            switch (newStatus) {
                case SIGNEDVIEW:
                    if (SIGNEDDATE.equals(logStatus)) {
                        cnt = packageRepository.signedConfirmPostingsStatus(pkgId, pkgStateOld, timestamp, userName, newStatus, statusIn);
                    } else {    // SIGNED or WAITDATE
                        cnt = packageRepository.signedPostingsStatus(pkgId, pkgStateOld, timestamp, userName, newStatus, statusIn);
                    }
                    break;
                case SIGNED:
                case WAITDATE:
                    cnt = packageRepository.signedPostingsStatus(pkgId, pkgStateOld, timestamp, userName, newStatus, statusIn);
                    break;
                case SIGNEDDATE:
                    if (step.isControlStep()) {
                        cnt = packageRepository.signedConfirmPostingsStatus(pkgId, pkgStateOld, timestamp, userName, newStatus, statusIn);
                        result.setProcessDate(BT_PAST);
                    } else {
                        cnt = packageRepository.confirmPostingsStatus(pkgId, pkgStateOld, timestamp, userName, newStatus, statusIn);
                        result.setStatus(CONFIRM);
                        result.setProcessDate(CONFIRM_NOW.equals(action) ? BT_NOW : BT_PAST);
                    }
                    break;
                default:
                    Assert.isTrue(true, "Неверный статус: " + newStatus.name());
            }
            if (0 == cnt)
                throw new ValidationError(PACKAGE_STATUS_WRONG, pkgStateOld.name(), pkgStateOld.getLabel());
            return null;
        });
        wrapper.setStatus(newStatus);
        result.setPackageStatistics(packageRepository.getPackageStatistics(pkg.getId()), false);
        String msg = result.getPackageSignedMessage();
//        wrapper.getErrorList().addErrorDescription("", "", msg, null);

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
            checkPostingsStatusNotIn(wrapper, COMPLETED, WORKING, SIGNED, SIGNEDDATE);
            BatchPosting posting = postingRepository.getOnePostingByPackage(pkg.getId());
            if (postingProcessor.needHistory(posting, BatchPostStep.HAND1, wrapper.getAction())) {
                packageRepository.setPackageInvisible(pkg.getId(), userContext.getTimestamp(), userContext.getUserName()); // сделать пакет невидимым
                return packageRepository.findById(pkg.getId());
            } else {
                packageRepository.deletePackage(pkg.getId());   // удалить пакет
                return null;
            }

        } catch (Throwable e) {
            String msg = "Ошибка при удалении пакета, загруженного из файла";
            postingController.addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.error(BatchOperation, msg, null, e);
            throw new DefaultApplicationException(msg, e);
        }
    }

    public RpcRes_Base<ManualOperationWrapper> authorizePackageRq(ManualOperationWrapper wrapper, BatchPostStep step) throws Exception {
        try {
            if (!step.isControlStep()) {
                throw new DefaultApplicationException("Неверный шаг оерации: " + step.name());
            }
//            postingController.checkProcessingAllowed(wrapper.getErrorList());
            BatchPackage pkg = getPackageWithCheck(wrapper, ON_CONTROL);
//            checkPostingsStatusNotError(wrapper.getPkgId()); //, CONTROL
            checkFilialPermission(wrapper.getPkgId(), wrapper.getUserId());
            BatchPostStatus oldStatus  = BatchPostStatus.CONTROL;
            BatchPosting posting0 = postingRepository.getOnePostingByPackageWithStatus(pkg.getId(), oldStatus);
            if (null == posting0) {
                throw new ValidationError(PACKAGE_IS_PROCESSED, pkg.getId().toString(), oldStatus.name());
            }
            checkHand12Diff(posting0);

            // TODO переделать
            updatePackageStateNew(pkg, IS_SIGNEDVIEW);
            createPackageHistory(pkg, oldStatus.getStep(), wrapper.getAction());
            BatchPostStatus newStatus = postingController.getOperationRqStatusSigned(wrapper.getUserId(), pkg.getPostDate());
            setPackageRqStatusSigned(wrapper, userContext.getUserName(), pkg, IS_SIGNEDVIEW, SIGNEDVIEW, newStatus, oldStatus);
            oldStatus = SIGNEDVIEW;
            List<Long> ctrlPostingsId = Collections.emptyList();
            if (!YesNo.Y.equals(pkg.getMovementOff())) {        // движение НЕ отключено
                // получить список контролируемых проводок
                ctrlPostingsId = packageRepository.getPostingsControllable(pkg.getId(), getMaxRowsExcel());
            }
            if (!ctrlPostingsId.isEmpty()) {
                // послать движения
                return sendMovements(pkg, ctrlPostingsId, wrapper, newStatus);
            } else {
                // изменить статус
                return setPackageRqStatusSigned(wrapper, userContext.getUserName(), pkg, IS_SIGNEDVIEW, newStatus, newStatus, oldStatus);
            }
        } catch (ValidationError e) {
            String msg = "Ошибка при авторизации пакета, загруженного из файла";
            String errMessage = postingController.addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.error(BatchOperation, msg, null, e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
    }

    public RpcRes_Base<ManualOperationWrapper> sendMovements(BatchPackage pkg, List<Long> postingsId, ManualOperationWrapper pkgWrapper,
                                                             BatchPostStatus nextStatus) throws Exception {
        try {
            auditController.info(BatchOperation, format("Пакет ID = '%d': запросов для отправки в сервис движений %d", pkg.getId(), postingsId.size()));
            if (postingsId.size() == 0) {
                return new RpcRes_Base<>( pkgWrapper, false, "Нет запросов для отправки в сервис");
            }
            BatchPostStep step = BatchPostStep.NOHAND;
            List<MovementCreateData> movementData = new ArrayList<>();
            for (Long postingId : postingsId) {
                BatchPosting posting = postingRepository.findById(postingId);
                step = posting.getStatus().getStep();
                if (null != posting.getReceiveTimestamp()) {
                    String msg = String.format("По запоросу ID = '%s' уже было выполнено движение в MovementCreate: '%s', время: '%s'",
                            posting.getId().toString(), posting.getMovementId(), postingController.timeFormat.format(posting.getReceiveTimestamp()));
                    auditController.info(BatchOperation, msg, posting);
                    continue;
                }
                ManualOperationWrapper wrapper = postingController.createStatusWrapper(posting);
                MovementCreateData data = postingController.createMovementData(posting, wrapper);
                String movementId = data.getMessageUUID();
                postingController.setOperationRqStatusSend(wrapper, movementId, WAITSRV, nextStatus);
                movementData.add(data);
            }
            updatePackageState(pkg, ON_WAITSRV, IS_SIGNEDVIEW);
            BatchProcessResult result = new BatchProcessResult(pkg.getId(), nextStatus);
            result.setProcessDate(SIGNEDDATE.equals(nextStatus) ? BT_PAST : BT_EMPTY);
            try {
                movementProcessor.sendRequests(movementData);
                result.setPackageStatistics(packageRepository.getPackageStatistics(pkg.getId()), false);
                return new RpcRes_Base<>( pkgWrapper, false, result.getPackageSendMessage());
            } catch (Throwable e) {     // ошибка отправки
                for (MovementCreateData data : movementData) {
                    if (MovementCreateData.StateEnum.ERROR == data.getState() )
                        postingController.receiveMovement(data);
                }
                if (null == postingRepository.getOnePostingByPackageWithoutStatus(pkg.getId(), ERRSRV)) { // все с ошибками
                    updatePackageState(pkg, ERROR, ON_WAITSRV);
                }
                result.setPackageStatistics(packageRepository.getPackageStatistics(pkg.getId()), true);
                result.setPackageState(IS_ERRSRV);
                return new RpcRes_Base<>( pkgWrapper, false, result.getPackageProcessMessage());
            }
        }
        catch (Throwable e) {
            pkg = packageRepository.findById(pkg.getId());
            updatePackageState(pkg, ERROR, pkg.getPackageState());
            throw new DefaultApplicationException(String.format("Ошибка при обращении к сервису движений для пакета ID = %s", pkg.getId()));
        }
    }

    public RpcRes_Base<ManualOperationWrapper> confirmPackageRq(ManualOperationWrapper wrapper, BatchPostStep step) throws Exception {
        try {
            if (!step.isConfirmStep()) {
                throw new DefaultApplicationException("Неверный шаг оерации: " + step.name());
            }
//            postingController.checkProcessingAllowed(wrapper.getErrorList());
            BatchPackage pkg0 = getPackageWithCheck(wrapper, ON_WAITDATE);
//            checkPostingsStatusNotError(wrapper.getPkgId()); //, WAITDATE);
            checkFilialPermission(wrapper.getPkgId(), wrapper.getUserId());
            BatchPostStatus oldStatus = WAITDATE;
            BatchPosting posting0 = postingRepository.getOnePostingByPackageWithStatus(pkg0.getId(), oldStatus);
            if (null == posting0) {
                throw new ValidationError(PACKAGE_IS_PROCESSED, pkg0.getId().toString(), oldStatus.name());
            }
            checkHand12Diff(posting0);

            updatePackageStateNew(pkg0, IS_SIGNEDDATE);
            createPackageHistory(pkg0, posting0.getStatus().getStep(), wrapper.getAction());
            if (CONFIRM_NOW.equals(wrapper.getAction())) {
                postingRepository.executeInNewTransaction(persistence -> {
                    packageRepository.setPackagePostingDate(pkg0.getId(), userContext.getCurrentDate());
                    return null;
                });
            }
            return setPackageRqStatusSigned(wrapper, userContext.getUserName(), pkg0, IS_SIGNEDDATE, SIGNEDDATE, SIGNEDDATE, oldStatus);
        } catch (ValidationError e) {
            String msg = "Ошибка при подтверждении даты пакета, загруженного из файла";
            String errMessage = postingController.addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.warning(BatchOperation, msg, null, e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
    }

/*
    public RpcRes_Base<ManualOperationWrapper> reprocessPackageRq(ManualOperationWrapper wrapper, BatchPostStep step) throws Exception {
        try {
            postingController.checkProcessingAllowed(wrapper.getErrorList());
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
            String errMessage = postingController.addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.warning(BatchOperation, msg, null, e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
    }
*/

/*
    public RpcRes_Base<ManualOperationWrapper> authorizePackageRqInternal(ManualOperationWrapper wrapper, BatchPackage pkg, BatchPostStatus oldStatus, BatchPostStatus newStatus) throws Exception {
        auditController.info(BatchOperation, "Начало обработки пакета ID = " + wrapper.getPkgId(), "GL_BATPKG", wrapper.getPkgId().toString());
        setPackageRqStatusSigned(wrapper, pkg, oldStatus, newStatus);
        BatchProcessResult result = excelPackageTask.execute(wrapper.getPkgId(), wrapper.getAction(), newStatus, true);
        String msg = result.getPackageProcessMessage();
        auditController.info(BatchOperation, msg, "GL_BATPKG", wrapper.getPkgId().toString());
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    public RpcRes_Base<ManualOperationWrapper> reprocessPackageRqInternal(ManualOperationWrapper wrapper, BatchPackage pkg, BatchPostStatus oldStatus) throws Exception {
        auditController.info(BatchOperation, "Начало повторной обработки пакета ID = " + wrapper.getPkgId(), "GL_BATPKG", wrapper.getPkgId().toString());
        BatchProcessResult result = excelPackageTask.execute(wrapper.getPkgId(), wrapper.getAction(), oldStatus, true);
        String msg = result.getPackageProcessMessage();
        auditController.info(BatchOperation, msg, "GL_BATPKG", wrapper.getPkgId().toString());
        return new RpcRes_Base<>(wrapper, false, msg);
    }
*/

    public RpcRes_Base<ManualOperationWrapper> refusePackageRq(ManualOperationWrapper wrapper, BatchPostStatus status) throws Exception {
        BatchPostStatus status0 = WAITDATE;
        BatchPackage pkg0 = getPackageWithCheck(wrapper, ON_CONTROL, ON_WAITDATE);
//        checkPostingsStatusNotError(wrapper.getPkgId(), status0);
        String msg;
        int momementCount = 0;
        if ((momementCount = packageRepository.getMovementCount(pkg0.getId())) > 0) {
            msg = "Нельзя вернуть пакет с ID = " + wrapper.getPkgId() + " на доработку,\n" +
                    "по нему есть операции (" + momementCount + ") с успешным запросом в сервис движений";
        } else {
            postingRepository.executeInNewTransaction(persistence -> {
                BatchPackageState stateOld = pkg0.getPackageState();
                createPackageHistory(pkg0, status0.getStep(), wrapper.getAction());
                int cnt = packageRepository.refusePackageStatus(pkg0.getId(), stateOld, userContext.getTimestamp(), userContext.getUserName(),
                        wrapper.getReasonOfDeny(), status);
                if (0 == cnt)
                    throw  new ValidationError(PACKAGE_STATUS_WRONG, stateOld.name(), stateOld.getLabel());
                return null;
            });
            wrapper.setStatus(status);
            msg = "Пакет с ID = " + wrapper.getPkgId() + " возвращён на доработку";
        }
        auditController.info(BatchOperation, msg, new BatchPackage().getTableName(), wrapper.getPkgId().toString());
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    private boolean createPackageHistory(BatchPackage pkg, BatchPostStep step, BatchPostAction action) throws Exception {
        BatchPosting posting = postingRepository.getOnePostingByPackage(pkg.getId());
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

    public void updatePackageState(BatchPackage pkg, BatchPackageState stateNew, BatchPackageState stateOld) throws Exception {
        int cnt = postingRepository.executeInNewTransaction(persistence -> packageRepository.updatePackageState(pkg.getId(), stateNew, stateOld));
        if (0 == cnt)
            throw  new ValidationError(PACKAGE_STATUS_WRONG, stateOld.name(), stateOld.getLabel());
    }

    public void updatePackageStateError(BatchPackage pkg, BatchPackageState stateNew, BatchPackageState stateOld) throws Exception {
        updatePackageState(pkg, stateNew, stateOld);
        auditController.warning(BatchOperation, format("Пакет ID = %s: перевелен в состояние ошибки", pkg.getId(), "Нет операций для обработки"));
    }

    public void updatePackageStateNew(BatchPackage pkg, BatchPackageState state) throws Exception {
        int cnt = postingRepository.executeInNewTransaction(persistence -> packageRepository.updatePackageStateNew(pkg.getId(), state));
        if (cnt != 1) {
            throw new ValidationError(ErrorCode.PACKAGE_IS_WORKING, pkg.getId().toString(), state.name());
        }
    }

}
