package ru.rbt.barsgl.gwt.client.audit;


import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.client.gridForm.GridForm;
import ru.rbt.barsgl.gwt.client.quickFilter.AuditQuickFilterDlg;
import ru.rbt.barsgl.gwt.client.quickFilter.AuditQuickFilterParams;
import ru.rbt.barsgl.gwt.client.quickFilter.IQuickFilterParams;
import ru.rbt.barsgl.gwt.client.quickFilter.QuickFilterAction;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.GridWidget;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;

import java.util.ArrayList;
import java.util.Date;


/**
 * Created by akichigi on 27.04.15.
 */
public class AuditForm extends GridForm {
    public static final String FORM_NAME = "Аудит";
    private Column colSysTime;
    private Column colLogLevel;
    private Column colLogCode;

    public AuditForm(){
        super(FORM_NAME, true);
        reconfigure();
    }

    private void reconfigure() {
        GridAction quickFilterAction;
        abw.addAction(quickFilterAction = new AuditQuickFilterAction(grid, false));
        abw.addAction(new GridAction(grid, null, "Свойства", new Image(ImageConstants.INSTANCE.properties()), 10) {
            private AuditFormDlg dlg;

            @Override
            public void execute() {
                Row row = grid.getCurrentRow();
                if (row == null) return;
                dlg = new AuditFormDlg();
                dlg.show(row);
            }
        });
        quickFilterAction.execute();
    }

    @Override
    protected String prepareSql() {
        return   "select ID_RECORD, SYS_TIME, LOG_CODE, LOG_LEVEL, MESSAGE, ENTITYTYPE, ENTITY_ID, " +
                 "SRC, ERRORSRC, ERRORMSG, TRANSACTID, USER_NAME, USER_HOST,  PROCTIMEMS " +
                 "from GL_AUDIT";
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();

        Column col;

        result.addColumn(new Column("ID_RECORD", Column.Type.LONG, "ID", 80, true, false, Column.Sort.DESC, ""));
        result.addColumn(colSysTime = new Column("SYS_TIME", Column.Type.DATETIME, "Время записи", 130));
        colSysTime.setFormat("dd.MM.yyyy HH:mm:ss");
        //col.setFilterable(false);
        result.addColumn(colLogCode = new Column("LOG_CODE", Column.Type.STRING, "Код", 100));
        result.addColumn(colLogLevel = new Column("LOG_LEVEL", Column.Type.STRING, "Уровень", 70));
        result.addColumn(new Column("MESSAGE", Column.Type.STRING, "Информация", 500));
        result.addColumn(new Column("ENTITYTYPE", Column.Type.STRING, "Тип сущности (имя таблицы)", 110));
        result.addColumn(new Column("ENTITY_ID", Column.Type.STRING, "ID сущности", 100));

        result.addColumn(col = new Column("SRC", Column.Type.STRING, "Источник (вызов)", 550, false, false));
        col.setFilterable(false);
        result.addColumn(col = new Column("ERRORSRC", Column.Type.STRING, "Источник (ошибка)", 600, false, false));
        col.setFilterable(false);
        result.addColumn(col = new Column("ERRORMSG", Column.Type.STRING, "Сообщение об ошибке", 800, false, false));
        col.setFilterable(false);
        result.addColumn(col = new Column("TRANSACTID", Column.Type.STRING, "ID транзакции", 350, false, false));
        col.setFilterable(false);

        result.addColumn(new Column("USER_NAME", Column.Type.STRING, "Пользователь", 100));
        result.addColumn(new Column("USER_HOST", Column.Type.STRING, "Компьютер", 80));
        result.addColumn(new Column("PROCTIMEMS", Column.Type.INTEGER, "Длительность процесса", 120, false, false));

        return result;
    }

    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("ID_RECORD", Column.Sort.DESC));
        return list;
    }

    class AuditQuickFilterAction extends QuickFilterAction {
        DlgFrame quickFilterDlg;
        private boolean fromPrevDay;

        public AuditQuickFilterAction(GridWidget grid, boolean fromPrevDay) {
            super(grid, null, "Быстрый фильтр", new Image(ImageConstants.INSTANCE.quickfilter()), 10);
            this.fromPrevDay = fromPrevDay;
        }

        @Override
        public DlgFrame getFilterDialog() {
            if (quickFilterDlg == null)
                quickFilterDlg = new AuditQuickFilterDlg();
            return quickFilterDlg;
        }

        @Override
        public IQuickFilterParams getFilterParams() {
            return new AuditQuickFilterParams(colSysTime, colLogLevel, colLogCode);
        }

        @Override
        public Object[] getInitialFilterParams(Date operday, Date prevday) {
            Date currentDate = new Date();
            return new Object[] {currentDate, currentDate};
//            return new Object[] {fromPrevDay ? prevday : operday, operday};
        }

    }

}

