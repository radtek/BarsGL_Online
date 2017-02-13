package ru.rbt.barsgl.gwt.client.security;

import com.google.gwt.user.client.ui.Image;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.gridForm.MDForm;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.*;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.access.RoleActionWrapper;
import ru.rbt.barsgl.shared.access.RoleWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.barsgl.shared.enums.SecurityActionCode;

import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;
//import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

/**
 * Created by akichigi on 12.04.16.
 */
public class RoleForm extends MDForm{
    public final static String FORM_NAME = "Роли";
    private Column id_role;

    public RoleForm() {
        super("Роли и функции", null, null);
        reconfigure();
    }

    private void reconfigure() {
        masterActionBar.addAction(new SimpleDlgAction(masterGrid, DlgMode.BROWSE, 10));
        masterActionBar.addSecureAction(createMasterEditAction(), SecurityActionCode.RoleChng);
        masterActionBar.addSecureAction(createMasterNewAction(), SecurityActionCode.RoleInp);
        masterActionBar.addSecureAction(createDetailEditAction(), SecurityActionCode.RoleChng);

        detailActionBar.addAction(new SimpleDlgAction(detailGrid, DlgMode.BROWSE, 10));
    }

     private GridAction createMasterNewAction(){
         return  new GridAction(masterGrid, null, "Создать роль", new Image(ImageConstants.INSTANCE.new24()), 10) {
             RoleDlg dlg = new RoleDlg();

             @Override
             public void execute() {
                 dlg.setCaption("Создание роли");
                 dlg.setDlgEvents(this);
                 dlg.show(null);
             }

             @Override
             public void onDlgOkClick(Object prms) throws Exception{
                 WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                 RoleWrapper wrapper = (RoleWrapper) prms;
                 wrapper.setAction(FormAction.CREATE);

                 BarsGLEntryPoint.accessService.doRole(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<RoleWrapper>>() {

                     @Override
                     public void onSuccess(RpcRes_Base<RoleWrapper> res) {
                         if (res.isError()) {
                             DialogManager.error("Ошибка", "Операция создания роли не удалась.\nОшибка: " + res.getMessage());
                         } else {
                             dlg.hide();
                             masterRefreshAction.execute();
                             //showInfo(res.getMessage());
                         }

                         WaitingManager.hide();
                     }
                 });
             }
         };
     }

    private GridAction createMasterEditAction(){
        return  new GridAction(masterGrid, null, "Править роль", new Image(ImageConstants.INSTANCE.edit24()), 10, true) {
            RoleDlg dlg = new RoleDlg();

            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                dlg.setCaption("Редактирование роли");
                dlg.setDlgEvents(this);
                dlg.show(grid.getCurrentRow());
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                RoleWrapper wrapper = (RoleWrapper) prms;
                wrapper.setAction(FormAction.UPDATE);

                BarsGLEntryPoint.accessService.doRole(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<RoleWrapper>>() {

                    @Override
                    public void onSuccess(RpcRes_Base<RoleWrapper> res) {
                        if (res.isError()) {
                            DialogManager.error("Ошибка", "Операция исправления роли не удалась.\nОшибка: " + res.getMessage());
                        } else {
                            dlg.hide();
                            masterRefreshAction.execute();
                            //showInfo(res.getMessage());
                        }

                        WaitingManager.hide();
                    }
                });
            }
        };
    }

    private GridAction createDetailEditAction(){
        return  new GridAction(masterGrid, null, "Назначить функции", new Image(ImageConstants.INSTANCE.function()), 10, true) {
            RoleActDlg dlg = new RoleActDlg();

            @Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                dlg.setCaption("Назначение функций");
                dlg.setDlgEvents(this);

                getRoleActions(row, dlg);
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                BarsGLEntryPoint.accessService.setRoleActions((RoleActionWrapper) prms, new AuthCheckAsyncCallback<RpcRes_Base<RoleActionWrapper>>() {

                    @Override
                    public void onSuccess(RpcRes_Base<RoleActionWrapper> res) {
                        if (res.isError()) {
                            DialogManager.error("Ошибка", "Операция назначения функций роли не удалась.\nОшибка: " + res.getMessage());
                        } else {
                            dlg.hide();
                            detailRefreshAction.execute();
                            //showInfo(res.getMessage());
                        }

                        WaitingManager.hide();
                    }
                });
            }
        };
    }

    private void getRoleActions(final Row row, final RoleActDlg dlg){
        WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

        RoleWrapper wrapper = new RoleWrapper();
        wrapper.setId((Integer) row.getField(0).getValue());
        wrapper.setName((String) row.getField(1).getValue());
        wrapper.setAction(FormAction.OTHER);

        BarsGLEntryPoint.accessService.getRoleActions(wrapper, new AuthCheckAsyncCallback<RpcRes_Base<RoleActionWrapper>>() {

            @Override
            public void onSuccess(RpcRes_Base<RoleActionWrapper> res) {
                if (res.isError()) {
                    DialogManager.error("Ошибка", "Операция получения функций не удалась.\nОшибка: " + res.getMessage());
                } else {
                    RoleActionWrapper wrapper = res.getResult();
                    dlg.show(wrapper);
                }
                WaitingManager.hide();
            }
        });
    }

    @Override
    protected Table prepareMasterTable() {
        Table result = new Table();

        result.addColumn(new Column("ID_ROLE", Column.Type.INTEGER, "ID_ROLE", 70));
        result.addColumn(new Column("ROLE_NAME", Column.Type.STRING, "Наименование роли", 200));

        return result;
    }

    @Override
    protected String prepareMasterSql() {
        return "select id_role, role_name from GL_AU_ROLE";
    }

    @Override
    public ArrayList<SortItem> getInitialMasterSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("ID_ROLE", Column.Sort.ASC));
        return list;
    }

    @Override
    protected Table prepareDetailTable() {
        Table result = new Table();

        result.addColumn(id_role = new Column("ID_ROLE", Column.Type.INTEGER, "ID_ROLE", 70, false, false));
        result.addColumn(new Column("ID_ACT", Column.Type.INTEGER, "ID_ACT", 70));
        result.addColumn(new Column("ACT_CODE", Column.Type.STRING, "Код функции", 100));
        result.addColumn(new Column("ACTDESCR", Column.Type.STRING, "Описание функции", 250));
        result.addColumn(new Column("GROUP_NAME", Column.Type.STRING, "Группа функции", 100));

        return result;
    }

    @Override
    protected String prepareDetailSql() {
    	
        return  "select id_role, id_act, act_code, actdescr, group_name from " + 
        		"(select r.id_role, r.id_act, a.act_code, a.actdescr, a.group_name " +
                "from GL_AU_ACTRL r join V_GL_AU_ACT a on a.id_act=r.id_act) t";        		        	        	
    }

    @Override
    public ArrayList<SortItem> getInitialDetailSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("ID_ACT", Column.Sort.ASC));
        return list;
    }


    @Override
    public ArrayList<FilterItem> createLinkFilterCriteria(Row row) {
        ArrayList<FilterItem> list = new ArrayList<FilterItem>();
        list.add(new FilterItem(id_role, FilterCriteria.EQ, row == null ? -1 : row.getField(0).getValue()));

        return list;
    }
}
