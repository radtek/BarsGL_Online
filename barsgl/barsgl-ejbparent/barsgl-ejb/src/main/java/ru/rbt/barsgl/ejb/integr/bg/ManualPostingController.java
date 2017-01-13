package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.BackvalueJournalController;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLManualOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.sec.AuditRecord;
import ru.rbt.barsgl.ejb.integr.oper.BatchPostingProcessor;
import ru.rbt.barsgl.ejb.integr.oper.ManualOperationProcessor;
import ru.rbt.barsgl.ejb.integr.oper.MovementCreateProcessor;
import ru.rbt.barsgl.ejb.integr.oper.OrdinaryPostingProcessor;
import ru.rbt.barsgl.ejb.integr.pst.GLOperationProcessor;
import ru.rbt.barsgl.ejb.integr.struct.MovementCreateData;
import ru.rbt.barsgl.ejb.repository.BatchPostingRepository;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.barsgl.ejb.repository.ManualOperationRepository;
import ru.rbt.barsgl.ejb.repository.PdRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbcore.util.StringUtils;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.Assert;
import ru.rbt.barsgl.shared.ErrorList;
import ru.rbt.barsgl.shared.ExceptionUtils;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.*;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.*;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.substr;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.*;
import static ru.rbt.barsgl.ejbcore.validation.ValidationError.initSource;
import static ru.rbt.barsgl.shared.enums.BatchPostAction.CONFIRM;
import static ru.rbt.barsgl.shared.enums.BatchPostAction.CONFIRM_NOW;
import static ru.rbt.barsgl.shared.enums.BatchPostStatus.*;

/**
 * Created by ER18837 on 13.08.15.
 */
@Stateless
@LocalBean
public class ManualPostingController {

    private static final Logger log = Logger.getLogger(EtlPostingController.class);
    private static final String postingName = "GL_BATPST";
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    @EJB
    private AuditController auditController;

    @EJB
    private EtlPostingController etlPostingController;

    @Inject
    private BatchPostingProcessor postingProcessor;

    @Inject
    private ManualOperationProcessor operationProcessor;

    @EJB
    private BatchPostingRepository postingRepository;

    @EJB
    private ManualOperationRepository operationRepository;

    @Inject
    GLAccountRepository accountRepository;

    @Inject
    private OrdinaryPostingProcessor ordinaryPostingProcessor;

    @Inject
    private OperdayController operdayController;

    @EJB
    private PdRepository pdRepository;

    @Inject
    private BackvalueJournalController journalController;

    @Inject
    private MovementCreateProcessor movementProcessor = new MovementCreateProcessor();  // TODO ??

    @Inject
    private UserContext userContext;

    @Inject
    private DateUtils dateUtils;

    /**
     * Обработка запроса от интерфейса на обработку запроса на операцию
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> processOperationRq(ManualOperationWrapper wrapper) throws Exception {
        try {
            checkOperdayOnline(wrapper.getErrorList());
            switch (wrapper.getAction()) {
                case SAVE:              // сохранить - шаг 1 (INPUT)
                    return saveOperationRq(wrapper, BatchPostStatus.INPUT);
                case SAVE_CONTROL:      // сохранить и на подпись - шаг 1 (CONTROL)
                    return saveOperationRq(wrapper, BatchPostStatus.CONTROL);
                case CONTROL:           // на подпись - шаг 1 (CONTROL)
                    return forSignOperationRq(wrapper);
                case DELETE:            // удалить - шаг 1 (INVISIBLE)
                    return deleteOperationRq(wrapper);
                case UPDATE:            // изменить - шаг 1 (INPUT), шаг 2 (CONTROL)
                    return updateOperationRq(wrapper, wrapper.getStatus().getStep().isInputStep() ? BatchPostStatus.INPUT : BatchPostStatus.CONTROL);
                case UPDATE_CONTROL:    // изменить и на подпись - шаг 1 (CONTROL)
                    return updateOperationRq(wrapper, BatchPostStatus.CONTROL);
                case UPDATE_SIGN:       // изменить и подписать - шаг 2 (SIGNED, WAITDATE)
                    {
                        updateOperationRq(wrapper, BatchPostStatus.CONTROL);
                        return authorizeOperationRq(wrapper);
                    }
                case SIGN:              // подписать - шаг 2 (SIGNED, WAITDATE)
                    return authorizeOperationRq(wrapper);
                case CONFIRM:           // подтвердить прошлой датой - шаг 3 (SIGNEDDATE)
                    return confirmOperationRq(wrapper);
                case CONFIRM_NOW:       // подтвердить текущей датой - шаг 3 (SIGNEDDATE)
                    return confirmOperationRq(wrapper);
                case REFUSE:            // отказать - шаг 2 (REFUSE), 3 (REFUSEDATE)
                    return refuseOperationRq(wrapper, wrapper.getStatus().getStep().isControlStep() ? BatchPostStatus.REFUSE : BatchPostStatus.REFUSEDATE);    // TODO
            }
            return new RpcRes_Base<ManualOperationWrapper>(
                    wrapper, true, "Неверное действие");
        } catch (Throwable e) {
            String errorMsg = wrapper.getErrorMessage();
            String msg = "Ошибка обработки запроса на операцию";
            if (null != wrapper.getId())
                msg += " ID = " + wrapper.getId();
            if (!isEmpty(errorMsg)) {
                auditController.error(ManualOperation, msg, postingName, getWrapperId(wrapper), errorMsg);
                return new RpcRes_Base<>(wrapper, true, errorMsg);
            } else { //           if (null == validationEx && ) { // null == defaultEx &&
                addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
                auditController.error(ManualOperation, msg, postingName, getWrapperId(wrapper), e);
                return new RpcRes_Base<>(wrapper, true, e.getMessage());
            }
        }
    }

    /**
     * Проверяет право пользователя на работу с филиалом и проводками BackValue
     * @param wrapper
     * @throws Exception
     */
    public void checkUserPermission(ManualOperationWrapper wrapper) throws Exception {
        postingProcessor.checkFilialPermission(wrapper.getFilialDebit(), wrapper.getFilialCredit(), wrapper.getUserId());
        postingProcessor.checkBackvaluePermission(dateUtils.onlyDateParse(wrapper.getPostDateStr()), wrapper.getUserId());
    }

