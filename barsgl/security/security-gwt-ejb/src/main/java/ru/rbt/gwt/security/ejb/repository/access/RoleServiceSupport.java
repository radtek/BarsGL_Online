package ru.rbt.gwt.security.ejb.repository.access;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.security.entity.access.Role;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.security.RequestContextBean;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.access.*;
import ru.rbt.shared.ctx.UserRequestHolder;
import ru.rbt.barsgl.shared.dict.FormAction;
import ru.rbt.shared.enums.PrmValueEnum;
import ru.rbt.shared.enums.RoleSys;

import javax.ejb.EJB;
import javax.inject.Inject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import ru.rbt.security.ejb.repository.access.RoleRepository;

import static ru.rbt.audit.entity.AuditRecord.LogCode.Role;


import static ru.rbt.shared.ExceptionUtils.getErrorMessage;
import static java.lang.String.format;
import ru.rbt.shared.security.RequestContext;

/**
 * Created by akichigi on 12.04.16.
 */
public class RoleServiceSupport {

    private static Logger log = Logger.getLogger(RoleServiceSupport.class);

    @EJB
    private AuditController auditController;

    @Inject
    private RoleRepository roleRepository;

    @Inject
    private RequestContext contextBean;
    //private RequestContextBean contextBean;

    @Inject
    private OperdayController operdayController;

    public RpcRes_Base<RoleWrapper> doRole(RoleWrapper wrapper){
        Role role;

        try{
            if (wrapper.getAction().equals(FormAction.CREATE)){
                //create
                role = roleRepository.selectFirst(Role.class, "from Role r where r.name = ?1", wrapper.getName());
                if (role != null){
                    return new RpcRes_Base<RoleWrapper>(wrapper, true, format("Роль '%s' уже существует!", wrapper.getName()));
                }

                role = new Role();
                role.setId(roleRepository.nextIntegerId("SEQ_GL_AUT"));
                role.setName(wrapper.getName());
                role.setSys(RoleSys.N);

                roleRepository.save(role);

            }else{
                //update
                role = roleRepository.selectFirst(Role.class, "from Role r where r.id = ?1", wrapper.getId());
                if (role == null){
                    return new RpcRes_Base<RoleWrapper>(wrapper, true, format("Роль id='%s' не существует!", wrapper.getId()));
                }

                if (role.getSys() == RoleSys.Y){
                    return new RpcRes_Base<RoleWrapper>(wrapper, true, format("Роль '%s' - системная. Правка запрещена!", wrapper.getName()));
                }

                if (!role.getName().equals(wrapper.getName())){
                    if (roleRepository.selectFirst(Role.class, "from Role r where r.name = ?1", wrapper.getName()) != null){
                        return new RpcRes_Base<RoleWrapper>(wrapper, true, format("Роль '%s' уже существует!", wrapper.getName()));
                    }
                    role.setName(wrapper.getName());
                    roleRepository.update(role);
                }
            }

            String str = wrapper.getAction().equals(FormAction.CREATE) ? "Создана": "Обновлена";
            auditController.info(Role, format(str + " роль: '%s'", wrapper.getName()));

            return new RpcRes_Base<>(wrapper, false, format(str + " роль: '%s'", wrapper.getName()));

        } catch (Exception e){
            String str = wrapper.getAction().equals(FormAction.CREATE) ? "создании": "обновлении";
            String errMessage = getErrorMessage(e);
            auditController.error(Role, format("Ошибка при " + str + " роли: '%s'", wrapper.getName()), null, e);
            return new RpcRes_Base<RoleWrapper>(wrapper, true, errMessage);
        }
    }

    public RpcRes_Base<ArrayList<ActionWrapper>> getActionsByGroupId(int groupId){
       try{
           return new RpcRes_Base<ArrayList<ActionWrapper>>(groupId == -1 ? allActions() : actionsByGroupId(groupId), false, "");
       }catch (Exception e){
           String errMessage = getErrorMessage(e);
           auditController.error(Role, format("Ошибка получения функций для группы: '%s'", groupId), null, e);
           return new RpcRes_Base<ArrayList<ActionWrapper>>(null, true, errMessage);
       }
    }

