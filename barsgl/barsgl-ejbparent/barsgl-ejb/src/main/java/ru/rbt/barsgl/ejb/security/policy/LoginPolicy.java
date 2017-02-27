package ru.rbt.barsgl.ejb.security.policy;

import ru.rbt.barsgl.ejb.access.AccessServiceSupport;
import ru.rbt.barsgl.ejb.entity.AppUser;
import ru.rbt.barsgl.ejb.entity.sec.AuditRecord;
import ru.rbt.barsgl.ejb.repository.AppUserRepository;
import ru.rbt.barsgl.ejb.repository.access.SecurityActionRepository;
import ru.rbt.barsgl.ejb.repository.access.UserMenuRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.repository.PropertiesRepository;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.shared.Assert;
import ru.rbt.barsgl.shared.LoginResult;
import ru.rbt.barsgl.shared.user.AppUserWrapper;

import javax.ejb.EJB;
import javax.inject.Inject;

import java.util.Date;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.Authorization;

/**
 * Created by Ivan Sevastyanov on 13.05.2016.
 */
public abstract class LoginPolicy {

    @Inject
    private SecurityActionRepository actionRepository;

    @Inject
    private AppUserRepository userRepository;

    @Inject
    private UserMenuRepository menuRepository;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private AuditController auditController;

    @Inject
    private AccessServiceSupport accessServiceSupport;

    @EJB
    private PropertiesRepository propertiesRepository;

    public final LoginResult login(String userName,String pasword) throws Exception {
        final AppUser user = userRepository.findUserByName(userName);
        if (null != user) {
            final LoginResult preliminaryResult = buildCommonLoginResult(user);
            if (preliminaryResult.isSucceeded()) {
                final LoginResult finalResult = buildLoginResult(userName, pasword);
                if (preliminaryResult.isSucceeded()) {
                    finalResult.setAvailableActions(actionRepository.getAvailableActions(user));
                    finalResult.setUserMenu(menuRepository.getUserMenu(user));
                    finalResult.setUser(getUserWrapper(user));
                }
                return finalResult;
            } else {
                auditController.warning(Authorization, format("LOGIN FAILED. User '%s'", userName),  null, preliminaryResult.getMessage());
                return preliminaryResult;
            }
        } else {
            return LoginResult.buildFailed(format("Пользователь '%s' не зарегистрирован в системе", userName));
        }
    }

    protected abstract LoginResult buildLoginResult(String userName,String password) throws Exception;

    public abstract boolean isSupported(AppUser user);

    private AppUserWrapper getUserWrapper(AppUser user){
        AppUserWrapper wrapper = new AppUserWrapper();
        wrapper.setId(user.getId());
        wrapper.setUserName(user.getUserName());
        wrapper.setSurname(user.getSurname());
        wrapper.setFirstName(user.getFirstName());
        wrapper.setPatronymic(user.getPatronymic());
        wrapper.setBranch(user.getBranch());
        wrapper.setFilial(user.getFilial());

        AppUserWrapper granted = accessServiceSupport.getGrantedBranchesAndSourses(user);
        wrapper.setGrantedSources(granted.getGrantedSources());
        wrapper.setGrantedHeadBranches(granted.getGrantedHeadBranches());

        wrapper.setErrorListProcPermit(propertiesRepository.getStringDef("list.err.proc.permit", "N"));
        return wrapper;
    }


    private LoginResult buildCommonLoginResult(AppUser user) {
        Assert.isTrue(user != null);
        if (user.isLocked()) {
            return LoginResult.buildFailed(format("Пользователь '%s' заблокирован", user.getUserName()));
        } else if (user.isExpired()) {
            return LoginResult.buildFailed(format("Истек срок действия '%s' учетной записи пользователя '%s'"
                    , dateUtils.dbDateString(user.getEnd_Date()), user.getUserName()));
        }
        return LoginResult.buildSusccessLoginResult(user.getUserName());
    }
}
