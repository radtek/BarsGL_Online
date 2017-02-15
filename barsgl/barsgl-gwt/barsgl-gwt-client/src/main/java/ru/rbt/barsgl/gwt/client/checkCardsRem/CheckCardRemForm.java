package ru.rbt.barsgl.gwt.client.checkCardsRem;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.Utils;

import java.util.ArrayList;

/**
 * Created by akichigi on 15.12.16.
 */
public class CheckCardRemForm extends GridForm {
    public final static String FORM_NAME = "Проверка остатков по картотеке";

    private CheckCardRemFilterDlg dlg = null;
    private GridAction preFilterAction;
    private final String _sql =
            "select a.branch, sum((value(b.obac,0) + value(b.dtac,0) + value(b.ctac,0) + value(c.dtac, 0) + value(c.ctac,0)) * 0.01) as sum, a.ccy, a.subdealid " +
            "from baltur b " +
            "left join gl_acc a on b.bsaacid = a.bsaacid " +
            "left join gl_baltur c on c.bsaacid = b.bsaacid and c.dat <= '{0}' " +
            "where b.bsaacid in ( select t.bsaacid from  dwh.gl_acc t " +
            "where t.cbccn ='{1}' and t.acc2 in ('90901','90902') and t.subdealid  in ('1.2','2')) " +
                   // "where t.cbccn ='{1}' ) " +
            "and b.dat <= '{0}' and b.datto >= '{0}' " +
            "group by a.ccy, a.branch, a.subdealid " +
            "order by a.subdealid, a.branch, a.ccy";

    public CheckCardRemForm() {
        super(FORM_NAME, true);
        reconfigure();
        doActionEnable(false);
        preFilterAction.execute();
    }


    private void reconfigure() {
        abw.addAction(preFilterAction = createPreFilter());
    }

    private GridAction createPreFilter() {
        return new GridAction(grid, null, "Выбор параметров проверки остатков", new Image(ImageConstants.INSTANCE.quickfilter()), 10){

            @Override
            public void execute() {
               if (dlg == null) dlg = new CheckCardRemFilterDlg();
               dlg.setDlgEvents(this);
               dlg.show(null);
            }

            public void onDlgOkClick(Object prms){
                dlg.hide();

                setSql(sql(((String[]) prms)[0], ((String[]) prms)[1]));
                //System.out.println(sql(((String[]) prms)[0], ((String[]) prms)[1]));

                doActionEnable(true);
                refreshAction.execute();
            }
        };
    }

    private void doActionEnable(boolean enable){
        refreshAction.setEnable(enable);
        filterAction.setEnable(enable);
        exportToExcel.setEnable(enable);
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();

        result.addColumn(new Column("branch", Column.Type.STRING, "Код отделения", 100));
        result.addColumn(new Column("sum", Column.Type.DECIMAL, "Сумма остатка на счетах", 100));
        result.addColumn(new Column("ccy", Column.Type.STRING, "Валюта", 70));
        result.addColumn(new Column("subdealid", Column.Type.STRING, "Тип картотеки", 100));

        return  result;
    }

   /* @Override
    protected ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("subdealid", Column.Sort.ASC));
        list.add(new SortItem("branch", Column.Sort.ASC));
        list.add(new SortItem("ccy", Column.Sort.ASC));
        return list;
    }*/

    @Override
    protected String prepareSql() {
        return null;
    }

    public void setSql(String text){
        sql_select = text;
        setExcelSql(sql_select);
    }

    private String sql(String date, String filial){
        return Utils.Fmt(_sql, date, filial);
    }
}
