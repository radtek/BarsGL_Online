package ru.rbt.barsgl.gwt.client.security;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.security.gwt.client.AuthCheckAsyncCallback;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.core.dialogs.DialogManager;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.dialogs.WaitingManager;
import ru.rbt.barsgl.gwt.core.ui.ImmutableLinkedBoxes;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.gwt.core.ui.ValuesBox;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.access.ActionGroupWrapper;
import ru.rbt.barsgl.shared.access.ActionWrapper;
import ru.rbt.barsgl.shared.access.RoleActionWrapper;

import java.io.Serializable;
import java.util.ArrayList;

import static ru.rbt.barsgl.gwt.core.resources.ClientUtils.TEXT_CONSTANTS;

/**
 * Created by akichigi on 12.04.16.
 */
public class RoleActDlg extends DlgFrame implements IAfterShowEvent {
    private TxtBox tbRoleName;
    private ValuesBox groups;
    private ImmutableLinkedBoxes linkedBoxes;
    private RoleActionWrapper roleActionWrapper;

    @Override
    public Widget createContent(){
        Grid grid1 = new Grid(1, 2);

        grid1.setWidget(0, 0, new Label("Наименование роли"));
        grid1.setWidget(0, 1, tbRoleName = new TxtBox());
        tbRoleName.setReadOnly(true);
        tbRoleName.setWidth("250px");

        Grid grid2 = new Grid(1, 2);
        grid2.getElement().getStyle().setMarginTop(10, Style.Unit.PX);

        grid2.setWidget(0, 0, new Label("Группа"));
        grid2.setWidget(0, 1, groups = new ValuesBox());
        groups.setWidth("250px");

        groups.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent changeEvent) {
                WaitingManager.show(TEXT_CONSTANTS.waitMessage_Load());
                BarsGLEntryPoint.accessService.getActionsByGroupId((Integer)groups.getValue(), new AuthCheckAsyncCallback<RpcRes_Base<ArrayList<ActionWrapper>>>() {
                    @Override
                    public void onSuccess(RpcRes_Base<ArrayList<ActionWrapper>> res) {
                        if (res.isError()) {
                            DialogManager.error("Ошибка", "Операция получения групп функций не удалась.\nОшибка: " + res.getMessage());
                        } else {
                            ArrayList<ActionWrapper> list = res.getResult();
                            linkedBoxes.clearBoxIn();
                            if (list != null){
                                for (ActionWrapper wrapper : list){
                                    linkedBoxes.addBoxInItem(wrapper.getId(), wrapper.getDescr());
                                }
                                linkedBoxes.setSelectedBoxInIndex(0);
                                linkedBoxes.updateButtonState();
                            }
                        }
                        WaitingManager.hide();
                    }
                });
            }
        });

        linkedBoxes = new ImmutableLinkedBoxes("Доступные функции:", "Функции роли:");
        linkedBoxes.setBoxInWidth("320px");
        linkedBoxes.setBoxOutWidth("320px");
        setAfterShowEvent(this);

        VerticalPanel panel = new VerticalPanel();
        panel.add(grid1);
        panel.add(grid2);
        panel.add(linkedBoxes);

        return panel;
    }

    private void clear(){
        tbRoleName.clear();
        linkedBoxes.clearBoxIn();
        linkedBoxes.clearBoxOut();
        groups.clear();
    }

    @Override
    protected void fillContent() {
        clear();

        roleActionWrapper = (RoleActionWrapper) params;

        tbRoleName.setValue(roleActionWrapper.getName());

        if (roleActionWrapper.getGroups() != null){
            for (ActionGroupWrapper wrapper : roleActionWrapper.getGroups()){
                groups.addItem(wrapper.getId(), wrapper.getName());
            }
        }

        if (roleActionWrapper.getAllActions() != null){
            for (ActionWrapper wrapper : roleActionWrapper.getAllActions()){
                linkedBoxes.addBoxInItem(wrapper.getId(), wrapper.getDescr());
            }
        }

        if (roleActionWrapper.getRoleActions() != null){
            for (ActionWrapper wrapper : roleActionWrapper.getRoleActions()){
                linkedBoxes.addBoxOutItem(wrapper.getId(), wrapper.getDescr());
            }
        }

        linkedBoxes.updateButtonState();
    }

    @Override
    public void afterShow() {
        linkedBoxes.setSelectedBoxInIndex(0);
        linkedBoxes.setSelectedBoxOutIndex(0);
    }

    private void setWrapperFields(){
        ArrayList<ActionWrapper> list = new ArrayList<ActionWrapper>();
        ActionWrapper wrapper;
        for(Serializable v : linkedBoxes.getBoxOutValues()){
            wrapper = new ActionWrapper();
            wrapper.setId((Integer) v);
            list.add(wrapper);
        }
        roleActionWrapper.setRoleActions(list);
    }

    @Override
    protected boolean onClickOK() throws Exception {
        setWrapperFields();
        params = roleActionWrapper;
        return true;
    }
}
