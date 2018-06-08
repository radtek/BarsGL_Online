package ru.rbt.barsgl.ejb.repository.dict;

import ru.rbt.barsgl.ejb.entity.dict.AccountingTypeAepl;
import ru.rbt.barsgl.ejb.entity.dict.SourcesDeals;
import ru.rbt.ejbcore.repository.AbstractCachedRepository;
import ru.rbt.ejbcore.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.ejb.AccessTimeout;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by er18837 on 08.06.2018.
 */
@Singleton
@AccessTimeout(unit = TimeUnit.MINUTES, value = 15)
@Lock(LockType.READ)
public class AccTypeAeplRepository extends AbstractCachedRepository<AccountingTypeAepl, String> {
    public Boolean isAepl(String accType) {
        AccountingTypeAepl aepl = super.findCached(accType);
        return (!StringUtils.isEmpty(accType) && (null != aepl));
    }

    @Override
    protected Class<AccountingTypeAepl> getEntityClass() {
        return AccountingTypeAepl.class;
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
