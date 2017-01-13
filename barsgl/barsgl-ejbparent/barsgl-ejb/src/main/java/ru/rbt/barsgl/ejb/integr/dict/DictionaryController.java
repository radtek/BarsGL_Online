/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.ejb.integr.dict;

import java.io.Serializable;
import ru.rbt.barsgl.shared.RpcRes_Base;

/**
 *
 * @author Andrew Samsonov
 */
public interface DictionaryController<T extends Serializable> {
  RpcRes_Base<T> create(T wrapper);

  RpcRes_Base<T> update(T wrapper);

  RpcRes_Base<T> delete(T wrapper);
}
