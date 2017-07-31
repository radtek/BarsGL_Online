package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.excel.BatchProcessResult;
import ru.rbt.barsgl.ejb.entity.acc.GlAccRln;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.integr.oper.BatchPostingProcessor;
import ru.rbt.barsgl.ejb.integr.oper.MovementCreateProcessor;
import ru.rbt.barsgl.ejb.integr.struct.MovementCreateData;
import ru.rbt.barsgl.ejb.integr.struct.PaymentDetails;
import ru.rbt.barsgl.ejb.repository.*;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.barsgl.shared.ErrorList;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.*;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.security.ejb.repository.AppUserRepository;
import ru.rbt.security.entity.AppUser;
import ru.rbt.shared.Assert;
import ru.rbt.shared.ExceptionUtils;
import ru.rbt.shared.enums.SecurityActionCode;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.math.BigDecimal;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.BatchOperation;
import static ru.rbt.audit.entity.AuditRecord.LogCode.ManualOperation;
import static ru.rbt.barsgl.ejb.controller.excel.BatchProcessResult.BatchProcessDate.*;
import static ru.rbt.barsgl.shared.enums.BatchPostAction.CONFIRM_NOW;
import static ru.rbt.barsgl.shared.enums.BatchPostStatus.*;
import static ru.rbt.ejbcore.util.StringUtils.*;
import static ru.rbt.ejbcore.validation.ErrorCode.POSTING_SAME_NOT_ALLOWED;
import static ru.rbt.ejbcore.validation.ErrorCode.POSTING_STATUS_WRONG;
import static ru.rbt.ejbcore.validation.ValidationError.initSource;

/**
 * Created by ER18837 on 13.08.15.
 */
@Stateless
@LocalBean
public class ManualPostingController {
    private static final Logger log = Logger.getLogger(EtlPostingController.class);
    private static final String postingName = "GL_BATPST";
    public static final SimpleDateFormat timeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    @EJB
    private AuditController auditController;

    @Inject
    private BatchPostingProcessor postingProcessor;

    @EJB
    private BatchPostingRepository postingRepository;

    @EJB
    private ManualOperationRepository operationRepository;

    @Inject
    private AppUserRepository userRepository;

    @Inject
    private OperdayController operdayController;

    @EJB
    private PdRepository pdRepository;

    @Inject
    private MovementCreateProcessor movementProcessor;

    @Inject
    private UserContext userContext;

    @Inject
    private DateUtils dateUtils;

    @Inject
    private AccRlnRepository accRlnRepository;

    @Inject
    private BankCurrencyRepository bankCurrencyRepository;


