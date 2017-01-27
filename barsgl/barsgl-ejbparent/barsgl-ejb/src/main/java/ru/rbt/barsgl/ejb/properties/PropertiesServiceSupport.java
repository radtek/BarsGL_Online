/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2016
 * Financial Board Project
 */
package ru.rbt.barsgl.ejb.properties;

import ru.rbt.barsgl.ejbcore.repository.PropertiesRepository;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Andrew Samsonov
 */
@Stateless
@LocalBean
public class PropertiesServiceSupport {

  private static final Logger logger = Logger.getLogger(PropertiesServiceSupport.class.getName());

  @Inject
  private PropertiesRepository propertyRepository;

  private static <T> T findJndiReference(String jndiName) throws NamingException {
    Context ctx = null;
    try {
      ctx = new InitialContext();
      return (T) ctx.lookup(jndiName);
    } finally {
      try {
        if (null != ctx) {
          ctx.close();
        }
      } catch (NamingException ignore) {
        logger.log(Level.WARNING, "Error on closing javax.naming.Context", ignore);
      }
    }
  }

  /**
   * Возращает значение переменной окружения из деплоймент дескрипота
   * @param propertyName имя переменной окружения
   * @return значение переменной окружения
   * @throws NamingException
   */
  public String getEnvProperty(String propertyName) throws NamingException {
    return findJndiReference(propertyName);
  }

  /**
   * Возращает значение системной переменной окружения
   * @param propertyName имя переменной окружения
   * @return значение переменной окружения
   */
  public String getSysProperty(String propertyName) {
    return System.getProperty(propertyName);
  }

  /**
   * Возращает значение переменной окружения из базы данных
   * @param propertyName имя переменной окружения
   * @return значение переменной окружения типа {@link String}
   * @throws ExecutionException
   */
  public String getDbProperyString(String propertyName) throws ExecutionException {
    return propertyRepository.getString(propertyName);
  }

  /**
   * Возращает значение переменной окружения из базы данных
   * @param propertyName имя переменной окружения
   * @return значение переменной окружения типа {@link Long}
   * @throws ExecutionException
   */
  public Long getDbProperyLong(String propertyName) throws ExecutionException {
    return propertyRepository.getNumber(propertyName);
  }
}
