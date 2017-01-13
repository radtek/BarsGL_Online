package ru.rbt.barsgl.gwt.client.security;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.check.CheckNotEmptyString;
import ru.rbt.barsgl.gwt.core.datafields.Row;
import ru.rbt.barsgl.gwt.core.dialogs.DlgFrame;
import ru.rbt.barsgl.gwt.core.dialogs.IAfterShowEvent;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.access.RoleWrapper;

import static ru.rbt.barsgl.gwt.core.utils.DialogUtils.check;

/**
 * Created by akichigi on 12.04.16.
 */
public class RoleDlg extends DlgFrame implements IAfterShowEvent {

    private TxtBox tbRoleId;
    private TxtBox tbRoleName;

    @Override
    public Widget createContent(){
        Grid grid = new Grid(2, 2);

        grid.setWidget(0, 0, new Label("Role_ID"));

        grid.setWidget(0, 1, tbRoleId = new TxtBox());
        tbRoleId.setReadOnly(true);
        tbRoleId.setWidth("60px");

        grid.setWidget(1, 0, new Label("Наименование роли"));
        grid.setWidget(1, 1, tbRoleName = new TxtBox());
        tbRoleName.setWidth("250px");
        tbRoleName.setMaxLength(64);

        setAfterShowEvent(this);

        return grid;
    }

    @Override
    public void afterShow() {
        tbRoleName.setFocus(true);
    }

    private void clear(){
        tbRoleId.clear();
        tbRoleName.clear();
    }

    @Override
    protected void fillContent() {
        clear();

        Row row = (Row) params;
        if (row == null) return;
        tbRoleId.setValue(row.getField(0).getValue().toString());
        tbRoleName.setValue((String) row.getField(1).getValue());
    }

    private RoleWrapper setWrapperFields(){
        RoleWrapper wrapper = new RoleWrapper();

        if (tbRoleId.getValue() != null) {
            wrapper.setId(Integer.parseInt(tbRoleId.getValue()));
        }

        wrapper.setName(check(tbRoleName.getValue(),
                "Наименование роли", "поле не заполнено", new CheckNotEmptyString()));

        return wrapper;
    }

    @Override
    protected boolean onClickOK() throws Exception {
        try {
            params = setWrapperFields();
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
