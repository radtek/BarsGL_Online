/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.barsgl.ejbcore;

import ru.rbt.barsgl.shared.Repository;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.PersistenceProvider;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 *
 * @author Andrew Samsonov
 */
public class BarsglPersistenceProvider implements PersistenceProvider<Repository>{
    private static final Logger log = Logger.getLogger(AbstractBaseEntityRepository.class);

    @PersistenceContext(unitName="GLOracleDataSource")
    protected EntityManager persistence;

    @PersistenceContext(unitName="RepAS400DataSource")
    protected EntityManager barsrepPersistence;

//    @Resource(mappedName="jdbc/OracleGL")
    private DataSource dataSource;

//    @Resource(mappedName="jdbc/As400Rep")
    private DataSource  barsrepDataSource;

    @Resource(lookup = "java:app/env/BarsglDataSourceName")
    private String barsglDataSourceName;

    @Resource(lookup = "java:app/env/BarsrepDataSourceName")
    private String barsrepDataSourceName;
    
    @Override
    public EntityManager getPersistence(Repository repository) throws Exception {
        if(null == repository) {
            return getDefaultPersistence();
        }
        switch (repository) {
            case BARSGL:
                return persistence;
            case BARSREP:
                return barsrepPersistence;
            default:
                throw new Exception("Неизвестный репозиторий: " + repository.name());
        }
    }

    @Override
    public DataSource getDataSource(Repository repository) throws Exception {
        if(null == repository) {
            return getDefaultDataSource();
        }
        switch (repository) {
            case BARSGL:
                return dataSource;
            case BARSREP:
                return barsrepDataSource;
            default:
                throw new Exception("Неизвестный репозиторий: " + repository.name());
        }
    }

    @Override
    public DataSource getDefaultDataSource() {
        return dataSource;
    }

    @Override
    public EntityManager getDefaultPersistence() {
        return persistence;
    }

    @PostConstruct
    public void init() {
        dataSource = findConnection(barsglDataSourceName);
        try{
            barsrepDataSource = findConnection(barsrepDataSourceName);
        }catch(Exception ex){
            log.info("DataSource not found: "+barsrepDataSourceName);
        }
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
/*
    @Resource(lookup = "java:app/env/jdbc/OracleGL")
    @Resource(lookup = "java:app/env/jdbc/As400Rep")

    need add to application.xml
    WF
    <resource-ref>
        <res-ref-name>java:app/env/jdbc/OracleGL</res-ref-name>
        <res-type>javax.sql.DataSource</res-type>
        <res-auth>Container</res-auth>
        <res-sharing-scope>Shareable</res-sharing-scope>
        <lookup-name>java:/jdbc/OracleGL</lookup-name>
    </resource-ref>

    <resource-ref>
        <res-ref-name>java:app/env/jdbc/As400Rep</res-ref-name>
        <res-type>javax.sql.DataSource</res-type>
        <res-auth>Container</res-auth>
        <res-sharing-scope>Shareable</res-sharing-scope>
        <lookup-name>java:/jdbc/As400Rep</lookup-name>
    </resource-ref>

    WLS    
    <resource-ref>
        <res-ref-name>java:app/env/jdbc/OracleGL</res-ref-name>
        <res-type>javax.sql.DataSource</res-type>
        <res-auth>Container</res-auth>
        <res-sharing-scope>Shareable</res-sharing-scope>
        <lookup-name>jdbc/OracleGL</lookup-name>
    </resource-ref>

    <resource-ref>
        <res-ref-name>java:app/env/jdbc/As400Rep</res-ref-name>
        <res-type>javax.sql.DataSource</res-type>
        <res-auth>Container</res-auth>
        <res-sharing-scope>Shareable</res-sharing-scope>
        <lookup-name>jdbc/As400Rep</lookup-name>
    </resource-ref>
    
*/    
