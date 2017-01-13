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
public class CodeNameWrapper implements Serializable, IsSerializable {

  private String code;
  private String name;

  // список ошибок
  private ErrorList errorList = new ErrorList();

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ErrorList getErrorList() {
    return errorList;
  }

  public String getErrorMessage() {
    return errorList.getErrorMessage();
  }
}
