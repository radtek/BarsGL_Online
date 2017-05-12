package ru.rbt.barsgl.ejb.integr.oper;

import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.entity.gl.BalanceChapter;
import ru.rbt.barsgl.ejb.entity.gl.GLManualOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.ValidationAwareHandler;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountProcessor;
import ru.rbt.barsgl.ejb.repository.BankCurrencyRepository;
import ru.rbt.barsgl.ejb.repository.BatchPostingRepository;
import ru.rbt.barsgl.ejb.repository.GLAccountRepository;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.ErrorList;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.ejbcore.util.StringUtils.*;
import static ru.rbt.ejbcore.validation.ErrorCode.*;

/**
 * Created by ER18837 on 13.08.15.
 */
public class ManualTechOperationProcessor extends ValidationAwareHandler<BatchPosting> {
    @Inject
    private BankCurrencyRepository currencyRepository;

    @Inject
    private OrdinaryPostingProcessor ordinaryPostingProcessor;

    @Inject
    private GLOperationRepository glOperationRepository;

    @Inject
    private GLAccountRepository glAccountRepository;

    @EJB
    private BatchPostingRepository postingRepository;

    @Inject
    private GLAccountProcessor glAccountProcessor;

    @Inject
    private DateUtils dateUtils;

    @Inject
    private UserContext userContext;

