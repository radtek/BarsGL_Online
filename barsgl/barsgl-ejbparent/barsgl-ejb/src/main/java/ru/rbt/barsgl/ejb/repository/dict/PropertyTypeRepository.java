/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.ejb.repository.dict;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import ru.rbt.barsgl.ejb.entity.dict.PropertyType;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;

/**
 *
 * @author Andrew Samsonov
 */
public class PropertyTypeRepository extends AbstractBaseEntityRepository<PropertyType, Short> {
    public  boolean isPropertyTypeExists(Short id) {
        return null != selectFirst(PropertyType.class, "from PropertyType T where T.id = ?1", id);
    }
  
}
