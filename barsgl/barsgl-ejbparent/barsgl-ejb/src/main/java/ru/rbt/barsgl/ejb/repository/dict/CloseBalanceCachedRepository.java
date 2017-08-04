package ru.rbt.barsgl.ejb.repository.dict;

import ru.rbt.barsgl.ejb.entity.dict.CloseLwdBalance;
import ru.rbt.barsgl.ejb.entity.dict.ClosedPeriodView;
import ru.rbt.ejbcore.repository.AbstractCachedRepository;

import javax.annotation.PostConstruct;
import javax.ejb.AccessTimeout;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by er18837 on 04.08.2017.
 */
@Singleton
@AccessTimeout(unit = TimeUnit.MINUTES, value = 15)
@Lock(LockType.READ)
public class CloseBalanceCachedRepository extends AbstractCachedRepository<CloseLwdBalance, Date> {
    @Override
    protected Class<CloseLwdBalance> getEntityClass() {
        return CloseLwdBalance.class;
    }

    @Lock(LockType.WRITE)
    @Override
    public void flushCache() {
        super.flushCache();
    }

    public CloseLwdBalance getRecord() {
        List<CloseLwdBalance> res = getAllObjectsCached();
        if (!res.isEmpty()) {
            return res.get(0);
        } else {
            return null;
        }
    }

    @PostConstruct
    @Override
    public void init() {
        super.init();
    }

}
