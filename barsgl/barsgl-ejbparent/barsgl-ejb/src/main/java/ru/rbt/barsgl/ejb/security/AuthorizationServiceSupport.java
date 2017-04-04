package ru.rbt.barsgl.ejb.security;


import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.entity.AppUser;
import ru.rbt.barsgl.ejb.repository.AppUserRepository;
import ru.rbt.barsgl.ejb.security.policy.LoginPolicy;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbcore.util.ServerUtils;
import ru.rbt.barsgl.shared.LoginResult;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.UserExternalType;
import ru.rbt.barsgl.shared.user.AppUserWrapper;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.sql.SQLException;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.Authorization;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.User;
import static ru.rbt.barsgl.ejbcore.util.ServerUtils.md5;
import static ru.rbt.barsgl.shared.ExceptionUtils.getErrorMessage;
import static ru.rbt.barsgl.shared.LoginResult.buildInvalidUsernameLoginResult;
import static ru.rbt.barsgl.shared.enums.YesNoType.No;
import static ru.rbt.barsgl.shared.enums.YesNoType.Yes;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless
@LocalBean
public class AuthorizationServiceSupport {

    private static Logger log = Logger.getLogger(AuthorizationServiceSupport.class);

    @EJB
    private AuditController auditController;

    @Inject
    private AppUserRepository appUserRepository;

    @Inject
    private Instance<LoginPolicy> loginPolicies;

    @Inject
    private DateUtils dateUtils;

    public String getUserSql(){
        return  "select * from ( " +
                "select ID_USER, USER_NAME, SURNAME, FIRSTNAME, PATRONYMIC, FILIAL, DEPID, CREATE_DT, END_DT, " +
                "case when LOCKED = '0' then trim('" + No.getLabel() + "') " +
                "else trim('" + Yes.getLabel() + "') " +
                "end LOCKED, " +
                "case when SEC_TYPE = '0' then trim('" + UserExternalType.L.name() + "') " +
                "else trim('" + UserExternalType.E.name() + "') end SEC_TYPE, USER_PWD " +
                "from GL_USER) v ";
    }

    public LoginResult login(String username, String password) throws Exception {
        AppUser user = appUserRepository.findUserByName(username);
        if (null != user) {
            LoginPolicy policy = ServerUtils.findSupported(loginPolicies, p -> p.isSupported(user));
            return policy.login(username, password);
        } else {
            auditController.warning(Authorization, format("LOGIN FAILED. Пользователь '%s'", username), null
                    , format("Пользователь '%s' должен быть зарегистрирован локально",username));
            return buildInvalidUsernameLoginResult(username);
        }
    }

    public LoginResult logoff(String username) {
        LoginResult result  = new LoginResult();
        result.setSucceeded();
        result.setUserName(username);
        String msg = format("Logoff SUCCEEDED. User: '%s'", result.getUserName());
        log.info(msg);
        auditController.info(Authorization, msg, null);
        return result;
    }

    public RpcRes_Base<AppUserWrapper> createUser(AppUserWrapper userWrapper) {
        // проверка уникальности имени в таблице
        AppUser user = appUserRepository.selectFirst(AppUser.class, "from AppUser u where u.userName = ?1", userWrapper.getUserName());
        if (null != user) {
            return new RpcRes_Base<AppUserWrapper>(userWrapper, true, "Пользователь '%s' уже существует!");
        }
        try {
            AppUser appUser = new AppUser();
            appUser.setUserName(userWrapper.getUserName());
            appUser.setUserPassword(userWrapper.getUserPassword() == null ? null : md5(userWrapper.getUserPassword()));
            appUser.setFirstName(userWrapper.getFirstName());
            appUser.setSurname(userWrapper.getSurname());
            appUser.setPatronymic(userWrapper.getPatronymic());
            appUser.setFilial(userWrapper.getFilial());
            appUser.setBranch(userWrapper.getBranch());
            appUser.setEnd_Date(dateUtils.onlyDateParse(userWrapper.getCloseDateStr()));
            appUser.setLocked(userWrapper.getLocked());
            appUser.setUserSecurityType(userWrapper.getUserType());

            appUserRepository.save(appUser);

            auditController.info(User, format("Создан пользователь: '%s'", userWrapper.getUserName()));

            return new RpcRes_Base<>(
                    userWrapper, false, format("Создан пользователь: '%s'", userWrapper.getUserName()));
        } catch (Exception e) {
            String errMessage = getErrorMessage(e);
            auditController.error(User, format("Ошибка при создании пользователя: '%s'", userWrapper.getUserName()), null, e);
            return new RpcRes_Base<AppUserWrapper>(userWrapper, true, errMessage);
        }
    }

    public RpcRes_Base<AppUserWrapper> updateUser(AppUserWrapper userWrapper) {
        // проверка уникальности имени в таблице
        AppUser appUser = appUserRepository.selectFirst(AppUser.class, "from AppUser u where u.userName = ?1", userWrapper.getUserName());
        if (null == appUser) {
            return new RpcRes_Base<AppUserWrapper>(userWrapper, true, format("Пользователь '%s' не существует!", userWrapper.getUserName()));
        }
        try {
            if (userWrapper.getUserPassword() != null) {
                appUser.setUserPassword(md5(userWrapper.getUserPassword()));
            } else {
                appUser.setUserPassword(userWrapper.getPwdMD5());
            }

            appUser.setFirstName(userWrapper.getFirstName());
            appUser.setSurname(userWrapper.getSurname());
            appUser.setPatronymic(userWrapper.getPatronymic());
            appUser.setFilial(userWrapper.getFilial());
            appUser.setBranch(userWrapper.getBranch());
            appUser.setEnd_Date(dateUtils.onlyDateParse(userWrapper.getCloseDateStr()));
            appUser.setLocked(userWrapper.getLocked());
            appUser.setUserSecurityType(userWrapper.getUserType());

            appUserRepository.update(appUser);

            auditController.info(User, format("Изменен пользователь: '%s'", userWrapper.getUserName()));

            return new RpcRes_Base<>(
                    userWrapper, false, format("Изменен пользователь: '%s'", userWrapper.getUserName()));
        } catch (Exception e) {
            String errMessage = getErrorMessage(e);
            auditController.error(User, format("Ошибка при изменении пользователя: '%s'", userWrapper.getUserName()), null, e);
            return new RpcRes_Base<AppUserWrapper>(userWrapper, true, errMessage);
        }
    }

    public String getDatabaseVersion() throws SQLException {
        return appUserRepository.selectOne("select m.version from gl_sysmod m").getString("version");
    }
}
