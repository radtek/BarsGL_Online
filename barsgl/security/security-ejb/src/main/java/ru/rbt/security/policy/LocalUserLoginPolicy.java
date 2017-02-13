package ru.rbt.security.policy;

import ru.rbt.security.ejb.entity.AppUser;
import ru.rbt.security.ejb.repository.AppUserRepository;
import ru.rbt.barsgl.audit.controller.AuditController;
import ru.rbt.barsgl.shared.LoginResult;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.barsgl.audit.entity.AuditRecord.LogCode.Authorization;
import static ru.rbt.barsgl.ejbcore.util.ServerUtils.md5;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by Ivan Sevastyanov on 13.05.2016.
 * вход пользователя с локальным паролем
 */
public class LocalUserLoginPolicy extends LoginPolicy {

    @Inject
    private AppUserRepository appUserRepository;

    @EJB
    private AuditController auditController;

    @Override
    protected LoginResult buildLoginResult(String userName, String password) throws Exception {
        if (isEmpty(userName) || isEmpty(password)) {
            return LoginResult.buildPasswordRequiredLoginResult();
        }
        List<AppUser> users = appUserRepository.select(AppUser.class
                , "from AppUser u where u.userName = ?1 and u.userPassword = ?2", userName, md5(password));
        if (users.isEmpty()) {
            auditController.warning(Authorization, format("Login FAILED. Attempted user: '%s'", userName), null, "");
            return LoginResult.buildInvalidUsernameLoginResult(userName);
        } else {
            LoginResult result = LoginResult.buildSusccessLoginResult(users.get(0).getUserName());
            auditController.info(Authorization, format("Login SUCCEEDED. User: '%s'", result.getUserName()));
            return result;
        }
    }

    @Override
    public boolean isSupported(AppUser user) {
        return user.isLocal();
    }

}
