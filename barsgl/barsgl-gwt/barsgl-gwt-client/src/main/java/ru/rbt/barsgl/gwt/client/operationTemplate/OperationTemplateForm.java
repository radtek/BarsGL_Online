package ru.rbt.barsgl.gwt.client.operationTemplate;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.dict.EditableDictionary;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.shared.enums.SecurityActionCode;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.shared.user.AppUserWrapper;

import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.core.datafields.Column.Type.*;
import static ru.rbt.barsgl.shared.dict.FormAction.*;

/**
 * Created by ER18837 on 16.03.16.
 */
public class OperationTemplateForm extends EditableDictionary<ManualOperationWrapper> {
    public static final String FORM_NAME = "Шаблоны операций";

    public OperationTemplateForm() {
        super(FORM_NAME);
        reconfigure(true);
    }

    public OperationTemplateForm(boolean full) {
        super(FORM_NAME);
        reconfigure(full);
    }

    private void reconfigure(boolean full) {
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        if (full) {
	        abw.addSecureAction(editTemplate(), SecurityActionCode.OperTmplProc);
	        abw.addSecureAction(createTemplate(), SecurityActionCode.OperTmplProc);
	        abw.addSecureAction(closeTemplate(), SecurityActionCode.OperTmplProc);
        }
    }

    @Override
    protected String prepareSql() {
        //AppUserWrapper wrapper = (AppUserWrapper) LocalDataStorage.getParam("current_user");
        return "select ID_TMPL, TMPL_NAME, TMPL_TYPE, SRC_PST, AC_DR, CCY_DR, CBCC_DR, AMT_DR, AC_CR, CCY_CR, CBCC_CR, AMT_CR, " +
                "NRT, RNRTL, DEPT_ID, PRFCNTR, SYS, USER_NAME from DWH.GL_OPRTMPL";

               /* Utils.Fmt("select ID_TMPL, TMPL_NAME, TMPL_TYPE, SRC_PST, AC_DR, CCY_DR, CBCC_DR, AMT_DR, AC_CR, CCY_CR, CBCC_CR, AMT_CR, " +
                         "NRT, RNRTL, DEPT_ID, PRFCNTR, SYS, USER_NAME from DWH.GL_OPRTMPL"
                          + " where exists(select 1 from GL_AU_PRMVAL where prm_code = 'Source' and prmval = SRC_PST and id_user = {0}) or\n" +
                            " exists(select 1 from GL_AU_PRMVAL where prm_code = 'Source' and prmval = '*' and id_user = {0})", wrapper.getId()); */
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();
        Column col;

        result.addColumn(new Column("ID_TMPL", LONG, "ИД GL", 70, false, false));
        result.addColumn(new Column("TMPL_NAME", STRING, "Наименование", 300));
        result.addColumn(new Column("SRC_PST", STRING, "Источник сделки", 70));
        result.addColumn(new Column("CCY_DR", STRING, "Валюта ДБ", 50, false, false));
        result.addColumn(new Column("CBCC_DR", STRING, "Филиал ДБ", 50));
        result.addColumn(new Column("AC_DR", STRING, "Счет ДБ", 120));
        result.addColumn(new Column("AMT_DR", DECIMAL, "Сумма ДБ", 100, false, false));

        result.addColumn(new Column("CCY_CR", STRING, "Валюта КР", 50, false, false));
        result.addColumn(new Column("CBCC_CR", STRING, "Филиал КР", 50));
        result.addColumn(new Column("AC_CR", STRING, "Счет КР", 120));
        result.addColumn(new Column("AMT_CR", DECIMAL, "Сумма КР", 100, false, false));

        result.addColumn(new Column("NRT", STRING, "Описание", 220, false, false));
        result.addColumn(new Column("RNRTL", STRING, "Описание русское", 220, false, false));

        result.addColumn(new Column("DEPT_ID", STRING, "Подразделение", 50));
        result.addColumn(new Column("PRFCNTR", STRING, "Профит центр", 50));

        result.addColumn(new Column("TMPL_TYPE", STRING, "Тип", 40));
        result.addColumn(new Column("SYS", STRING, "Системный", 40));
        result.addColumn(new Column("USER_NAME", STRING, "Пользователь", 100));

        return result;
    }

    @Override
    protected ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("TMPL_NAME", Column.Sort.ASC));
        return list;
    }

    private GridAction createTemplate() {
        return (GridAction)
                commonLazyAction("OperationTemplateDlg", "Создание шаблона GL", CREATE, table.getColumns(),
                "Шаблон не создан",
                "Ошибка создания шаблона",
                "Шаблон создан успешно ");
    }
    private GridAction editTemplate() {
        return (GridAction) commonLazyAction("OperationTemplateDlg", "Редактирование шаблона GL", UPDATE, table.getColumns(),
                "Шаблон не изменен",
                "Ошибка изменения шаблона",
                "Шаблон изменен успешно");
    }

    private GridAction closeTemplate() {
        return (GridAction) commonLazyAction("OperationTemplateDlg", "Удаление шаблона GL", DELETE, table.getColumns(),
                "Шаблон не удален",
                "Ошибка удаления шаблона",
                "Шаблон удален успешно");
    }

    @Override
    protected void save(ManualOperationWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<ManualOperationWrapper>> asyncCallbackImpl) throws Exception {
        BarsGLEntryPoint.dictionaryService.saveOperationTemplate(cnw, action, asyncCallbackImpl);
    }

}
