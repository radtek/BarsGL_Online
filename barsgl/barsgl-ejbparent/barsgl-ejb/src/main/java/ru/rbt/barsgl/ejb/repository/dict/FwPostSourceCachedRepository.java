package ru.rbt.barsgl.ejb.repository.dict;

import ru.rbt.barsgl.ejb.entity.dict.BVSourceDealView;
import ru.rbt.barsgl.ejb.entity.dict.ForwardPostSources;
import ru.rbt.ejbcore.repository.AbstractCachedRepository;

import javax.annotation.PostConstruct;
import javax.ejb.AccessTimeout;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by er18837 on 14.02.2018.
 */
@Singleton
@AccessTimeout(unit = TimeUnit.MINUTES, value = 15)
@Lock(LockType.READ)
public class FwPostSourceCachedRepository extends AbstractCachedRepository<ForwardPostSources, String> {
    @Override
    protected Class<ForwardPostSources> getEntityClass() {
        return ForwardPostSources.class;
    }

    @Lock(LockType.WRITE)
    @Override
    public void flushCache() {
        super.flushCache();
    }

    public Boolean isForward(final String src, final Date procdate) {
        ForwardPostSources sourcesDeal = findCached(src);
        return (null != sourcesDeal && null != procdate
                && (!procdate.before(sourcesDeal.getStartDate()))
                && (null == sourcesDeal.getEndDate() || !procdate.after(sourcesDeal.getEndDate())));
    }

    @PostConstruct
    @Override
    public void init() {
        super.init();
    }
}
