package ru.rbt.barsgl.gwt.server.rpc.access;

import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.access.*;
import ru.rbt.shared.enums.PrmValueEnum;

import java.util.ArrayList;

/**
 * Created by akichigi on 06.04.16.
 */
public interface AccessServiceAsync {
    void setBackValue(PrmValueWrapper wrapper, AsyncCallback<RpcRes_Base<PrmValueWrapper>> callback);
    void getBackValue(int userId, PrmValueEnum prmCode, AsyncCallback<RpcRes_Base<PrmValueWrapper>> callback);
    void doRole(RoleWrapper wrapper, AsyncCallback<RpcRes_Base<RoleWrapper>> callback);
    void getRoleActions(RoleWrapper wrapper, AsyncCallback<RpcRes_Base<RoleActionWrapper>> callback);
    void setRoleActions(RoleActionWrapper wrapper, AsyncCallback<RpcRes_Base<RoleActionWrapper>> callback);
    void getActionsByGroupId(int groupId, AsyncCallback<RpcRes_Base<ArrayList<ActionWrapper>>> callback);
    void getAccessRights(int userId, AsyncCallback<RpcRes_Base<AccessRightsWrapper>> callback);
    void setAccessRights(AccessRightsWrapper wrapper, AsyncCallback<RpcRes_Base<AccessRightsWrapper>> callback);
    void killAllSession(AsyncCallback<RpcRes_Base<Boolean>> callback);
    void killSessionById(String id, AsyncCallback<RpcRes_Base<Boolean>> callback);
    void killSessionByName(String name, AsyncCallback<RpcRes_Base<Boolean>> callback);
}