    public RpcRes_Base<RoleActionWrapper> getRoleActions(RoleWrapper wrapper){

        RoleActionWrapper actionWrapper = new RoleActionWrapper();
        try{
            Role role = roleRepository.selectFirst(Role.class, "from Role r where r.id = ?1", wrapper.getId());
            if (role != null && role.getSys() == RoleSys.Y){
                return new RpcRes_Base<RoleActionWrapper>(actionWrapper, true, format("Роль '%s' - системная. Правка запрещена!", role.getName()));
            }

            actionWrapper.setId(wrapper.getId());
            actionWrapper.setName(wrapper.getName());
            actionWrapper.setAllActions(allActions());
            actionWrapper.setGroups(actionGroups());
            actionWrapper.setRoleActions(actionsByRoleId(wrapper.getId()));
            actionWrapper.setAction(FormAction.UPDATE);

            return new RpcRes_Base<RoleActionWrapper>(actionWrapper, false, "");
        }catch (Exception e){
            String errMessage = getErrorMessage(e);
            auditController.error(Role, format("Ошибка получения функций роли: '%s'", wrapper.getName()), null, e);
            return new RpcRes_Base<RoleActionWrapper>(actionWrapper, true, errMessage);
        }
    }

    public RpcRes_Base<RoleActionWrapper> setRoleActions(RoleActionWrapper wrapper){
        try{
            String sql_delete = "delete from GL_AU_ACTRL where id_role=?";
            String sql_insert = "insert into GL_AU_ACTRL(id_role, id_act, usr_aut) values(?, ?, ?)";
            roleRepository.executeNativeUpdate(sql_delete, wrapper.getId());

            String usr = getUserAut();
            for (ActionWrapper actionWrapper : wrapper.getRoleActions()){
                roleRepository.executeNativeUpdate(sql_insert, wrapper.getId(), actionWrapper.getId(), usr);
                auditController.info(Role, format("Роли '%s' назначена функция '%s'",
                        wrapper.getName(), actionWrapper.getCode()));
            }

            return new RpcRes_Base<RoleActionWrapper>(wrapper, false, "");
        }catch (Exception e){
            String errMessage = getErrorMessage(e);
            auditController.error(Role, format("Ошибка назаначения функций роли: '%s'", wrapper.getName()), null, e);
            return new RpcRes_Base<RoleActionWrapper>(null, true, errMessage);
        }
    }

    public RpcRes_Base<AccessRightsWrapper> getAccessRights(int userId){
        AccessRightsWrapper accessWrapper = new AccessRightsWrapper();
        try{
            accessWrapper.setUserId(userId);

            accessWrapper.setRoles(roles(userId, true));
            accessWrapper.setGranted_roles(roles(userId, false));

            accessWrapper.setProducts(products(userId, true));
            accessWrapper.setGranted_products(products(userId, false));

            accessWrapper.setBranches(branches(userId, true));
            accessWrapper.setGranted_branches(branches(userId, false));

            return new RpcRes_Base<AccessRightsWrapper>(accessWrapper, false, "");
        }catch (Exception e){
            String errMessage = getErrorMessage(e);
            auditController.error(Role, "Ошибка получения функций прав доступа!", null, e);
            return new RpcRes_Base<AccessRightsWrapper>(accessWrapper, true, errMessage);
        }
    }


