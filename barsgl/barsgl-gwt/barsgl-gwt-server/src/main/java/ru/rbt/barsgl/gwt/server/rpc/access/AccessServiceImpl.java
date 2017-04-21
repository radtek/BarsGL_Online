package ru.rbt.barsgl.gwt.server.rpc.access;

import ru.rbt.gwt.security.ejb.repository.access.AccessServiceSupport;
import ru.rbt.gwt.security.ejb.repository.access.RoleServiceSupport;
import ru.rbt.barsgl.gwt.core.server.rpc.AbstractGwtService;
import ru.rbt.barsgl.gwt.core.server.rpc.RpcResProcessor;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.access.*;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.shared.enums.PrmValueEnum;


import java.util.ArrayList;


/**
 * Created by akichigi on 06.04.16.
 */
public class AccessServiceImpl extends AbstractGwtService implements AccessService{
  
      @Override
    public RpcRes_Base<PrmValueWrapper> setBackValue(final PrmValueWrapper wrapper) throws Exception {
       return new RpcResProcessor<PrmValueWrapper>() {
           @Override
           protected RpcRes_Base<PrmValueWrapper> buildResponse() throws Throwable {
               RpcRes_Base<PrmValueWrapper> res = localInvoker.invoke(AccessServiceSupport.class, "setBackValue", wrapper);
               if (res == null) throw new Throwable("Не удалось сохранить данные доступа в архив!");
               return res;
           }
       }.process();
    }

    @Override
    public RpcRes_Base<PrmValueWrapper> getBackValue(final int userId, final PrmValueEnum prmCode) throws Exception {
        return new RpcResProcessor<PrmValueWrapper>() {
            @Override
            protected RpcRes_Base<PrmValueWrapper> buildResponse() throws Throwable {
                RpcRes_Base<PrmValueWrapper> res = localInvoker.invoke(AccessServiceSupport.class, "getBackValue", userId, prmCode);
                if (res == null) throw new Throwable("Не удалось получить данные доступа в архив!");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<RoleWrapper> doRole(final RoleWrapper wrapper) throws Exception {
        return new RpcResProcessor<RoleWrapper>() {
            @Override
            protected RpcRes_Base<RoleWrapper> buildResponse() throws Throwable {
                RpcRes_Base<RoleWrapper> res = localInvoker.invoke(RoleServiceSupport.class, "doRole", wrapper);
                String str = wrapper.getAction().equals(FormAction.CREATE) ? "создании" : "исправлении";

                if (res == null) throw new Throwable("Ошибка " + str + " роли!");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<RoleActionWrapper> getRoleActions(final RoleWrapper wrapper) throws Exception {
        return new RpcResProcessor<RoleActionWrapper>() {
            @Override
            protected RpcRes_Base<RoleActionWrapper> buildResponse() throws Throwable {
                RpcRes_Base<RoleActionWrapper> res = localInvoker.invoke(RoleServiceSupport.class, "getRoleActions", wrapper);
                if (res == null) throw new Throwable("Не удалось получить функции для роли!");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<RoleActionWrapper> setRoleActions(final RoleActionWrapper wrapper) throws Exception {
        return new RpcResProcessor<RoleActionWrapper>() {
            @Override
            protected RpcRes_Base<RoleActionWrapper> buildResponse() throws Throwable {
                RpcRes_Base<RoleActionWrapper> res = localInvoker.invoke(RoleServiceSupport.class, "setRoleActions", wrapper);
                if (res == null) throw new Throwable("Не удалось назначить функции для роли!");
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<ArrayList<ActionWrapper>> getActionsByGroupId(final int groupId) throws Exception {
        return new RpcResProcessor<ArrayList<ActionWrapper>>() {
            @Override
            protected RpcRes_Base<ArrayList<ActionWrapper>> buildResponse() throws Throwable {
                RpcRes_Base<ArrayList<ActionWrapper>> res = localInvoker.invoke(RoleServiceSupport.class, "getActionsByGroupId", groupId);
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<AccessRightsWrapper> getAccessRights(final int userId) throws Exception {
        return new RpcResProcessor<AccessRightsWrapper>(){

            @Override
            protected RpcRes_Base<AccessRightsWrapper> buildResponse() throws Throwable {
                RpcRes_Base<AccessRightsWrapper> res = localInvoker.invoke(RoleServiceSupport.class, "getAccessRights", userId);
                return res;
            }
        }.process();
    }

    @Override
    public RpcRes_Base<AccessRightsWrapper> setAccessRights(final AccessRightsWrapper wrapper) throws Exception {
        return new RpcResProcessor<AccessRightsWrapper>(){

            @Override
            protected RpcRes_Base<AccessRightsWrapper> buildResponse() throws Throwable {
                RpcRes_Base<AccessRightsWrapper> res = localInvoker.invoke(RoleServiceSupport.class, "setAccessRights", wrapper);
                return res;
            }
        }.process();
    }
}
