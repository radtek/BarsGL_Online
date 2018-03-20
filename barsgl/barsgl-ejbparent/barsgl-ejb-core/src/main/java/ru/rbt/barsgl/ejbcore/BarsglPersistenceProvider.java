/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.barsgl.ejbcore;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.shared.Repository;
import ru.rbt.ejbcore.PersistenceProvider;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

/**
 *
 * @author Andrew Samsonov
 */
public class BarsglPersistenceProvider implements PersistenceProvider<Repository>{
    private static final Logger log = Logger.getLogger(AbstractBaseEntityRepository.class);

    @PersistenceContext(unitName="GLOracleDataSource")
    protected EntityManager persistence;

    @PersistenceContext(unitName="GLOracleDataSourceNoXA")
    protected EntityManager persistenceNoXA;

    @PersistenceContext(unitName="RepAS400DataSource")
    protected EntityManager barsrepPersistence;

    @Resource(lookup="java:app/env/jdbc/NoXaOracleGL")
    private DataSource dataSourceNoXA;

    @Resource(lookup="java:app/env/jdbc/OracleGL")
    private DataSource dataSource;

    @Resource(lookup="java:app/env/jdbc/As400Rep")
    private DataSource  barsrepDataSource;
    
    @Override
    public EntityManager getPersistence(Repository repository) throws Exception {
        if(null == repository) {
            return getDefaultPersistence();
        }
        switch (repository) {
            case BARSGL:
                return persistence;
            case BARSGLNOXA:
                return  persistenceNoXA;
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
            case BARSGLNOXA:
                return dataSourceNoXA;
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

    /*
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
    */
    
}