    private void validateOperationRq(ManualOperationWrapper wrapper) {
        List<ValidationError> errors = postingProcessor.validate(wrapper, new ValidationContext());

        if (!errors.isEmpty()) {
            throw new DefaultApplicationException(postingProcessor.validationErrorMessage(errors, wrapper.getErrorList()));
        }
    }

    /**
     * Интерфейс: Создает запрос на операцию с проверкой прав
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> saveOperationRq(ManualOperationWrapper wrapper, BatchPostStatus newStatus) throws Exception {
        try {
            checkUserPermission(wrapper);
        } catch (ValidationError e) {
            String msg = "Ошибка при сохранении запроса на операцию";
            if (null != wrapper.getId())
                msg += " ID = " + wrapper.getId();
            String errMessage = addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.warning(ManualOperation, msg, postingName, getWrapperId(wrapper), e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
        return saveOperationRqInternal(wrapper, newStatus);
    }

    /**
     * Интерфейс: Создает запрос на операцию без проверки прав (для тестов)
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> saveOperationRqInternal(ManualOperationWrapper wrapper, BatchPostStatus newStatus) throws Exception {
            BatchPosting posting = operationRepository.executeInNewTransaction(persistence -> createPosting(wrapper, newStatus));
            wrapper.setId(posting.getId());
            wrapper.setStatus(posting.getStatus());
            String msg = "Запрос на операцию ID = " + wrapper.getId() +
                    (BatchPostAction.SAVE_CONTROL.equals(wrapper.getAction()) ? " передан на подпись" : " сохранён");
            auditController.info(ManualOperation, msg, posting);
            return new RpcRes_Base<>(wrapper, false, msg);
    }

    /**
     * Интерфейс: Создает запрос на операцию с проверкой прав
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> updateOperationRq(ManualOperationWrapper wrapper, BatchPostStatus newStatus) throws Exception {
        try {
            checkUserPermission(wrapper);
        } catch (ValidationError e) {
            String msg = "Ошибка при изменении запроса на операцию ID = " + wrapper.getId();
            String errMessage = addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.warning(ManualOperation, msg, postingName, getWrapperId(wrapper), e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
        return updateOperationRqInternal(wrapper, newStatus);
    }

    /**
     * Интерфейс: Создает запрос на операцию без проверки прав (для тестов)
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> updateOperationRqInternal(ManualOperationWrapper wrapper, BatchPostStatus newStatus) throws Exception {
            BatchPostStatus status = wrapper.getStatus().getStep().isControlStep() ? CONTROL : newStatus;
            BatchPosting posting = operationRepository.executeInNewTransaction(persistence -> updatePosting(wrapper, status));
            wrapper.setId(posting.getId());
            wrapper.setStatus(posting.getStatus());
            String msg = "Запрос на операцию ID = " + wrapper.getId() +
                    (BatchPostAction.UPDATE_CONTROL.equals(wrapper.getAction()) ? " изменён и передан на подпись" : " изменён");
            auditController.info(ManualOperation, msg, posting);
            return new RpcRes_Base<>(wrapper, false, msg);
    }

    /**
     * Интерфейс: Создает запрос на операцию без проверки прав (для тестов)
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> deleteOperationRq(ManualOperationWrapper wrapper) throws Exception {
            operationRepository.executeInNewTransaction(persistence -> deletePosting(wrapper));
            String msg = "Запрос на операцию ID = " + wrapper.getId() + " удалён";
            auditController.info(ManualOperation, msg, postingName, getWrapperId(wrapper));
            return new RpcRes_Base<>(wrapper, false, msg);
    }

    /**
     * Интерфейс: Передает запрос на операцию на подпись
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> forSignOperationRq(ManualOperationWrapper wrapper) throws Exception {
        try {
            checkUserPermission(wrapper);
        } catch (ValidationError e) {
            String msg = "Ошибка при передаче запроса на операцию ID = " + wrapper.getId() + " на подпись";
            String errMessage = addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.warning(ManualOperation, msg, postingName, getWrapperId(wrapper), e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
        return forSignOperationRqInternal(wrapper);
    }

    public RpcRes_Base<ManualOperationWrapper> forSignOperationRqInternal(ManualOperationWrapper wrapper) throws Exception {
        BatchPosting posting0 = getPostingWithCheck(wrapper, INPUT);
        BatchPosting posting = createPostingHistory(posting0, BatchPostStep.HAND1, wrapper.getAction());
        return setOperationRqStatusControl(wrapper, posting, posting != posting0);
    }

    public RpcRes_Base<ManualOperationWrapper> refuseOperationRq(ManualOperationWrapper wrapper, BatchPostStatus status) throws Exception {
        BatchPosting posting0 = getPostingWithCheck(wrapper, CONTROL, REFUSEDATE, ERRSRV, REFUSESRV, WAITDATE);
        String msg;
        if (hasMovement(posting0)) {
            msg = "Нельзя вернуть запрос на операцию ID = " + wrapper.getId() + " на доработку,\nпо нему выполнен успешный запрос в сервис движений";
        } else {
            operationRepository.executeInNewTransaction(persistence -> {
                BatchPosting posting = createPostingHistory(posting0, wrapper.getStatus().getStep(), wrapper.getAction());
                postingRepository.refusePostingStatus(posting, wrapper.getReasonOfDeny(), userContext.getTimestamp(), userContext.getUserName(), status);
                return null;
            });
            wrapper.setStatus(status);
            msg = "Запрос на операцию ID = " + wrapper.getId() + " возвращён на доработку";
        }
        auditController.info(ManualOperation, msg, postingName, getWrapperId(wrapper));
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    public RpcRes_Base<ManualOperationWrapper> authorizeOperationRq(ManualOperationWrapper wrapper) throws Exception {
        try {
            checkProcessingAllowed(wrapper.getErrorList());
            BatchPostStep step = wrapper.getStatus().getStep();
            if (!step.isControlStep()) {
                return new RpcRes_Base<>(wrapper, false, "Неверный шаг оерации: " + step.name());
            }
            if (SIGNED.equals(wrapper.getStatus())) {
                return reprocessOperationRq(wrapper);
            }
            postingProcessor.checkFilialPermission(wrapper.getFilialDebit(), wrapper.getFilialCredit(), wrapper.getUserId());
            BatchPosting posting0 = getPostingWithCheck(wrapper, CONTROL);
            checkHand12Diff(posting0);
            Date operday = operdayController.getOperday().getCurrentDate();
            BatchPostStatus newStatus;
            if (posting0.getPostDate().equals(operday)) {    // текущий день
                newStatus = SIGNED;
            } else if (postingProcessor.checkActionEnable(wrapper.getUserId(), SecurityActionCode.OperHand3)) {
                newStatus = SIGNEDDATE;      // архивный день и 3-я рука
            } else {
                newStatus = WAITDATE;         // ждать 3-ю руку
            }
            BatchPosting posting = createPostingHistory(posting0, wrapper.getStatus().getStep(), wrapper.getAction());
            // тестируем статус - что никто еще не менял
            updatePostingStatusNew(posting0, SIGNEDVIEW);
            // устанавливаем статус
            RpcRes_Base<ManualOperationWrapper>  result = setOperationRqStatusSigned(wrapper, posting, newStatus, false);
            // посылка запроса в MovementCreate
            sendMovement(posting, wrapper);
            if (WAITDATE.equals(newStatus)) {       // ждать 3 руку
                result = setOperationRqStatusSigned(wrapper, posting, newStatus, true);
                return result;
            } else {                                // авторизовать
                return authorizeOperationRqInternal(posting, wrapper, newStatus);
            }
        } catch (ValidationError e) {
                String msg = "Ошибка при авторизации запроса на операцию ID = " + wrapper.getId();
                String errMessage = addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
                auditController.error(ManualOperation, msg, postingName, getWrapperId(wrapper), e);
                return new RpcRes_Base<>( wrapper, true, errMessage);
        }
    }

    public RpcRes_Base<ManualOperationWrapper> confirmOperationRq(ManualOperationWrapper wrapper) throws Exception {
        try {
            checkProcessingAllowed(wrapper.getErrorList());
            BatchPostStep step = wrapper.getStatus().getStep();
            if (!step.isConfirmStep()) {
                return new RpcRes_Base<>(wrapper, false, "Неверный шаг оерации: " + step.name());
            }
            if (SIGNEDDATE.equals(wrapper.getStatus())) {
                return reprocessOperationRq(wrapper);
            }
            postingProcessor.checkFilialPermission(wrapper.getFilialDebit(), wrapper.getFilialCredit(), wrapper.getUserId());
            BatchPosting posting0 = getPostingWithCheck(wrapper, WAITDATE);
            BatchPosting posting = createPostingHistory(posting0, wrapper.getStatus().getStep(), wrapper.getAction());
            BatchPostStatus newStatus = SIGNEDDATE;
            // тестируем статус - что никто еще не менял
            updatePostingStatusNew(posting0, newStatus);
            // устанавливаем статус
            RpcRes_Base<ManualOperationWrapper>  result = setOperationRqStatusSigned(wrapper, posting, newStatus, true);
            Date operday = operdayController.getOperday().getCurrentDate();
            if (BatchPostAction.CONFIRM_NOW.equals(wrapper.getAction())) {
                wrapper.setPostDateStr(operday == null ? null : dateUtils.onlyDateString(operday));
                postingRepository.executeInNewTransaction(persistence -> {
                    postingRepository.setPostingDate(posting, userContext.getCurrentDate());
                    return null;
                });
            }
            return authorizeOperationRqInternal(posting, wrapper, newStatus);
        } catch (ValidationError e) {
            String msg = "Ошибка при подтверждении даты запроса на операцию ID = " + wrapper.getId();
            String errMessage = addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.error(ManualOperation, msg, postingName, getWrapperId(wrapper), e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
    }

    public RpcRes_Base<ManualOperationWrapper> reprocessOperationRq(ManualOperationWrapper wrapper) throws Exception {
        try {
            checkProcessingAllowed(wrapper.getErrorList());
            BatchPostStep step = wrapper.getStatus().getStep();
            BatchPosting posting0 = getPostingWithCheck(wrapper, SIGNED, SIGNEDDATE);
            BatchPosting posting = createPostingHistory(posting0, wrapper.getStatus().getStep(), wrapper.getAction());
            Date operday = operdayController.getOperday().getCurrentDate();
            if (step.isConfirmStep()) {
                wrapper.setAction(userContext.getCurrentDate().equals(posting0.getPostDate()) ? CONFIRM_NOW : CONFIRM);
            }
            return reprocessOperationRqInternal(posting, wrapper);
        } catch (ValidationError e) {
            String msg = "Ошибка при переобработки операции ID = " + wrapper.getId();
            String errMessage = addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.error(ManualOperation, msg, postingName, getWrapperId(wrapper), e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
    }

    public RpcRes_Base<ManualOperationWrapper> authorizeOperationRqInternal(BatchPosting posting, ManualOperationWrapper wrapper, BatchPostStatus newStatus) throws Exception {
            setOperationRqStatusSigned(wrapper, posting, newStatus, true);      // установить статус пожтверждения
            auditController.info(ManualOperation, "Запрос на операцию ID = " + wrapper.getId() + " передан на обработку", posting);
            GLManualOperation operation = processMessage(posting, wrapper, true);
            wrapper.setOperationId(operation.getId());
            wrapper.setPstScheme(operation.getPstScheme().getName());
            String msg = "Операция ID = " + operation.getId() + " по запросу ID = " + posting.getId() +  " создана успешно" +
                    (operation.getPostDate().equals(userContext.getCurrentDate()) ? " в текущем опердне" : " с прошлой датой");
            auditController.info(ManualOperation, msg, operation);
            return new RpcRes_Base<>(wrapper, false, msg);
    }

    public RpcRes_Base<ManualOperationWrapper> reprocessOperationRqInternal(BatchPosting posting, ManualOperationWrapper wrapper) throws Exception {
        auditController.info(ManualOperation, "Запрос на операцию ID = " + wrapper.getId() + " передан на повторную обработку", posting);
        GLManualOperation operation = processMessage(posting, wrapper, true);
        wrapper.setOperationId(operation.getId());
        wrapper.setPstScheme(operation.getPstScheme().getName());
        String msg = "Операция ID = " + operation.getId() + " по запросу ID = " + posting.getId() +  " создана успешно" +
                (operation.getPostDate().equals(userContext.getCurrentDate()) ? " в текущем опердне" : " с прошлой датой");
        auditController.info(ManualOperation, msg, operation);
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    public RpcRes_Base<ManualOperationWrapper> setOperationRqStatusControl(ManualOperationWrapper wrapper, BatchPosting posting, boolean withChange) throws Exception {
        final BatchPostStatus status = CONTROL;
        operationRepository.executeInNewTransaction(persistence -> {
            if (withChange) {
                postingRepository.updatePostingStatusChanged(posting, userContext.getTimestamp(), userContext.getUserName(), status);
            } else {
                postingRepository.updatePostingStatus(posting, status);
            }
            return null;
        });
        wrapper.setStatus(status);
        String msg = "Запрос на операцию ID = " + wrapper.getId() + " передан на подпись";
        auditController.info(ManualOperation, msg, postingName, getWrapperId(wrapper));
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    public RpcRes_Base<ManualOperationWrapper> setOperationRqStatusSigned(ManualOperationWrapper wrapper, BatchPosting posting, BatchPostStatus newStatus, boolean signed) throws Exception {
        BatchPostStatus dbStatus = signed ? newStatus : SIGNEDVIEW;
        String message = operationRepository.executeInNewTransaction(persistence -> {    // TODO надо ??
            String msg = "???";
            switch (newStatus) {
                case SIGNED:
                    postingRepository.signedPostingStatus(posting, userContext.getTimestamp(), userContext.getUserName(), dbStatus);
                    msg = " подписан";
                    break;
                case SIGNEDDATE:
                    if (wrapper.getStatus().getStep().isControlStep()) {
                        postingRepository.signedConfirmPostingStatus(posting, userContext.getTimestamp(), userContext.getUserName(), dbStatus);
                        msg = " подписан с подтверждением даты проводки";
                    } else {
                        postingRepository.confirmPostingStatus(posting, userContext.getTimestamp(), userContext.getUserName(), dbStatus);
                        msg = " подтверждена дата проводки";
                    }
                    break;
                case WAITDATE:
                    postingRepository.signedPostingStatus(posting, userContext.getTimestamp(), userContext.getUserName(), dbStatus);
                    msg = " подписан и передан на подтверждение даты проводки";
                    break;
                default:
                    Assert.isTrue(true, "Неверный статус");
            }
            return signed ? msg : " визуально проверен";
        });
        wrapper.setStatus(dbStatus);
        String msg = "Запрос на операцию ID = " + wrapper.getId() + message;
        wrapper.getErrorList().addErrorDescription("", "", msg, null);
        auditController.info(ManualOperation, msg, postingName, getWrapperId(wrapper));
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    private boolean hasMovement(BatchPosting posting) {
        //TODO не вполне корректная проверка. Нет однозначного признака прохождения movement
        return (null != posting.getMovementId() && null != posting.getMovementTimestamp()); // && !(ERRSRV.equals(posting.getStatus()) || REFUSESRV.equals(posting.getStatus()));
    }

    /**
     * Создает запрос на операцию с предварительной валидацией
     * @param wrapper
     * @return
     * @throws Exception
     */
    private BatchPosting createPosting(ManualOperationWrapper wrapper, BatchPostStatus status) throws Exception {
        validateOperationRq(wrapper);

        try {
            BatchPosting posting = postingProcessor.createPosting(wrapper);       // создать операцию
            posting.setStatus(status);
            return postingRepository.save(posting);     // сохранить входящую операцию

        } catch (Throwable e) {
            String msg = "Ошибка при создании запроса на операцию";
            addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.error(ManualOperation, msg, null, e);
            throw new DefaultApplicationException(msg, e);
        }
    }