    /**
     * Обработка запроса от интерфейса на обработку запроса на операцию
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> processOperationRq(ManualOperationWrapper wrapper) throws Exception {
        try {
//            checkOperdayOnline(wrapper.getErrorList());
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
     * Проверка баланса счёта на Красное сальдо
     * @param wrapper
     * @throws ParseException
     */
    private void checkAccountsBalance(ManualOperationWrapper wrapper) throws ParseException, SQLException {
        if (wrapper.isNoCheckBalance()) {
            wrapper.setBalanceError(false);
            return;
        }
        GlAccRln accountDr = accRlnRepository.checkAccointIsPair(wrapper.getAccountDebit())?null:accRlnRepository.findAccRlnAccount(wrapper.getAccountDebit());
        GlAccRln accountCr = accRlnRepository.checkAccointIsPair(wrapper.getAccountCredit())?null:accRlnRepository.findAccRlnAccount(wrapper.getAccountCredit());

        Date postDate = BatchPostAction.CONFIRM_NOW.equals(wrapper.getAction())? operdayController.getOperday().getCurrentDate() : dateUtils.onlyDateParse(wrapper.getPostDateStr());

        if (accountDr != null && "П".equalsIgnoreCase(accountDr.getPassiveActive().trim())) {
            GlAccRln tehoverAcc = accRlnRepository.findAccountTehover(accountDr.getId().getBsaAcid(),accountDr.getId().getAcid());
            BankCurrency currencyDr = bankCurrencyRepository.getCurrency(wrapper.getCurrencyDebit());
            BigDecimal amountDr = convertToScale(wrapper.getAmountDebit(),currencyDr.getScale().intValue());
            BigDecimal n = "А".equalsIgnoreCase(accountDr.getPassiveActive())? BigDecimal.valueOf(-1):BigDecimal.valueOf(1);
            DataRecord resDr = null;
            if (tehoverAcc!=null) {
                resDr = accRlnRepository.checkAccountBalance(accountDr, postDate, amountDr.multiply(BigDecimal.valueOf(-1)),tehoverAcc);
            }
            else {
                resDr = accRlnRepository.checkAccountBalance(accountDr, postDate, amountDr.multiply(BigDecimal.valueOf(-1)));
            }
            if (resDr != null && (resDr.getBigDecimal(2).compareTo(BigDecimal.ZERO) < 0)) {
                wrapper.setBalanceError(true);
                BigDecimal bac = convertFromScale(resDr.getBigDecimal(1),currencyDr.getScale().intValue());
                BigDecimal outrest = convertFromScale(resDr.getBigDecimal(2),currencyDr.getScale().intValue());
                wrapper.getErrorList().addErrorDescription(String.format("На счёте %s не хватает средств. \n Текущий отстаток на дату %s = %s (с учётом операции = %s)", accountDr.getId().getBsaAcid(), resDr.getDate(0),  bac, outrest));
            }
        }

        if (accountCr != null && "А".equalsIgnoreCase(accountCr.getPassiveActive().trim())) {
            GlAccRln tehoverAcc = accRlnRepository.findAccountTehover(accountCr.getId().getBsaAcid(),accountCr.getId().getAcid());
            BankCurrency currencyCr = bankCurrencyRepository.getCurrency(wrapper.getCurrencyCredit());
            BigDecimal amountCr = convertToScale(wrapper.getAmountCredit(),currencyCr.getScale().intValue());
            BigDecimal n = "А".equalsIgnoreCase(accountDr.getPassiveActive())? BigDecimal.valueOf(-1):BigDecimal.valueOf(1);
            DataRecord resCr = null;
            if (tehoverAcc!=null) {
                resCr = accRlnRepository.checkAccountBalance(accountCr, postDate, amountCr,tehoverAcc);
            }
            else {
                resCr = accRlnRepository.checkAccountBalance(accountCr, postDate, amountCr);
            }
            if (resCr != null && resCr.getBigDecimal(2).compareTo(BigDecimal.ZERO) > 0) {
                wrapper.setBalanceError(true);
                BigDecimal bac = convertFromScale(resCr.getBigDecimal(1),currencyCr.getScale().intValue());
                BigDecimal outrest = convertFromScale(resCr.getBigDecimal(2),currencyCr.getScale().intValue());
                wrapper.getErrorList().addErrorDescription(String.format("На счёте %s не хватает средств. \n Текущий отстаток на дату %s = %s (с учётом операции = %s)", accountCr.getId().getBsaAcid(), resCr.getDate(0), bac, outrest));
            }
        }

        if (wrapper.isBalanceError())
            throw new ValidationError(ErrorCode.ACCOUNT_BALANCE_ERROR, wrapper.getErrorMessage());
    }

    private BigDecimal convertToScale(BigDecimal amount,int scale)
    {
        return amount.multiply(BigDecimal.TEN.pow(scale));
    }

    private BigDecimal convertFromScale(BigDecimal amount,int scale)
    {
        return amount.divide(BigDecimal.TEN.pow(scale));
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
            if (newStatus==CONTROL) {
                checkAccountsBalance(wrapper);
            }
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
            if (newStatus==CONTROL) {
                checkAccountsBalance(wrapper);
            }
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
            checkAccountsBalance(wrapper);
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
        return setOperationRqStatusControl(wrapper, posting != posting0);
    }

