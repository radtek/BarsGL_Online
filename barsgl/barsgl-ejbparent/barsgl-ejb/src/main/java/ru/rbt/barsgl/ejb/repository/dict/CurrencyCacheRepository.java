/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.ejb.repository.dict;

import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.dict.SourcesDeals;
import ru.rbt.barsgl.ejbcore.repository.AbstractCachedRepository;

import javax.annotation.PostConstruct;
import javax.ejb.AccessTimeout;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author er22228
 */
@Singleton
@AccessTimeout(unit = TimeUnit.MINUTES, value = 15)
@Lock(LockType.READ)
public class CurrencyCacheRepository extends AbstractCachedRepository<BankCurrency, String> {

    @Override
    public BankCurrency findCached(String primaryKey) {
        return super.findCached(primaryKey);
    }

    @Override
    public List<BankCurrency> getAllObjectsCached() {
        return super.getAllObjectsCached();
    }

    @Override
    protected Class<BankCurrency> getEntityClass() {
        return BankCurrency.class;
    }

    @Lock(LockType.WRITE)
    @Override
    public void flushCache() {
        super.flushCache();
    }

    @PostConstruct
    @Override
    public void init() {
        super.init();
    }
}
