package ru.rbt.gwt.security.ejb;


import org.apache.log4j.Logger;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.security.entity.AppUser;
import ru.rbt.shared.LoginResult;
import ru.rbt.shared.user.AppUserWrapper;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.SQLException;
import ru.rbt.security.AuthorizationServiceSupport;
import ru.rbt.shared.enums.UserExternalType;

import static java.lang.String.format;
import static ru.rbt.barsgl.shared.enums.YesNoType.No;
import static ru.rbt.barsgl.shared.enums.YesNoType.Yes;
import static ru.rbt.shared.ExceptionUtils.getErrorMessage;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless
@LocalBean
public class AuthorizationServiceGwtSupport {

    private static Logger log = Logger.getLogger(AuthorizationServiceGwtSupport.class);

    @EJB
    private AuthorizationServiceSupport authorizationSupport;
    
    public String getUserSql(){
        return  "select /*+ first_rows(30) */ * from ( " +
                "select ID_USER, USER_NAME, SURNAME, FIRSTNAME, PATRONYMIC, FILIAL, DEPID, CREATE_DT, END_DT, " +
                "case when LOCKED = '0' then trim('" + No.getLabel() + "') " +
                "else trim('" + Yes.getLabel() + "') " +
                "end LOCKED, " +
                "case when SEC_TYPE = '0' then trim('" + UserExternalType.L.name() + "') " +
                "else trim('" + UserExternalType.E.name() + "') end SEC_TYPE, USER_PWD " +
                "from GL_USER) v ";
    }    
        
    
    public LoginResult login(String username, String password) throws Exception {
      return authorizationSupport.login(username, password);
    }

    public LoginResult logoff(String username) {
      return authorizationSupport.logoff(username);
    }

    public RpcRes_Base<AppUserWrapper> createUser(AppUserWrapper userWrapper) {
        // проверка уникальности имени в таблице        
        AppUser user = authorizationSupport.findUserByName(userWrapper.getUserName());
        if (null != user) {
            return new RpcRes_Base<>(userWrapper, true, "Пользователь '%s' уже существует!");
        }
        try {          
          authorizationSupport.createUser(userWrapper);

            return new RpcRes_Base<>(
                    userWrapper, false, format("Создан пользователь: '%s'", userWrapper.getUserName()));
        } catch (Exception e) {
            String errMessage = getErrorMessage(e);
            return new RpcRes_Base<>(userWrapper, true, errMessage);
        }
    }

    public RpcRes_Base<AppUserWrapper> updateUser(AppUserWrapper userWrapper) {
        // проверка уникальности имени в таблице
        AppUser appUser = authorizationSupport.findUserByName(userWrapper.getUserName());
        if (null == appUser) {
            return new RpcRes_Base<AppUserWrapper>(userWrapper, true, format("Пользователь '%s' не существует!", userWrapper.getUserName()));
        }
        try {
            authorizationSupport.updateUser(appUser, userWrapper);
          
            return new RpcRes_Base<>(
                    userWrapper, false, format("Изменен пользователь: '%s'", userWrapper.getUserName()));
        } catch (Exception e) {
            String errMessage = getErrorMessage(e);
            return new RpcRes_Base<AppUserWrapper>(userWrapper, true, errMessage);
        }
    }

    public String getDatabaseVersion() throws SQLException {
        return authorizationSupport.getDatabaseVersion();
    }
}
