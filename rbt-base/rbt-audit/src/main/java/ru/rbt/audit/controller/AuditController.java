/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.audit.controller;

import javax.ejb.Local;
import ru.rbt.ejbcore.mapping.BaseEntity;

import ru.rbt.audit.entity.AuditRecord;



/**
 *
 * @author Andrew Samsonov
 */
@Local()
public interface AuditController {

  void info(AuditRecord.LogCode operCode, String message, BaseEntity entity);
  void info(AuditRecord.LogCode operCode, String message);
  void info(AuditRecord.LogCode operCode, String message, String entity_type, String entity_id);

  void error(AuditRecord.LogCode operCode, String message, String entity_type, String entity_id, Throwable e);
  void error(AuditRecord.LogCode operCode, String message, String entity_type, String entity_id, String errorMessage);
  void error(AuditRecord.LogCode operCode, String message, BaseEntity entity, Throwable e);
  void error(AuditRecord.LogCode operCode, String message, BaseEntity entity, String errorMessage);

  void warning(AuditRecord.LogCode operCode, String message);
  void warning(AuditRecord.LogCode operCode, String message, BaseEntity entity, String errorMessage);
  void warning(AuditRecord.LogCode operCode, String message, BaseEntity entity, Throwable e);
  void warning(AuditRecord.LogCode operCode, String message, String entity_type, String entity_id, Throwable e);
  void warning(AuditRecord.LogCode operCode, String message, String entity_type, String entity_id, String errorMessage);

  void stat(AuditRecord.LogCode operCode, String message, String longMessage, String entityId);
  void stat(AuditRecord.LogCode operCode, String message, String longMessage);

}
