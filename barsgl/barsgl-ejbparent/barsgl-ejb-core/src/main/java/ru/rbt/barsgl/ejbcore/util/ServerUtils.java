package ru.rbt.barsgl.ejbcore.util;

import org.apache.commons.codec.digest.DigestUtils;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;

import javax.enterprise.inject.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static java.lang.String.format;

/**
 * Created by Ivan Sevastyanov
 */
public class ServerUtils {

    public static String md5(String target) {
        return DigestUtils.md5Hex(target);
    }

    public static <T> T findAssignable(Class<? extends T> clazz, Instance<T> services) {
        for (T service : services) {
            if (clazz.isAssignableFrom(service.getClass())) return service;
        }
        throw new DefaultApplicationException("Not found: " + clazz.getName());
    }


    /**
     * Ищем сервис по нужным критериям.
     * Похоже это более общий случай поиска и перекрывает функциональность
     * метода выше <code>(ru.rbt.barsgl.ejbcore.util.ServerUtils#findAssignable(java.lang.Class, javax.enterprise.inject.Instance))</code>
     * @param services сервисы
     * @param predicate условие отбора
     * @param <T> тип
     * @return сервис
     * @throws RuntimeException if not found or too many
     */
    public static <T> T findSupported(Instance<T> services, Predicate<T> predicate) {
        final List<T> beans = new ArrayList<>();
        for (T service : services) {
            if (predicate.test(service)) {
                beans.add(service);
            }
        }
        if (beans.isEmpty()) {
            throw new RuntimeException("No services found");
        } else
        if (1 < beans.size()) {
            throw new RuntimeException(format("Too many services found: '%s'", beans));
        } else {
            return beans.get(0);
        }

    }

}
