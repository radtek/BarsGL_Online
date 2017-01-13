package ru.rbt.barsgl.shared.access;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.ArrayList;


/**
 * Created by akichigi on 14.04.16.
 */
public class RoleActionWrapper extends RoleWrapper implements Serializable, IsSerializable {

    private ArrayList<ActionGroupWrapper> groups;
    private ArrayList<ActionWrapper> allActions;
    private ArrayList<ActionWrapper> roleActions;

    public ArrayList<ActionGroupWrapper> getGroups() {
        return groups;
    }

    public void setGroups(ArrayList<ActionGroupWrapper> groups) {
        this.groups = groups;
    }

    public ArrayList<ActionWrapper> getAllActions() {
        return allActions;
    }

    public void setAllActions(ArrayList<ActionWrapper> allActions) {
        this.allActions = allActions;
    }

    public ArrayList<ActionWrapper> getRoleActions() {
        return roleActions;
    }

    public void setRoleActions(ArrayList<ActionWrapper> roleActions) {
        this.roleActions = roleActions;
    }
}
