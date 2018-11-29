package ru.rbt.barsgl.ejb.repository.dict;

import ru.rbt.barsgl.ejb.entity.dict.BVSourceDealView;
import ru.rbt.ejbcore.repository.AbstractCachedRepository;

import javax.annotation.PostConstruct;
import javax.ejb.AccessTimeout;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import java.util.concurrent.TimeUnit;

/**
 * Created by er18837 on 28.06.2017.
 */
@Singleton
@AccessTimeout(unit = TimeUnit.MINUTES, value = 15)
@Lock(LockType.READ)
public class BVSouceCachedRepository extends AbstractCachedRepository<BVSourceDealView, String> {

    @Override
    protected Class<BVSourceDealView> getEntityClass() {
        return BVSourceDealView.class;
    }

    @Lock(LockType.WRITE)
    @Override
    public void flushCache() {
        super.flushCache();
    }

    public Integer getDepth(final String src) {
        BVSourceDealView sourcesDeal = findCached(src);
        if (null != sourcesDeal) {
            return sourcesDeal.getShift();
        } else {
            return null;
        }
    }

    public boolean isStornoInvisible(final String src) {
        BVSourceDealView sourcesDeal = findCached(src);
        if (null != sourcesDeal) {
            return sourcesDeal.isStornoInvisible();
        } else {
            return false;
        }
    }

    @PostConstruct
    @Override
    public void init() {
        super.init();
    }
}
