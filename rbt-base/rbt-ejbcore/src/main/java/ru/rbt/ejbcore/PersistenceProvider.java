/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.ejbcore;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

/**
 *
 * @author Andrew Samsonov
 */
public interface PersistenceProvider<T extends Enum> {
    EntityManager getPersistence(T repository) throws Exception;
    DataSource getDataSource(T repository) throws Exception;
    
    public DataSource getDefaultDataSource();
    public EntityManager getDefaultPersistence();
}