    public RpcRes_Base<ManualOperationWrapper> refuseOperationRq(ManualOperationWrapper wrapper, BatchPostStatus status) throws Exception {
        BatchPosting posting0 = getPostingWithCheck(wrapper, CONTROL, REFUSEDATE, ERRSRV, REFUSESRV, WAITDATE, ERRPROC, ERRPROCDATE);
        String msg;
        if (hasMovement(posting0)) {
            msg = "Нельзя вернуть запрос на операцию ID = " + wrapper.getId() + " на доработку,\nпо нему выполнен успешный запрос в сервис движений";
        } else {
            BatchPosting posting = createPostingHistory(posting0, wrapper.getStatus().getStep(), wrapper.getAction());
            BatchPostStatus oldStatus = posting.getStatus();
            int count = operationRepository.executeInNewTransaction(persistence -> {
                return postingRepository.refusePostingStatus(posting.getId(), wrapper.getReasonOfDeny(),
                        userContext.getTimestamp(), userContext.getUserName(), status, oldStatus);
            });
            if (0 == count)
                throw new ValidationError(POSTING_STATUS_WRONG, oldStatus.name(), oldStatus.getLabel());
            wrapper.setStatus(status);
            msg = "Запрос на операцию ID = " + wrapper.getId() + " возвращён на доработку";
        }
        auditController.info(ManualOperation, msg, postingName, getWrapperId(wrapper));
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    public RpcRes_Base<ManualOperationWrapper> authorizeOperationRq(ManualOperationWrapper wrapper) throws Exception {
        try {
//            checkProcessingAllowed(wrapper.getErrorList());
            BatchPostStep step = wrapper.getStatus().getStep();
            if (!step.isControlStep()) {
                return new RpcRes_Base<>(wrapper, false, "Неверный шаг оерации: " + step.name());
            }
//            if (SIGNED.equals(wrapper.getStatus())) {
//                return reprocessOperationRq(wrapper);
//            }
            postingProcessor.checkFilialPermission(wrapper.getFilialDebit(), wrapper.getFilialCredit(), wrapper.getUserId());
            BatchPosting posting0 = getPostingWithCheck(wrapper, CONTROL);
            checkHand12Diff(posting0);
            checkAccountsBalance(wrapper); //Проверяем баланс
            BatchPosting posting = createPostingHistory(posting0, wrapper.getStatus().getStep(), wrapper.getAction());
            // тестируем статус - что никто еще не менял
            updatePostingStatusNew(posting0, SIGNEDVIEW, wrapper);
            BatchPostStatus newStatus = getOperationRqStatusSigned(wrapper.getUserId(), posting.getPostDate());
            setOperationRqStatusSigned(wrapper, userContext.getUserName(), SIGNEDVIEW, newStatus);

            if (posting0.isControllable()) {                    // есть контролируемы счет
                return sendMovement(posting, wrapper, newStatus);          // посылка запроса в MovementCreate
            } else {
                // устанавливаем статус SIGNED / SIGNEDDATE / WAITDATE
                return setOperationRqStatusSigned(wrapper, userContext.getUserName(), newStatus, newStatus);
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
//            checkProcessingAllowed(wrapper.getErrorList());
            BatchPostStep step = wrapper.getStatus().getStep();
            if (!step.isConfirmStep()) {
                return new RpcRes_Base<>(wrapper, false, "Неверный шаг оерации: " + step.name());
            }
//            if (SIGNEDDATE.equals(wrapper.getStatus())) {
//                return reprocessOperationRq(wrapper);
//            }
            postingProcessor.checkFilialPermission(wrapper.getFilialDebit(), wrapper.getFilialCredit(), wrapper.getUserId());
            BatchPosting posting0 = getPostingWithCheck(wrapper, WAITDATE);
            checkAccountsBalance(wrapper); //Проверяем баланс
            BatchPosting posting = createPostingHistory(posting0, wrapper.getStatus().getStep(), wrapper.getAction());
            BatchPostStatus newStatus = SIGNEDDATE;
            // тестируем статус - что никто еще не менял
            updatePostingStatusNew(posting0, CLICKDATE, wrapper);
            // устанавливаем статус
            Date operday = operdayController.getOperday().getCurrentDate();
            if (BatchPostAction.CONFIRM_NOW.equals(wrapper.getAction())) {
                wrapper.setPostDateStr(operday == null ? null : dateUtils.onlyDateString(operday));
                postingRepository.executeInNewTransaction(persistence -> {
                    postingRepository.setPostingDate(posting.getId(), userContext.getCurrentDate());
                    return null;
                });
            }
            return setOperationRqStatusSigned(wrapper, userContext.getUserName(), newStatus, newStatus);
//            return authorizeOperationRqInternal(posting, wrapper, newStatus);
        } catch (ValidationError e) {
            String msg = "Ошибка при подтверждении даты запроса на операцию ID = " + wrapper.getId();
            String errMessage = addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.error(ManualOperation, msg, postingName, getWrapperId(wrapper), e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
    }

    /*
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
    */

    public void updatePostingStatusNew(BatchPosting posting, BatchPostStatus newStatus, ManualOperationWrapper wrapper) throws Exception {
        int cnt = postingRepository.executeInNewTransaction(persistence -> postingRepository.updatePostingStatusNew(posting.getId(), newStatus));
        if (cnt != 1) {
            throw new ValidationError(ErrorCode.POSTING_IS_WORKING, posting.getId().toString(), newStatus.name());
        }
        if (null != wrapper)
            wrapper.setStatus(newStatus);
    }

    public RpcRes_Base<ManualOperationWrapper> setOperationRqStatusControl(ManualOperationWrapper wrapper, boolean withChange) throws Exception {
        final BatchPostStatus status = CONTROL;
        BatchPostStatus oldStatus = wrapper.getStatus();
        int count = operationRepository.executeInNewTransaction(persistence -> {
            if (withChange) {
                return postingRepository.updatePostingStatusChanged(wrapper.getId(), userContext.getTimestamp(), userContext.getUserName(), status, oldStatus);
            } else {
                return postingRepository.updatePostingStatus(wrapper.getId(), status, oldStatus);
            }
        });
        if (0 == count)
            throw new ValidationError(POSTING_STATUS_WRONG, oldStatus.name(), oldStatus.getLabel());
        wrapper.setStatus(status);
        String msg = "Запрос на операцию ID = " + wrapper.getId() + " передан на подпись";
        auditController.info(ManualOperation, msg, postingName, getWrapperId(wrapper));
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    public BatchPostStatus getOperationRqStatusSigned(String signerName, Date postDate) throws Exception {
        AppUser user = userRepository.findUserByName(signerName);
        Assert.notNull(user, "Не найден пользователь: " + signerName);
        return getOperationRqStatusSigned(user.getId(), postDate);
    }

    public BatchPostStatus getOperationRqStatusSigned(Long signerId, Date postDate) throws Exception {
        Date operday = operdayController.getOperday().getCurrentDate();
        BatchPostStatus newStatus;
        if (operday.equals(postDate)) {    // текущий день
            newStatus = SIGNED;
        } else if (postingProcessor.checkActionEnable(signerId, SecurityActionCode.OperHand3)) {
            newStatus = SIGNEDDATE;      // архивный день и 3-я рука
        } else {
            newStatus = WAITDATE;         // ждать 3-ю руку
        }
        return newStatus;
    }

    public RpcRes_Base<ManualOperationWrapper> setOperationRqStatusSigned(ManualOperationWrapper wrapper, String userName,
                                                                          BatchPostStatus newStatus, BatchPostStatus logStatus) throws Exception {
        BatchProcessResult result = new BatchProcessResult(wrapper.getId(), newStatus);
        BatchPostStatus oldStatus = wrapper.getStatus();
        BatchPostAction action = wrapper.getAction();
        int count = operationRepository.executeInNewTransaction(persistence -> {
            Date timestamp = userContext.getTimestamp();
            int cnt = 0;
            switch (newStatus) {
                case SIGNEDVIEW:
                    if (SIGNEDDATE.equals(logStatus)) {
                        cnt = postingRepository.signedConfirmPostingStatus(wrapper.getId(), timestamp, userName, newStatus, oldStatus);
                    } else {
                        cnt = postingRepository.signedPostingStatus(wrapper.getId(), timestamp, userName, newStatus, oldStatus);
                    }
                    break;
                case SIGNED:
                case WAITDATE:
                    cnt = postingRepository.signedPostingStatus(wrapper.getId(), timestamp, userName, newStatus, oldStatus);
                    break;
                case CLICKDATE:
                case SIGNEDDATE:
                    if (wrapper.getStatus().getStep().isControlStep()) {
                        cnt = postingRepository.signedConfirmPostingStatus(wrapper.getId(), timestamp, userName, newStatus, oldStatus);
                        result.setProcessDate(BT_PAST);
                    } else {
                        cnt = postingRepository.confirmPostingStatus(wrapper.getId(), timestamp, userName, newStatus, oldStatus);
                        result.setStatus(CONFIRM);
                        result.setProcessDate(CONFIRM_NOW.equals(action) ? BT_NOW : BT_PAST);
                    }
                   break;
                default:
                    Assert.isTrue(false, "Неверный статус");
            }
            return cnt; //msg;
        });
        if (0 == count)
            throw new ValidationError(POSTING_STATUS_WRONG, oldStatus.name(), oldStatus.getLabel());
        wrapper.setStatus(newStatus);
        String msg = result.getPostSignedMessage();
        wrapper.getErrorList().addErrorDescription(msg);
        auditController.info(ManualOperation, msg, postingName, getWrapperId(wrapper));
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    public RpcRes_Base<ManualOperationWrapper> setOperationRqStatusSend(ManualOperationWrapper wrapper, String movementId, BatchPostStatus srvStatus, BatchPostStatus nextStatus) throws Exception {
//        BatchPostStatus srvStatus = WAITSRV;
        // устанавливаем статус = WAITSRV, movementId , SEND_SRV
        BatchPostStatus oldStatus = wrapper.getStatus();
        int count = operationRepository.executeInNewTransaction(persistence -> {
            return postingRepository.sendPostingStatus(wrapper.getId(), movementId, userContext.getTimestamp(), srvStatus, oldStatus);
        });
        if (0 == count) {
            throw new ValidationError(POSTING_STATUS_WRONG, oldStatus.name(), oldStatus.getLabel());
        }
        wrapper.setStatus(srvStatus);
//        String msg = "Запрос на операцию ID = " + wrapper.getId() + message;
        BatchProcessResult result = new BatchProcessResult(wrapper.getId(), nextStatus);
        result.setProcessDate(SIGNEDDATE.equals(nextStatus) ? BT_PAST : BT_EMPTY);
        String msg = result.getPostSendMessage();
        wrapper.getErrorList().addErrorDescription(msg);
        auditController.info(ManualOperation, msg, postingName, getWrapperId(wrapper));
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    public RpcRes_Base<ManualOperationWrapper> setOperationRqStatusReceive(ManualOperationWrapper wrapper, String movementId,
                                                                           BatchPostStatus newStatus, int errorCode, String errorMessage) throws Exception {
        BatchPostStatus oldStatus = wrapper.getStatus();
        BatchPostStatus stepStatus = getStepStatus(wrapper.getStatus().getStep(), newStatus);
        Date timstamp = (TIMEOUTSRV == newStatus) ? null : userContext.getTimestamp();
        int count = operationRepository.executeInNewTransaction(persistence -> {
            return postingRepository.receivePostingStatus(wrapper.getId(), movementId, timstamp, stepStatus, oldStatus, errorCode, errorMessage);
        });
        if (0 == count) {
            throw new ValidationError(POSTING_STATUS_WRONG, oldStatus.name(), oldStatus.getLabel());
        }
        wrapper.setStatus(newStatus);
        BatchProcessResult result = new BatchProcessResult(wrapper.getId(), newStatus);
        String msg = result.getPostSignedMessage();

        wrapper.getErrorList().addErrorDescription(msg + errorMessage, null);
        if (errorCode == 0) {
            auditController.info(ManualOperation, msg, postingName, getWrapperId(wrapper));
        } else {
            auditController.error(ManualOperation, msg, postingName, getWrapperId(wrapper), errorMessage);
        }
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    private boolean hasMovement(BatchPosting posting) {
        //TODO не вполне корректная проверка. Нет однозначного признака прохождения movement
        return ((null != posting.getMovementId()) && !(ERRSRV.equals(posting.getStatus()) || REFUSESRV.equals(posting.getStatus())))
                || TIMEOUTSRV.equals(posting.getStatus()) ;
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
            posting.setIsTech(YesNo.N); // TODO устанавливаем признак операции не по техсчетам
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
                postingRepository.setPostingInvisible(posting.getId(), userContext.getTimestamp(), userContext.getUserName());
                return postingRepository.findById(posting.getId());
            } else {
                postingRepository.deletePosting(posting.getId());   // удалить запрос на операцию
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
        final BatchPosting posting = Optional.ofNullable(postingRepository.findById(wrapper.getId()))
                .orElseThrow(() -> new DefaultApplicationException("Не найден запрос на операцию с Id = " + wrapper.getId()));
        checkPostingStatus(posting, wrapper, enabledStatus);
        return posting;
    }

    private boolean checkPostingStatus(BatchPosting posting, ManualOperationWrapper wrapper, BatchPostStatus ... enabledStatus) {
        if (!posting.getStatus().equals(wrapper.getStatus())) {
            String msg = String.format("Запрос на операцию ID = %d изменен, статус: '%s' ('%s')." +
                    "\n Обновите информацию и выполните операцию повторно"
                    , posting.getId(), posting.getStatus().name(), posting.getStatus().getLabel());
            wrapper.getErrorList().addErrorDescription(msg);
            throw new DefaultApplicationException(wrapper.getErrorMessage());
        }
        if (!InvisibleType.N.equals(posting.getInvisible())) {
            String msg = String.format("Запрос на операцию ID = %d изменен, признак 'Удален': '%s' ('%s')\n Обновите информацию",
                    posting.getId(), posting.getInvisible().name(), posting.getInvisible().getLabel() );
            wrapper.getErrorList().addErrorDescription(msg);
            throw new DefaultApplicationException(msg);
        }
        if (enabledStatus.length == 0)
            return true;
        for (BatchPostStatus status : enabledStatus) {
            if (status.equals(posting.getStatus())) {
                return true;
            }
        }
        String msg = String.format("Запрос на операцию ID = '%d': нельзя '%s' запрос в статусе: '%s' ('%s')", posting.getId(),
                wrapper.getAction().getLabel(), posting.getStatus().name(), posting.getStatus().getLabel());
        wrapper.getErrorList().addErrorDescription(msg);
        throw new DefaultApplicationException(msg);
    }

    private BatchPosting createPostingHistory(BatchPosting posting, BatchPostStep step, BatchPostAction action) throws Exception {
        return postingProcessor.needHistory(posting, step, action) ?
                operationRepository.executeInNewTransaction(persistence ->
                        postingRepository.createPostingHistory(posting.getId(), userContext.getTimestamp(), userContext.getUserName()))
                : posting;
    }

    private BatchPosting createPostingHistory(Long postingId) throws Exception {
        return operationRepository.executeInNewTransaction(persistence ->
                        postingRepository.createPostingHistory(postingId, userContext.getTimestamp(), userContext.getUserName()));
    }

    public BatchPostStatus getStepStatus(BatchPostStep step, BatchPostStatus status) {
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

    private String getWrapperId(ManualOperationWrapper wrapper) {
        return null == wrapper.getId() ? "" : wrapper.getId().toString();
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
                errorList.addNewErrorDescription(errMessage, errCode);
            }
        }
        return errMessage;
    }

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class,
                PersistenceException.class);
    }

    public String logPostingError(Throwable e, String msg, ManualOperationWrapper wrapper,
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

    public ManualOperationWrapper createStatusWrapper(BatchPosting posting) {
        ManualOperationWrapper wrapper = new ManualOperationWrapper();
        wrapper.setId(posting.getId());
        wrapper.setPkgId(posting.getPackageId());
        wrapper.setInputMethod(posting.getInputMethod());
        wrapper.setStatus(posting.getStatus());
        return wrapper;
    }

    // ==============================================================================================
    // Сервис движений

    /**
     * Проверяет, есть ли в операции контролируемые счета
     * Если есть, посылает движение и возвращает movementId, иначе пустую строку
     * В случае ошибки возникает исключение
     * @param wrapper
     * @return
     */
    public RpcRes_Base<ManualOperationWrapper> sendMovement(BatchPosting posting, ManualOperationWrapper wrapper, BatchPostStatus nextStatus) {
        if (null != posting.getReceiveTimestamp()) {
            String msg = String.format("По запоросу ID = '%s' уже было выполнено движение в MovementCreate: '%s', время: '%s'",
                    posting.getId().toString(), posting.getMovementId(), timeFormat.format(posting.getReceiveTimestamp()));
            auditController.info(getLogCode(wrapper), msg, posting);
            return new RpcRes_Base<>(wrapper, true, msg);              // TODO норма ?? ValidationError ??
        }
        try {
            MovementCreateData data = createMovementData(posting, wrapper);
            String movementId = data.getMessageUUID();
            RpcRes_Base<ManualOperationWrapper> res = setOperationRqStatusSend(wrapper, movementId, WAITSRV, nextStatus);
            List<MovementCreateData> movementData = new ArrayList<>();
            movementData.add(data);
            try {
                movementProcessor.sendRequests(movementData);
                return res;
            } catch (Throwable e) {     // ошибка отправки
                MovementErrorTypes error = data.getErrType();
                String msg = error.getMessage() + "\n" + data.getErrDescr();
                return setOperationRqStatusReceive(wrapper, movementId, ERRSRV, error.getCode(), msg);
            }
        }
        catch (Throwable e) {
            throw new DefaultApplicationException(logPostingError(e,
                    String.format("Ошибка при обращении к сервису движений по запросу ID = %s", getWrapperId(wrapper)),
                    wrapper, ERRSRV, MovementErrorTypes.ERR_REQUEST.getCode()));
        }
    }

    public void receiveMovement(MovementCreateData data) {
        String movementId = data.getMessageUUID();
        try {
            BatchPosting posting = postingRepository.findPostingByMovementId(movementId, operdayController.getOperday().getCurrentDate());
            if (null == posting) { // || postings.size() == 0) {     // posting not fount
                throw new DefaultApplicationException(format("Не найдены запросы с movementId = '%s' в текущем опердне", movementId));
            }

            log.error(format("Найден запрос с movementId = '%s' в текущем опердне: ID = %d", movementId, posting.getId()));
/*
            if (BatchPostStatus.WAITSRV != posting.getStatus()) {   // invalid status
                throw new DefaultApplicationException(format("Неверный статус запроса %d: '%s'", posting.getId(), posting.getStatus().name()));
            }
*/

            ManualOperationWrapper wrapper = createStatusWrapper(posting);
            String signerName = posting.getSignerName();
            String msg = String.format("Получено из MovementCreate: Id = '%s', State = '%s', ErrType = '%s', ErrDescr = '%s'",
                    data.getMessageUUID(), data.getState(), data.getErrType(), data.getErrDescr());
            log.info(msg);
            auditController.info(getLogCode(wrapper), msg, posting);
            // записываем время приема ответа из АБС, меняем статус
            BatchPostStatus oldStatus = wrapper.getStatus();
            if (!MovementCreateData.StateEnum.ERROR.equals(data.getState())) {  // SUCCESS или WARNING
                if (WAITSRV == oldStatus) {     // все ОК
                    setOperationRqStatusReceive(wrapper, movementId, OKSRV, 0, null);        // одобрен
                    if (null == posting.getPackageId()) {
                        BatchPostStatus newStatus = getOperationRqStatusSigned(signerName, posting.getPostDate());
                        wrapper.setAction(BatchPostAction.SIGN);
                        setOperationRqStatusSigned(wrapper, signerName, newStatus, newStatus);  // авторизован
                    }
                    return;
                } else {        // получен ответ после таймаута или ошибки
                    createPostingHistory(wrapper.getId());
                    auditController.error(ManualOperation,
                            "Ошибка при получении ответа от сервиса движений по запросу ID = " + wrapper.getId(), postingName, getWrapperId(wrapper),
                            String.format("Получен положительный ответ от сервиса движений по запросу в статусе %s (%s)", oldStatus.name(), oldStatus.getLabel()));
                    setOperationRqStatusReceive(wrapper, movementId, ERROPERSRV, 0, null);        // одобрен
                }
            } else {
                // TODO надо вынести запись статуса ошибки наружу? Падает при обработке пакетных операций
                log.info(String.format("MovementCreate ERROR: Id = '%s' '%s'", movementId, data.getErrDescr()));
                MovementErrorTypes error = data.getErrType();
                BatchPostStatus errStatus = MovementErrorTypes.ERR_BUSINESS.equals(error) ? REFUSESRV : ERRSRV;
                String errDescr = data.getErrDescr();
                if (!isEmpty(errDescr) && (errDescr.contains("Insufficient balance") || errDescr.contains("Insufficient funds")) ) {
                    error = MovementErrorTypes.ERR_NOMONEY;
                }
                String errorMsg = error.getMessage() + "\n" + errDescr;
                if (WAITSRV != oldStatus) {   // получен ответ после таймаута или ошибки
                    createPostingHistory(wrapper.getId());
                }
                setOperationRqStatusReceive(wrapper, movementId, errStatus, error.getCode(), errorMsg);        // одобрен
            }
        }
        catch (Exception e) {
            String msg = String.format("Ошибка при обработке ответа от сервиса движений по запросу ID = %s", data);
            log.info(msg);
            auditController.error(ManualOperation, msg, null, movementId, e);
        }
    }

    public MovementCreateData createMovementData(BatchPosting posting, ManualOperationWrapper wrapper) {
        String movementDr = null, movementCr = null;
        String oper = posting.getId().toString();
        String rand = getRundomUUID(6);
        if (posting.isControllableDebit()) {
            movementDr = oper + "D." + rand;
        }
        if (posting.isControllableCredit()) {
            movementCr = oper + "C." + rand;
        }
        if (isEmpty(movementDr) && isEmpty(movementCr)) {
            return null;
        }
        Date operday = operdayController.getOperday().getCurrentDate();
        MovementCreateData data = fillMovementData(posting, movementDr, movementCr, operday);
        String msg1 = String.format("Отправка в MovementCreate: ID = '%s', AC_DR = '%s', AMT_DR = %s, AC_CR = '%s', AMT_CR = %s",
                data.getMessageUUID(), data.getAccountCBD(), null == data.getOperAmountD() ? "" : data.getOperAmountD().toString(),
                data.getAccountCBC(), null == data.getOperAmountC() ? "" : data.getOperAmountC().toString());
        log.info(msg1);

        auditController.info(getLogCode(wrapper), msg1, postingName, getWrapperId(wrapper));

        return data;
    }

    private String getRundomUUID(int num) {
        return UUID.randomUUID().toString().substring(0, num);
    }

    private MovementCreateData fillMovementData(BatchPosting posting,
                                                String movementDr, String movementCr, Date operday) {
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

        DataRecord drForDebit = getCustomerInfo(posting.getAccountDebit());
        
        PaymentDetails debitPaymentDetails = new PaymentDetails(posting.getCurrencyDebit().getCurrencyCode(), 
                posting.isControllableDebit(), 
                (drForDebit != null) ? drForDebit.getString("BXRUNM").trim() : "", 
                (drForDebit != null) ? drForDebit.getString("BXTPID").trim() : "",
                posting.getAccountDebit(),
                posting.getAmountDebit()
        );
                
        data.setDebitPaymentDetails(debitPaymentDetails);

        DataRecord drForCredit = getCustomerInfo(posting.getAccountCredit());
        
        PaymentDetails creditPaymentDetails = new PaymentDetails(posting.getCurrencyCredit().getCurrencyCode(),
                posting.isControllableCredit(), 
                (drForCredit != null) ? drForCredit.getString("BXRUNM").trim() : "", 
                (drForCredit != null) ? drForCredit.getString("BXTPID").trim() : "",
                posting.getAccountCredit(),
                posting.getAmountCredit()
        );
        
        data.setCreditPaymentDetails(creditPaymentDetails);

        data.setMessageUUID(ifEmpty(data.getOperIdD(), "") + "." + ifEmpty(data.getOperIdC(), "")); //+"."+UUID.randomUUID().toString().substring(0,6));

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

        return data;
    }

    public DataRecord getCustomerInfo(String accountNumber) {
        try {
            String sql = "SELECT BXRUNM, BXTPID FROM SDCUSTPD SD " +
                            "JOIN BSAACC BSA ON BSA.BSAACNNUM = SD.BBCUST " +
                            "WHERE BSA.ID = ?";
            DataRecord res = postingRepository.selectFirst(sql, accountNumber);
            return res;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }
    
}
