/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.shared.dict;

import com.google.gwt.user.client.rpc.IsSerializable;
import java.io.Serializable;
import ru.rbt.barsgl.shared.ErrorList;

/**
 *
 * @author Andrew Samsonov
 */
public class PropertyTypeWrapper implements Serializable, IsSerializable {
  private Short code;

  private String name;

  private String group;

  private String residentType;
  
  // список ошибок
  private ErrorList errorList = new ErrorList();

  public Short getCode() {
    return code;
  }

  public void setCode(Short code) {
    this.code = code;
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

  public ErrorList getErrorList() {
    return errorList;
  }

  public void setErrorList(ErrorList errorList) {
    this.errorList = errorList;
  }
  
}
