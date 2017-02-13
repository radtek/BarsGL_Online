package ru.rbt.security.policy;

import ru.rbt.security.ejb.entity.AppUser;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.security.ejb.repository.AppUserRepository;
import ru.rbt.barsgl.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.conf.map.StringProperty;
import ru.rbt.barsgl.ejbcore.repository.PropertiesRepository;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.Assert;
import ru.rbt.barsgl.shared.LoginResult;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.rbt.barsgl.audit.entity.AuditRecord.LogCode.Authorization;
import static ru.rbt.barsgl.ejbcore.util.StringUtils.isEmpty;

/**
 * Created by Ivan Sevastyanov on 13.05.2016.
 */
public class ExternalUserLoginPolicy extends LoginPolicy {

    public static final Logger log = Logger.getLogger(ExternalUserLoginPolicy.class.getName());

    private static final String AUTH_SRVUSER_DNAME_KEY = "auth.srvuser.dname";
    private static final String AUTH_SRVUSER_PSWD_KEY = "auth.srvuser.pswd";
    private static final String AUTH_SEARCHFROM = "auth.searchFrom";
    private static final String DISTINGUISHED_NAME_STRIMG = "distinguishedName";

    @EJB
    private PropertiesRepository propertiesRepository;

    @EJB
    private AuditController auditController;

    @Inject
    private AppUserRepository userRepository;

    @Override
    protected LoginResult buildLoginResult(String userName, String password) throws Exception {
        if (isEmpty(userName) || isEmpty(password)) {
            return LoginResult.buildPasswordRequiredLoginResult();
        } else {
            if (checkCredentials(userName, password)) {
                if (findUser(userName).isPresent()) {
                    return LoginResult.buildSusccessLoginResult(userName);
                } else {
                    return LoginResult.buildFailed("Пользователь не зарегистрирован как внешний");
                }
            } else {
                return LoginResult.buildFailed(format("Пользователь '%s' не прошел LDAP аутентификацию", userName));
            }
        }
    }

    @Override
    public boolean isSupported(AppUser user) {
        return !user.isLocal();
    }

    private boolean checkCredentials(final String userName, final String password) throws Exception {
        try {
            // логин под сервисным профилем
            DirContext ctx = ldapLogin(propertiesRepository
                    .getString(AUTH_SRVUSER_DNAME_KEY), propertiesRepository.getString(AUTH_SRVUSER_PSWD_KEY));

            String distinguishedName = getDistinguishedName(ctx, userName);

            closeLdapContext(ctx);

            // логин под профилем пользователя
            ctx = ldapLogin(distinguishedName, password);

            closeLdapContext(ctx);

            auditController.info(Authorization, format("LDAP login SUCCEEDED. User: '%s'", userName), null);
            return true;
        } catch (Throwable e) {
            ValidationError error = new ValidationError(ErrorCode.AUTH_LOGIN_FAILED, userName, e.getMessage());
            error.setStackTrace(e.getStackTrace());
            auditController.warning(Authorization, format("LDAP login FAILED. Attempted user: '%s'", userName), null, error);
            return false;
        }
    }

    private DirContext ldapLogin(String distinguishedName, String password) throws ExecutionException, NamingException {
        Hashtable env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, propertiesRepository
                .getCachedProperty(PropertyName.AD_LDAP_URI.getName()).getValue());
        env.put(Context.SECURITY_PRINCIPAL, distinguishedName);
        env.put(Context.SECURITY_CREDENTIALS, password);

        return new InitialDirContext(env);
    }

    private String getDistinguishedName(DirContext ctx, String userName) throws ExecutionException, NamingException {
        SearchControls controls  = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(new String[]{DISTINGUISHED_NAME_STRIMG});
        NamingEnumeration<?> namingEnumeration
                = ctx.search(propertiesRepository.getString(AUTH_SEARCHFROM)
                , format("(cn=%s)", userName), new String[]{DISTINGUISHED_NAME_STRIMG}, controls);
        String distinguishedName = null;
        try {
            if (namingEnumeration.hasMore()) {
                SearchResult searchResult = (SearchResult) namingEnumeration.next();
                Attributes searchedAttrs = searchResult.getAttributes();
                Attribute dnameAttr = searchedAttrs.get(DISTINGUISHED_NAME_STRIMG);
                distinguishedName = dnameAttr.get().toString();
            }
        } catch (Throwable e) {
            auditController.warning(Authorization, format("Ошибка при поиске пользователя  '%s' в LDAP", userName), null, e);
        }
        Assert.isTrue(distinguishedName != null && !distinguishedName.isEmpty()
                , () -> new ValidationError(ErrorCode.AUTH_LOGIN_FAILED, userName, "не удалось определить полное имя пользователя"));
        log.info(format("Аутентификация: найден пользователь '%s', полное имя '%s'", userName, distinguishedName));
        return distinguishedName;
    }

    private Optional<AppUser> findUser(String userName) {
        AppUser user = userRepository.findUserByName(userName);
        if (null == user) {
            auditController.warning(Authorization, format("Пользователь '%s' должен быть зарегистрирован как внешний", userName), null, "");
            return Optional.empty();
        } else {
            return Optional.of(user);
        }
    }

    private void closeLdapContext(DirContext context) {
        try {
            context.close();
        } catch (NamingException e) {
            log.log(Level.SEVERE, "Ошибка закрытия LDAP контекста", e);
        }
    }

}
