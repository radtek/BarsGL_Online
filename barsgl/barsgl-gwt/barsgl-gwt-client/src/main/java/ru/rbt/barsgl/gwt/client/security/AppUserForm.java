package ru.rbt.barsgl.gwt.client.security;


import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.security.gwt.client.security.SecurityEntryPoint;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.backvalue.BackValueDlg;
import ru.rbt.barsgl.gwt.client.dict.EditableDictionary;
import ru.rbt.barsgl.gwt.core.actions.GridAction;
import ru.rbt.barsgl.gwt.core.actions.SimpleDlgAction;
import ru.rbt.barsgl.gwt.core.datafields.Column;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.datafields.Table;
import ru.rbt.barsgl.gwt.core.dialogs.*;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.gwt.core.widgets.SortItem;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.access.AccessRightsWrapper;
import ru.rbt.barsgl.shared.access.PrmValueWrapper;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.shared.enums.PrmValueEnum;
import ru.rbt.shared.enums.SecurityActionCode;
import ru.rbt.shared.enums.UserExternalType;
import ru.rbt.shared.user.AppUserWrapper;

import java.util.ArrayList;
//import ru.rbt.barsgl.gwt.client.security.AccessRightsDlg;
//import ru.rbt.barsgl.gwt.client.security.AccessRightsDlg;
//import ru.rbt.barsgl.gwt.client.security.AppUserAddDlg;
//import ru.rbt.barsgl.gwt.client.security.AppUserAddDlg;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;



/**
 * Created by ER18837 on 29.09.15.
 */
public class AppUserForm extends EditableDictionary<AppUserWrapper> {

	public final static String FORM_NAME = "Пользователи";
    private Column colDateClose;
	
    public AppUserForm(){
        super(FORM_NAME);
        reconfigure();
    }

    private void reconfigure() {
        abw.addAction(new SimpleDlgAction(grid, DlgMode.BROWSE, 10));
        abw.addSecureAction(editAction(new AppUserAddDlg(AppUserAddDlg.EDIT, FormAction.UPDATE, grid.getTable().getColumns()),
                "Пользователь не сохранен.\n Ошибка: ",
                "Ошибка при изменении пользователя.\n",
                "Пользователь изменен успешно"), SecurityActionCode.UserChng);
        abw.addSecureAction(createAction(new AppUserAddDlg(AppUserAddDlg.CREATE, FormAction.CREATE, grid.getTable().getColumns()),
                "Пользователь не создан.\n Ошибка: ",
                "Ошибка создания пользователя. \n",
                "Пользователь создан успешно."), SecurityActionCode.UserChng);
//        abw.addAction(deleteAction(new AppUserAddDlg(AppUserAddDlg.DELETE, FormAction.DELETE, grid.getTable().getColumns()),
//                "Пользователь не удален.\n Ошибка: ",
//                "Ошибка удаления пользователя: \n",
//                "Пользователь удален успешно: "));
        
        abw.addSecureAction(createAccessRights(), SecurityActionCode.UserCntl);
        abw.addSecureAction(createBackValue(), SecurityActionCode.UserCntlBackValue);
    }