    @Override
    public void fillValidationContext(BatchPosting target, ValidationContext context) {
        // ============== Дата ===============
        // TODO эти проверки надо убрать, они дублипуют проверки при создании запроса на операцию
        // Value Date
        context.addValidator(() -> {
            checkDate(target, target.getValueDate(), "Дата валютирования");
        });
        // Posting Date
        context.addValidator(() -> {
            Date postdate = target.getPostDate();
            checkDate(target, postdate, "Дата операции");
            Date valuedate = target.getValueDate();
            if ( (null != postdate )&& (null != valuedate) && valuedate.after(postdate)) {
                throw new ValidationError(POSTDATE_NOT_VALID,
                        dateUtils.onlyDateString(postdate),
                        dateUtils.onlyDateString(valuedate));
            }
        });

        // ============= Счета ==============
        // Формат счета дебета
        context.addValidator(() -> {
            checkAccount(target, GLOperation.OperSide.D, target.getAccountDebit(), "Дебет / Счет");
        });
        // Формат счета кредита
        context.addValidator(() -> {
            checkAccount(target, GLOperation.OperSide.C, target.getAccountCredit(), "Кредит / Счет");
        });

        // ====== Филиалы и глава ===========
        // Глава баланса
        context.addValidator(() -> {
            final BalanceChapter bchDt = glOperationRepository.getBalanceChapter(target.getAccountDebit());
            final BalanceChapter bchCt = glOperationRepository.getBalanceChapter(target.getAccountCredit());
            if (null == bchDt || null == bchCt)
                return;
            String bsChapterDebit = bchDt.getRuLetter();
            String bsChapterCredit = bchCt.getRuLetter();
            if ( !bsChapterDebit.equals(bsChapterCredit)) {
                throw new ValidationError(BALANSE_CHAPTER_IS_DIFFERENT, bsChapterDebit, "Дебет / Счет", bsChapterCredit , "Кредит / Счет");
            }

            String cbccDebit = substr(target.getAccountDebit(), 11, 13);
            String cbccCredit = substr(target.getAccountCredit(), 11, 13);
            if (!cbccDebit.equals(cbccCredit)   // межфилиал
                    && !BalanceChapter.A.getRuLetter().equals(bsChapterDebit)) {   // глава не А
                throw new ValidationError(MFO_CHAPTER_NOT_A, target.getAccountDebit(), "Дебет / Счет", target.getAccountCredit(), "Кредит / Счет");
            }
        });

        context.addValidator(() -> {
            ValidationError error = glAccountRepository.checkAccount9999(target.getAccountDebit(), target.getAccountCredit(), GLOperation.OperSide.D);
            if (null != error)
                throw error;

            error = glAccountRepository.checkAccount9999(target.getAccountCredit(), target.getAccountDebit(), GLOperation.OperSide.D);
            if (null != error)
                throw error;
        });


        // ============ Валюта ==============
        // Валюта дебета
        context.addValidator(() -> {
            checkCurrency(GLOperation.OperSide.D, target.getCurrencyDebit(), "Дебет / Валюта",
                    target.getAccountDebit());
        });
        // Валюта кредита
        context.addValidator(() -> {
            checkCurrency(GLOperation.OperSide.C, target.getCurrencyCredit(), "Кредит / Валюта",
                    target.getAccountCredit());
        });

        // ============ Сумма ===============
        // сумма по дебету
        context.addValidator(() -> {
            // сумма в валюте
            checkAmount(GLOperation.OperSide.D, BankCurrency.RUB.equals(target.getCurrencyDebit()),
                    target.getAmountDebit(), target.getAmountRu(), "Дебет / Сумма");
        });
        // сумма по кредиту
        context.addValidator(() -> {
            checkAmount(GLOperation.OperSide.C, BankCurrency.RUB.equals(target.getCurrencyCredit()),
                    target.getAmountCredit(), target.getAmountRu(), "Кредит / Сумма");
        });
        // равные суммы в одной валюте
        context.addValidator(() -> {
            if ( target.getCurrencyDebit().equals(target.getCurrencyCredit())
                    && !target.getAmountDebit().equals(target.getAmountCredit())) {
                throw new ValidationError(AMOUNT_INVALID, GLOperation.OperSide.C.getMsgName(),
                        target.getAmountCredit().toString(), "суммы по дебету и кредиту не равны");
            }
        });

        // ============== Общее ===============
        // Источник сделки
        context.addValidator(() -> {
            String fieldName = "Источник сделки";
            String fieldValue = target.getSourcePosting();
            int maxLen = 7;
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            } else if (maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        // ИД платежа
        context.addValidator(() -> {
            String fieldName = "Номер платежного документа";
            String fieldValue = target.getPaymentRefernce();
            int maxLen = 20;
            if ( null != fieldValue && maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        // Номер сделки
        context.addValidator(() -> {
            String fieldName = "Номер сделки";
            String fieldValue = target.getDealId();
            int maxLen = 20;
            if (null != fieldValue && maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        // Номер субсделки
        context.addValidator(() -> {
            String fieldName = "Номер субсделки";
            String fieldValue = target.getSubDealId();
            int maxLen = 20;
            if (null != fieldValue) {
                if (maxLen < fieldValue.length())
                    throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
            checkDealId(target);
        });

        // Код департамента, создавшего проводку
        context.addValidator(() -> {
            String fieldName = "Подразделение";
            String fieldValue = target.getDeptId();
            int maxLen = 4;
            if (null != fieldValue &&  maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        // описания
        context.addValidator(() -> {
            String fieldName = "Основание ENG";
            String fieldValue = target.getNarrative();
            int maxLen = 300;
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            } else if (null != fieldValue && maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        context.addValidator(() -> {
            String fieldName = "Основание RUS";
            String fieldValue = target.getRusNarrativeLong();
            int maxLen = 300;
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            } else if (maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
    }

    private void checkAccount(BatchPosting target, GLOperation.OperSide operSide,
                              String account, String accountField) {
        String sideRus = operSide.getMsgName();
        if (isEmpty(account)) {
            throw new ValidationError(FIELD_IS_EMPTY, accountField);
        } else if (20 != account.length() || !ordinaryPostingProcessor.patternAccount.matcher(account).matches()) {
            throw new ValidationError(ACCOUNT_FORMAT_INVALID, sideRus, account, accountField);
        }

        // счет существует и открыт
        int flag = glAccountRepository.checkBsaAccount(account, target.getValueDate());
        if (flag == -1) {
            throw new ValidationError(ACCOUNT_NOT_FOUND, sideRus, account, accountField);
        } else if (flag == 0) {
            throw new ValidationError(ACCOUNT_IS_CLOSED, sideRus, account, accountField);
        }

        // счета доходов - расходов
        if (!account.startsWith("706"))
            return;
        if (!"810".equals(substr(account, 5, 8)))
            throw new ValidationError(ACCOUNT_706_NOT_RUR, sideRus, account, accountField);
        if (!glAccountRepository.checkAccountRlnNotPseudo(account))
            throw new ValidationError(ACCOUNT_706_PSEUDO, sideRus, account, accountField);
    }

    private  void checkFilial(ManualOperationWrapper target, GLOperation.OperSide operSide,
                              String filial, String bsaAsid, String filialField) {
        String accountFilial = "";
        try {
            accountFilial = glOperationRepository.getFilialByAccount(bsaAsid);
        } catch (Exception e) {
            throw new ValidationError(FILIAL_NOT_VALID, operSide.getMsgName(), filial, accountFilial);
        }
        filial = ifEmpty(filial, "");
        if (!filial.equals(accountFilial)){
            throw new ValidationError(FILIAL_NOT_VALID, operSide.getMsgName(), filial, accountFilial);
        }
    }

    public String getFilial(String bsaAsid) {
        return glOperationRepository.getFilialByAccount(bsaAsid);
    }

    private void checkCurrency(GLOperation.OperSide operSide,
                               BankCurrency currency, String currencyField, String account) {
        String sideRus = operSide.getMsgName();
        currency = currencyRepository.refreshCurrency(currency);
        String ccyAccount = glAccountRepository.getAccountCurrency(account);
        if (!ccyAccount.equals(currency.getDigitalCode())) {
            throw new ValidationError(CURRENCY_CODE_NOT_MATCH_ACCOUNT, sideRus,
                    currency.getCurrencyCode(), ccyAccount,
                    currencyField);
        }
    }

    private void checkAmount(GLOperation.OperSide operSide, boolean isCurrencyRUR,
                             BigDecimal amount, BigDecimal amountRu, String fieldName) {
        boolean trueAmount = true;
        if (isCurrencyRUR) {                        // валюта - рубли
            trueAmount = (amount.signum() > 0);                             // в валюте > 0
        } else {                                    // в валюте > 0 или (в валюте == 0 и в рублях не пусто)
            trueAmount = (amount.signum() > 0) || ((amount.signum() == 0) && (amountRu != null));
        }
        // сумма в валюте
        if (!trueAmount) {
            throw new ValidationError(AMOUNT_INVALID, operSide.getMsgName(), amount.toString(), fieldName);
        }
    }

    private void checkAmountRu(ManualOperationWrapper target, GLOperation.OperSide operSide, boolean isCurrencyRUR,
                               BigDecimal amount, BigDecimal amountRu, String fieldName) {
        boolean trueAmountRu = true;
        if (isCurrencyRUR) {                        // валюта - рубли
            trueAmountRu = (amountRu == null) || (amountRu.equals(amount)); // в рублях пусто или = в валюте
        } else {                                    // не рубли
            // в валюте > 0 или (в валюте == 0 и в рублях не пусто)
            trueAmountRu = (amountRu == null) || (amountRu.signum() > 0);   // в рублях пусто или > 0
        }
        // сумма в рублях
        if (!trueAmountRu) {
            throw new ValidationError(AMOUNT_RU_INVALID, operSide.getMsgName(), amountRu.toString(), fieldName);
        }
    }

    public Date checkDate(BatchPosting target, Date checkDate, String fieldName) {
        Date currentDate = userContext.getCurrentDate();     // текущий опердень

        if ( null == checkDate ) {
            throw new ValidationError(DATE_AFTER_OPERDAY, fieldName,
                    "не задана",
                    dateUtils.onlyDateString(currentDate));
        } else if (checkDate.after(currentDate)){
            throw new ValidationError(DATE_AFTER_OPERDAY, fieldName,
                    dateUtils.onlyDateString(checkDate),
                    dateUtils.onlyDateString(currentDate));
        }
        // TODO проверка на выходные
        return checkDate;
    }

    public Date checkDate(ManualOperationWrapper wrapper, String checkDateStr, String fieldName) {
        Date currentDate = userContext.getCurrentDate();     // текущий опердень
        Date checkDate = null;
        try {
            if(null != checkDateStr)
                checkDate = new SimpleDateFormat(wrapper.dateFormat).parse(checkDateStr);
        } catch (ParseException e) {

        }
        if ( null == checkDate ) {
            throw new ValidationError(DATE_AFTER_OPERDAY, fieldName,
                    "не задана",
                    dateUtils.onlyDateString(currentDate));
        } else if (checkDate.after(currentDate)){
            throw new ValidationError(DATE_AFTER_OPERDAY, fieldName,
                    dateUtils.onlyDateString(checkDate),
                    dateUtils.onlyDateString(currentDate));
        }
        // TODO проверка на выходные
        return checkDate;
    }

    public void checkDealId(BatchPosting posting) {
        glAccountProcessor.checkDealId(posting.getPostDate(), posting.getSourcePosting(),
                posting.getDealId(), posting.getSubDealId(), posting.getPaymentRefernce());
    }

    // ======================================================================================
    /* Создает операцию по запросу
    * */
    public final GLManualOperation createOperation(BatchPosting posting)  {
        GLManualOperation operation = new GLManualOperation();

        // Основные параметры
        operation.setEtlPostingRef(posting.getId());
        operation.setInputMethod(posting.getInputMethod());
        operation.setSourcePosting(posting.getSourcePosting());
        operation.setDealId(posting.getDealId());
        operation.setSubdealId(posting.getSubDealId());
        operation.setPaymentRefernce(posting.getPaymentRefernce());

        operation.setDeptId(StringUtils.trim(posting.getDeptId()));
        operation.setProfitCenter(posting.getProfitCenter());
        operation.setIsCorrection(posting.getIsCorrection());

        // Даты и время //
        operation.setValueDate(posting.getValueDate());
        operation.setPostDate(posting.getPostDate());
        // текущее системное время
        operation.setOperationTimestamp(userContext.getTimestamp());

        // Описание
        operation.setNarrative(StringUtils.removeCtrlChars(posting.getNarrative()));
        operation.setRusNarrativeLong(StringUtils.removeCtrlChars(posting.getRusNarrativeLong()));
        operation.setRusNarrativeShort(StringUtils.removeCtrlChars(
                    !isEmpty(posting.getRusNarrativeShort()) ?
                    posting.getRusNarrativeShort() : substr(posting.getRusNarrativeLong(), 100)
                ));

        // Дебет
        operation.setAccountDebit(posting.getAccountDebit());
        operation.setCurrencyDebit(posting.getCurrencyDebit());
        operation.setAmountDebit(posting.getAmountDebit());
        operation.setAmountDebitRu(posting.getAmountRu());
        operation.setFilialDebit(posting.getFilialDebit());

        // Кредит
        operation.setAccountCredit(posting.getAccountCredit());
        operation.setCurrencyCredit(posting.getCurrencyCredit());
        operation.setAmountCredit(posting.getAmountCredit());
        operation.setAmountCreditRu(posting.getAmountRu());
        operation.setFilialCredit(posting.getFilialCredit());

        // Дополнительные параметры
        operation.setStorno(YesNo.N);                   // Сторно
        operation.setStornoReference(null);
        operation.setFan(YesNo.N);                      // Веер
        operation.setParentReference(null);
        if (posting.getIsTech().equals(YesNo.Y)) {
            operation.setBsChapter("T");
        }

        operation.setUserName(userContext.getUserName());

        return operation;
    }

    public String validationErrorMessage(List<ValidationError> errors, ErrorList descriptors) {
        StringBuilder result = new StringBuilder(format("Обнаружены ошибки валидации входных данных\n"));
        result.append(validationErrorsToString(errors, descriptors));
        return result.toString();
    }

}
