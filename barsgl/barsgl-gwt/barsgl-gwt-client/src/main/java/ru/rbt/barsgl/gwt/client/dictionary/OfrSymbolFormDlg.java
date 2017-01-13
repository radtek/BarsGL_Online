package ru.rbt.barsgl.gwt.client.dictionary;

import ru.rbt.barsgl.gwt.client.gridForm.GridFormDlgBase;
import ru.rbt.barsgl.gwt.client.dict.OfrSymbolsBase;
import ru.rbt.barsgl.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.core.dialogs.FilterCriteria;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;

import java.util.ArrayList;
import java.util.Date;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.isEmpty;

/**
 * Created by ER18837 on 09.12.15.
 */
public abstract class OfrSymbolFormDlg extends GridFormDlgBase {

    public OfrSymbolFormDlg(Date currentDate, String acod, String ctype) {
        super("Выбор символа ОФР");
    }

    @Override
    protected GridForm getGridForm() {
        return new OfrSymbolGridForm();
    }

    class OfrSymbolGridForm extends OfrSymbolsBase {
        public OfrSymbolGridForm() {
            super("Список символов ОФР");
        }

        @Override
        public ArrayList<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams) {
            Date currentDate = (Date)initialFilterParams[0];
            String acod = (String)initialFilterParams[1];
            String ctype = (String)initialFilterParams[2];

            ArrayList<FilterItem> list = new ArrayList<FilterItem>();
            list.add(new FilterItem(colDateBegin, FilterCriteria.LE, currentDate, true));
            list.add(new FilterItem(colDateEnd, FilterCriteria.GE, currentDate, true));
            if (!isEmpty(acod)) list.add(new FilterItem(colAcod, FilterCriteria.EQ, acod));
//            if (!isEmpty(ctype)) list.add(new FilterItem("HBCTYP", FilterCriteria.EQ, ctype));
            return list;
        }

        @Override
        public Object[] getInitialFilterParams() {
            return OfrSymbolFormDlg.this.getInitialFilterParams();
        }
    }
}
