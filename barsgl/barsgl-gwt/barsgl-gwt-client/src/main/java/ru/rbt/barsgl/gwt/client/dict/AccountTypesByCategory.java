/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2015
 * BARS GL
 */
package ru.rbt.barsgl.gwt.client.dict;

import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.shared.filter.FilterCriteria;

import java.util.ArrayList;

/**
 *
 * @author Andrew Samsonov
 */
public class AccountTypesByCategory extends ActParm {
  @Override
  protected ArrayList<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams) {
    ArrayList<FilterItem> list = new ArrayList<FilterItem>();
    FilterItem item = new FilterItem(colTechAct, FilterCriteria.EQ, "Нет");
    list.add(item);
    return list;
  }


  //for cache
 /* @Override
  public void dispose() {
    super.dispose();
    LocalDataStorage.removeParam(AccTypeLookUp.class.getSimpleName());
  }*/

}
