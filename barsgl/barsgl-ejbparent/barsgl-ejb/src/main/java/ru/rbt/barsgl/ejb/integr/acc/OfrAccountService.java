package ru.rbt.barsgl.ejb.integr.acc;

import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.ErrorList;
import ru.rbt.shared.ExceptionUtils;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Account;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.ejbcore.util.StringUtils.substr;
import static ru.rbt.ejbcore.validation.ErrorCode.*;
import static ru.rbt.ejbcore.validation.ValidationError.initSource;
/**
 * Created by ER18837 on 13.11.15.
 */
@Stateless
@LocalBean
public class OfrAccountService {

    @EJB
    private GLAccountRepository accountRepository;

    @EJB
    private GLOperationRepository operationRepository;

    @Inject
    private OfrAccountProcessor ofrAccountProcessor;

    @EJB
    private AuditController auditController;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

    public String findAccountCB(AccountKeys keys, Date dateFrom) throws Exception {
        return accountRepository.executeInNonTransaction(connection -> {
            try {
                return ofrAccountProcessor.findAccount(connection, keys, dateFrom);
            } catch (Exception e) {
                throw new DefaultApplicationException(getErrorMessage(e), e);
            }
        });
    }

    public String createAccountCB(AccountKeys keys, Date dateOpen, Date dateClose, Date dateFrom) throws Exception {
        return accountRepository.executeTransactionally(connection -> {
            try {
                return ofrAccountProcessor.createAccount(connection, keys, dateOpen, dateClose, dateFrom);
            } catch (Exception e) {
                throw new DefaultApplicationException(getErrorMessage(e), e);
            }
        });
    }

    public String createAccountCBnoTrans(AccountKeys keys, Date dateOpen, Date dateClose, Date dateFrom) throws Exception {
        return accountRepository.executeInNonTransaction(connection -> {
            try {
                return ofrAccountProcessor.createAccount(connection, keys, dateOpen, dateClose, dateFrom);
            } catch (Exception e) {
                throw new DefaultApplicationException(getErrorMessage(e), e);
            }
        });
    }

    public RpcRes_Base<ManualAccountWrapper> createOfrManualAccount(ManualAccountWrapper wrapper) throws Exception {
        try {
            // создать ключи счета, заполнить недостающие поля
            AccountKeys keys = ofrAccountProcessor.createWrapperAccountKeys(wrapper);

            // валидация
            List<ValidationError> errors = ofrAccountProcessor.validate(keys, new ValidationContext());
            if (!errors.isEmpty()) {
                throw new DefaultApplicationException(ofrAccountProcessor.validationErrorMessage(errors, wrapper.getErrorList()));
            }
            ofrAccountProcessor.fillWrapperFields(wrapper, keys);

            // проверить, есть ли счет
            Date dateFrom = accountRepository.getDateStart446p();   // начало действия 446П
            String accountCB = findAccountCB(keys, dateFrom);
            if (!isEmpty(accountCB)) {          // Такой счет уже есть
                wrapper.setBsaAcid(accountCB);
                throw new ValidationError(ACCOUNT_OFR_ALREADY_EXISTS, accountCB);
            }

            // создать счет
            accountCB = createAccountCBnoTrans(keys,
//                    accountCB = createAccountCB(keys,
                            ofrAccountProcessor.getDateOpen(wrapper), ofrAccountProcessor.getDateClose(wrapper), dateFrom);

            // проверить, что счет создался
            if (!accountRepository.checkAccountRlnExists(accountCB, keys.getAccountMidas())) {
                // только в случае, если внешняя процедура отработала неверно
                throw new ValidationError(ACCOUNT_OFR_NOT_EXISTS, accountCB);
            };
            wrapper.setBsaAcid(accountCB);

            auditController.info(Account, format("Создан счет ОФР '%s' по ручному вводу", accountCB));
            return new RpcRes_Base<>(wrapper, false, format("Создан счет ОФР: '%s'", wrapper.getBsaAcid()));
        } catch (Throwable e) {
//            String errMessage = wrapper.getErrorMessage();
            String errMessage = accountErrorMessage(e, wrapper.getErrorList(), initSource());
            auditController.error(Account, format("Ошибка при создании счета ОФР по ручному вводу для AccountType = '%s'",
                    wrapper.getAccountType()), null, e);
            return new RpcRes_Base<>(wrapper, true, errMessage);

        }
    }

    /**
     * Получить счет Майдас и дополнительные параметры счета по основным параметрам
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualAccountWrapper> getOfrAccountParameters(ManualAccountWrapper wrapper) throws Exception {
        try {
            AccountKeys keys = ofrAccountProcessor.createWrapperAccountKeys(wrapper);

            short custType = wrapper.getCbCustomerType();
            ofrAccountProcessor.fillWrapperFields(wrapper, keys);

            if (isEmpty(keys.getCustomerNumber())) {
                throw new ValidationError(BRANCH_NOT_FOUND, "ОФР", wrapper.getBranch(), "Отделение");
            }
            if (isEmpty(keys.getAccount2())) {
                throw new ValidationError(ACCOUNT_MIDAS_NOT_FOUND, keys.getAccountMidas());
            }
            if (isEmpty(keys.getPlCode())) {
                throw new ValidationError(PLCODE_NOT_FOUND, keys.getAccountCode(), keys.getAccSequence(), keys.getCustomerType());
            }
            String message = "";
            if (custType != wrapper.getCbCustomerType()){
                message = format("Для символа ОФР не найден тип собственности '%d', заменен на '%d'\n",
                        custType, wrapper.getCbCustomerType());
            }
            return new RpcRes_Base<>(wrapper, false, message);
        } catch (Throwable e) {
            String errMessage = accountErrorMessage(e, wrapper.getErrorList(), initSource());
            auditController.error(Account, format("Ошибка при создании счета ОФР по ручному вводу для счета Midas '%s'",
                    wrapper.getAcid()), null, e);
            return new RpcRes_Base<>(wrapper, true, errMessage);

        }
    }

    private String accountErrorMessage(Throwable e, ErrorList errorList, String source) {
        String errMessage = getErrorMessage(e);
        final String start = "DefaultApplicationException: ";
        final String stop = "\r\n\tat";
        String errorCode = ValidationError.getErrorCode(errMessage);
        String errorMessage = substr(errMessage, start, stop);
        String errorText = ValidationError.getErrorText(errorMessage);
        errorList.addNewErrorDescription("", "", errorText, errorCode);
        return errorList.getErrorMessage();
    }

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class);
    }

}