    private GridAction createBackValue(){
        return new GridAction(grid, null, "Доступ в архив", new Image(ImageConstants.INSTANCE.back_value()), 10, true) {
        	BackValueDlg dlg = new BackValueDlg();
        	
        	@Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;
                              
                dlg.setCaption("Доступ в архив");
                dlg.setDlgEvents(this);
                
                getBackValue(row, dlg);
            }
        	
        	@Override
            public void onDlgOkClick(Object prms) throws Exception{
        		WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
        		BarsGLEntryPoint.accessService.setBackValue((PrmValueWrapper) prms, new AuthCheckAsyncCallback<RpcRes_Base<PrmValueWrapper>>() {
					
					@Override
					public void onSuccess(RpcRes_Base<PrmValueWrapper> res) {
						if (res.isError()){
		                    DialogManager.error("Ошибка", "Операция сохранения не удалась.\nОшибка: " + res.getMessage());
		                } else {
		                	
		                	//showInfo(res.getMessage());
		                	dlg.hide();		            		
		                } 
						WaitingManager.hide();						
					}					
				});        		        		
        	}        	
        };
    }
    
    private void getBackValue(final Row row, final BackValueDlg dlg){
    	WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
    	
    	BarsGLEntryPoint.accessService.getBackValue((Integer)row.getField(0).getValue(), PrmValueEnum.BackValue, new AuthCheckAsyncCallback<RpcRes_Base<PrmValueWrapper>>() {
			
			@Override
			public void onSuccess(RpcRes_Base<PrmValueWrapper> res) {
				if (res.isError()){
                    DialogManager.error("Ошибка", "Операция не удалась.\nОшибка: " + res.getMessage());
                } else {
                	PrmValueWrapper wrapper = res.getResult();
                	dlg.show(new Object[]{wrapper, row.getField(1).getValue(), row.getField(2).getValue(),
                            row.getField(3).getValue(), row.getField(4).getValue()});
                } 
				WaitingManager.hide();
            }												
		});
    }
            
    private GridAction createAccessRights(){
        return new GridAction(grid, null, "Права доступа", new Image(ImageConstants.INSTANCE.page_key()), 10, true) {
            AccessRightsDlg dlg = new AccessRightsDlg();
        	@Override
            public void execute() {
                final Row row = grid.getCurrentRow();
                if (row == null) return;

                dlg.setCaption("Права доступа");
                dlg.setDlgEvents(this);

                getAccessRights(row, dlg);
            }

            @Override
            public void onDlgOkClick(Object prms) throws Exception {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                BarsGLEntryPoint.accessService.setAccessRights((AccessRightsWrapper) prms, new AuthCheckAsyncCallback<RpcRes_Base<AccessRightsWrapper>>() {
                    @Override
                    public void onSuccess(RpcRes_Base<AccessRightsWrapper> res) {
                        if (res.isError()) {
                            DialogManager.error("Ошибка", "Операция назначения прав доступа не удалась.\nОшибка: " + res.getMessage());
                        } else {
                            dlg.hide();
                        }

                        WaitingManager.hide();
                    }
                });
            }

            private void getAccessRights(final Row row, final AccessRightsDlg dlg) {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());

                int userId = (Integer) row.getField(0).getValue();

                BarsGLEntryPoint.accessService.getAccessRights(userId, new AuthCheckAsyncCallback<RpcRes_Base<AccessRightsWrapper>>() {
                    @Override
                    public void onSuccess(RpcRes_Base<AccessRightsWrapper> res) {
                        if (res.isError()) {
                            DialogManager.error("Ошибка", "Операция получения прав доступа не удалась.\nОшибка: " + res.getMessage());
                        } else {
                            AccessRightsWrapper wrapper = res.getResult();
                            wrapper.setLogin((String) row.getField(1).getValue());
                            wrapper.setSurname((String) row.getField(2).getValue());
                            wrapper.setFirstName((String) row.getField(3).getValue());
                            wrapper.setPatronymic((String) row.getField(4).getValue());
                            wrapper.setFilial((String) row.getField(5).getValue());
                            dlg.show(wrapper);
                        }
                        WaitingManager.hide();
                    }
                });
            }
        };
    }
                 
    @Override
    protected String prepareSql() {
        return "ru.rbt.barsgl.ejb.security.AuthorizationServiceSupport@getUserSql";
    }
     
    @Override
    public ArrayList<SortItem> getInitialSortCriteria() {
        ArrayList<SortItem> list = new ArrayList<SortItem>();
        list.add(new SortItem("ID_USER", Column.Sort.ASC));
        return list;
    }

    @Override
    protected Table prepareTable() {
        Table result = new Table();

        Column col;

        result.addColumn(new Column("ID_USER", Column.Type.INTEGER, "ID", 80, true, false));
        result.addColumn(new Column("USER_NAME", Column.Type.STRING, "Логин", 100));
        result.addColumn(new Column("SURNAME", Column.Type.STRING, "Фамилия", 200));
        result.addColumn(new Column("FIRSTNAME", Column.Type.STRING, "Имя", 140));
        result.addColumn(new Column("PATRONYMIC", Column.Type.STRING, "Отчество", 190));
        
        result.addColumn(new Column("FILIAL", Column.Type.STRING, "Филиал", 70));
        result.addColumn(new Column("DEPID", Column.Type.STRING, "Подразделение", 115));
        
        result.addColumn(col = new Column("CREATE_DT", Column.Type.DATETIME, "Дата ввода", 130));
        col.setFormat("dd.MM.yyyy HH:mm:ss");
        result.addColumn(colDateClose = new Column("END_DT", Column.Type.DATE, "Дата закрытия", 90));
        colDateClose.setFormat("dd.MM.yyyy");
        result.addColumn(new Column("LOCKED", Column.Type.STRING, "Заблокирован", 110));
        result.addColumn(new Column("SEC_TYPE", Column.Type.STRING, "Тип доступа", 80));
        result.addColumn(col = new Column("USER_PWD", Column.Type.STRING, "Пароль", 100, false, true));
        col.setEditable(false);
        col.setFilterable(false);

        return result;
    }

	@Override
	protected void save(AppUserWrapper wrapper, FormAction action,
			AsyncCallback<RpcRes_Base<AppUserWrapper>> asyncCallbackImpl) throws Exception {
        SecurityEntryPoint.authSrv.createUser(wrapper, action, asyncCallbackImpl);
    }

    @Override
    protected ArrayList<FilterItem> getInitialFilterCriteria(Object[] initialFilterParams) {
        ArrayList<FilterItem> list = new ArrayList<FilterItem>();
        list.add(new FilterItem(colDateClose, FilterCriteria.IS_NULL, null, false));
        return list;
    }

    @Override
    protected Object[] getInitialFilterParams() {
        return null;
    }
}
