package ru.rbt.barsgl.shared.access;

import com.google.gwt.user.client.rpc.IsSerializable;
import ru.rbt.barsgl.shared.dict.FormAction;

import java.io.Serializable;

/**
 * Created by akichigi on 12.04.16.
 */
public class RoleWrapper implements Serializable, IsSerializable {
    private Integer id;
    private String name;
    private FormAction action;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public FormAction getAction() {
        return action;
    }

    public void setAction(FormAction action) {
        this.action = action;
    }
}
