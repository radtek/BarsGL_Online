package ru.rbt.ejbcore;

import javax.persistence.EntityManager;

/**
 * Created by Ivan Sevastyanov
 */
public interface JpaAccessCallback<T> {
    T call(EntityManager persistence) throws Exception;
}
