package ru.rbt.ejbcore.repository;

import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.mapping.BaseEntity;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Suppliers.memoize;

/**
 * Created by Ivan Sevastyanov on 13.07.2016.<br/>
 * Общий класс для кэшированных результатов при поиске по первичному ключу<br/>
 * для этого используется специальный метод {@link ru.rbt.barsgl.ejbcore.repository.AbstractCachedRepository#findCached(java.io.Serializable)}<br/>
 * Кэшированный сервис должен быть помечен как @{@link Singleton} с типом блокировки {@link Lock} ({@link LockType#READ})<br/>
 * Обязательна иницализация на {@link PostConstruct} с вызовом метода {@link AbstractCachedRepository#init()}<br/>
 * Обязательно переопредение метода {@link AbstractCachedRepository#flushCache()}, он должен быть помечен {@link Lock}({@link LockType#WRITE}) <br/>
 * <br/>
 * <br/>
 * По умолчанию кэш имеет максимальный размер - 200, срок жизни объектов - 5 минут после загрузки
 * <br/>
 * <br/>
 * Пример<br/>
 * <pre>
 * {@code
    @code @Singleton
    @code @Lock(LockType.READ)
    public class PropertiesRepository extends AbstractCachedRepository <AbstractConfigProperty, String> {

        public <T extends AbstractConfigProperty> T getCachedProperty(String key) throws ExecutionException {
            return (T) findCached(key);
        }

        public Long getNumber(String key) throws ExecutionException {
            return Optional.ofNullable((NumberProperty)getCachedProperty(key)).orElse(nullNumberProperty()).getValue();
        }

        @code @Override
        public Class<AbstractConfigProperty> getEntityClass() {
            return AbstractConfigProperty.class;
        }

        @code @Lock(LockType.WRITE)
        public void flushCache() {
            super.flushCache();
        }

        @code @PostConstruct
        public void init() {
            super.init();
        }
    }
 * }
 * </pre>
 * */
public abstract class AbstractCachedRepository<T extends BaseEntity, K extends Serializable> extends AbstractBaseEntityRepository<T,K> {

    public static final Serializable ALL_OBJECTS_KEY = "ALL_OBJECTS_KEY";

    /**
     * Default cache implementation
     * @return cache
     */
    protected LoadingCache<Serializable, Optional> createCache() {
        return CacheBuilder.<Serializable, Object>newBuilder()
                .maximumSize(200).expireAfterWrite(5*60, TimeUnit.SECONDS)
                .build(new CacheLoader<Serializable, Optional>() {
                    @Override
                    public Optional load(final Serializable key) throws Exception {
                        // в кэше храним в тч и все объекты
                        if (key.equals(ALL_OBJECTS_KEY)) {
                            return Optional.of(getAllObjects());
                        }
                        return Optional.ofNullable(AbstractCachedRepository.super.findById(getEntityClass(), (K)key));
                    }
                });
    }

    private Supplier<LoadingCache<Serializable, Optional>> INTERNAL_CACHE;

    public T findCached(K primaryKey) {
        try {
            return (T) INTERNAL_CACHE.get().get(primaryKey).orElse(null);
        } catch (ExecutionException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    /**
     * @return Кэшированный список всех объектов
     */
    public List<T> getAllObjectsCached() {
        try {
            return (List<T>) INTERNAL_CACHE.get().get(ALL_OBJECTS_KEY).orElse(Collections.emptyList());
        } catch (ExecutionException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private List<T> getAllObjects() {
        return getPersistence().createQuery("from " + getEntityClass().getSimpleName() + " c").setMaxResults(200).getResultList();
    }

    protected Class<T> getEntityClass() {
        throw new IllegalAccessError("Not implemented");
    }


    public void flushCache() throws NullPointerException {
        INTERNAL_CACHE.get().invalidateAll();
    }

    public void init() {
        INTERNAL_CACHE = memoize(this::createCache);
    }

}
