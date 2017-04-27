package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;

import java.math.BigDecimal;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless
@LocalBean
public class BankCurrencyRepository extends AbstractBaseEntityRepository<BankCurrency, String> {

    public BankCurrency getCurrency(String currencyCode) {
        BankCurrency currency = findById(BankCurrency.class, currencyCode);
        if (null == currency) {
            throw new ValidationError(ErrorCode.CURRENCY_CODE_NOT_EXISTS, currencyCode);
        }
        return currency;
    }
    public BankCurrency refreshCurrency(String currencyCode) {
        BankCurrency currency = getCurrency(currencyCode);
        return refresh(currency);
    }

    public BankCurrency refreshCurrency(BankCurrency currency) {
        Assert.notNull(currency, "Не задана валюта");
        currency = findById(BankCurrency.class, currency.getId());
        if (null == currency) {
            throw new ValidationError(ErrorCode.CURRENCY_CODE_NOT_EXISTS, currency.getId());
        }
        return refresh(currency);
    }

    /**
     * Переводит сумму в минорные единицы (н-р копейки)
     * @param currency      - валюта
     * @param majorAmount   - сумма в минорных ед
     * @return
     */
    public Long getMinorAmount(BankCurrency currency, BigDecimal majorAmount) {
//        int exp = (int) Math.pow(10, currency.getScale());
//        return majorAmount.multiply(BigDecimal.valueOf(exp)).longValue();
        return majorAmount.movePointRight(currency.getScale().intValue()).longValue();
    }

    /**
     * Переводит сумму в мажорные единицы (н-р рубли)
     * @param currency      - валюта
     * @param minorAmount   - сумма в мажорных ед
     * @return
     */
    public BigDecimal getMajorAmount(BankCurrency currency, Long minorAmount) {
        return new BigDecimal(minorAmount).movePointLeft(currency.getScale().intValue());
    }

    public String getCurrencyDigital(String ccy) {
        return getCurrency(ccy).getDigitalCode();
    }
}