  //TODO
    public RpcRes_Base<AccessRightsWrapper> setAccessRights(AccessRightsWrapper wrapper){
        try{
            //Roles
            String sql_deleteUserRole = "delete from GL_AU_USRRL where id_user=?";
            String sql_insertUserRole = "insert into GL_AU_USRRL(id_user, id_role, usr_aut) values(?, ?, ?)";
            roleRepository.executeNativeUpdate(sql_deleteUserRole, wrapper.getUserId());
            auditController.info(Role, format("Пользоветелю id=%d удалены все роли",
                    wrapper.getUserId()));

            String usr = getUserAut();
            for (RoleWrapper roleWrapper : wrapper.getGranted_roles()){
                roleRepository.executeNativeUpdate(sql_insertUserRole, wrapper.getUserId(), roleWrapper.getId(), usr);
                auditController.info(Role, format("Пользоветелю id=%d назначена роль id=%d",
                        wrapper.getUserId(), roleWrapper.getId()));
            }

            //History
            String sql_history = format("insert into GL_AU_PRMVALH(id_prm, id_user, prm_code, prmval, dt_begin, dt_end, usr_aut, dt_aut, dt_sys, usr_sys, chng_type)\n" +
                    "select id_prm, id_user, prm_code, prmval, dt_begin, dt_end, usr_aut, dt_aut, current_timestamp, '%s', 'D'\n" +
                    "from GL_AU_PRMVAL where id_user=? and prm_code in ('Source', 'HeadBranch')", usr);

            roleRepository.executeNativeUpdate(sql_history, wrapper.getUserId());

            //Delete products & branches
            String sql_deletePrmVal = "delete from GL_AU_PRMVAL where id_user=? and prm_code in ('Source', 'HeadBranch')";
            roleRepository.executeNativeUpdate(sql_deletePrmVal, wrapper.getUserId());
            auditController.info(Role, format("Пользоветелю id=%d удалены все значения параметров",
                    wrapper.getUserId()));


            String sql_insert_PrmVal = "insert into GL_AU_PRMVAL(id_user, prm_code, prmval, dt_begin, dt_end, usr_aut)\n"+
                                         "values(?, ?, ?, ?, null, ?)";

            Date currentDate = operdayController.getOperday().getCurrentDate();

            //Products
            for(UserProductsWrapper productsWrapper : wrapper.getGranted_products()){
                roleRepository.executeNativeUpdate(sql_insert_PrmVal, wrapper.getUserId(), PrmValueEnum.Source.name(), productsWrapper.getCode(),
                        currentDate, usr);
                auditController.info(Role, format("Пользоветелю id=%d задан параметр '%s' со значением '%s'",
                        wrapper.getUserId(), PrmValueEnum.Source.name(), productsWrapper.getCode()));
            }

            //Branches
            for(UserBranchesWrapper branchesWrapper : wrapper.getGranted_branches()){
                roleRepository.executeNativeUpdate(sql_insert_PrmVal, wrapper.getUserId(), PrmValueEnum.HeadBranch.name(), branchesWrapper.getCodeNum(),
                        currentDate, usr);
                auditController.info(Role, format("Пользоветелю id=%d задан параметр '%s' со значением '%s'",
                        wrapper.getUserId(), PrmValueEnum.HeadBranch.name(), branchesWrapper.getCodeNum()));
            }

            return new RpcRes_Base<AccessRightsWrapper>(wrapper, false, "");
        }catch (Exception e){
            String errMessage = getErrorMessage(e);
            auditController.error(Role, "Ошибка назаначения прав доступа!", null, e);
            return new RpcRes_Base<AccessRightsWrapper>(null, true, errMessage);
        }
    }

