package ru.rbt.barsgl.gwt.client.session;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.core.actions.Action;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.shared.enums.SecurityActionCode;

/**
 * Created by er17503 on 10.11.2017.
 */
public class UserSession extends GridForm {
    public final static String FORM_NAME = "Управление сессиями пользователей";
    public UserSession() {
        super(FORM_NAME);
        reconfigure();
    }

    private void reconfigure(){
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addSecureAction(createUserSessionsKillAction(), SecurityActionCode.SessionKill);
        abw.addSecureAction(createCurrentSessionKillAction(), SecurityActionCode.SessionKill);
        abw.addSecureAction(createAllSessionsKillAction(), SecurityActionCode.SessionsKill);
    }

    private GridAction createUserSessionsKillAction() {
        return new GridAction(grid, null, "Закрытие сессий текущего пользователя", new Image(ImageConstants.INSTANCE.male()), 20, true) {

            @Override
            public void execute() {
                DialogManager.confirm("Закрытие сессий текущего пользователя", Utils.Fmt("Подтверждаете закрытие сессий текущего пользователя {0}?", "Person"), "Да", "Нет", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        Window.alert("Killed");
                    }
                }, null);
            }
        };
    }

    private GridAction createCurrentSessionKillAction() {
        return new GridAction(grid, null, "Закрытие текущей сессии", new Image(ImageConstants.INSTANCE.stop()), 5, true) {

            @Override
            public void execute() {
                DialogManager.confirm("Закрытие текущей сессии", Utils.Fmt("Подтверждаете закрытие текущей сессии ID = {0}?", "122345678"), "Да", "Нет", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        Window.alert("Killed");
                    }
                }, null);
            }
        };
    }

    private GridAction createAllSessionsKillAction() {
        return new GridAction(grid, null, "Закрытие всех сессий", new Image(ImageConstants.INSTANCE.stop_all()), 5, true) {

            @Override
            public void execute() {
                DialogManager.confirm("Закрытие всех сессий", "Подтверждаете закрытие всех сессий ?", "Да", "Нет", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        Window.alert("Killed");
                    }
                }, null);
            }
        };
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();

        Column col;
        result.addColumn(col = new Column("ID_PRM", Column.Type.LONG, "ID_PRM", 80, false, false));
        col.setEditable(false);
        col.setFilterable(false);

        result.addColumn(new Column("ID_USER", Column.Type.INTEGER, "ID", 70, true, false));
        result.addColumn(new Column("USER_NAME", Column.Type.STRING, "Логин", 170));
        result.addColumn(new Column("SURNAME", Column.Type.STRING, "Фамилия", 200));
        result.addColumn(new Column("FIRSTNAME", Column.Type.STRING, "Имя", 160));
        result.addColumn(new Column("PATRONYMIC", Column.Type.STRING, "Отчество", 180));

        result.addColumn(col = new Column("PRM_CODE", Column.Type.STRING, "Параметр", 90, false, false));
        col.setEditable(false);
        col.setFilterable(false);
        result.addColumn(new Column("PRMVAL", Column.Type.STRING, "Кол-во дней назад", 105));

        result.addColumn(col = new Column("DT_BEGIN", Column.Type.DATE, "Дата начала действия", 105));
        col.setFormat("dd.MM.yyyy");
        result.addColumn(col = new Column("DT_END", Column.Type.DATE, "Дата окончания действия", 95));
        col.setFormat("dd.MM.yyyy");

        result.addColumn(new Column("FILIAL", Column.Type.STRING, "Филиал", 70));
        result.addColumn(new Column("DEPID", Column.Type.STRING, "Подразделение", 115));

        return result;
    }

    @Override
    protected String prepareSql() {
        return "select id_prm, id_user, user_name, surname, firstname, patronymic, prm_code, " +
                "prmval, dt_begin, dt_end, filial, depid from V_GL_BACKVALUE " +
                "where end_dt is null and locked='0'";
    }
}