    /**
     * Изменяет запрос на операцию с предварительной валидацией
     * @param wrapper
     * @return
     * @throws Exception
     */
    private BatchPosting updatePosting(ManualOperationWrapper wrapper, BatchPostStatus status) throws Exception {
        validateOperationRq(wrapper);

        try {
            BatchPosting posting0 = getPostingWithCheck(wrapper);
            BatchPosting posting = createPostingHistory(posting0, BatchPostStep.HAND1, wrapper.getAction());
            // редактировать операцию
            posting = postingProcessor.updatePosting(wrapper, posting);
            posting.setStatus(status);
            return postingRepository.update(posting);     // сохранить входящую операцию

        } catch (Throwable e) {
            String msg = "Ошибка при изменении запроса на операцию ID = " + wrapper.getId();
            addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.error(ManualOperation, msg, postingName, getWrapperId(wrapper), e);
            throw new DefaultApplicationException(msg, e);
        }
    }

    private BatchPosting deletePosting(ManualOperationWrapper wrapper) {
        try {
            BatchPosting posting = getPostingWithCheck(wrapper);
            if (postingProcessor.needHistory(posting, BatchPostStep.HAND1, BatchPostAction.DELETE)) {
                postingRepository.setPostingInvisible(posting, userContext.getTimestamp(), userContext.getUserName());
                return postingRepository.findById(BatchPosting.class, posting.getId());
            } else {
                postingRepository.deletePosting(posting);   // удалить запрос на операцию
                return null;
            }

        } catch (Throwable e) {
            String msg = "Ошибка при удалении запроса на операцию ID = " + wrapper.getId();
            addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.error(ManualOperation, msg, postingName, getWrapperId(wrapper), e);
            throw new DefaultApplicationException(msg, e);
        }
    }

