package ru.rbt.barsgl.ejb.integr.acc;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.acc.*;
import ru.rbt.barsgl.ejb.entity.dict.AccountingType;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.ValidationAwareHandler;
import ru.rbt.barsgl.ejb.repository.*;
import ru.rbt.barsgl.ejb.repository.dict.AccountingTypeRepository;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.barsgl.shared.ErrorList;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.acc.GLAccount.RelationType.*;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.ejbcore.validation.ErrorCode.*;

/**
 * Created by ER18837 on 04.08.15.
 */
public class GLAccountProcessor extends ValidationAwareHandler<AccountKeys> {

    @Inject
    private GLAccountRepository glAccountRepository;

    @Inject
    private AccountingTypeRepository accountingTypeRepository;

    @EJB
    private GLAccountRequestRepository accountRequestRepository;

    @Inject
    private  GLOperationRepository glOperationRepository;

    @Inject
    private AccRepository accRepository;

    @Inject
    private BsaAccRepository bsaAccRepository;

    @Inject
    private BankCurrencyRepository bankCurrencyRepository;

    @Inject
    private OperdayController operdayController;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

    @EJB
    private AuditController auditController;

    @EJB
    private GLAccountService accountService;

    @Override
    public void fillValidationContext(AccountKeys target, ValidationContext context) {

        // Валюта
        context.addValidator(() -> {
            if (isEmpty(target.getCurrency())) {
                throw new ValidationError(FIELD_IS_EMPTY, "Валюта");
            }
            BankCurrency currency = bankCurrencyRepository.getCurrency(target.getCurrency());    // есди нет - ValidationError
        });

        // Бранч
        context.addValidator(() -> {
            String fieldValue = target.getBranch();
            String fieldName = "Отделение";
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            }
            String filial = glOperationRepository.getFilialByBranch(fieldValue);
            if (isEmpty(filial)) {
                throw new ValidationError(BRANCH_NOT_FOUND, "", fieldValue, fieldName);
            }
        });