    private ArrayList<UserProductsWrapper> products(Integer userId, boolean all){
        try{
             ArrayList<UserProductsWrapper> list = new ArrayList<>();
             List<DataRecord>  res;
             String sql;

            if (all){
                 sql = "select  id_src,  lgnm from (\n" +
                       "select '*'  as  id_src, 'Все'  as  lgnm from DUAL\n" +
                       "union\n" +
                       "select id_src, lgnm from GL_SRCPST) t\n" +
                       "where t. id_src not in (select id_src from V_GL_AU_USRPR where id_user=?)";
             }else {
                 sql = "select id_src, lgnm from V_GL_AU_USRPR where id_user=?";
             }

            res = roleRepository.select(sql, userId);

            if (res.isEmpty()) return list;

            for (DataRecord rec : res ){
                UserProductsWrapper  wrapper = new UserProductsWrapper();
                wrapper.setCode(rec.getString("id_src"));
                wrapper.setDescr(rec.getString("lgnm"));

                list.add(wrapper);
            }

            return list;
        }catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private ArrayList<RoleWrapper> roles(Integer userId, boolean all){
        try{
            ArrayList<RoleWrapper> list = new ArrayList<>();
            List<DataRecord>  res;
            String sql;

            if (all){
                sql = "select id_role, role_name from GL_AU_ROLE " +
                      "where id_role not in (select id_role from V_GL_AU_USRRL where id_user=?)";
            }else {
                sql = "select id_role, role_name from V_GL_AU_USRRL where id_user=?";
            }

            res = roleRepository.select(sql, userId);
            if (res.isEmpty()) return list;
            for (DataRecord rec : res ){
                RoleWrapper wrapper= new RoleWrapper();
                wrapper.setId(rec.getInteger("id_role"));
                wrapper.setName(rec.getString("role_name"));
                wrapper.setAction(FormAction.OTHER);

                list.add(wrapper);
            }

            return list;
        }catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private ArrayList<UserBranchesWrapper> branches(Integer userId, boolean all){
        try{
            ArrayList<UserBranchesWrapper> list = new ArrayList<>();
            List<DataRecord>  res;
            String sql;

            if (all){
                sql = "select CCPCD, CCPNR, CCBBR from\n" +
                      "(select '*'  as  CCPCD, 'Все'  as  CCPNR, 'Все'  as CCBBR  from DUAL\n" +
                      "union\n" +
                      "select CCPCD, CCPNR, CCBBR from IMBCBCMP) t\n" +
                      "where t.CCPCD not in (select CCPCD from  V_GL_AU_USRBR where id_user=?)";
            }else {
                sql = "select CCPCD, CCPNR, CCBBR from  V_GL_AU_USRBR where id_user=?";
            }

            res = roleRepository.select(sql, userId);

            if (res.isEmpty()) return list;

            for (DataRecord rec : res ){
                UserBranchesWrapper wrapper = new UserBranchesWrapper();
                wrapper.setCodeStr(rec.getString("CCPCD"));
                wrapper.setName(rec.getString("CCPNR"));
                wrapper.setCodeNum(rec.getString("CCBBR"));

                list.add(wrapper);
            }

            return list;
        }catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private ArrayList<ActionWrapper> actionsByRoleId(Integer roleId){
        try {
            String sql = "select  r.id_act, a.act_code, a.actdescr " +
                         "from GL_AU_ACTRL r join V_GL_AU_ACT a on a.id_act=r.id_act and r.id_role =?";
            ArrayList<ActionWrapper> list = new ArrayList<ActionWrapper>();
            if (roleId == null) return list;

            List<DataRecord>  res = roleRepository.select(sql, roleId);
            if (res.isEmpty()) return list;

            for (DataRecord rec : res ){
                ActionWrapper wrapper = new ActionWrapper();
                wrapper.setId(rec.getInteger("id_act"));
                wrapper.setCode(rec.getString("act_code"));
                wrapper.setDescr(rec.getString("actdescr"));

                list.add(wrapper);
            }
            return list;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private ArrayList<ActionWrapper> allActions(){
        try {
            String sql = "select id_act, act_code, actdescr  from GL_AU_ACT";
            ArrayList<ActionWrapper> list = new ArrayList<ActionWrapper>();
            List<DataRecord>  res = roleRepository.select(sql);
            if (res.isEmpty()) return list;

            for (DataRecord rec : res ){
                ActionWrapper wrapper = new ActionWrapper();
                wrapper.setId(rec.getInteger("id_act"));
                wrapper.setCode(rec.getString("act_code"));
                wrapper.setDescr(rec.getString("actdescr"));

                list.add(wrapper);
            }
            return list;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private ArrayList<ActionWrapper> actionsByGroupId(int groupId){
        try {
            String sql = "select id_act, act_code, actdescr from GL_AU_ACT where id_group =?";
            ArrayList<ActionWrapper> list = new ArrayList<ActionWrapper>();
            List<DataRecord>  res = roleRepository.select(sql, groupId);
            if (res.isEmpty()) return list;

            for (DataRecord rec : res ){
                ActionWrapper wrapper = new ActionWrapper();
                wrapper.setId(rec.getInteger("id_act"));
                wrapper.setCode(rec.getString("act_code"));
                wrapper.setDescr(rec.getString("actdescr"));

                list.add(wrapper);
            }
            return list;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private ArrayList<ActionGroupWrapper> actionGroups(){
        try {
            String sql = "select id_group, group_name from GL_AU_GRACT";
            List<DataRecord>  res = roleRepository.select(sql);
            ArrayList<ActionGroupWrapper> list = new ArrayList<ActionGroupWrapper>();
            if (res.isEmpty()) return list;

            ActionGroupWrapper wrapper = new ActionGroupWrapper();
            wrapper.setId(-1);
            wrapper.setName("");
            list.add(wrapper);

            for (DataRecord rec : res ){
                wrapper = new ActionGroupWrapper();
                wrapper.setId(rec.getInteger("id_group"));
                wrapper.setName(rec.getString("group_name"));

                list.add(wrapper);
            }
            return list;
        } catch (SQLException e) {
            throw new DefaultApplicationException(e.getMessage(), e);
        }
    }

    private String getUserAut(){
        UserRequestHolder requestHolder = contextBean.getRequest().orElse(UserRequestHolder.empty());
        return requestHolder.getUser();
    }
}
