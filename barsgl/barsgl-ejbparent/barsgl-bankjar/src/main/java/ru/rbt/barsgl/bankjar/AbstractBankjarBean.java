package ru.rbt.barsgl.bankjar;

import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by Ivan Sevastyanov
 */
@RequestScoped
public abstract class AbstractBankjarBean {

//    @Resource(mappedName="/jdbc/As400GL")
    @Resource(mappedName="/jdbc/OracleGL")
    private DataSource dataSource;

    @Inject
    private PropertiesBean properties;

    public Connection connection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public String schemaDWH() {
        return properties.getOption("schema_dwh");
    }

    public String PD() {
        return properties.getOption("pd");
    }

}
