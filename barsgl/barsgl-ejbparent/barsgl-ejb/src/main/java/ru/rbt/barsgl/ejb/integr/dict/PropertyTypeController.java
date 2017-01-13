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

import ru.rbt.barsgl.ejb.entity.dict.PropertyType;
import ru.rbt.barsgl.ejb.repository.dict.PropertyTypeRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.PropertyTypeWrapper;

/**
 *
 * @author Andrew Samsonov
 */
@Stateless
@LocalBean
public class PropertyTypeController extends BaseDictionaryController<PropertyTypeWrapper, Short, PropertyType, PropertyTypeRepository> {
  @Inject
  private PropertyTypeRepository repository;

  @Override
  public RpcRes_Base<PropertyTypeWrapper> create(PropertyTypeWrapper wrapper) {
    Short primaryKey = wrapper.getCode();    
    return create(wrapper, repository, PropertyType.class, primaryKey, 
            format("Тип собственности '%s' уже существует!", primaryKey), 
            format("Создан тип собственности: '%s'", primaryKey), 
            format("Ошибка при создании типа собственности: '%s'", primaryKey), 
            () -> {
              return new PropertyType(wrapper.getCode(), wrapper.getName(), wrapper.getGroup(), wrapper.getResidentType());
            });
  }

  @Override
  public RpcRes_Base<PropertyTypeWrapper> update(PropertyTypeWrapper wrapper) {
    Short primaryKey = wrapper.getCode();
    return update(wrapper,repository, PropertyType.class, 
            primaryKey,
            format("Тип собственности '%s' не найден!", primaryKey),
            format("Изменен тип собственности: '%s'", primaryKey), 
            format("Ошибка при изменении типа собственности: '%s'", primaryKey), 
            propertyType -> {
              propertyType.setName(wrapper.getName());
              propertyType.setGroup(wrapper.getGroup());
              propertyType.setResidentType(wrapper.getResidentType());
            });
  }

  @Override
  public RpcRes_Base<PropertyTypeWrapper> delete(PropertyTypeWrapper wrapper) {
    Short primaryKey = wrapper.getCode();
    return delete(wrapper, repository, PropertyType.class, primaryKey, 
            format("Тип собственности '%s' не найден!", primaryKey), 
            format("Удален тип собственности: '%s'", primaryKey),
            format("Ошибка при удалении типа собственности: '%s'", primaryKey)
            );
  }
  
}
