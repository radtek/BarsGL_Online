package ru.rbt.barsgl.bankjar;

import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;

/**
 * Created by Ivan Sevastyanov
 */
@RequestScoped
public abstract class AbstractBankjarBean {

//    @Resource(mappedName="/jdbc/As400GL")
//    @Resource(mappedName="jdbc/OracleGL")
    private DataSource dataSource;

    @Resource(lookup = "java:app/env/BarsglDataSourceName")
    private String barsglDataSourceName;
    
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

    @PostConstruct
    public void init() {
        dataSource = findConnection(barsglDataSourceName);
    }

    private DataSource findConnection(String jndiName) {
        try {
            InitialContext c = new InitialContext();
            return  (DataSource) c.lookup(jndiName);
        } catch (Throwable e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }
}