    // TODO сверять значения Wrapper и Posting ?
    private BatchPosting getPostingWithCheck(ManualOperationWrapper wrapper, BatchPostStatus ... enabledStatus) {
        final BatchPosting posting = Optional.ofNullable(postingRepository.findById(BatchPosting.class, wrapper.getId()))
                .orElseThrow(() -> new DefaultApplicationException("Не найден запрос на операцию с Id = " + wrapper.getId()));
        checkPostingStatus(posting, wrapper, enabledStatus);
        return posting;
    }

    private boolean checkPostingStatus(BatchPosting posting, ManualOperationWrapper wrapper, BatchPostStatus ... enabledStatus) {
        if (!posting.getStatus().equals(wrapper.getStatus())) {
            String msg = String.format("Запрос на операцию ID = %s изменен, статус: '%s'." +
                    "\n Обновите информацию и выполните операцию повторно"
                    , posting.getId().toString(), posting.getStatus().name());
            wrapper.getErrorList().addErrorDescription("", "", msg, null);
            throw new DefaultApplicationException(wrapper.getErrorMessage());
        }
        if (!InvisibleType.N.equals(posting.getInvisible())) {
            String msg = String.format("Запрос на операцию ID = %s изменен, признак 'Удален': '%s'" +
                    "\n Обновите информацию", posting.getId().toString(), posting.getInvisible().name() );
            wrapper.getErrorList().addErrorDescription("", "", msg, null);
            throw new DefaultApplicationException(msg);
        }
        if (enabledStatus.length == 0)
            return true;
        for (BatchPostStatus status : enabledStatus) {
            if (status.equals(posting.getStatus())) {
                return true;
            }
        }
        String msg = String.format("Запрос на операцию ID = '%s' в недопустимом статусе: '%s'"
                , posting.getId().toString(), posting.getStatus().name());
        wrapper.getErrorList().addErrorDescription("", "", msg, null);
        throw new DefaultApplicationException(msg);
    }

