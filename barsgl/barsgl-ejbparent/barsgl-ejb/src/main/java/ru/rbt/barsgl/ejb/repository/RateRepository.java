package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.CurrencyRate;
import ru.rbt.barsgl.ejb.entity.dict.CurrencyRateId;
import ru.rbt.barsgl.ejb.repository.dict.CurrencyCacheRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Date;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Created by Ivan Sevastyanov
 */
public class RateRepository extends AbstractBaseEntityRepository<CurrencyRate, CurrencyRateId> {

    @Inject
    private DateUtils dateUtils;

    @EJB
    private CurrencyCacheRepository currencyCacheRepository;

    public BigDecimal getRate(String ccy, Date valDate) {
        Assert.isTrue(!isEmpty(ccy), "Не указан код валюты");
        return getRate(new BankCurrency(ccy), valDate);
    }

    public BigDecimal getRate(BankCurrency currency, Date valDate) {
        CurrencyRate rate = findRate(currency, valDate);
        if (null == rate) {
            throw new ValidationError(ErrorCode.CURRENCY_RATE_NOT_FOUND, currency.getCurrencyCode(),
                    dateUtils.onlyDateString(valDate));
        }
        return rate.getRate();
    }

    /**
     * курс на дату
     * @param currency валюта
     * @param ondate дата курса
     * @return курс
     */
    public CurrencyRate findRate(BankCurrency currency, Date ondate) {
        Assert.isTrue(!isEmpty(currency.getCurrencyCode()), "Не указан код валюты");

        if (BankCurrency.RUB.equals(currency)) {
            return new CurrencyRate(currency, ondate, new BigDecimal("1"));
        } else {
            CurrencyRate rate = findById(CurrencyRate.class, new CurrencyRateId(currency.getCurrencyCode(), ondate));
            return rate;
        }
    }

    public BigDecimal getEquivalent(BigDecimal rate, BigDecimal majorAmount) {
        BankCurrency RUR = currencyCacheRepository.findCached(BankCurrency.RUB.getCurrencyCode());
        Assert.isTrue(null != RUR, () -> new DefaultApplicationException("Не найдена валюта RUR"));
        return majorAmount.multiply(rate).setScale(RUR.getScale().intValue(), BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal getValEquivalent(BankCurrency currency, BigDecimal rate, BigDecimal majorAmount) {
        return majorAmount.divide(rate, currency.getScale().intValue(), BigDecimal.ROUND_HALF_UP);
    }
}
