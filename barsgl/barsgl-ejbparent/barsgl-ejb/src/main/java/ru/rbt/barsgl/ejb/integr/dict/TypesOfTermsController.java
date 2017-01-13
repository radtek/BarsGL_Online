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

import ru.rbt.barsgl.ejb.entity.dict.TypesOfTerms;
import ru.rbt.barsgl.ejb.repository.dict.TypesOfTermsRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.CodeNameWrapper;

/**
 *
 * @author Andrew Samsonov
 */
@Stateless
@LocalBean
public class TypesOfTermsController extends BaseDictionaryController<CodeNameWrapper, String, TypesOfTerms, TypesOfTermsRepository> {

  @Inject
  private TypesOfTermsRepository repository;
  
  @Override
  public RpcRes_Base<CodeNameWrapper> create(CodeNameWrapper wrapper) {
    String primaryKey = wrapper.getCode();    
    return create(wrapper, repository, TypesOfTerms.class, primaryKey, 
            format("Код срока '%s' уже существует!", primaryKey), 
            format("Создан код срока: '%s'", primaryKey), 
            format("Ошибка при создании кода срока: '%s'", primaryKey), 
            () -> {
              return new TypesOfTerms(wrapper.getCode(), wrapper.getName());
            });
  }

  @Override
  public RpcRes_Base<CodeNameWrapper> update(CodeNameWrapper wrapper) {
    String primaryKey = wrapper.getCode();
    return update(wrapper,repository, TypesOfTerms.class, 
            primaryKey,
            format("Код срока '%s' не найден!", primaryKey),
            format("Изменен код срока: '%s'", primaryKey), 
            format("Ошибка при изменении кода срока: '%s'", primaryKey), 
            typesOfTerms -> {
              typesOfTerms.setTermName(wrapper.getName());
            });
  }

  @Override
  public RpcRes_Base<CodeNameWrapper> delete(CodeNameWrapper wrapper) {
    String primaryKey = wrapper.getCode();
    return delete(wrapper, repository, TypesOfTerms.class, primaryKey, 
            format("Код срока '%s' не найден!", primaryKey),
            format("Удален код срока: '%s'", primaryKey),
            format("Ошибка при удалении код срока: '%s'", primaryKey)
            );
  }
}
