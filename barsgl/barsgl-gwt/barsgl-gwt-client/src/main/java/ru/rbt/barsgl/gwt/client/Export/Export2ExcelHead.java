package ru.rbt.barsgl.gwt.client.Export;

import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.shared.Export.ExcelExportHead;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.user.AppUserWrapper;

import java.util.List;

/**
 * Created by akichigi on 20.03.17.
 */
public class Export2ExcelHead {
    private List<FilterItem> items;
    private String formTitle;

    public Export2ExcelHead(String formTitle, List<FilterItem> items){
        this.formTitle = formTitle;
        this.items = items;
    }

    public ExcelExportHead createExportHead(){
        String user = "";
        AppUserWrapper current_user = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        if (current_user != null){
            user = Utils.Fmt("{0}({1})", current_user.getUserName(), current_user.getSurname());
        }
        return new ExcelExportHead(formTitle, user, getFilter(items));
    }

    private String getFilter(List<FilterItem> items){
        String res = "";
        if (items == null) return res;
        for (int i = 0; i < items.size(); i++){
            FilterItem item = items.get(i);
            res += Utils.Fmt("[\"{0}\" {1} '{2}']{3} ", item.getCaption(), item.getCriteria().getValue(), item.getStrValue(),
                    i == items.size()-1 ? "" : ";");
        }
        return res;
    }
}
