/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.security;

import org.apache.log4j.Logger;
import java.sql.SQLException;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.ServerUtils;
import ru.rbt.shared.LoginResult;
import ru.rbt.shared.user.AppUserWrapper;
import ru.rbt.security.entity.AppUser;
import ru.rbt.security.ejb.repository.AppUserRepository;
import ru.rbt.security.policy.LoginPolicy;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Authorization;
import static ru.rbt.audit.entity.AuditRecord.LogCode.User;
import static ru.rbt.shared.LoginResult.buildInvalidUsernameLoginResult;

/**
 *
 * @author Andrew Samsonov
 */
@Stateless
@LocalBean
public class AuthorizationServiceSupport {

  private static Logger log = Logger.getLogger(AuthorizationServiceSupport.class);

  @EJB
  private AuditController auditController;

  @EJB
  private AppUserRepository appUserRepository;

  @Inject
  private DateUtils dateUtils;

  @Inject
  private Instance<LoginPolicy> loginPolicies;

  public AppUser findUserByName(String userName) {
    return appUserRepository.findUserByName(userName);
  }

  public LoginResult login(String username, String password) throws Exception {
    AppUser user = findUserByName(username);
    if (null != user) {
      LoginPolicy policy = ServerUtils.findSupported(loginPolicies, p -> p.isSupported(user));
      return policy.login(username, password);
    } else {
      auditController.warning(Authorization, format("LOGIN FAILED. Пользователь '%s'", username), null,
              format("Пользователь '%s' должен быть зарегистрирован локально", username));
      return buildInvalidUsernameLoginResult(username);
    }
  }

  public LoginResult logoff(String username) {
    LoginResult result = new LoginResult();
    result.setSucceeded();
    result.setUserName(username);
    String msg = format("Logoff SUCCEEDED. User: '%s'", result.getUserName());
    log.info(msg);
    auditController.info(Authorization, msg, null);
    return result;
  }

  public AppUser createUser(AppUserWrapper userWrapper) throws Exception {
    try {
      AppUser appUser = new AppUser();
      appUser.setUserName(userWrapper.getUserName());
      appUser.setUserPassword(userWrapper.getUserPassword() == null ? null : ServerUtils.md5(userWrapper.getUserPassword()));
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

      return appUser;
    } catch (Exception e) {
      auditController.error(User, format("Ошибка при создании пользователя: '%s'", userWrapper.getUserName()), null, e);
      throw e;
    }
  }

  public AppUser updateUser(AppUser appUser, AppUserWrapper userWrapper) throws Exception {
    try {
      if (userWrapper.getUserPassword() != null) {
        appUser.setUserPassword(ServerUtils.md5(userWrapper.getUserPassword()));
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

      return appUser;
    } catch (Exception e) {
      auditController.error(User, format("Ошибка при изменении пользователя: '%s'", userWrapper.getUserName()), null, e);
      throw e;
    }
  }

  public String getDatabaseVersion() throws SQLException {
    return appUserRepository.selectOne("select m.version from gl_sysmod m").getString("version");
  }
}
