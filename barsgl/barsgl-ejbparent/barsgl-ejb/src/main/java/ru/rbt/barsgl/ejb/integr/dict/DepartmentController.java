/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.ejb.integr.dict;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;

import ru.rbt.barsgl.ejb.entity.dict.Department;
import ru.rbt.barsgl.ejb.repository.dict.DepartmentRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.CodeNameWrapper;
import static java.lang.String.format;

/**
 *
 * @author Andrew Samsonov
 */
@Stateless
@LocalBean
public class DepartmentController extends BaseDictionaryController<CodeNameWrapper, String, Department, DepartmentRepository> {

  @Inject
  private DepartmentRepository repository;

  @Override
  public RpcRes_Base<CodeNameWrapper> create(CodeNameWrapper wrapper) {
    String primaryKey = wrapper.getCode();    
    return create(wrapper, repository, Department.class, primaryKey, 
            format("Подразделение '%s' уже существует!", primaryKey), 
            format("Создано подразделение: '%s'", primaryKey), 
            format("Ошибка при создании подразделения: '%s'", primaryKey), 
            () -> {
              return new Department(wrapper.getCode(), wrapper.getName());
            });
  }

  @Override
  public RpcRes_Base<CodeNameWrapper> update(CodeNameWrapper wrapper) {
    String primaryKey = wrapper.getCode();
    return update(wrapper,repository, Department.class, 
            primaryKey,
            format("Подразделение '%s' не найдено!", primaryKey),
            format("Изменено подразделение: '%s'", primaryKey), 
            format("Ошибка при изменении подразделения: '%s'", primaryKey), 
            department -> {
              department.setDepartmentName(wrapper.getName());
            });
  }

  @Override
  public RpcRes_Base<CodeNameWrapper> delete(CodeNameWrapper wrapper) {
    String primaryKey = wrapper.getCode();
    return delete(wrapper, repository, Department.class, primaryKey, 
            format("Подразделение '%s' не найдено!", primaryKey), 
            format("Удалено подразделение: '%s'", primaryKey),
            format("Ошибка при удалении подразделения: '%s'", primaryKey)
            );
  }

}
