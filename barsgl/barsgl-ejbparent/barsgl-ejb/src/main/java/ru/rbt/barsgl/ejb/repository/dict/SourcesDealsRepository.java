/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.ejb.repository.dict;

import javax.annotation.PostConstruct;
import javax.ejb.*;

import ru.rbt.barsgl.ejb.entity.dict.SourcesDeals;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.barsgl.ejbcore.repository.AbstractCachedRepository;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Andrew Samsonov
 */
@Singleton
@AccessTimeout(unit = TimeUnit.MINUTES, value = 15)
@Lock(LockType.READ)
public class SourcesDealsRepository extends AbstractCachedRepository<SourcesDeals, String> {

    @Override
    public SourcesDeals findCached(String primaryKey) {
        return super.findCached(primaryKey);
    }

    @Override
    public List<SourcesDeals> getAllObjectsCached() {
        return super.getAllObjectsCached();
    }

    @Override
    protected Class<SourcesDeals> getEntityClass() {
        return SourcesDeals.class;
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
