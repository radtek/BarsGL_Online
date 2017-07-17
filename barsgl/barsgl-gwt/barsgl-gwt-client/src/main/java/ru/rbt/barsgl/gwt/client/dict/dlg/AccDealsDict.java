package ru.rbt.barsgl.gwt.client.dict.dlg;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.dict.EditableDictionary;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.AccDealsWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;

/**
 * Created by er22317 on 17.07.2017.
 */
public class AccDealsDict extends EditableDictionary<AccDealsWrapper> {
    public final static String FORM_NAME = "Счета для контроля параметров сделки";
    public final static String FIELD_ACC2 = "Б/счет 2-го порядка";
    public final static String FIELD_NAME = "Наименование счета";
    public final static String FIELD_FLAG_OFF = "Исключен";

    public AccDealsDict() {
        super(FORM_NAME);
        reconfigure();
    }

    private void reconfigure() {
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
    }

    @Override
    protected void save(AccDealsWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<AccDealsWrapper>> asyncCallbackImpl) throws Exception {
        BarsGLEntryPoint.dictionaryService.saveAccDeals(cnw, action, asyncCallbackImpl);
    }

    @Override
    protected Table prepareTable() {
        Table table = new Table();
        table.addColumn(new Column("ACC2", Column.Type.STRING, FIELD_ACC2, 100, true, false));
        table.addColumn(new Column("NAME", Column.Type.STRING, FIELD_NAME, 100, true, false));
        table.addColumn(new Column("FLAG_OFF", Column.Type.STRING, FIELD_FLAG_OFF, 100, true, false));
        return table;
    }

    @Override
    protected String prepareSql() {
        return "select ad.acc2,  BSS.ACC1NAM ||' '|| BSS.ACC2NAM name, flag_off from GL_ACCDEALS ad, bss bss where bss.acc2=ad.acc2";
    }
}
