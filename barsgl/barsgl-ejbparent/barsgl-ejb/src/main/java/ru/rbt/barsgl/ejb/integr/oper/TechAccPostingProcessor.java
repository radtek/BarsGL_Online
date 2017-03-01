package ru.rbt.barsgl.ejb.integr.oper;

import ru.rbt.barsgl.ejb.entity.acc.AccountKeys;
import ru.rbt.barsgl.ejb.entity.dict.AccountingType;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.repository.dict.AccountingTypeRepository;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.ejbcore.util.StringUtils;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;

import javax.inject.Inject;
import java.math.BigDecimal;

import static ru.rbt.barsgl.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.*;
import static ru.rbt.barsgl.ejbcore.validation.ErrorCode.STRING_FIELD_IS_TOO_LONG;

/**
 * Created by er23851 on 27.02.2017.
 * Класс обработки технических счетов
 */
public class TechAccPostingProcessor extends IncomingPostingProcessor  {

    @Inject
    private AccountingTypeRepository accountingTypeRepository;

    @Override
    public boolean isSupported(EtlPosting posting) {

        return null != posting
                && !posting.isFan() && !posting.isStorno() && posting.isTech();
    }

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
        /*
        context.addValidator(() -> {
            checkAccount(target, GLOperation.OperSide.D, target.getAccountDebit(), "accountDebit",
                    target.getAccountKeyDebit(), "accountKeyDebit");
        });
        // Формат счета кредита
        context.addValidator(() -> {
            checkAccount(target, GLOperation.OperSide.C, target.getAccountCredit(), "accountCredit",
                    target.getAccountKeyCredit(), "accountKeyCredit");
        });
        */
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

        //Проверки для технических счетов

        //Проверка ключа счёта по дебету
        context.addValidator(()->{
            String fieldName = target.getColumnName("accountKeyDebit");
            String fieldValue = target.getAccountKeyDebit();

            AccountKeys keys = new AccountKeys(fieldValue);

            if (!isEmpty(keys.getAccountType()) && !isEmpty(keys.getCurrency()) &&
                    !isEmpty(keys.getGlSequence()) && !isEmpty(keys.getCompanyCode()) && !isEmpty(keys.getDealSource())
                    && !keys.getGlSequence().startsWith("TH"))
            {
                throw  new ValidationError(ACCOUNT_TH_ACCKEY_NOT_VALID,"дебету",fieldValue);
            }

        });

        //Проверка ключа счёта по кредиту
        context.addValidator(()->{
            String fieldName = target.getColumnName("accountKeyCredit");
            String fieldValue = target.getAccountKeyCredit();

            AccountKeys keys = new AccountKeys(fieldValue);

            if (!isEmpty(keys.getAccountType()) && !isEmpty(keys.getCurrency()) &&
                    !isEmpty(keys.getGlSequence()) && !isEmpty(keys.getCompanyCode()) && !isEmpty(keys.getDealSource())
                    && !keys.getGlSequence().startsWith("TH"))
            {
                throw  new ValidationError(ACCOUNT_TH_ACCKEY_NOT_VALID,"кредиту",fieldValue);
            }

        });

        //Проверка ключа счёта по кредиту
        context.addValidator(()->{
            String fieldName1 = target.getColumnName("accountKeyCredit");
            String fieldValue1 = target.getAccountKeyCredit();

            String fieldName2 = target.getColumnName("accountKeyDebit");
            String fieldValue2 = target.getAccountKeyDebit();

            AccountKeys keys1 = new AccountKeys(fieldValue1);
            AccountKeys keys2 = new AccountKeys(fieldValue2);

            if (!keys1.getCompanyCode().equals(keys2.getCompanyCode()))
            {
                throw  new ValidationError(ACCOUNT_TH_CBCCN_NOT_EQUALS, keys1.getCompanyCode(),keys2.getCompanyCode());
            }

        });

        //Для одного из полей CCY равер RUR
        context.addValidator(()->{
            String fieldName1 = target.getColumnName("accountKeyCredit");
            String fieldValue1 = target.getAccountKeyCredit();

            String fieldName2 = target.getColumnName("accountKeyDebit");
            String fieldValue2 = target.getAccountKeyDebit();

            AccountKeys keys1 = new AccountKeys(fieldValue1);
            AccountKeys keys2 = new AccountKeys(fieldValue2);

            if (!keys1.getCurrency().equals("RUR") && !keys2.getCurrency().equals("RUR"))
            {
                throw  new ValidationError(ACCOUNT_TH_ССY_NOT_RUR, keys1.getCurrency(),keys2.getCurrency());
            }
        });

        //Проверка корректности значения AccType для технического счёта по кредиту
        context.addValidator(()->{
            String fieldName = target.getColumnName("accountKeyCredit");
            String fieldValue = target.getAccountKeyCredit();

            AccountKeys keys = new AccountKeys(fieldValue);

            if (keys.getGlSequence().startsWith("TH"))
            {
                String sAccType = keys.getAccountType();
                //Получение записи ACCTYPE  по коду типа счёта
                AccountingType accType = accountingTypeRepository.findById(AccountingType.class,sAccType);
                if (accType!=null)
                {
                    if (!accType.isTech())
                    {
                        throw  new ValidationError(ACCOUNT_TH_ACCTYPE_NOT_VALID,"кредиту",sAccType);
                    }
                }
                else {
                    throw  new ValidationError(ACCOUNT_TH_ACCTYPE_NOT_VALID,"кредиту",sAccType);
                }
            }
            else{
                throw  new ValidationError(ACCOUNT_TH_ACCKEY_NOT_VALID,"кредиту",fieldValue);
            }

        });

        //Проверка корректности значения AccType для технического счёта по дебету
        context.addValidator(()->{
            String fieldName = target.getColumnName("accountKeyDebit");
            String fieldValue = target.getAccountKeyDebit();

            AccountKeys keys = new AccountKeys(fieldValue);

            if (keys.getGlSequence().startsWith("TH"))
            {
                String sAccType = keys.getAccountType();
                //Получение записи ACCTYPE  по коду типа счёта
                AccountingType accType = accountingTypeRepository.findById(AccountingType.class,sAccType);
                if (accType!=null)
                {
                    if (!accType.isTech())
                    {
                        throw  new ValidationError(ACCOUNT_TH_ACCTYPE_NOT_VALID,"дебету",sAccType);
                    }
                }
                else {
                    throw  new ValidationError(ACCOUNT_TH_ACCTYPE_NOT_VALID,"дебету",sAccType);
                }
            }
            else{
                throw  new ValidationError(ACCOUNT_TH_ACCKEY_NOT_VALID,"дебету",fieldValue);
            }

        });




    }

}
