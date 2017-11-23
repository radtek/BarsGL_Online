package ru.rbt.security.policy;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.security.ejb.repository.AppUserRepository;
import ru.rbt.security.ejb.repository.access.PrmValueRepository;
import ru.rbt.security.ejb.repository.access.SecurityActionRepository;
import ru.rbt.security.ejb.repository.access.UserMenuRepository;
import ru.rbt.security.entity.AppUser;
import ru.rbt.security.entity.access.PrmValue;
import ru.rbt.shared.Assert;
import ru.rbt.shared.LoginResult;
import ru.rbt.shared.enums.PrmValueEnum;
import ru.rbt.shared.enums.SecurityActionCode;
import ru.rbt.shared.user.AppUserWrapper;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Authorization;
import static ru.rbt.shared.LoginResult.LoginResultStatus.LIMIT;
import static ru.rbt.shared.enums.SecurityActionCode.UserRestrictedAccess;

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
    private PrmValueRepository prmValueRepository;

    @EJB
    private PropertiesRepository propertiesRepository;

    public final LoginResult login(String userName,String pasword) throws Exception {
        final AppUser user = userRepository.findUserByName(userName);
        if (null != user) {
            final LoginResult preliminaryResult = buildCommonLoginResult(user);
            if (preliminaryResult.isSucceeded()) {
                final LoginResult finalResult = buildLoginResult(userName, pasword);
                if (preliminaryResult.isSucceeded()) {
                    List<SecurityActionCode> actions = actionRepository.getAvailableActions(user);
                    finalResult.setAvailableActions(actions);
                    finalResult.setUserMenu(menuRepository.getUserMenu(user));
                    finalResult.setUser(getUserWrapper(user));
                    calcaulateTotalResult(finalResult, actions);
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

        AppUserWrapper granted = getGrantedBranchesAndSourses(user);
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
    
    public AppUserWrapper getGrantedBranchesAndSourses(AppUser user){
        AppUserWrapper wrapper = new AppUserWrapper();
        ArrayList<String> grantedHeadBranches = new ArrayList<>();
        ArrayList<String> grantedSources = new ArrayList<>();

        String query = "from PrmValue p where p.userId = ?1 and p.prmCode = ?2";

        List<PrmValue> prmValues = prmValueRepository.select(PrmValue.class, query, user.getId(), PrmValueEnum.HeadBranch);
        for(PrmValue value : prmValues){
            grantedHeadBranches.add(value.getPrmValue());
        }

        prmValues = prmValueRepository.select(PrmValue.class, query, user.getId(), PrmValueEnum.Source);
        for(PrmValue value : prmValues){
            grantedSources.add(value.getPrmValue());
        }

        wrapper.setGrantedHeadBranches(grantedHeadBranches);
        wrapper.setGrantedSources(grantedSources);

        return wrapper;
    }

    private boolean isLimitAccessMode() throws SQLException {
        return "LIMIT".equals(actionRepository.selectFirst("select ACSMODE from GL_OD").getString(0));
    }

    /**
     * для прошедших основную аутентификацию анализируем возможность работать в ограниченном доступе
     * @param original - входящий результат
     * @param actions
     * @throws SQLException
     */
    private void calcaulateTotalResult(LoginResult original, List<SecurityActionCode> actions) throws SQLException {
        if (original.isSucceeded()) {
            if (isLimitAccessMode() && actions.stream().anyMatch(p -> p.equals(UserRestrictedAccess))) {
                original.setLoginResultStatus(LIMIT);
            } else if (isLimitAccessMode()) {
                original.setFailed("Внимание !!!\n" +
                        "Временно приостановлена работа в системе.\n" +
                        "О доступности системы будет сообщено дополнительно.");
            }
        }
    }
}
