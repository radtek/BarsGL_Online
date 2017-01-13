package ru.rbt.barsgl.bankjar;

import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by Ivan Sevastyanov
 */
@Singleton
@Lock(LockType.READ)
public class PropertiesBean {

    private static final Logger log = Logger.getLogger(PropertiesBean.class);

    private Properties properties;

    @PostConstruct
    public void init() {
        loadProperties();
    }

    private void loadProperties() {
        log.info("Initializing SINGLETON properties");
        properties = new Properties();
        try (InputStream is = BankProcess.class.getClassLoader().getResourceAsStream("connection.properties")) {
            properties.load(is);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public String getOption(String key) {
        return (String) properties.get(key);
    }


}
