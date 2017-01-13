package ru.rbt.barsgl.ejbcore;

import java.sql.Connection;

/**
 * Created by Ivan Sevastyanov
 */
public interface DataAccessCallback<T> {

    T call(Connection connection) throws Exception;

}