    private BatchPosting createPostingHistory(BatchPosting posting, BatchPostStep step, BatchPostAction action) throws Exception {
        return postingProcessor.needHistory(posting, step, action) ?
                operationRepository.executeInNewTransaction(persistence ->
                        postingRepository.createPostingHistory(posting.getId(), userContext.getTimestamp(), userContext.getUserName()))
                : posting;
    }

    private BatchPostStatus getStepStatus(BatchPostStep step, BatchPostStatus status) {
        if (step.equals(status.getStep()))
            return status;
        if (step.isConfirmStep()) {
            switch (status) {
                case SIGNED:        return SIGNEDDATE;
                case REFUSE:        return REFUSEDATE;
                case ERRPROC:       return ERRPROCDATE;
                case ERRSRV:        return ERRSRV;
                case REFUSESRV:     return REFUSESRV;
            }
        } else if (step.isControlStep()){
            switch (status) {
                case SIGNEDDATE:    return SIGNED;
                case REFUSEDATE:    return REFUSE;
                case ERRPROCDATE:   return ERRPROC;
                case ERRSRV:        return ERRSRV;
                case REFUSESRV:     return REFUSESRV;
            }
        }
        return status;
    }

    public void checkHand12Diff(BatchPosting posting) {
        BatchPostStep step = posting.getStatus().getStep();
        if (step.isControlStep()) {
            if (userContext.getUserName().equals(posting.getUserName())) {
                throw new ValidationError(POSTING_SAME_NOT_ALLOWED, posting.getId().toString());
            }
        }
    }

    // ==============================================================================================

    public void checkOperdayOnline(ErrorList errList) {
        if (ONLINE != operdayController.getOperday().getPhase()) {
            String msg = "Ошибка при создании операции";
            ValidationError e = new ValidationError(OPERDAY_NOT_ONLINE, operdayController.getOperday().getPhase().name());
            addOperationErrorMessage(e, msg, errList, initSource());
            throw new DefaultApplicationException(null == errList ? msg : errList.getErrorMessage(), e);
        }
    }

