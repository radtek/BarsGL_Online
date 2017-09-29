/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.ejb.integr.dict;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.ExceptionUtils;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.PersistenceException;
import java.io.Serializable;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static ru.rbt.audit.entity.AuditRecord.LogCode.User;

/**
 *
 * @author Andrew Samsonov
 */
public abstract class BaseDictionaryController<T extends Serializable, K extends Serializable, E extends BaseEntity<K>, U extends AbstractBaseEntityRepository<E, K>> implements DictionaryController<T> {

  protected AuditController auditController;

  public RpcRes_Base<T> create(T wrapper, U repository, Class<E> clazz, K primaryKey,
          String checkMessage,
          String infoMessage,
          String auditErrorMessage,
          Supplier<E> supplier) {
    E entity;
    // проверка уникальности имени в таблице
    if (null != primaryKey) {
      entity = repository.findById(clazz, primaryKey);
      if (entity != null) {
        return new RpcRes_Base<>(wrapper, true, checkMessage);
      }
    }

    try {
      entity = supplier.get();
      beforeCreate(entity);
      repository.save(entity);
      afterCreate(entity);
      return auditInfo(wrapper, infoMessage);
    } catch (Exception ex) {
      return auditError(wrapper, auditErrorMessage, ex);
    }
  }
  
  protected RpcRes_Base<T> update(T wrapper, U repository, Class<E> clazz, K primaryKey,
          String checkMessage,
          String infoMessage,
          String auditErrorMessage,
          Consumer<E> consumer) {
    E entity = repository.findById(clazz, primaryKey);
    if (entity == null) {
      return new RpcRes_Base<>(wrapper, true, checkMessage);
    }

    try {
      beforeUpdate(entity);
      consumer.accept(entity);

      repository.update(entity);
      afterUpdate(entity);

      return auditInfo(wrapper, infoMessage);
    } catch (Exception ex) {
      return auditError(wrapper, auditErrorMessage, ex);
    }
  }

  public RpcRes_Base<T> delete(T wrapper, U repository, Class<E> clazz, K primaryKey,
          String checkMessage,
          String infoMessage,
          String auditErrorMessage) {
    E entity = repository.findById(clazz, primaryKey);
    if (entity == null) {
      return new RpcRes_Base<>(wrapper, true, checkMessage);
    }

    try {
      beforeDelete(entity);
      repository.remove(entity);
      afterDelete();

      return auditInfo(wrapper, infoMessage);
    } catch (Exception ex) {
      return auditError(wrapper, auditErrorMessage, ex);
    }
  }

  protected RpcRes_Base<T> auditInfo(T wrapper, String infoMessage) {
    if (infoMessage != null && infoMessage.length() > 0) auditController.info(User, infoMessage);
    return new RpcRes_Base<>(wrapper, false, infoMessage);
  }

  protected RpcRes_Base<T> auditError(T wrapper, String auditErrorMessage, Exception ex) {
    auditController.error(User, auditErrorMessage, null, ex);
    return new RpcRes_Base<>(wrapper, true, auditErrorMessage + ":\n" + getErrorMessage(ex));
  }

  private String getErrorMessage(Throwable throwable) {
    return ExceptionUtils.getErrorMessage(throwable,
            ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class,
            IllegalArgumentException.class, PersistenceException.class, DefaultApplicationException.class);
  }

  public void beforeCreate(E entity){}
  public void afterCreate(E entity){}

  public void beforeUpdate(E entity){}
  public void afterUpdate(E entity){}

  public void beforeDelete(E entity){}
  public void afterDelete(){}

  @PostConstruct
  public void postConstruct() {
    if (auditController == null) {
      InitialContext ctx = null;
      try {
        ctx = new InitialContext();
        auditController = (AuditController) ctx.lookup("java:global.barsgl.rbt-audit.AuditControllerEJBImpl!ru.rbt.audit.controller.AuditController");
      } catch (NamingException e) {
        throw new DefaultApplicationException(e.getMessage(), e);
      } finally {
        try {
          if (null != ctx) ctx.close();
        } catch (NamingException ignore) {
          ignore.printStackTrace();
        }
      }
    }
  }

}
