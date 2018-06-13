package ru.rbt.barsgl.ejb.integr.acc;

/**
 * Created by er23851 on 10.04.2017.
 */
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.ValidationAwareHandler;
import ru.rbt.barsgl.ejb.repository.BankCurrencyRepository;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejb.repository.GLTechAccountRepository;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.barsgl.shared.ErrorList;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ValidationError;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.ejbcore.validation.ErrorCode.*;


public class GLAccountProcessorTech extends ValidationAwareHandler<AccountKeys> {

    @Inject
    private BankCurrencyRepository bankCurrencyRepository;

    @Inject
    private GLTechAccountRepository glTechAccountRepository;

    @Inject
    private GLOperationRepository glOperationRepository;

    @Inject
    private OperdayController operdayController;

    @Inject
    private DateUtils dateUtils;


    public void fillValidationContext(AccountKeys target, ValidationContext context) {

        // Валюта
        context.addValidator(() -> {
            if (isEmpty(target.getCurrency())) {
                throw new ValidationError(FIELD_IS_EMPTY, "Валюта");
            }
            BankCurrency currency = bankCurrencyRepository.getCurrency(target.getCurrency());    // есди нет - ValidationError
        });

        // Тип счета
        context.addValidator(() -> {
            String fieldValue = target.getAccountType();
            String fieldName = "Тип счета";
            int maxLen = 10;
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            }
            Long accType;
            try {
                accType = Long.parseLong(fieldValue);
            } catch (NumberFormatException e) {
                throw new ValidationError(ACCOUNT_TYPE_IS_NOT_NUMBER, "", fieldValue, fieldName);
            }
        });

        // Источник сделки
        context.addValidator(() -> {
            String fieldValue = target.getDealSource();
            String fieldName = "Источник сделки";
            int maxLen = 8;
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            } else if (fieldValue.length() > maxLen) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });

        // TODO дата открытия
    }

    // Баланс счета при изменении даты закрвтия / открытия
    private void checkBalance(GLAccount account, Date dateFrom, Date dateTo, String fieldName) {
        BigDecimal balance = glTechAccountRepository.getAccountBalance(account.getBsaAcid(), account.getId(), dateTo);
        if (!balance.equals(BigDecimal.ZERO)) {
            throw new ValidationError(BALANCE_NOT_ZERO, account.getBsaAcid());
        }
    }

    // Баланс счета при изменении даты закрвтия / открытия
    private void checkBalanceBefore(GLAccount account, Date toDate, String fieldName) {
        if (glTechAccountRepository.hasAccountBalanceBefore(account.getBsaAcid(), account.getId(), toDate)) {
            throw new ValidationError(ACCOUNT_IN_USE_BEFORE, account.getBsaAcid(),
                    "", dateUtils.onlyDateString(toDate));
        }
    }

    private void checkBalanceAfter(GLAccount account, Date toDate, String fieldName) {
        if (glTechAccountRepository.hasAccountBalanceAfter(account.getBsaAcid(), account.getId(), toDate)) {
            throw new ValidationError(ACCOUNT_IN_USE_AFTER, account.getBsaAcid(),
                    "", dateUtils.onlyDateString(toDate));
        }
    }

    public void checkDateOpenMnl(GLAccount glAccount, Date dateOpen) {
        // проверка даты открытия
        checkDateOpen(dateOpen, glAccount.getBsaAcid(), "Дата открытия");
        // проверка необарботанных операций
        checkOperationsBefore(glAccount, dateOpen, "Операции");
        // проверка движений после
        checkBalanceBefore(glAccount, dateOpen, "Баланс");
    }

    public void checkDateCloseMnl(GLAccount glAccount, Date dateOpen, Date dateClose) {
        if (null != dateClose) {
            // проверка даты закрытия
            checkDateClose(dateOpen, dateClose, "Дата закрытия");
            // проверка баланса
            checkBalance(glAccount, dateClose, glAccount.getDateLast(), "Баланс");
            // проверка необарботанных операций
            //checkOperationsAfter(glAccount, dateClose, "Операции");
            // проверка движений после
            checkBalanceAfter(glAccount, dateClose, "Баланс");
        }
    }

    // Операции по счету
    private void checkOperationsBefore(GLAccount account, Date toDate, String fieldName) {
        int count = glOperationRepository.getOperationsByAccountBefore(account.getBsaAcid(), toDate);
        if (count != 0) {
            throw new ValidationError(ACCOUNT_IN_USE_BEFORE, account.getBsaAcid(),
                    Integer.toString(count), dateUtils.onlyDateString(toDate));
        }
    }

    // Дата открытия счета
    private void checkDateOpen(Date dateOpen, String bsaAcid, String fieldName) {
        Date currentDate = operdayController.getOperday().getCurrentDate();     // текущий опердень
        if ( null == dateOpen ) {
            throw new ValidationError(DATE_AFTER_OPERDAY, fieldName,
                    "не задана",
                    dateUtils.onlyDateString(currentDate));
        } else if (dateOpen.after(currentDate)){
            throw new ValidationError(DATE_AFTER_OPERDAY, fieldName,
                    dateUtils.onlyDateString(dateOpen),
                    dateUtils.onlyDateString(currentDate));
        }
        // TODO проверка на выходные
    }

    // Дата закрытия счета
    private void checkDateClose(Date dateOpen, Date dateClose, String fieldName) {
        if (null == dateClose) {        // надо отменить закрытие счета
            return;
        }
        if (dateClose.before(dateOpen)) {
            throw new ValidationError(CLOSEDATE_NOT_VALID,
                    dateUtils.onlyDateString(dateClose),
                    dateUtils.onlyDateString(dateOpen));
        }
        Date currentDate = operdayController.getOperday().getCurrentDate();     // текущий опердень
        if (dateClose.after(currentDate)){
            throw new ValidationError(DATE_AFTER_OPERDAY, fieldName,
                    dateUtils.onlyDateString(dateClose),
                    dateUtils.onlyDateString(currentDate));
        }
    }

    // Операции по счету
    private void checkOperationsAfter(GLAccount account, Date fromDate, String fieldName) {
        int count = glOperationRepository.getOperationsByAccountAfter(account.getBsaAcid(), fromDate);
        if (count != 0) {
            throw new ValidationError(ACCOUNT_IN_USE_AFTER, account.getBsaAcid(),
                    Integer.toString(count), dateUtils.onlyDateString(fromDate));
        }
    }

    public void validateGLAccountMnlTech(GLAccount glAccount, Date dateOpen, Date dateClose,
                                         AccountKeys keys, ErrorList descriptors) throws Exception {
        List<ValidationError> errors = validate(keys, new ValidationContext());
        if (!errors.isEmpty()) {
            throw new DefaultApplicationException(validationErrorMessage(GLOperation.OperSide.N, errors, descriptors));
        }
        checkDateOpenMnl(glAccount, dateOpen);
        if (null != dateClose) {
            checkDateCloseMnl(glAccount, dateOpen, dateClose);
        }
    }

    public String validationErrorMessage(GLOperation.OperSide operSide, List<ValidationError> errors, ErrorList descriptors) {
        StringBuilder result = new StringBuilder(format("Ошибки валидации ключей счета %s:\n", operSide.getMsgName()));
        result.append(validationErrorsToString(errors, descriptors));
        return result.toString();
    }

}