    public void checkProcessingAllowed(ErrorList errList) {
        if (!operdayController.isProcessingAllowed()) {
            String msg = "Ошибка при создании операции";
            ValidationError e = new ValidationError(OPERDAY_IN_SYNCHRO);
            addOperationErrorMessage(e, msg, errList, initSource());
            throw new DefaultApplicationException(null == errList ? msg : errList.getErrorMessage(), e);
        }
    }

    public GLManualOperation processMessage(BatchPosting posting0, ManualOperationWrapper wrapper, boolean withCheck) {
        // проверка статуса опердня
        if (withCheck) {
            checkOperdayOnline(wrapper.getErrorList());
            checkProcessingAllowed(wrapper.getErrorList());
        }

        String oper = " операции по запросу ID = " + posting0.getId();
        auditController.info(Operation, "Начало обработки" + oper, posting0);
        try {
            return operationRepository.executeInNewTransaction(persistence -> {
                try {
                    BatchPostStep step = posting0.getStatus().getStep();
                    // устанавливаем статус обработки с проверкой, что еще не было обработки
                    updatePostingStatusNew(posting0, BatchPostStatus.UNKNOWN);
                    BatchPosting posting = postingRepository.findById(BatchPosting.class, posting0.getId());

                    GLManualOperation operation0;
                    try {
                        operation0 = operationRepository.executeInNewTransaction(persistence1 -> createOperation(posting, wrapper));
                    } catch (Throwable e) {
                        throw new DefaultApplicationException(logPostingError(e, "Ошибка при создании" + oper, wrapper, ERRPROC, 1));
                    }

                    final Long operationId = operation0.getId();
                    try {
                        final GLManualOperation enrichedOperation = operationRepository.findById(GLManualOperation.class, operationId);
                        operationRepository.executeInNewTransaction(persistence1 -> enrichmentOperation(enrichedOperation));
                    } catch (Throwable e) {
                        throw new DefaultApplicationException(
                                logOperationError(e, "Ошибка при заполнения данных" + oper, wrapper, operationId, OperState.ERCHK));
                    }

                    try {
                        GLManualOperation resultOperation = operationRepository.executeInNewTransaction(persistence1 -> {
                            final GLManualOperation processedOperation = operationRepository.findById(GLManualOperation.class, operationId);
                            if (processOperation(wrapper, processedOperation)) {
                                auditController.info(Operation, "Успешное завершение обработки"  + oper, processedOperation);
                            }
                            postingRepository.updatePostingStatusSuccess(posting.getId(), processedOperation);
                            return processedOperation;
                        });
                        return resultOperation;
                    } catch (Throwable e) {
                        throw new DefaultApplicationException(
                                logOperationError(e, "Ошибка при обработке данных" + oper, wrapper, operationId, OperState.ERCHK));
                    }
                } catch (DefaultApplicationException e) {
                    operationRepository.setRollbackOnly();
                    auditController.error(Operation, "Системная ошибка при обработке"  + oper, posting0, e);
                    throw new DefaultApplicationException(e.getMessage(), e);
                } finally {
                    operationRepository.executeInNewTransaction(persistence1 -> {
                        if (DIRECT == operdayController.getOperday().getPdMode()) {
                            try {
                                journalController.recalculateBackvalueJournal();
                            } catch (Exception e){
                                log.error("Ошибка при пересчете/локализации. See GL_AUDIT for details");
                            }
                        }
                        return null;
                    });
                }
            });
        } catch (Exception e) {
            throw new DefaultApplicationException(e);
        }
    }

