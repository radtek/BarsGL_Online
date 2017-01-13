package ru.rbt.barsgl.gwt.client.security;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.LinkedBoxes;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.access.AccessRightsWrapper;
import ru.rbt.barsgl.shared.access.RoleWrapper;
import ru.rbt.barsgl.shared.access.UserBranchesWrapper;
import ru.rbt.barsgl.shared.access.UserProductsWrapper;

import java.io.Serializable;
import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.showInfo;

public class AccessRightsDlg extends DlgFrame implements IAfterShowEvent {
	private TxtBox tbLogin;
	private TxtBox tbUser;
	private LinkedBoxes branches;
	private LinkedBoxes products;
	private LinkedBoxes roles;
	private AccessRightsWrapper wrapper;

	@Override
    public Widget createContent() {
		Grid grid1 = new Grid(1, 3);

		grid1.setWidget(0, 0, new Label("Права пользователя"));

		tbLogin = new TxtBox();
		tbLogin.setWidth("100px");
		tbLogin.setReadOnly(true);
		tbLogin.setTabStopOff();
		grid1.setWidget(0, 1, tbLogin);

		tbUser = new TxtBox();
		tbUser.setWidth("414px");
		tbUser.setReadOnly(true);
		tbUser.setTabStopOff();
		grid1.setWidget(0, 2, tbUser);


		branches = new LinkedBoxes("Все доступные филиалы:", "Доступные пользователю:");
		branches.setBoxesHeight("100px");
		branches.setBoxInWidth("300px");
		branches.setBoxOutWidth("300px");

		products = new LinkedBoxes("Все доступные продукты:", "Доступные пользователю:");
		products.setBoxesHeight("100px");
		products.setBoxInWidth("300px");
		products.setBoxOutWidth("300px");

		roles = new LinkedBoxes("Все доступные роли:", "Доступные пользователю:");
		roles.setBoxesHeight("180px");
		roles.setBoxInWidth("300px");
		roles.setBoxOutWidth("300px");

		VerticalPanel panel = new VerticalPanel();
		panel.add(grid1);
        panel.add(branches);
		panel.add(products);
		panel.add(roles);

		setAfterShowEvent(this);

        return panel;
    }

	@Override
	public void afterShow() {
       /* if (branches.getBoxOutCount() == 0){
			branches.moveInToOut(wrapper.getFilial());
		}*/

		branches.setSelectedBoxInIndex(0);
		branches.setSelectedBoxOutIndex(0);

		products.setSelectedBoxInIndex(0);
		products.setSelectedBoxOutIndex(0);

		roles.setSelectedBoxInIndex(0);
		roles.setSelectedBoxOutIndex(0);
	}

	private void clear(){
		tbUser.clear();
		tbLogin.clear();
		branches.clearBoxIn();
		branches.clearBoxOut();
		products.clearBoxIn();
		products.clearBoxOut();
		roles.clearBoxIn();
		roles.clearBoxOut();
	}

	@Override
	protected void fillContent() {
		clear();

		wrapper = (AccessRightsWrapper) params;

		tbLogin.setValue(wrapper.getLogin());
		tbUser.setValue(wrapper.getSurname() + " " + wrapper.getFirstName() + " " +
				(wrapper.getPatronymic() == null ? "" : wrapper.getPatronymic()));

		for(UserBranchesWrapper w : wrapper.getBranches()){
			branches.addBoxInItem(w.getCodeStr(), formatBranchText(w.getCodeStr(), w.getName()));
		}

		for(UserBranchesWrapper w : wrapper.getGranted_branches()){
			branches.addBoxOutItem(w.getCodeStr(), formatBranchText(w.getCodeStr(), w.getName()));
		}

		for(UserProductsWrapper w : wrapper.getProducts()){
			products.addBoxInItem(w.getCode(), w.getCode());
		}

		for(UserProductsWrapper w : wrapper.getGranted_products()){
			products.addBoxOutItem(w.getCode(), w.getCode());
		}

		for(RoleWrapper w : wrapper.getRoles()){
			roles.addBoxInItem(w.getId(), w.getName());
		}

		for(RoleWrapper w : wrapper.getGranted_roles()){
			roles.addBoxOutItem(w.getId(), w.getName());
		}

		branches.updateButtonState();
		products.updateButtonState();
		roles.updateButtonState();
	}

	private String formatBranchText(String code, String name){
		int idx =  name.indexOf("Филиал");

	    return code +  (code.equals("*") ? "     " :	"   ") +
				(idx == -1 ? name :	name.substring(0, name.indexOf("Филиал") + 6));
	}

	private void setWrapperFields(){
		ArrayList<UserBranchesWrapper> branchesList = new ArrayList<UserBranchesWrapper>();
		UserBranchesWrapper branchesWrapper;
		for(Serializable v : branches.getBoxOutValues()){
			branchesWrapper = new UserBranchesWrapper();
			branchesWrapper.setCodeNum((String) v);
			branchesList.add(branchesWrapper);
		}
		wrapper.setGranted_branches(branchesList);

		ArrayList<UserProductsWrapper> productsList = new ArrayList<UserProductsWrapper>();
		UserProductsWrapper productsWrapper;
		for(Serializable v : products.getBoxOutValues()){
			productsWrapper = new UserProductsWrapper();
			productsWrapper.setCode((String) v);
			productsList.add(productsWrapper);
		}
		wrapper.setGranted_products(productsList);

		ArrayList<RoleWrapper> rolesList = new ArrayList<RoleWrapper>();
		RoleWrapper rolesWrapper;
		for(Serializable v : roles.getBoxOutValues()){
			rolesWrapper = new RoleWrapper();
			rolesWrapper.setId((Integer) v);
			rolesList.add(rolesWrapper);
		}
		wrapper.setGranted_roles(rolesList);
	}


	private void checkUp(){
		//Filial
		ArrayList<Serializable> list = branches.getBoxOutValues();
		if (list.size() == 0){
			showInfo("Ошибка", "Не выбрано значение филиала доступного пользователю");
			throw new IllegalArgumentException("column");
		}

		if (!list.contains("*") && !list.contains(wrapper.getFilial()) || list.size() > 1){
			showInfo("Ошибка", "Значение филиала доступного пользователю должно включать '*' или значение своего филиала: " + wrapper.getFilial());
			throw new IllegalArgumentException("column");
		}

		//Product
		list = products.getBoxOutValues();
		if (list.size() == 0){
			showInfo("Ошибка", "Не выбрано значение продукта доступного пользователю");
			throw new IllegalArgumentException("column");
		}

		if (list.contains("*") && list.size() > 1) {
			showInfo("Ошибка", "Значение продукта доступного пользователю '*' не может использоваться в сочетании с другими продуктами");
			throw new IllegalArgumentException("column");
		}
	}

	@Override
	protected boolean onClickOK() throws Exception {
		try {
			checkUp();
			setWrapperFields();
			params = wrapper;
		} catch (IllegalArgumentException e) {
			if (e.getMessage() != null && e.getMessage().equals("column")) {
				return false;
			} else {
				throw e;
			}
		}
		return true;
	}
}
