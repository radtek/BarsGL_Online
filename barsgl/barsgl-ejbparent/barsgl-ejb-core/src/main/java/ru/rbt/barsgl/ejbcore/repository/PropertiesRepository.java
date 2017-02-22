package ru.rbt.barsgl.ejbcore.repository;

import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.conf.map.AbstractConfigProperty;
import ru.rbt.barsgl.ejbcore.conf.map.NumberProperty;
import ru.rbt.barsgl.ejbcore.conf.map.StringProperty;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;

import javax.annotation.PostConstruct;
import javax.ejb.AccessTimeout;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejbcore.conf.map.NumberProperty.nullNumberProperty;
import static ru.rbt.barsgl.ejbcore.conf.map.StringProperty.nullStringProperty;

/**
 * Created by ER21006 on 11.04.2016.
 */
@Singleton
@Lock(LockType.READ)
@AccessTimeout(unit = TimeUnit.MINUTES, value = 15)
public class PropertiesRepository extends AbstractCachedRepository <AbstractConfigProperty, String> {

    private static final Logger log = Logger.getLogger(PropertiesRepository.class.getName());

    /**
     * Результат <b>НЕ КЭШИРОВАН</b>, возможны проблемы с производительностью
     * @param key ключ для поиска
     * @param <T> параметр конфигурации
     * @return свойство
     */
    public <T extends AbstractConfigProperty> T getProperty(String key) {
        T property = (T) selectFirst(AbstractConfigProperty.class, "from AbstractConfigProperty p where p.id = ?1", key);
        if (null == property) {
            String message = format("Property '%s' is absent", key);
            log.log(Level.SEVERE, message, new DefaultApplicationException(message));
            return null;
        }
        if (null != property && null == property.getValue() && property.getRequired() == YesNo.Y) {
            String message = format("Required property '%s' is empty", key);
            log.log(Level.SEVERE, message, new DefaultApplicationException(message));
            return null;
        }
        return property;
    }

    /**
     * Результат кэширован, рекомендуется к использованю
     * @param key ключ для поиска
     * @param <T> параметр конфигурации
     * @return свойство
     * @throws ExecutionException
     */
    public <T extends AbstractConfigProperty> T getCachedProperty(String key) throws ExecutionException {
        return (T) findCached(key);
    }

    @Override
    public List<AbstractConfigProperty> getAllObjectsCached() {
        return super.getAllObjectsCached();
    }


    @Override
    protected Class<AbstractConfigProperty> getEntityClass() {
        return AbstractConfigProperty.class;
    }

    /**
     * Кэшированое Long значение параметра
     * @param key ключ
     * @return Кэшированое Long значение параметра
     * @throws ExecutionException
     */
    public Long getNumber(String key) throws ExecutionException {
        return Optional.ofNullable((NumberProperty)getCachedProperty(key)).orElse(nullNumberProperty()).getValue();
    }

    public Long getNumberDef(String key, Long def) {
        try {
            return Optional.ofNullable((NumberProperty)getCachedProperty(key)).orElse(new NumberProperty(def)).getValue();
        } catch (ExecutionException e) {
            return def;
        }
    }

    /**
     * Кэшированое String значение параметра
     * @param key ключ
     * @return Кэшированое String значение параметра
     * @throws ExecutionException
     */
    public String getString(String key) throws ExecutionException {
        return Optional.ofNullable((StringProperty)getCachedProperty(key)).orElse(nullStringProperty()).getValue();
    }

    public String getStringDef(String key, String def) {
        try {
            return Optional.ofNullable((StringProperty)getCachedProperty(key)).orElse(new StringProperty(def)).getValue();
        } catch (ExecutionException e) {
            return def;
        }
    }

    /**
     * Некэширемый список параметров по родительскому узлу
     * @param name
     * @param <T>
     * @return
     */
    public <T extends AbstractConfigProperty> List<T> getProperties(String name) {
        return (List<T>)select(AbstractConfigProperty.class, "from AbstractConfigProperty p where p.node.nodeName = ?1", name);
    }

    @Lock(LockType.WRITE)
    public void flushCache() {
        super.flushCache();
    }

    @PostConstruct
    public void init() {
        super.init();
    }
}
