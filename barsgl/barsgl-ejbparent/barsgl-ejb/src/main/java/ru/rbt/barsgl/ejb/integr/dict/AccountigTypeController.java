/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.ejb.integr.dict;

import static java.lang.String.format;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;

import ru.rbt.barsgl.ejb.entity.dict.AccountingType;
import ru.rbt.barsgl.ejb.repository.dict.AccountingTypeRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.CodeNameWrapper;

/**
 *
 * @author Andrew Samsonov
 */
@Stateless
@LocalBean
public class AccountigTypeController extends BaseDictionaryController<CodeNameWrapper, String, AccountingType, AccountingTypeRepository> {
  
  @Inject
  private AccountingTypeRepository repository;

  @Override
  public RpcRes_Base<CodeNameWrapper> create(CodeNameWrapper wrapper) {
    String primaryKey = wrapper.getCode();    
    return create(wrapper, repository, AccountingType.class, primaryKey, 
            format("Accounting Type '%s' уже существует!", primaryKey), 
            format("Создан Accounting Type: '%s'", primaryKey), 
            format("Ошибка при создании Accounting Type: '%s'", primaryKey), 
            () -> {
              return new AccountingType(wrapper.getCode(), wrapper.getName());
            });
  }

  @Override
  public RpcRes_Base<CodeNameWrapper> update(CodeNameWrapper wrapper) {
    String primaryKey = wrapper.getCode();
    return update(wrapper,repository, AccountingType.class, 
            primaryKey,
            format("Accounting Type '%s' не найден!", primaryKey),
            format("Изменен Accounting Type: '%s'", primaryKey), 
            format("Ошибка при изменении Accounting Type: '%s'", primaryKey), 
            accountingType -> {
              accountingType.setAccountName(wrapper.getName());
            });
  }

  @Override
  public RpcRes_Base<CodeNameWrapper> delete(CodeNameWrapper wrapper) {
    String primaryKey = wrapper.getCode();
    return delete(wrapper, repository, AccountingType.class, primaryKey, 
            format("Accounting Type '%s' не найден!", primaryKey), 
            format("Удален Accounting Type: '%s'", primaryKey),
            format("Ошибка при удалении Accounting Type: '%s'", primaryKey)
            );
  }
  
}