    /**
     * Проверяет, есть ли в операции контролируемые счета
     * Если нет, возвращает false
     * Если есть - true
     * В случае ошибки возникает исключение
     * @param wrapper
     * @return
     */
    public boolean sendMovement(BatchPosting posting, ManualOperationWrapper wrapper) {
//        if (true)
//            return false;     // TODO DEBUG - пока исключаем обращение к MovementCreate

        if (null != posting.getMovementTimestamp()) {
            auditController.info(getLogCode(wrapper), String.format("По запоросу ID = '%s' уже было выполнено успешное движение в MovementCreate: '%s', время: '%s'",
                    posting.getId().toString(), posting.getMovementId(), timeFormat.format(posting.getMovementTimestamp()))
                    , postingName, posting.getId().toString());
            return true;
        }
        try {
            String movementDr = null, movementCr = null;
            String oper = posting.getId().toString();
            String rand = getRundomUUID(6);
            if (accountRepository.isControlableAccount(posting.getAccountDebit())) {
                movementDr = oper + "D." + rand;
            }
            if (accountRepository.isControlableAccount(posting.getAccountCredit())) {
                movementCr = oper + "C." + rand;
            }
            if (isEmpty(movementDr) && isEmpty(movementCr))
                return false;
            // сохранить movementId в таблице
            final String movementId = !isEmpty(movementDr) ? movementDr : movementCr;

            Date operday = operdayController.getOperday().getCurrentDate();

            operationRepository.executeInNewTransaction(persistence ->
                    postingRepository.updateMovementId(posting.getId(), movementId, userContext.getTimestamp(), userContext.getUserName()));
            List<MovementCreateData> movementData = fillMovementData(posting, movementDr, movementCr, operday);

            MovementCreateData data = movementData.get(0);
            String msg1 = String.format("Отправлено в MovementCreate: " +
                    "ID_DR = '%s', AC_DR = '%s', AMT_DR = %s, ID_CR = '%s', AC_CR = '%s', AMT_CR = %s",
                    data.getOperIdD(), data.getAccountCBD(), null == data.getOperAmountD() ? "null" : data.getOperAmountD().toString(),
                    data.getOperIdC(), data.getAccountCBC(), null == data.getOperAmountC() ? "null" : data.getOperAmountC().toString());
            log.info(msg1);

            auditController.info(getLogCode(wrapper), msg1, postingName, getWrapperId(wrapper));
            movementProcessor.process(movementData);
//            processMovementDebug(movementData);   // TODO DEBUG - для отладки без MovementCreate

            data = movementData.get(0);
            String msg2 = String.format("Получено из MovementCreate: Id = '%s', State = '%s', ErrType = '%s', ErrDescr = '%s'",
                    data.getOperIdD(), data.getState(), data.getErrType(), data.getErrDescr());
            log.info(msg2);
            auditController.info(getLogCode(wrapper), msg2, postingName, getWrapperId(wrapper));
            if (!MovementCreateData.StateEnum.ERROR.equals(data.getState())) {  // SUCCESS или WARNING
                // TODO не перезаписываем время посылки в АБС
                operationRepository.executeInNewTransaction(persistence -> postingRepository.updateMovementTimestamp(wrapper.getId(), movementId, userContext.getTimestamp()));
                return true;
            }

            // TODO надо вынести запись статуса ошибки наружу? Падает при обработке пакетных операций
            log.info(String.format("MovementCreate ERROR: Id = '%s'", data.getOperIdD()));
            MovementErrorTypes error = data.getErrType();
            BatchPostStatus status = MovementErrorTypes.ERR_BUSINESS.equals(error) ? REFUSESRV : ERRSRV;
            String errorMsg = error.getMessage();
            if (!isEmpty(data.getErrDescr())) {
                errorMsg += "\n" + data.getErrDescr();
            }
            ValidationError validationError = new ValidationError(MOVEMENT_ERROR, getWrapperId(wrapper), errorMsg);
            logPostingError(validationError, error.getMessage(), wrapper, status, error.getCode());
            throw validationError;

        } catch (Exception e) {
                throw new DefaultApplicationException(logPostingError(e,
                        String.format("Ошибка при обращении к сервису движений по запросу ID = %s", getWrapperId(wrapper)),
                        wrapper, ERRSRV, MovementErrorTypes.ERR_REQUEST.getCode()));
        }
    }

    private String getRundomUUID(int num) {
        return UUID.randomUUID().toString().substring(0, num);
    }

    private List<MovementCreateData> fillMovementData(BatchPosting posting,
                                                      String movementDr, String movementCr, Date operday) throws ParseException {
        MovementCreateData data = new MovementCreateData();
        if (!isEmpty(movementDr)) {
            data.setOperIdD(movementDr);
            data.setAccountCBD(posting.getAccountDebit());
            data.setOperAmountD(posting.getAmountDebit());
        }
        if (!isEmpty(movementCr)) {
            data.setOperIdC(movementCr);
            data.setAccountCBC(posting.getAccountCredit());
            data.setOperAmountC(posting.getAmountCredit());
        }
        data.setDestinationR(StringUtils.removeCtrlChars(posting.getRusNarrativeLong()));
        data.setPnar(pdRepository.getPnarManual(posting.getDealId(), posting.getSubDealId(), posting.getPaymentRefernce()));

        data.setPstDate(posting.getValueDate());
        data.setDealId(substr(!isEmpty(posting.getDealId()) ? posting.getDealId() : posting.getPaymentRefernce(), 15));
        data.setPstSource(posting.getSourcePosting());
        data.setDeptId(posting.getDeptId());
        data.setProfitCenter(posting.getProfitCenter());
        data.setCorrectionPst(YesNo.Y.equals(posting.getIsCorrection()));
        // TODO DEBUG
//        data.setOperCreate(userContext.getTimestamp());
//        data.setOperCreate(data.getPstDate());
        data.setOperCreate(operday);

        List<MovementCreateData> datas = new ArrayList<>();
        datas.add(data);

        return datas;
    }

    private void processMovementDebug(List<MovementCreateData> movementData) {
        MovementCreateData item = movementData.get(0);
        char deal = (null == item.getDealId()) ? ' ' : item.getDealId().charAt(0);
        if (null == item.getDealId()) {
            item.setState(MovementCreateData.StateEnum.ERROR);
            item.setErrType(MovementErrorTypes.ERR_SERVICE);
        }
        if (deal == '0') {
            item.setState(MovementCreateData.StateEnum.SUCCESS);
        } else {
            switch (deal) {
                case '2':
                    item.setErrType(MovementErrorTypes.ERR_BUSINESS); break;
                case '3':
                    item.setErrType(MovementErrorTypes.ERR_SERVICE); break;
                case '4':
                    item.setErrType(MovementErrorTypes.ERR_REQUEST); break;
                default:
                    item.setErrType(MovementErrorTypes.ERR_REQUEST); break;
            }
            item.setState(MovementCreateData.StateEnum.ERROR);
            item.setErrDescr(item.getDealId());
        }
    }

