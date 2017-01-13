/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.gwt.client.dict;

import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;

import java.util.ArrayList;

/**
 *
 * @author Andrew Samsonov
 */
public class AccountTypesByCategory extends ActParm {
  @Override
  protected ArrayList<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams) {
    return null;
  }

  //for cache
 /* @Override
  public void dispose() {
    super.dispose();
    LocalDataStorage.removeParam(AccTypeLookUp.class.getSimpleName());
  }*/

}
