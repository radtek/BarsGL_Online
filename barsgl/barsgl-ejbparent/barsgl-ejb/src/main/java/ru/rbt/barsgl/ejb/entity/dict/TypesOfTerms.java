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
@Table(name = "GL_DICTERM")
public class TypesOfTerms extends BaseEntity<String> {
  @Id
  @Column(name = "TERM", nullable = false, length = 2)
  private String id;

  @Column(name = "TERMNAME", nullable = false, length = 128)
  private String termName;

  public TypesOfTerms() {
  }

  public TypesOfTerms(String id, String termName) {
    this.id = id;
    this.termName = termName;
  }
  
  @Override
  public String getId() {
    return id;
  }

  public String getTermName() {
    return termName;
  }

  public void setTermName(String termName) {
    this.termName = termName;
  }
  
}