    /**
     * Создает операцию по набору данных, введенных с экрана
     * Заполняет дополнительные параметры операции
     * @param wrapper
     * @return
     * @throws Exception
     */
    private GLManualOperation createOperation(BatchPosting posting, ManualOperationWrapper wrapper) throws Exception {
        List<ValidationError> errors = operationProcessor.validate(posting, new ValidationContext());

        if (errors.isEmpty()) {
            GLManualOperation operation = operationProcessor.createOperation(posting);       // создать операцию
            operation.setState(OperState.LOAD);

            return operationRepository.save(operation);     // сохранить операцию
        } else {
            throw new DefaultApplicationException(postingProcessor.validationErrorMessage(errors, wrapper.getErrorList()));
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
    private boolean processOperation(ManualOperationWrapper wrapper, GLManualOperation operation) throws Exception {
        final Long operationId = operation.getId();
        GLOperationProcessor operationProcessor = etlPostingController.findOperationProcessor(operation);
        String msgCommon = format(" операцию ID = %s", operation.getId());
        boolean toContinue = true;
/*      // TODO исключаю как лишнее действие
        try {
            toContinue = validateOperation(operationProcessor, operation);
        } catch ( Throwable e ) {
            if (!(e instanceof DefaultApplicationException)) {
                String msg = "Ошибка валидации данных" + msgCommon;
                setOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource(), operationId, OperState.ERCHK);
                auditController.error(Operation, msg, operation, e);
            }
            throw new DefaultApplicationException(e.getMessage(), e);
        }
*/
        if ( toContinue ) {
            try {
                operationRepository.executeInNewTransaction(persistence -> updateOperation(operationProcessor, operation));
            } catch ( Throwable e ) {
                String msg = "Ошибка заполнения данных" + msgCommon;
                setOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource(), operationId, OperState.ERCHK);
                auditController.error(ManualOperation, msg, operation, e);
                throw new DefaultApplicationException(e.getMessage(), e);
            }
            try {
                finalOperation(operationProcessor, operation);
            } catch ( Throwable e ) {
                String msg = "Ошибка обработки" + msgCommon;
                setOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource(), operationId, OperState.ERPOST);
                auditController.error(ManualOperation, msg, operation, e);
                throw new DefaultApplicationException(e.getMessage(), e);
            }
        }
        return true;
    }

    private boolean validateOperation(GLOperationProcessor operationProcessor, GLManualOperation operation) {
        List<ValidationError> errors = operationProcessor.validate(operation, new ValidationContext());
        boolean toContinue = errors.isEmpty();
        if (!toContinue) {
            String msg = "Ошибка валидации" + format(" операции '%s'", operation.getId());
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

    private String logPostingError(Throwable e, String msg, ManualOperationWrapper wrapper,
                                   BatchPostStatus status, int errorCode) {
        log.error("-->" + msg, e);
        addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
        try {
            // TODO здесь падает при обработке пакета!!
            operationRepository.executeInNewTransaction(persistence -> postingRepository.updatePostingStatusError(wrapper.getId(), wrapper.getErrorMessage(),
                        getStepStatus(wrapper.getStatus().getStep(), status), errorCode));
        } catch (Throwable e1) {
            return msg + "\n" + e.getMessage();
        }
        log.error("<--" + msg, e);
        auditController.error(getLogCode(wrapper), msg, postingName, getWrapperId(wrapper), e);
        return msg;
    }

    private String logOperationError(Throwable e, String msg, ManualOperationWrapper wrapper, Long operationId, OperState state) {
        log.error(msg, e);
        String errMsg = setOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource(), operationId, state);
        if (!(e instanceof ValidationError))
            auditController.error(getLogCode(wrapper), msg, postingName, getWrapperId(wrapper), e);
        return msg;
    }

    private AuditRecord.LogCode getLogCode(ManualOperationWrapper wrapper) {
        return InputMethod.M.equals(wrapper.getInputMethod()) ? ManualOperation : BatchOperation;
    }

    public String addOperationErrorMessage(Throwable e, String msg, ErrorList errorList, String source) {
        String errCode = "";
        String errMessage = getErrorMessage(e);
        log.error(format("%s: %s. Обнаружена: %s\n'", msg, errMessage, source), e);
        if (null != errorList) {
            errCode = ValidationError.getErrorCode(errMessage);
            errMessage = ValidationError.getErrorText(errMessage);
            if (!errMessage.isEmpty()) {
                errorList.addNewErrorDescription("", "", errMessage, errCode);
            }
        }
        return errMessage;
    }

    private String setOperationErrorMessage(Throwable e, String msg, ErrorList errorList, String source, Long operationId, OperState state) {
        String errMessage = addOperationErrorMessage(e, msg, errorList, source);
        final GLManualOperation operation = operationRepository.findById(GLManualOperation.class, operationId);
        if (null != operation) {
            try {
                operationRepository.executeInNewTransaction(persistence -> {
                    operationRepository.updateOperationStatusError(operation, state, substr(errMessage, 4000));
                    return null;
                });
            } catch (Exception e1) {
                return errMessage + "\n" + e.getMessage();
            }
        }
        return errMessage;
    }

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class,
                PersistenceException.class);
    }

    public void updatePostingStatusNew(BatchPosting posting, BatchPostStatus state) throws Exception {
        int cnt = postingRepository.executeInNewTransaction(persistence -> postingRepository.updatePostingStatusNew(posting, state));
        if (cnt != 1) {
            throw new ValidationError(ErrorCode.POSTING_IS_WORKING, posting.getId().toString(), state.name());
        }
    }

    public ManualOperationWrapper createStatusWrapper(BatchPosting posting) {
        ManualOperationWrapper wrapper = new ManualOperationWrapper();
        wrapper.setId(posting.getId());
        wrapper.setPkgId(posting.getPackageId());
        wrapper.setInputMethod(posting.getInputMethod());
        wrapper.setStatus(posting.getStatus());
        return wrapper;
    }

    private String getWrapperId(ManualOperationWrapper wrapper) {
        return null == wrapper.getId() ? "" : wrapper.getId().toString();
    }
}
