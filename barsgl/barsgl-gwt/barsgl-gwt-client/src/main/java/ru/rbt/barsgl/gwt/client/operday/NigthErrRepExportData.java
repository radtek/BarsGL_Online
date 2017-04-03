package ru.rbt.barsgl.gwt.client.operday;

import ru.rbt.barsgl.gwt.client.Export.IExportData;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Columns;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.FilterItem;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;

import java.util.List;

import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.DATE;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.DECIMAL;
import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.STRING;

/**
 * Created by ak on 01.04.17.
 */
public class NigthErrRepExportData implements IExportData {
   public String sql(){
      /* return "select CUST_DR, CUST_CR, ID, ID_PST, ID_PKG, SRC_PST, EVT_ID, DEAL_ID, CHNL_NAME, " +
               "PMT_REF, DEPT_ID, VDATE, OTS, NRT, RNRTL, RNRTS, STRN, STRNRF, AC_DR, CCY_DR, " +
               "AMT_DR, AMTRU_DR, AC_CR, CCY_CR, AMT_CR, AMTRU_CR, FAN, PAR_RF, ECODE, EMSG, " +
               "ACCKEY_DR, ACCKEY_CR, ADDKEY_DR, ADDKEY_CR, EVTP, IS_VIP from V_GLA2_ERRORS";*/
       return "ru.rbt.barsgl.ejb.rep.NightErrRep@getSql";
   }

    @Override
    public Columns columns() {
        return table().getColumns();
    }

    @Override
    public List<FilterItem> masterFilterItems() {
        return null;
    }

    @Override
    public List<FilterItem> detailFilterItems() {
        return null;
    }

    @Override
    public List<SortItem> sortItems() {
        return null;
    }

    private Table table() {
       Table result = new Table();
       Column col;

       result.addColumn(new Column("CUST_DR", STRING, "CUST_DR", 100));
       result.addColumn(new Column("CUST_CR", STRING, "CUST_CR", 100));
       result.addColumn(new Column("ID", Column.Type.LONG, "ID", 100));
       result.addColumn(new Column("ID_PST", STRING, "ID_PST", 100));
       result.addColumn(new Column("ID_PKG", Column.Type.LONG, "ID_PKG", 100));
       result.addColumn(new Column("SRC_PST", STRING, "SRC_PST", 100));
       result.addColumn(new Column("EVT_ID", STRING, "EVT_ID", 100));
       result.addColumn(new Column("DEAL_ID", STRING, "DEAL_ID", 100));
       result.addColumn(new Column("CHNL_NAME", STRING, "CHNL_NAME", 100));
       result.addColumn(new Column("PMT_REF", STRING, "PMT_REF", 100));
       result.addColumn(new Column("DEPT_ID", STRING, "DEPT_ID", 100));
       result.addColumn(new Column("VDATE", DATE, "VDATE", 100));
       result.addColumn(col = new Column("OTS", Column.Type.DATETIME, "OTS", 100));
       col.setFormat("dd.MM.yyyy HH:mm:ss");
       result.addColumn(new Column("NRT", STRING, "NRT", 100));
       result.addColumn(new Column("RNRTL", STRING, "RNRTL", 100));
       result.addColumn(new Column("RNRTS", STRING, "RNRTS", 100));
       result.addColumn(new Column("STRN", STRING, "STRN", 100));
       result.addColumn(new Column("STRNRF", STRING, "STRNRF", 100));
       result.addColumn(new Column("AC_DR", STRING, "AC_DR", 100));
       result.addColumn(new Column("CCY_DR", STRING, "CCY_DR", 100));
       result.addColumn(new Column("AMT_DR", DECIMAL, "AMT_DR", 100));
       result.addColumn(new Column("AMTRU_DR", DECIMAL, "AMTRU_DR", 100));
       result.addColumn(new Column("AC_CR", STRING, "AC_CR", 100));
       result.addColumn(new Column("CCY_CR", STRING, "CCY_CR", 100));
       result.addColumn(new Column("AMT_CR", DECIMAL, "AMT_CR", 100));
       result.addColumn(new Column("AMTRU_CR", DECIMAL, "AMTRU_CR", 100));
       result.addColumn(new Column("FAN", STRING, "FAN", 100));
       result.addColumn(new Column("PAR_RF", STRING, "PAR_RF", 100));
       result.addColumn(new Column("ECODE", Column.Type.INTEGER, "ECODE", 100));
       result.addColumn(new Column("EMSG", STRING, "EMSG", 100));
       result.addColumn(new Column("ACCKEY_DR", STRING, "ACCKEY_DR", 100));
       result.addColumn(new Column("ACCKEY_CR", STRING, "ACCKEY_CR", 100));
       result.addColumn(new Column("ADDKEY_DR", STRING, "ADDKEY_DR", 100));
       result.addColumn(new Column("ADDKEY_CR", STRING, "ADDKEY_CR", 100));
       result.addColumn(new Column("EVTP", STRING, "EVTP", 100));
       result.addColumn(new Column("IS_VIP", Column.Type.INTEGER, "IS_VIP", 100));

       return result;
   }
}
