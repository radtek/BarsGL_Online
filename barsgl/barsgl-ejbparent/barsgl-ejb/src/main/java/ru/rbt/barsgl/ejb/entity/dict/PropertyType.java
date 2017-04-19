/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.ejb.entity.dict;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import ru.rbt.ejbcore.mapping.BaseEntity;

/**
 *
 * @author Andrew Samsonov
 */
@Entity
@Table(name = "CBCTP")
public class PropertyType extends BaseEntity<Short> {
  @Id
  @Column(name = "CTYPE", nullable = false)
  private Short id;

  @Column(name = "NAME", nullable = false, length = 70)
  private String name;

  @Column(name = "PRCD", nullable = false, length = 1)
  private String group;

  @Column(name = "RECD", nullable = false, length = 1)
  private String residentType;

  public PropertyType() {
  }

  public PropertyType(Short id, String name, String group, String residentType) {
    this.id = id;
    this.name = name;
    this.group = group;
    this.residentType = residentType;
  }
  
  @Override
  public Short getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String getResidentType() {
    return residentType;
  }

  public void setResidentType(String residentType) {
    this.residentType = residentType;
  }

  
}