        // Клиент
        context.addValidator(() -> {
            String fieldValue = target.getCustomerNumber();
            String fieldName = "Код клиента";
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            }
            if (null == glAccountRepository.getCustomerType(fieldValue)) {
                throw new ValidationError(CUSTOMER_NUMBER_NOT_FOUND, "", fieldValue, fieldName);
            }
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

        // Тип счета
        context.addValidator(() -> {
            String fieldValue = target.getAccount2();
            String fieldName = "БС2";
            int maxLen = 5;
            if (isEmpty(fieldValue) || fieldValue.length() != maxLen) {
                throw new ValidationError(ACCOUNT_FORMAT_INVALID, "", fieldValue,  fieldName);
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

        // Номер сделки и субсделки - часть есть в GLAccountController
        context.addValidator(() -> {
            String fieldValue = target.getDealId();
            String fieldName = "Номер сделки";
            int maxLen = 20;
            if (null != fieldValue && fieldValue.length() > maxLen) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });

        context.addValidator(() -> {
            String fieldValue = target.getSubDealId();
            String fieldName = "Номер субсделки";
            int maxLen = 20;
            if (null != fieldValue && fieldValue.length() > maxLen) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });

        //если задан номер субсделки, должен быть номер сделки
        context.addValidator(() -> {
            String dealId = target.getDealId();
            String subDealId = target.getSubDealId();
            if (isEmpty(dealId) && !isEmpty(subDealId))
                throw new ValidationError(SUBDEAL_ID_NOT_EMPTY, GLOperation.OperSide.N.getMsgName());
        });

        // TODO дата открытия
    }

    public void checkDateOpenMnl(GLAccount glAccount, Date dateOpen) {
        // проверка даты открытия
        checkDateOpen(dateOpen, glAccount.getBsaAcid(), "Дата открытия");
        // проверка даты открытия для 707 счетов
        checkDateOpen707(dateOpen, glAccount.getBsaAcid(), "Дата открытия");
        // проверка необарботанных операций
//        checkOperationsBefore(glAccount, dateOpen, "Операции");
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
//            checkOperationsAfter(glAccount, dateClose, "Операции");
            // проверка движений после
            checkBalanceAfter(glAccount, dateClose, "Баланс");
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

    // Дата открытия счета
    private void checkDateOpen707(Date dateOpen, String bsaAcid, String fieldName) {
        Date currentDate = operdayController.getOperday().getCurrentDate();     // текущий опердень
        if (StringUtils.substr(bsaAcid, 0, 3).equals("707")) {
            Date[] spodDates = glAccountRepository.getSpodDates(operdayController.getOperday().getCurrentDate());
            Assert.notNull(spodDates, "Невозможно определить даты начала и окончания SPOD");
            Date start446p = spodDates[0];
            Date lastSpod = spodDates[1];
            if (dateOpen.after(lastSpod)) {
                throw new ValidationError(ACCOUNT_707_AFTER_SPOD, dateUtils.onlyDateString(lastSpod));
            }
            if (dateOpen.before(start446p)) {
                throw new ValidationError(ACCOUNT_707_BEFORE_446p, dateUtils.onlyDateString(lastSpod));
            }
        }
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

    // Баланс счета при изменении даты закрытия / открытия
    private void checkBalance(GLAccount account, Date dateFrom, Date dateTo, String fieldName) {
        BigDecimal balance = glAccountRepository.getAccountBalance(account.getBsaAcid(), account.getAcid(), dateTo);
        if (!balance.equals(BigDecimal.ZERO)) {
            throw new ValidationError(BALANCE_NOT_ZERO, account.getBsaAcid());
        }
    }

    // Баланс счета при изменении даты закрвтия / открытия
    private void checkBalanceBefore(GLAccount account, Date toDate, String fieldName) {
        if (glAccountRepository.hasAccountBalanceBefore(account.getBsaAcid(), account.getAcid(), toDate)) {
            throw new ValidationError(ACCOUNT_IN_USE_BEFORE, account.getBsaAcid(),
                    "", dateUtils.onlyDateString(toDate));
        }
    }

    private void checkBalanceAfter(GLAccount account, Date toDate, String fieldName) {
        if (glAccountRepository.hasAccountBalanceAfter(account.getBsaAcid(), account.getAcid(), toDate)) {
            throw new ValidationError(ACCOUNT_IN_USE_AFTER, account.getBsaAcid(),
                    "", dateUtils.onlyDateString(toDate));
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

    // Операции по счету
    private void checkOperationsBefore(GLAccount account, Date toDate, String fieldName) {
        int count = glOperationRepository.getOperationsByAccountBefore(account.getBsaAcid(), toDate);
        if (count != 0) {
            throw new ValidationError(ACCOUNT_IN_USE_BEFORE, account.getBsaAcid(),
                    Integer.toString(count), dateUtils.onlyDateString(toDate));
        }
    }

    public void checkDealId(final Date dateOpen, final String dealSource, final String dealId, final String subDealId) {
        checkDealId(dateOpen, dealSource, dealId, subDealId, null);
    }

    public void checkDealId(final Date dateOpen, final String dealSource, final String dealId, final String subDealId, final String paymentRef) {   // AccountKeys keys,
        GLOperation.OperSide side = GLOperation.OperSide.N;

        // должен быть задан номер сделки
        if (isEmpty(dealId) && isEmpty(paymentRef)) {
            throw new ValidationError(DEALID_PYMANTREF_IS_EMPTY, "Номер сделки", "Номер платежного документа");
        }

        // параметры по источнику сделки
        DataRecord param = glAccountRepository.getDealSQParams(dealSource, dateOpen);
        if (null == param) {
            return;              // для этого источника номер сделки ни нужен
        }

/*
        // должен быть задан номер сделки
        if (isEmpty(dealId)) {
            throw new ValidationError(DEAL_ID_IS_EMPTY, side.getMsgName());
        }
*/

        //если задан номер субсделки, должен быть номер сделки
        if (isEmpty(dealId) && !isEmpty(subDealId))
            throw new ValidationError(SUBDEAL_ID_NOT_EMPTY, side.getMsgName());

        // найти SQ по номеру клиента, валюте и номеру сделки
        final String dealIDfrom = param.getString("DEALID");
        if (!dealIDfrom.equals("Y")) {
            if (isEmpty(subDealId)) {              // не задан subDealID
                throw new ValidationError(SUBDEAL_ID_IS_EMPTY, side.getMsgName());
            }
        }

    }

    /**
     * Заполняет основные параметры счета
     * @param bsaAcid       - номер счета ЦБ
     * @param operation     - операция
     * @param side      - сторона операции
     * @param keys          - ключи счета
     * @return
     */
    public GLAccount createGlAccount(String bsaAcid, GLOperation operation, GLOperation.OperSide side, Date dateOpen,
                                     AccountKeys keys, GLAccount.RelationType rlnType, String exCCY, GLAccount.OpenType openType) {
        Assert.notNull(rlnType);
        GLAccount glAccount = new GLAccount();

        // номер счета, способ создания, операция, сторона операции
        glAccount.setBsaAcid(bsaAcid);
        glAccount.setOpenType(openType.name());
        glAccount.setOperation(operation);
        glAccount.setOperSide(side);

        // даты
        checkDateOpen(dateOpen, bsaAcid, "Дата открытия");
        if (null == operation)  // только для ручных
            checkDateOpen707(dateOpen, bsaAcid, "Дата открытия");
        glAccount.setDateOpen(dateOpen);
        glAccount.setDateClose(null);                       // TODO уточнить
        Operday operday = operdayController.getOperday();
        glAccount.setDateRegister(operday.getCurrentDate());
        glAccount.setDateModify(operday.getCurrentDate());

        // валюта ЦБ
        BankCurrency ccy = bankCurrencyRepository.getCurrency(keys.getCurrency());
        glAccount.setCurrency(ccy);

        // бранч
        glAccount.setBranch(keys.getBranch());
        // номер клиента
        glAccount.setCustomerNumber(keys.getCustomerNumber());
        int cnum = (int) stringToLong(side, "номер клиента", keys.getCustomerNumber(), AccountKeys.getiCustomerNumber());
        glAccount.setCustomerNumberD(cnum);
        // тип счета
        long acctype = stringToLong(side, "тип счета", keys.getAccountType(), AccountKeys.getiAccountType());
        glAccount.setAccountType(acctype);

        // тип собственности клиента
        if(!isEmpty(keys.getCustomerType()))
            glAccount.setCbCustomerType((short)stringToLong(side, "тип клиента", keys.getCustomerType(), AccountKeys.getiCustomerType()));
        // код срока
        if(!isEmpty(keys.getTerm()))
            glAccount.setTerm((short)stringToLong(side, "код срока", keys.getTerm(), AccountKeys.getiTerm()));

        // номер последовательности из АЕ
        glAccount.setGlSequence(keys.getGlSequence());

        glAccount.setRelationType(rlnType);
//        glAccount.setRelationType(null != operation && accountService.isAccountWithKeys(operation, side)
//                ? ZERO : (isEmpty(keys.getPlCode()) ? FOUR : TWO));

        glAccount.setExcangeCurrency(exCCY);

        return glAccount;
    }

    public GLAccount createGlAccountTH(String bsaAcid, GLOperation operation, GLOperation.OperSide side, Date dateOpen, AccountKeys keys, GLAccount.OpenType openType) {

        GLAccount glAccount = new GLAccount();

        // номер счета, способ создания, операция, сторона операции
        glAccount.setBsaAcid(bsaAcid);
        glAccount.setOpenType(openType.name());
        glAccount.setOperation(operation);
        glAccount.setOperSide(side);

        // даты
        checkDateOpen(dateOpen, bsaAcid, "Дата открытия");
        if (null == operation)  // только для ручных
            checkDateOpen707(dateOpen, bsaAcid, "Дата открытия");
        glAccount.setDateOpen(dateOpen);
        glAccount.setDateClose(null);                       // TODO уточнить
        Operday operday = operdayController.getOperday();
        glAccount.setDateRegister(operday.getCurrentDate());
        glAccount.setDateModify(operday.getCurrentDate());

        // валюта ЦБ
        BankCurrency ccy = bankCurrencyRepository.getCurrency(keys.getCurrency());
        glAccount.setCurrency(ccy);

        glAccount.setFilial(glAccountRepository.getFilialByCompanyCode(keys.getCompanyCode()));
        glAccount.setCompanyCode(keys.getCompanyCode());
        // бранч
        DataRecord dr = glAccountRepository.getIMBCBBRP(keys.getCompanyCode());
        glAccount.setBranch(null!=dr?dr.getString("A8BRCD"):"");
        // номер клиента
        glAccount.setCustomerNumber(null!=dr?dr.getString("A8BICN"):"");
        keys.setCustomerNumber(null!=dr?dr.getString("A8BICN"):"");
        int cnum = (int) stringToLong(side, "номер клиента", keys.getCustomerNumber(), AccountKeys.getiCustomerNumber());
        glAccount.setCustomerNumberD(cnum);
        // тип счета
        long acctype = stringToLong(side, "тип счета", keys.getAccountType(), AccountKeys.getiAccountType());
        glAccount.setAccountType(acctype);

        // тип собственности клиента
        keys.setCustomerType("0");
        if(!isEmpty(keys.getCustomerType()))
            glAccount.setCbCustomerType((short)stringToLong(side, "тип клиента", keys.getCustomerType(), AccountKeys.getiCustomerType()));
        // код срока
        keys.setTerm("0");
        if(!isEmpty(keys.getTerm()))
            glAccount.setTerm((short)stringToLong(side, "код срока", keys.getTerm(), AccountKeys.getiTerm()));

        // номер последовательности из АЕ
        glAccount.setGlSequence(keys.getGlSequence());


        dr = glAccountRepository.getActParamByAccType(keys.getAccountType());
        glAccount.setBalanceAccount2(dr.getString("ACC2"));
        glAccount.setAccountCode(!isEmpty(dr.getString("ACOD")) ? dr.getShort("ACOD") : null);
        glAccount.setAccountSequence(!isEmpty(dr.getString("AC_SQ")) ? dr.getShort("AC_SQ") : null);
        glAccount.setAcid(" ");
        glAccount.setPassiveActive(" ");
        glAccount.setDealSource(keys.getDealSource());
        glAccount.setDealId(null);
        glAccount.setSubDealId(null);

        AccountingType accType = accountingTypeRepository.findById(AccountingType.class,keys.getAccountType());

        glAccount.setDescription(accType.getAccountName());
        glAccount.setRelationType(GLAccount.RelationType.NINE);

        return glAccount;
    }

    /**
     * Заполняет доп параметры счета по ключам или из справочников
     * @param glAccount     - счет
     * @param keys          - ключи счета
     * @throws SQLException
     */
    public void enrichment(GLAccount glAccount, AccountKeys keys) {
        glAccount.setFilial(keys.getFilial());
        glAccount.setCompanyCode(keys.getCompanyCode());

        // TODO потом проверять наличие полей и, если надо, определять по справочникам
        glAccount.setBalanceAccount2(keys.getAccount2());
        glAccount.setPlCode(keys.getPlCode());
        glAccount.setPassiveActive(glAccountRepository.getPassiveActive(glAccount.getBalanceAccount2()));
/*
        short acode = (short) stringToLong(side, "ACOD Midas", keys.getAccountCode(), AccountKeys.getiAccountCode());
        glAccount.setAccountCode(acode);
        short sq = (short) stringToLong(side, "SQ Midas", keys.getAccSequence(), AccountKeys.getiAccSequence());
        glAccount.setAccountSequence(sq);
        String acid = glAccountRepository.makeMidasAccount(
                glAccount.getCustomerNumberD(),
                glAccount.getCurrency().getCurrencyCode(),
                glAccount.getBranch(),
                glAccount.getAccountCode(),
                glAccount.getAccountSequence()
        );
*/
        glAccount.setAccountCode(Short.parseShort(keys.getAccountCode()));
        glAccount.setAccountSequence(Short.parseShort(keys.getAccSequence()));
        glAccount.setAcid(keys.getAccountMidas());

        glAccount.setDealSource(keys.getDealSource());
        glAccount.setDealId(keys.getDealId());
        glAccount.setSubdealId(keys.getSubDealId());

        String customerName = glAccountRepository.getCustomerName(keys.getCustomerNumber());
        if (isEmpty(customerName)) {
            throw new ValidationError(CUSTOMER_NUMBER_NOT_FOUND,
                    keys.getCustomerNumber(), Long.toString(AccountKeys.getiCustomerNumber()), "номер клиента");
        }
        String accountName = keys.getDescription();
        if (isEmpty(accountName))
            accountName = glAccountRepository.getAccountNameByType(glAccount.getAccountType());
        glAccount.setDescription(accountName);

    }

    /**
     * Заполняет доп параметры счета по ключам или из справочников без Майдас специфики
     * @param glAccount     - счет
     * @param keys          - ключи счета
     * @throws SQLException
     */
    public void enrichmentPureParams(GLAccount glAccount, AccountKeys keys) {
        glAccount.setFilial(keys.getFilial());
        glAccount.setCompanyCode(keys.getCompanyCode());

        glAccount.setBalanceAccount2(keys.getAccount2());
        glAccount.setPlCode(keys.getPlCode());
        glAccount.setPassiveActive(glAccountRepository.getPassiveActive(glAccount.getBalanceAccount2()));

        glAccount.setDealSource(keys.getDealSource());
        glAccount.setDealId(keys.getDealId());
        glAccount.setSubdealId(keys.getSubDealId());
        glAccount.setAcid(" ");


        String customerName = glAccountRepository.getCustomerName(keys.getCustomerNumber());
        if (isEmpty(customerName)) {
            throw new ValidationError(CUSTOMER_NUMBER_NOT_FOUND,
                    keys.getCustomerNumber(), Long.toString(AccountKeys.getiCustomerNumber()));
        }
        String accountName = keys.getDescription();
        if (isEmpty(accountName))
            accountName = glAccountRepository.getAccountNameByType(glAccount.getAccountType());
        glAccount.setDescription(accountName);

    }

    public long stringToLong(GLOperation.OperSide side, String fieldName, String fieldValue, int fieldIndex) {
        try {
            return Long.parseLong(fieldValue);
        } catch (NumberFormatException e) {
            throw new ValidationError(ACCOUNT_KEY_FORMAT_INVALID, side.getMsgName(), fieldName, fieldValue, Long.toString(fieldIndex));
        }
    }

    public String validationErrorMessage(GLOperation.OperSide operSide, List<ValidationError> errors, ErrorList descriptors) {
        StringBuilder result = new StringBuilder(format("Ошибки валидации ключей счета %s:\n", operSide.getMsgName()));
        result.append(validationErrorsToString(errors, descriptors));
        return result.toString();
    }

}
