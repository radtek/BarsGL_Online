/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.ejb.integr.dict;

import ru.rbt.barsgl.ejb.entity.dict.SourcesDeals;
import ru.rbt.barsgl.ejb.repository.dict.SourcesDealsRepository;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.ExtCodeNameWrapper;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.util.List;

import static java.lang.String.format;

/**
 *
 * @author Andrew Samsonov
 */
@Stateless
@LocalBean
public class SourcesDealsController extends BaseDictionaryController<ExtCodeNameWrapper, String, SourcesDeals, SourcesDealsRepository> {
  
  @EJB
  private SourcesDealsRepository repository;

  @Override
  public RpcRes_Base<ExtCodeNameWrapper> create(ExtCodeNameWrapper wrapper) {
    // Check unique short name
    String shortName = wrapper.getShortName();
    List<SourcesDeals> sourcesDealsList =  repository.select(SourcesDeals.class, "select SD from SourcesDeals SD where SD.shortName = ?1", shortName);
    if(sourcesDealsList != null && !sourcesDealsList.isEmpty()){
      return new RpcRes_Base<>(wrapper, true, format("Источник сделок c кратким кодом '%s' уже существует!", shortName));
    }
    
    String primaryKey = wrapper.getCode();    
    return create(wrapper, repository, SourcesDeals.class, primaryKey, 
            format("Источник сделок '%s' уже существует!", primaryKey), 
            format("Создан источник сделок: '%s'", primaryKey), 
            format("Ошибка при создании источника сделок: '%s'", primaryKey), 
            () -> {
              return new SourcesDeals(wrapper.getCode(), wrapper.getShortName(), wrapper.getName());
            });
  }

  @Override
  public RpcRes_Base<ExtCodeNameWrapper> update(ExtCodeNameWrapper wrapper) {
    String primaryKey = wrapper.getCode();

    // Check unique short name
    String shortName = wrapper.getShortName();
    List<SourcesDeals> sourcesDealsList =  repository.select(SourcesDeals.class, 
            "select SD from SourcesDeals SD "
                    + "where SD.shortName = ?1 and SD.id != ?2", 
            shortName, primaryKey);
    if(sourcesDealsList != null && !sourcesDealsList.isEmpty()){
      return new RpcRes_Base<>(wrapper, true, format("Источник сделок c кратким кодом '%s' уже существует!", shortName));
    }

    return update(wrapper,repository, SourcesDeals.class, 
            primaryKey,
            format("Источник сделок '%s' не найден!", primaryKey),
            format("Изменен источник сделок: '%s'", primaryKey), 
            format("Ошибка при изменении источника сделок: '%s'", primaryKey), 
            sourcesDeals -> {
              sourcesDeals.setShortName(wrapper.getShortName());
              sourcesDeals.setLongName(wrapper.getName());
            });
  }

  @Override
  public RpcRes_Base<ExtCodeNameWrapper> delete(ExtCodeNameWrapper wrapper) {
    String primaryKey = wrapper.getCode();
    return delete(wrapper, repository, SourcesDeals.class, primaryKey, 
            format("Источник сделок '%s' не найден!", primaryKey), 
            format("Удален источник сделок: '%s'", primaryKey),
            format("Ошибка при удалении источника сделок: '%s'", primaryKey)
            );
  }
}
