/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.ejb.entity.dict;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import ru.rbt.barsgl.ejbcore.mapping.BaseEntity;

/**
 *
 * @author Andrew Samsonov
 */
@Entity
@Table(name = "GL_DEPT")
public class Department extends BaseEntity<String> {
  
  @Id
  @Column(name = "DEPID", nullable = false, length = 3)
  private String id;
  
  @Column(name = "DEPNAME", nullable = false, length = 30)
  private String departmentName;

  public Department() {
  }

  public Department(String id, String departmentName) {
    this.id = id;
    this.departmentName = departmentName;
  }

  @Override
  public String getId() {
    return id;
  }

  public String getDepartmentName() {
    return departmentName;
  }

  public void setDepartmentName(String departmentName) {
    this.departmentName = departmentName;
  }
  
  
}
