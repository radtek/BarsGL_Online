package ru.rbt.barsgl.ejb.integr.oper;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.ValidationAwareHandler;
import ru.rbt.barsgl.ejb.repository.*;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.enums.InputMethod;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.common.mapping.od.BankCalendarDay.HolidayFlag.T;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;
import ru.rbt.barsgl.shared.enums.DealSource;
import ru.rbt.ejbcore.util.DateUtils;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.ejbcore.util.StringUtils.substr;
import static ru.rbt.ejbcore.validation.ErrorCode.*;

/**
 * Created by Ivan Sevastyanov
 * Базовый класс для обработчиков проводок в режиме online
 */
public abstract class IncomingPostingProcessor extends ValidationAwareHandler<EtlPosting> {

    @Inject
    private RateRepository rateRepository;

    @Inject
    private BankCurrencyRepository bankCurrencyRepository;

    @EJB
    private EtlPostingRepository etlPostingRepository;

    @EJB
    private GLOperationRepository glOperationRepository;

    @Inject
    private GLPostingRepository glPostingRepository;

    @Inject
    private GLAccountRepository glAccountRepository;

    @Inject
    private OperdayController operdayController;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

    @Inject
    private BankCalendarDayRepository calendarDayRepository;

    public abstract boolean isSupported(EtlPosting posting);

    public static final Pattern patternAccount = Pattern.compile("\\d{5}.{3}\\d{12}");
    public static final Pattern patternAccountKey = AccountKeys.getPattern();
    public static final Pattern patternDealId = Pattern.compile("([^;].{0,19};*)|([^;].{0,19};.{1,20})");

