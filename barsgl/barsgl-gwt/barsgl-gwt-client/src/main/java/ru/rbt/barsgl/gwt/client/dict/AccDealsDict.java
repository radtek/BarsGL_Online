package ru.rbt.barsgl.gwt.client.dict;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.dict.EditableDictionary;
import ru.rbt.barsgl.gwt.client.dict.dlg.AccDealsDlg;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.dict.AccDealsWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.security.gwt.client.formmanager.FormManagerUI;
import ru.rbt.shared.enums.SecurityActionCode;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Created by er22317 on 17.07.2017.
 */
public class AccDealsDict extends EditableDictionary<AccDealsWrapper> {
    static Logger log = Logger.getLogger("AccDealsDict");

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
//        abw.addSecureAction(createAction(), SecurityActionCode.ReferAccTypeChng);
//        abw.addSecureAction(editAction(), SecurityActionCode.ReferAccTypeChng);
//        abw.addSecureAction(deleteAction(), SecurityActionCode.ReferAccTypeDel);

        abw.addSecureAction(createAction(new AccDealsDlg(AccDealsDlg.CREATE, grid.getTable().getColumns(), FormAction.CREATE),
                "Настройка не создана.\n Ошибка: ",
                "Ошибка создания настройки: \n",
                "Настройка создана успешно: "), SecurityActionCode.ReferAcc2Deals);
        abw.addSecureAction(editAction(new AccDealsDlg(AccDealsDlg.EDIT, grid.getTable().getColumns(), FormAction.UPDATE),
                "Настройка не сохранена.\n Ошибка: ",
                "Ошибка при изменении настройки: \n",
                "Настройка изменена успешно: "), SecurityActionCode.ReferAcc2Deals);
        abw.addSecureAction(deleteAction(new AccDealsDlg(AccDealsDlg.DELETE, grid.getTable().getColumns(), FormAction.DELETE),
                "Настройка не удалена.\n Ошибка: ",
                "Ошибка удаления настройки: \n",
                "Настройка удалена успешно: "), SecurityActionCode.ReferAcc2Deals);

    }

    @Override
    protected void save(AccDealsWrapper cnw, FormAction action, AsyncCallback<RpcRes_Base<AccDealsWrapper>> asyncCallbackImpl) throws Exception {
        BarsGLEntryPoint.dictionaryService.saveAccDeals(cnw, action, asyncCallbackImpl);
        log.info("save loadAcc2forDeals()");
        ArrayList Acc2ForDeals =((ArrayList)LocalDataStorage.getParam("Acc2ForDeals"));
        if (action.equals(FormAction.CREATE) && cnw.isFlagOff()) {
            Acc2ForDeals.add(cnw.getAcc2());
        }else if (action.equals(FormAction.UPDATE)){
            if (cnw.isFlagOff() && Acc2ForDeals.indexOf(cnw.getAcc2())< 0) Acc2ForDeals.add(cnw.getAcc2());
            else if (!cnw.isFlagOff()) Acc2ForDeals.remove(cnw.getAcc2());
        }else if (action.equals(FormAction.DELETE)){
            Acc2ForDeals.remove(cnw.getAcc2());
        }
        LocalDataStorage.putParam("Acc2ForDeals", Acc2ForDeals);
//        FormManagerUI.loadAcc2forDeals();
        log.info("indexOf(\"45204\") + "+((ArrayList)LocalDataStorage.getParam("Acc2ForDeals")).indexOf("45204"));
    }

    @Override
    protected Table prepareTable() {
        Table table = new Table();
        table.addColumn(new Column("ACC2", Column.Type.STRING, FIELD_ACC2, 10, true, false));
        table.addColumn(new Column("NAME", Column.Type.STRING, FIELD_NAME, 100, true, false));
        table.addColumn(new Column("FLAG_OFF", Column.Type.STRING, FIELD_FLAG_OFF, 10, true, false));
        return table;
    }

    @Override
    protected String prepareSql() {
        return "select ad.acc2,  BSS.ACC1NAM ||' '|| BSS.ACC2NAM name, case when flag_off='N' then '' else flag_off end flag_off from GL_ACCDEALS ad, bss bss where bss.acc2=ad.acc2";
    }
}
