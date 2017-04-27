package ru.rbt.barsgl.gwt.server.rpc.access;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.access.*;
import ru.rbt.shared.enums.PrmValueEnum;

import java.util.ArrayList;



/**
 * Created by akichigi on 06.04.16.
 */
@RemoteServiceRelativePath("service/AccessService")
public interface AccessService extends RemoteService {
    RpcRes_Base<PrmValueWrapper> setBackValue(PrmValueWrapper wrapper) throws Exception;
    RpcRes_Base<PrmValueWrapper> getBackValue(int userId, PrmValueEnum prmCode) throws Exception;
    RpcRes_Base<RoleWrapper> doRole(RoleWrapper wrapper) throws Exception;
    RpcRes_Base<RoleActionWrapper> getRoleActions(RoleWrapper wrapper) throws Exception;
    RpcRes_Base<RoleActionWrapper> setRoleActions(RoleActionWrapper wrapper) throws Exception;
    RpcRes_Base<ArrayList<ActionWrapper>> getActionsByGroupId(int groupId) throws Exception;
    RpcRes_Base<AccessRightsWrapper> getAccessRights(int userId) throws Exception;
    RpcRes_Base<AccessRightsWrapper> setAccessRights(AccessRightsWrapper wrapper) throws Exception;
}