    @Override
    public void fillValidationContext(EtlPosting target, ValidationContext context) {
        // =========== Длина строковых полей =============
        // ИД проводки в АЕ
        context.addValidator(() -> {
            String fieldName = target.getColumnName("aePostingId");
            String fieldValue = target.getAePostingId();
            int maxLen = 20;
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            } else if (maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        // Система - Источник
        context.addValidator(() -> {
            String fieldName = target.getColumnName("sourcePosting");
            String fieldValue = target.getSourcePosting();
            int maxLen = 7;
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            } else if (maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        // Канал - Источник
        context.addValidator(() -> {
            String fieldName = target.getColumnName("chnlName");
            String fieldValue = target.getChnlName();
            int maxLen = 16;
            if (null != fieldValue &&  maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        // Идентификатор учётного события
        context.addValidator(() -> {
            String fieldName = target.getColumnName("eventId");
            String fieldValue = target.getEventId();
            int maxLen = 20;
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            } else if (maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        // Код департамента, создавшего проводку
        context.addValidator(() -> {
            String fieldName = target.getColumnName("deptId");
            String fieldValue = target.getDeptId();
            int maxLen = 4;
            if (null != fieldValue &&  maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        // описания
        context.addValidator(() -> {
            String fieldName = target.getColumnName("narrative");
            String fieldValue = target.getNarrative();
            final int maxLen = 300;
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            } else if (maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        context.addValidator(() -> {
            String fieldName = target.getColumnName("rusNarrativeLong");
            if (isEmpty(target.getRusNarrativeLong())) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            }
        });
        context.addValidator(() -> {
            String fieldName = target.getColumnName("rusNarrativeShort");
            if (isEmpty(target.getRusNarrativeShort())) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            }
        });

        // ============= Идентификаторы проводок ============
        // ИД платежа
        context.addValidator(() -> {
            String fieldName = target.getColumnName("paymentRefernce");
            String fieldValue = target.getPaymentRefernce();
            int maxLen = 20;
            if ( null != fieldValue && maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        // Номер сделки
        context.addValidator(() -> {
            String fieldName = target.getColumnName("dealId");
            String fieldValue = target.getDealId();
            if (!isEmpty(fieldValue)) {             // формат номера сделки
                if (!patternDealId.matcher(fieldValue).matches()) {
                    throw new ValidationError(DEALID_FORMAT_INVALID, fieldValue, fieldName);
                }
            }
        });
        // Идентификатор проводки хотя бы один
        context.addValidator(() -> {
            if (isEmpty(target.getDealId()) && isEmpty(target.getPaymentRefernce())) {
                throw new ValidationError(DEALID_PYMANTREF_IS_EMPTY,
                        target.getColumnName("dealId"), target.getColumnName("paymentRefernce"));
            }
        });

        // ============= Счета ==============
        // Формат счета дебета
        context.addValidator(() -> {
            checkAccount(target, GLOperation.OperSide.D, target.getAccountDebit(), "accountDebit",
                    target.getAccountKeyDebit(), "accountKeyDebit");
        });
        // Формат счета кредита
        context.addValidator(() -> {
            checkAccount(target, GLOperation.OperSide.C, target.getAccountCredit(), "accountCredit",
                    target.getAccountKeyCredit(), "accountKeyCredit");
        });
/*
        // Совпадение счетов или ключей счета по дебету и кредиту
        context.addValidator(() -> {
            if (!isEmpty(target.getAccountDebit()) && !isEmpty(target.getAccountCredit()) &&
                    target.getAccountDebit().equals(target.getAccountCredit())) {
                throw new ValidationError(ACCOUNTS_EQUAL, target.getAccountDebit(),
                        target.getColumnName("accountDebit"), target.getColumnName("accountCredit"));
            } else
            if (!isEmpty(target.getAccountKeyDebit()) && !isEmpty(target.getAccountKeyCredit()) &&
                    target.getAccountKeyDebit().equals(target.getAccountKeyCredit())) {
                throw new ValidationError(ACCKEYS_EQUAL, target.getAccountKeyDebit(),
                        target.getColumnName("accountKeyDebit"), target.getColumnName("accountKeyCredit"));
            }
        });
*/

        // ============ Валюта ==============
        // Валюта дебета
        context.addValidator(() -> {
            checkCurrency(target, GLOperation.OperSide.D, target.getCurrencyDebit(), "currencyDebit",
                    target.getAccountDebit(), target.getAccountKeyDebit());
        });
        // Валюта кредита
        context.addValidator(() -> {
            checkCurrency(target, GLOperation.OperSide.C, target.getCurrencyCredit(), "currencyCredit",
                    target.getAccountCredit(), target.getAccountKeyCredit());
        });

        // ============== Суммы ================
        // сумма в валюте дебета
        context.addValidator(() -> {
            checkAmount(target, GLOperation.OperSide.D, target.isCurrencyDebitRUR(), 
                    target.getAmountDebit(), target.getAmountDebitRu(), "amountDebit");
        });
        // сумма в валюте кредита
        context.addValidator(() -> {
            checkAmount(target, GLOperation.OperSide.C, target.isCurrencyCreditRUR(),
                    target.getAmountCredit(), target.getAmountCreditRu(), "amountCredit");
        });
        // сумма в рублях дебета
        context.addValidator(() -> {
            checkAmountRu(target, GLOperation.OperSide.D, target.isCurrencyDebitRUR(),
                    target.getAmountDebit(), target.getAmountDebitRu(), "amountDebitRu");
        });
        // сумма в рублях кредита
        context.addValidator(() -> {
            checkAmountRu(target, GLOperation.OperSide.C, target.isCurrencyCreditRUR(),
                    target.getAmountCredit(), target.getAmountCreditRu(), "amountCreditRu");
        });
        // сумма дебета и кредита
        context.addValidator(() -> {
            if (null == target.getCurrencyDebit() || null == target.getCurrencyCredit())
                return;
            if (target.getCurrencyDebit().equals(target.getCurrencyCredit())) {
                // если валюты равны - суммы должны быть равны
                if (!target.getAmountDebit().equals(target.getAmountCredit())) {
                    throw new ValidationError(AMOUNT_NOT_EQUAL,
                            target.getAmountDebit().toString(), target.getAmountCredit().toString(),
                            target.getColumnName("amountDebit"), target.getColumnName("amountCredit"));
                }
            } else if (target.isCurrencyDebitRUR() || target.isCurrencyCreditRUR()) {
                // если одна из валют рубль - суммы не должны быть равны
                if (target.getAmountDebit().equals(target.getAmountCredit())
                        && target.getAmountDebit().compareTo(new BigDecimal("0.1")) > 0) {
                    throw new ValidationError(AMOUNT_EQUAL,
                            target.getCurrencyDebit().getCurrencyCode(), target.getCurrencyCredit().getCurrencyCode(),
                            target.getAmountDebit().toString(),
                            target.getColumnName("amountDebit"), target.getColumnName("amountCredit"));
                }
            }
        });
        // сумма в рублях дебета и кредита
        context.addValidator(() -> {
            // если обе валюты не рубли
            if (!target.isCurrencyDebitRUR() && !target.isCurrencyCreditRUR()) {
                boolean isAmountDebitRu = (null != target.getAmountDebitRu());
                boolean isAmountCreditRu = (null != target.getAmountCreditRu());
                if (isAmountDebitRu != isAmountCreditRu) {
                    String msg = "по дебету " + (isAmountDebitRu ? "" : "не ") + "задана, " +
                            "по кредиту " + (isAmountCreditRu ? "" : "не ") + "задана";
                    throw new ValidationError(AMOUNT_RU_INVALID, "", msg,
                            target.getColumnName("amountDebitRu") + ", " + target.getColumnName("amountCreditRu"));
                }
            }
        });

        // ============== Ссылки ===============
        // ссылка на сторно
        context.addValidator(() -> {
            String fieldName = target.getColumnName("stornoReference");
            String fieldValue = target.getStornoReference();
            int maxLen = 20;
            if (target.getStorno().equals(YesNo.Y)) {   // только для сторно
                if (isEmpty(fieldValue)) {
                    throw new ValidationError(STORNO_REF_IS_EMPTY, fieldName);
                } else if (maxLen < fieldValue.length()) {
                    throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
                }
            }
        });
        // Ссылка на родительскую операцию в продуктовой системе
        context.addValidator(() -> {
            String fieldName = target.getColumnName("parentReference");
            String fieldValue = target.getParentReference();
            int maxLen = 20;
            if (isEmpty(target.getParentReference())) {
                if (target.getFan().equals(YesNo.Y))    // только для веерров
                    throw new ValidationError(PARENT_REF_IS_EMPTY, fieldName);
            }
            else if (maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
    }

    private void checkAccount(EtlPosting target, GLOperation.OperSide operSide,
                              String account, String accountField, String accountKey, String accountKeyField) {
        String sideRus = operSide.getMsgName();
        if (!isEmpty(account)) {             // формат счета
            if (20 != account.length() || !patternAccount.matcher(account).matches()) {
                throw new ValidationError(ACCOUNT_FORMAT_INVALID, sideRus, account, target.getColumnName(accountField));
            }
            // счета доходов - расходов
            if (!account.startsWith("706"))
                return;
            if (!"810".equals(substr(account, 5, 8)))
                throw new ValidationError(ACCOUNT_706_NOT_RUR, sideRus, account, accountField);
            if (!glAccountRepository.checkAccountRlnNotPseudo(account))
                throw new ValidationError(ACCOUNT_706_PSEUDO, sideRus, account, accountField);
        } else if (!isEmpty(accountKey)) {   // формат ключей счета
            if (!patternAccountKey.matcher(accountKey).matches()) {
                throw new ValidationError(ACCOUNTKEY_FORMAT_INVALID, sideRus, accountKey, target.getColumnName(accountKeyField));
            }
        } else {
            throw new ValidationError(ACCOUNT_NOT_DEFINED, sideRus,
                    target.getColumnName(accountField), target.getColumnName(accountKeyField));
        }
    }

    private void checkCurrency(EtlPosting target, GLOperation.OperSide operSide,
                               BankCurrency currency, String currencyField, String account, String accountKey) {
        String sideRus = operSide.getMsgName();
        if (null == currency) {
            throw new ValidationError(CURRENCY_CODE_IS_EMPTY, sideRus,
                    etlPostingRepository.getOperationCurrency(target, operSide), target.getColumnName(currencyField));
        }
        currency = bankCurrencyRepository.refreshCurrency(currency);
        if (!isEmpty(account)) {
            String ccyAccount = glAccountRepository.getAccountCurrency(account);
            if (!ccyAccount.equals(currency.getDigitalCode())) {
                throw new ValidationError(CURRENCY_CODE_NOT_MATCH_ACCOUNT, sideRus,
                        currency.getCurrencyCode(), ccyAccount,
                        target.getColumnName(currencyField));
            }
        }
        else if (!isEmpty(accountKey)) {
            String ccyKey = new AccountKeys(accountKey).getCurrency();
            if (!ccyKey.equals(currency.getCurrencyCode())) {
                throw new ValidationError(CURRENCY_CODE_NOT_MATCH_ACCKEY, sideRus,
                        currency.getCurrencyCode(), ccyKey,
                        target.getColumnName(currencyField));
            }
        }
    }

    private void checkAmount(EtlPosting target, GLOperation.OperSide operSide, boolean isCurrencyRUR,
                                BigDecimal amount, BigDecimal amountRu, String amountField) {
        boolean trueAmount = true;
        if (isCurrencyRUR) {                        // валюта - рубли
            trueAmount = (amount.signum() > 0);     // в валюте > 0
        } else {                                    // в валюте > 0 или (в валюте == 0 и в рублях не пусто)
            trueAmount = (amount.signum() > 0) || ((amount.signum() == 0) && (amountRu != null));
        }
        // сумма в валюте
        if (!trueAmount) {
            throw new ValidationError(AMOUNT_INVALID, operSide.getMsgName(), amount.toString(), target.getColumnName(amountField));
        }
    }

    private void checkAmountRu(EtlPosting target, GLOperation.OperSide operSide, boolean isCurrencyRUR,
                             BigDecimal amount, BigDecimal amountRu, String amountFieldRu) {
        boolean trueAmountRu = true;
        if (isCurrencyRUR) {                        // валюта - рубли
            // в рублях пусто или = в валюте
            trueAmountRu = (amountRu == null) || (amountRu.equals(amount));
        } else {                                    // не рубли
            // в рублях пусто или > 0 и != в валюте
            trueAmountRu = (amountRu == null) || (amountRu.signum() > 0 && !amountRu.equals(amount));
        }
        // сумма в рублях
        if (!trueAmountRu) {
            throw new ValidationError(AMOUNT_RU_INVALID, operSide.getMsgName(), amountRu.toString(), target.getColumnName(amountFieldRu));
        }
    }

/*
    public String validationErrorMessage(EtlPosting posting, List<ValidationError> errors) {
        StringBuilder result = new StringBuilder(format("Обнаружены ошибки валидации входных данных по проводке АЕ '%s': \n", posting.getId()));
        result.append(validationErrorsToString(errors));
        return result.toString();
    }
*/

    /**
     * Создает GL операцию, заполняет параметры, пришедшие из ETL
     * @param posting   - ETL posting
     * @return          - GL operation
     * @throws SQLException
     */
    public final GLOperation createOperation(EtlPosting posting) throws SQLException {
        GLOperation operation = new GLOperation();

        // Основные параметры
        operation.setEtlPostingRef(posting.getId());
        operation.setAePostingId(posting.getAePostingId());
        operation.setSourcePosting(posting.getSourcePosting());
        operation.setInputMethod(InputMethod.AE);
        operation.setEventId(posting.getEventId());
        operation.setEventType(posting.getEventType());
        operation.setChnlName(posting.getChnlName());
        operation.setDeptId(StringUtils.trim(posting.getDeptId()));

        // Идентификаторы сделки
        operation.setPaymentRefernce(posting.getPaymentRefernce());
        setDealSubdealId(operation, posting.getDealId());

        // Даты и время
        operation.setValueDate(posting.getValueDate());
        operation.setProcDate(operdayController.getOperday().getCurrentDate());
        operation.setOperationTimestamp(posting.getOperationTimestamp());

        // Описание
        operation.setNarrative(posting.getNarrative());
        operation.setRusNarrativeLong(posting.getRusNarrativeLong());
        operation.setRusNarrativeShort(posting.getRusNarrativeShort());

        // Дебет
        operation.setAccountDebit(posting.getAccountDebit());
        operation.setCurrencyDebit(posting.getCurrencyDebit());
        operation.setAmountDebit(posting.getAmountDebit());
        operation.setAmountDebitRu(posting.getAmountDebitRUR());

        // Кредит
        operation.setAccountCredit(posting.getAccountCredit());
        operation.setCurrencyCredit(posting.getCurrencyCredit());
        operation.setAmountCredit(posting.getAmountCredit());
        operation.setAmountCreditRu(posting.getAmountCreditRUR());

        // Дополнительные параметры
        operation.setStorno(posting.getStorno());                   // Сторно
        operation.setIsCorrection(posting.getStorno());             // Исправительная
        operation.setStornoReference(posting.getStornoReference());
        operation.setFan(posting.getFan());                         // Веер
        operation.setParentReference(posting.getParentReference());

        // Параметры открываемых счетов
        operation.setAccountKeyDebit(posting.getAccountKeyDebit());
        operation.setAccountKeyCredit(posting.getAccountKeyCredit());

        // дата операции и дата проводки
        Operday operday = operdayController.getOperday();
        operation.setCurrentDate(operday.getCurrentDate());
        operation.setLastWorkdayStatus(operday.getLastWorkdayStatus());

        return operation;
    }

    /**
     * Заполняет в GL операции вычисляемые поля, общие для всех видов операций
     * @param operation
     * @throws Exception
     */
    public void enrichment(GLOperation operation) throws SQLException {
        // дата операции и дата проводки
        operation.setProcDate(operdayController.getOperday().getCurrentDate());
        Date postDate = calculatePostingDate(operation);
        operation.setPostDate(postDate);

        // параметры ДЕБЕТА: курс валюты, рублевый эквивалент
        BankCurrency ccyDebit = bankCurrencyRepository.refreshCurrency(operation.getCurrencyDebit());
        operation.setCurrencyDebit(ccyDebit);
        BigDecimal rateDebit = rateRepository.getRate(ccyDebit.getCurrencyCode(), postDate);
        BigDecimal eqvDebit = rateRepository.getEquivalent(ccyDebit, rateDebit, operation.getAmountDebit());

        operation.setRateDebit(rateDebit);
        operation.setEquivalentDebit(eqvDebit);

        // параметры КРЕДИТА: курс валюты, рублевый эквивалент
        BankCurrency ccyCredit = bankCurrencyRepository.refreshCurrency(operation.getCurrencyCredit());
        operation.setCurrencyCredit(ccyCredit);
        BigDecimal rateCredit = rateRepository.getRate(ccyCredit.getCurrencyCode(), postDate);
        BigDecimal eqvCredit = rateRepository.getEquivalent(ccyCredit, rateCredit, operation.getAmountCredit());

        operation.setRateCredit(rateCredit);
        operation.setEquivalentCredit(eqvCredit);

        // параметры счетов - они нужны для определения филиала и главы баланса в случае отсутствифя счетов
        if (isEmpty(operation.getAccountDebit()))
            operation.createAccountParamDebit();
        if (isEmpty(operation.getAccountCredit()))
            operation.createAccountParamCredit();

        // филиалы по дебету и кредиту
        glOperationRepository.setFilials(operation);

        // глава балансового счета
        // Добавили определение также в processOperation, тк счет может быть не задан
        glOperationRepository.setBsChapter(operation);            // Глава баланса

        // параметры обмена валюты
        setExchengeParameters(operation);
    }

    /**
     * Заполняет ИД слелки и ИД транша
     * @param operation
     * @param dealSubdeal
     */
    private void setDealSubdealId(GLOperation operation, String dealSubdeal) {
        if (null != dealSubdeal) {
            int sep = dealSubdeal.indexOf(";");

            String dealId = (sep < 0) ? dealSubdeal : substr(dealSubdeal, sep);
            String subdealId = (sep < 0) ? null : substr(dealSubdeal, sep + 1, dealSubdeal.length());

            operation.setDealId(dealId);
            operation.setSubdealId(subdealId);
        }
    }


    /**
     * Определяет главу баланса для операции
     * @param operation
     */
/*
    private void setBsChapter(GLOperation operation) throws SQLException {
        // глава балансового счета
        String chapterDebit = glOperationRepository.getBSChapter(operation.getAccountDebit(), operation.getAccountParamDebit());
        String chapterCredit = glOperationRepository.getBSChapter(operation.getAccountCredit(), operation.getAccountParamCredit());
        if (chapterDebit.isEmpty()) {
            throw new ValidationError(BALANSE_CHAPTER_NOT_DEFINED, "по дебету", operation.getAccountDebit());
        }
        if (chapterCredit.isEmpty()) {
            throw new ValidationError(BALANSE_CHAPTER_NOT_DEFINED, "по кредиту", operation.getAccountCredit());
        }
        if (!chapterDebit.equals(chapterCredit)) {
            throw new ValidationError(BALANSE_CHAPTER_IS_DIFFERENT,
                    operation.getAccountDebit(), chapterDebit, operation.getAccountCredit(), chapterCredit);
        }
        operation.setBsChapter(chapterDebit);
    }
*/

    /**
     * Заполняет в GL operation поля для отвода курсовой разницы
     * @param operation
     * @throws Exception
     */
    public void setExchengeParameters (GLOperation operation) {
        // Основная валюта, сумма проводки в рублях
        BankCurrency ccyDebit = operation.getCurrencyDebit();
        BankCurrency ccyCredit = operation.getCurrencyCredit();

        // курсовая разница
        BigDecimal amountDebitRu = ( null != operation.getAmountDebitRu()) ?
                operation.getAmountDebitRu() : operation.getEquivalentDebit();
        BigDecimal amountCreditRu = ( null != operation.getAmountCreditRu()) ?
                operation.getAmountCreditRu() : operation.getEquivalentCredit();
        operation.setExchangeDifference(amountDebitRu.subtract(amountCreditRu));

        // основная валюта и сумма проводки
        if (RUB.equals(ccyDebit)) {         // Если ДЕБЕТ в рублях
            operation.setCurrencyMain(ccyDebit);        // основная валюта и сумма проводки по Дебету
            operation.setAmountPosting(operation.getAmountDebit());
        } else {                                        // Иначе
            operation.setCurrencyMain(ccyCredit);       // основная валюта по Кредиту
            if (RUB.equals(ccyCredit)) {    // Если КРЕДИТ в рублях
                operation.setAmountPosting(operation.getAmountCredit());
            } else {                        // Иначе
                operation.setAmountPosting(amountCreditRu);
            }
        }
    }

    /**
     * вычисляем дату проводки по настройке оформленной в виде вьюхи V_GL_OPER_POD
     * @param operation операция
     * @return дата проводки
     */
    public final Date calculatePostingDate(GLOperation operation) {
        try {
            Operday operday = operdayController.getOperday();
            String dayType = "";
            if ((InputMethod.AE == operation.getInputMethod())
                    && !isEmpty(dayType = calendarDayRepository.getDayType(operation.getValueDate()))) {
                // попали в выходной день
                return processHoliday(operation, dayType);
            } else {
                final DataRecord record = glOperationRepository.selectOne("select * from V_GL_OPER_POD where GLOID = ?", operation.getId());
                final PostingDateType podType = PostingDateType.valueOf(record.getString("POD_TYPE"));
                switch (podType) {
                    case CURRENT: return operday.getCurrentDate();
                    case LAST: return operday.getLastWorkingDay();
                    case STANDARD: return getStandardPostingDate(operation.getValueDate());
                    case HARD: return record.getDate("POD");
                    default: throw new DefaultApplicationException(format("Ошибка вычисления даты проводки по операции '%s'", operation.getId()));
                }
            }
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * Определяет для операции PostingDate
     * @param valueDate
     * @return
     */
    protected Date getStandardPostingDate(Date valueDate) {
        Date currentDay = operdayController.getOperday().getCurrentDate();
        Date lastWorkingDay = operdayController.getOperday().getLastWorkingDay();
        // valueDate = СЕГОДНЯ, фаза ONLINE
        if (valueDate.equals(currentDay)
                && ONLINE == operdayController.getOperday().getPhase()) {
            return valueDate;
        } else
        // valueDate = ПРЕДЫДУЩИЙ раб день
        if (valueDate.equals(lastWorkingDay)) {
            if (OPEN == operdayController.getOperday().getLastWorkdayStatus()) {
                return valueDate;       // баланс не закрыт
            } else {
                return currentDay;      // баланс закрыт
            }
        } else
        // valueDate < ПРЕДЫДУЩИЙ раб день
        if (valueDate.before(lastWorkingDay)) {
            return currentDay;
        } else {
            // valueDate > СЕГОДНЯ или valueDate между СЕГОДНЯ и ПРЕДЫДУЩИЙ раб ден
            throw new ValidationError(DATE_NOT_VALID, "валютирования",
                    dateUtils.onlyDateString(valueDate),
                    dateUtils.onlyDateString(currentDay), operdayController.getOperday().getPhase().name(),
                    dateUtils.onlyDateString(lastWorkingDay), operdayController.getOperday().getLastWorkdayStatus().name());
        }
    }

    /**
     * Если дата валютирования попала на выходной
     * @return дата postdate
     */
    private Date processHoliday(GLOperation operation, String dayType) {
        Operday operday = operdayController.getOperday();

        // Для ARMPRO разрешен технический опердень, но не ранее 14 лней назад
        if (DealSource.ARMPRO.name().equals(operation.getSourcePosting()) && T.name().equals(dayType)
                && operation.getValueDate().after(DateUtils.addDay(operday.getCurrentDate(), -14))) { // технический опердень
            return operation.getValueDate();
        }
        // после предыд ОД
        Calendar vdatecal = Calendar.getInstance();
        vdatecal.setTime(operation.getValueDate());

        Calendar vcurrcal = Calendar.getInstance();
        vcurrcal.setTime(operday.getCurrentDate());

        if (vdatecal.get(Calendar.MONTH) != vcurrcal.get(Calendar.MONTH)) {
            // текущий ОД находится в след месяце
            return operday.getLastWorkingDay();
        } else {
            return operday.getCurrentDate();
        }
    }

}
