package ru.rbt.barsgl.ejbcore;

import java.io.Serializable;
import ru.rbt.ejbcore.repository.AbstractCachedRepository;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.rbt.ejbcore.mapping.BaseEntity;

/**
 * Created by Ivan Sevastyanov on 14.07.2016.
 */
public class CacheController {

    private static final Logger logger = Logger.getLogger(CacheController.class.getName());

    @Inject
    private Instance<AbstractCachedRepository<? extends BaseEntity, ? extends Serializable>> cachedRepositories;

    public void flushAllCaches() {
        for (AbstractCachedRepository repository : cachedRepositories) {
            try {
                repository.flushCache();
            } catch (NullPointerException e) {
                logger.log(Level.SEVERE
                        , String.format("Error on cache flushing: '%s'. Possible cache is not initilized", repository), e);
            }
        }
    }
}
