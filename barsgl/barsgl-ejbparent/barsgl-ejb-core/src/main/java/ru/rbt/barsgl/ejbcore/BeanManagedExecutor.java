package ru.rbt.barsgl.ejbcore;

import javax.persistence.EntityManager;
import java.sql.Connection;

/**
 * Created by Ivan Sevastyanov
 */
public interface BeanManagedExecutor <E> {

    E execute(EntityManager persistence, Connection connection) throws Exception;
}
