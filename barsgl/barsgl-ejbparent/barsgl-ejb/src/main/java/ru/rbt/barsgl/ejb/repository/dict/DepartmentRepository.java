/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.ejb.repository.dict;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import ru.rbt.barsgl.ejb.entity.dict.Department;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 *
 * @author Andrew Samsonov
 */
@Stateless
@LocalBean
public class DepartmentRepository extends AbstractBaseEntityRepository<Department, String> {
  
}
