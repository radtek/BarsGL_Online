package ru.rbt.barsgl.ejb.integr.acc;

import ru.rb.ucb.util.AccountUtil;
import ru.rb.ucb.util.StringUtils;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeysBuilder;
import ru.rbt.barsgl.ejb.integr.ValidationAwareHandler;
import ru.rbt.barsgl.ejb.repository.BankCurrencyRepository;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.ErrorList;
import ru.rbt.barsgl.shared.account.ManualAccountWrapper;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.ejbcore.validation.ErrorCode.*;

/**
 * Created by ER18837 on 19.11.15.
 */

public class OfrAccountProcessor extends ValidationAwareHandler<AccountKeys> {

    @Inject
    private GLOperationRepository operationRepository;

    @Inject
    private GLAccountRepository accountRepository;

    @Inject
    private BankCurrencyRepository currencyRepository;

    @Inject
    private OperdayController operdayController;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

    @EJB
    private AuditController auditController;

    @Override
    public void fillValidationContext(AccountKeys target, ValidationContext context) {

        // Балансовый счет 2-го порядка
        context.addValidator(() -> {
            String fieldValue = target.getAccount2();
            String fieldName = "Балансовый счет 2-го порядка";
            int len = 5;
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            }
            if (fieldValue.length() != len) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(len));
            }
        });

        // Валюта
        context.addValidator(() -> {
            String fieldValue = target.getCurrencyDigital();
            String fieldName = "Валюта";
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            }
        });

        // Код филиала
        context.addValidator(() -> {
            String fieldValue = target.getCompanyCode();
            String fieldName = "Код филиала";
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            }
        });

        // Символ ОФР
        context.addValidator(() -> {
            String fieldValue = target.getPlCode();
            String fieldName = "Символ ОФР";
            int len = 5;
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            }
            if (fieldValue.length() != len) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(len));
            }
        });

        // Счет Майдас
        context.addValidator(() -> {
            String fieldValue = target.getAccountMidas();
            String fieldName = "Счет Midas";
            int len = 20;
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            }
            if (fieldValue.length() != len) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(len));
            }
        });

        // Тип собственности
        context.addValidator(() -> {
            String fieldValue = target.getCustomerType();
            String fieldName = "Тип собственности клиента";
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            }
        });

        // Пассив / актив
        context.addValidator(() -> {
            String fieldValue = target.getPassiveActive();
            String fieldName = "Тип счета (пассив / актив)";
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            }
        });

    }

    // Дата открытия счета
    private void checkDateOpen(Date dateOpen, String fieldName) {
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
    }

    public String createAccount(Connection connection, AccountKeys keys, Date dateOpen, Date dateClose, Date dateFrom) throws Exception {
        checkDateOpen(dateOpen, "Дата открытия'");
        String bsaAcid = AccountUtil.createAccountCbNC(connection,      // Connection con,
                // String acc2, String ccc, String filialCode, String plCode, Date openDate, Date closeDate,
                keys.getAccount2(), keys.getCurrencyDigital(), keys.getCompanyCode(), keys.getPlCode(), dateOpen, dateClose,
                // String accid, String relationType, String customerType, String type)
                keys.getAccountMidas(), keys.getRelationType(), keys.getCustomerType(), keys.getPassiveActive()
                , dateFrom);     // 1.0.5

        return bsaAcid;
    }

    public String findAccount(Connection connection, AccountKeys keys, Date dateFrom) throws Exception {
        return AccountUtil.searchAccountCB(connection, keys.getAccountMidas(), keys.getCustomerType(), keys.getPlCode()
                , dateFrom, false); // 1.0.5
    }

    public void fillWrapperFields(ManualAccountWrapper wrapper, AccountKeys keys) {
        wrapper.setCustomerNumber((keys.getCustomerNumber()));
        wrapper.setAcid(keys.getAccountMidas());
        wrapper.setBalanceAccount2(keys.getAccount2());
        wrapper.setPlCode(keys.getPlCode());
        wrapper.setCbCustomerType(Short.parseShort(keys.getCustomerType()));
    }

    public String validationErrorMessage(List<ValidationError> errors, ErrorList descriptors) {
        StringBuilder result = new StringBuilder("Ошибки валидации ключей счета:\n");
        result.append(validationErrorsToString(errors, descriptors));
        return result.toString();
    }

    public Date getDateOpen(ManualAccountWrapper wrapper) {
        try {
            return new SimpleDateFormat(wrapper.dateFormat).parse(wrapper.getDateOpenStr());
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Date getDateClose(ManualAccountWrapper wrapper) {
        String dateStr = !isEmpty(wrapper.getDateCloseStr()) ? wrapper.getDateCloseStr() : wrapper.dateNull;
        try {
            return new SimpleDateFormat(wrapper.dateFormat).parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }


}
