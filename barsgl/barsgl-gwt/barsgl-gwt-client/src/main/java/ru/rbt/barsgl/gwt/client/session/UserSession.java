package ru.rbt.barsgl.gwt.client.session;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.core.actions.Action;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.DlgMode;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.Utils;
import ru.rbt.barsgl.shared.enums.AccessMode;
import ru.rbt.grid.gwt.client.gridForm.GridForm;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.shared.enums.SecurityActionCode;

import java.io.Serializable;
import java.util.HashMap;

import static ru.rbt.barsgl.gwt.client.comp.GLComponents.getYesNoList;
import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

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
                final Row row = grid.getCurrentRow();
                if (row == null) return;
                final String login = (String) row.getField(2).getValue();
                final String user = (String) row.getField(6).getValue();
                DialogManager.confirm("Закрытие сессий текущего пользователя",
                        Utils.Fmt("Подтверждаете закрытие сессий текущего пользователя {0}?", login  + " (" + user+ ")"), "Да", "Нет", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                        BarsGLEntryPoint.accessService.killSessionByName(login, new AuthCheckAsyncCallback<RpcRes_Base<Boolean>>() {
                            @Override
                            public void onSuccess(RpcRes_Base<Boolean> res) {
                                WaitingManager.hide();

                                if (res.isError()) {
                                    DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + res.getMessage());
                                } else {
                                    refreshAction.execute();
                                }
                            }
                        });
                    }
                }, null);
            }
        };
    }

    private GridAction createCurrentSessionKillAction() {
        return new GridAction(grid, null, "Закрытие текущей сессии", new Image(ImageConstants.INSTANCE.stop()), 5, true) {

            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;
                final String id = (String) row.getField(1).getValue();

                DialogManager.confirm("Закрытие текущей сессии", Utils.Fmt("Подтверждаете закрытие текущей сессии {0}?", id), "Да", "Нет", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                        BarsGLEntryPoint.accessService.killSessionById(id, new AuthCheckAsyncCallback<RpcRes_Base<Boolean>>() {
                            @Override
                            public void onSuccess(RpcRes_Base<Boolean> res) {
                                WaitingManager.hide();

                                if (res.isError()) {
                                    DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + res.getMessage());
                                } else {
                                    refreshAction.execute();
                                }
                            }
                        });
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
                        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                        BarsGLEntryPoint.accessService.killAllSession(new AuthCheckAsyncCallback<RpcRes_Base<Boolean>>() {
                            @Override
                            public void onSuccess(RpcRes_Base<Boolean> res) {
                                WaitingManager.hide();

                                if (res.isError()) {
                                    DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + res.getMessage());
                                } else {
                                    refreshAction.execute();
                                }
                            }
                        });
                    }
                }, null);
            }
        };
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();
        HashMap<Serializable, String> yesNoList = getYesNoList();
        Column col;
        result.addColumn(new Column("ID_ROW", Column.Type.STRING, "ID", 50));
        result.addColumn(new Column("SESSION_ID", Column.Type.STRING, "ID сессии", 300));
        result.addColumn(new Column("USER_NAME", Column.Type.STRING, "Логин", 170));
        result.addColumn(col = new Column("CREATED_DT", Column.Type.DATETIME, "Создание сессии", 105));
        col.setFormat("dd.MM.yyyy hh:mm:ss");
        result.addColumn(col = new Column("LASTACS_DT", Column.Type.DATETIME, "Окончание активности", 105));
        col.setFormat("dd.MM.yyyy hh:mm:ss");
        result.addColumn(new Column("INVALIDATED", Column.Type.STRING, "Удалена", 50));
        col.setList(yesNoList);
        result.addColumn(new Column("SURNAME", Column.Type.STRING, "Фамилия", 200));
        result.addColumn(new Column("FIRSTNAME", Column.Type.STRING, "Имя", 140));
        result.addColumn(new Column("PATRONYMIC", Column.Type.STRING, "Отчество", 190));
        result.addColumn(new Column("FILIAL", Column.Type.STRING, "Филиал", 70));

        return result;
    }

    @Override
    protected String prepareSql() {
        return "select h.*, u.SURNAME, u.FIRSTNAME, u.PATRONYMIC, u.FILIAL " +
               "from GL_HTTPSESS h " +
               "join GL_USER u on h.USER_NAME = u.USER_NAME";
    }
}
