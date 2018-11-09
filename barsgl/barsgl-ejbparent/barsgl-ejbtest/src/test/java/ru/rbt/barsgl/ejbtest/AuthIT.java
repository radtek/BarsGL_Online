package ru.rbt.barsgl.ejbtest;

import org.junit.*;
import ru.rbt.barsgl.shared.Builder;
import ru.rbt.barsgl.shared.enums.AccessMode;
import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.util.ServerUtils;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.gwt.security.ejb.AuthorizationServiceGwtSupport;
import ru.rbt.security.entity.AppUser;
import ru.rbt.security.entity.access.*;
import ru.rbt.shared.LoginResult;
import ru.rbt.shared.access.UserMenuItemWrapper;
import ru.rbt.shared.access.UserMenuWrapper;
import ru.rbt.shared.enums.*;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.rbt.shared.LoginResult.LoginResultStatus.FAILED;


/**
 * Created by ER21006 on 18.04.2016.
 */
public class AuthIT extends AbstractRemoteIT{

    private static AppUser user;

    private static List<BaseEntity> objects = new ArrayList<>();

    private static final Logger log = Logger.getLogger(AuthIT.class.getName());

    @BeforeClass
    public static void init() {
        user = (AppUser) baseEntityRepository.save(GLUserBuilder.create()
                .withName("test" + StringUtils.rsubstr(System.currentTimeMillis()+"", 5)).build());
        objects.add(user);
        baseEntityRepository.executeNativeUpdate("update gl_od set ACSMODE = ?", AccessMode.FULL.name());
    }

    @AfterClass
    public static void tearDown() {
        Collections.reverse(objects);
        for (BaseEntity entity : objects) {
            baseEntityRepository.executeUpdate(format("delete from %s u where u.id = ?1"
                    , entity.getClass().getSimpleName()), entity.getId());
        }
    }

    @Test public void test() throws SQLException {
        // role
        Role role = new Role();
        role.setId(nextId());
        role.setName("123");
        role.setSys(RoleSys.N);
        role = saveObject(role);
        objects.add(role);

        UserRoleRln userRoleRln = new UserRoleRln(user, role);
        userRoleRln.setUserAuth(user.getUserName());
        userRoleRln.setDateAuth(new Date());
        userRoleRln = saveObject(userRoleRln);
        objects.add(userRoleRln);

        SecurityActionGroup group = new SecurityActionGroup();
        group.setId(nextId());
        group.setName(StringUtils.rsubstr(System.currentTimeMillis()+"", 3));
        group.setCode(group.getName());
        group = saveObject(group);
        objects.add(group);

        SecurityAction action = new SecurityAction();
        action.setId(nextId());
        action.setSecurityActionCode(SecurityActionCode.RoleInp);
        action.setActionGroup(group);
        action.setDescr(SecurityActionCode.RoleInp.getDesc());
        action = createActionIfAbsent(action);

        SecurityRoleActionRln roleActionRln = new SecurityRoleActionRln();
        roleActionRln.setId(new SecurityRoleActionRlnId(role, action));
        roleActionRln.setUserAuth(""); roleActionRln.setDateAuth(new Date());
        roleActionRln = saveObject(roleActionRln);
        objects.add(roleActionRln);

        // menu
        UserMenuNode rootNode = new UserMenuNode();
        rootNode.setId(nextId());
        rootNode.setMenuCode(UserMenuCode.SystemMenu);
        rootNode.setName("Menu" + StringUtils.rsubstr(System.currentTimeMillis() + "", 5));
        rootNode.setMenuType(UserMenuType.N);
        rootNode = (UserMenuNode) createMenuIfAbsent(rootNode);

        UserMenuItem item0 = new UserMenuItem();
        item0.setId(nextId());
        item0.setMenuCode(UserMenuCode.Operday);
        item0.setName("Menu" + StringUtils.rsubstr(System.currentTimeMillis() + "", 5));
        item0.setMenuType(UserMenuType.N);
        item0.setParent(rootNode);
        item0 = createMenuIfAbsent(item0);

        Assert.assertEquals(1, baseEntityRepository.executeUpdate("update UserMenuItem i set i.parent = ?1 where i.menuCode = ?2"
            , rootNode, item0.getMenuCode()));

        UserMenuItem item1 = new UserMenuItem();
        item1.setId(nextId());
        item1.setMenuCode(UserMenuCode.Upload);
        item1.setName("Menu" + StringUtils.rsubstr(System.currentTimeMillis() + "", 5));
        item1.setMenuType(UserMenuType.N);
        item1.setParent(rootNode);
        item1 = createMenuIfAbsent(item1);
        Assert.assertEquals(1, baseEntityRepository.executeUpdate("update UserMenuItem i set i.parent = ?1 where i.menuCode = ?2"
                , rootNode, item1.getMenuCode()));

        // добавляем в пункты меню один и тот же экшн

        UserMenuActionRln menuActionRln0 = new UserMenuActionRln(action, item0);
        menuActionRln0 = createMenuActIfAbsent(menuActionRln0);

        UserMenuActionRln menuActionRln1 = new UserMenuActionRln(action, item1);
        menuActionRln1 = createMenuActIfAbsent(menuActionRln1);

        // залогиница
        LoginResult result = remoteAccess.invoke(AuthorizationServiceGwtSupport.class, "login", user.getUserName(), "123");
        Assert.assertEquals("Message: " + result.getMessage(), result.getLoginResultStatus(), LoginResult.LoginResultStatus.SUCCEEDED);

        Assert.assertEquals(1, result.getAvailableActions().size());
        Assert.assertEquals(SecurityActionCode.RoleInp, result.getAvailableActions().get(0));

        // получить меню в соответствие с правами
        UserMenuWrapper menu = result.getUserMenu();
        Assert.assertNotNull(menu);
        Assert.assertNotNull(menu.getRootElements());
        Assert.assertTrue(!menu.getRootElements().isEmpty());

        List<UserMenuItemWrapper> sysItems = menu.getRootElements().stream()
                .filter(m -> m.getMenuCode() == UserMenuCode.SystemMenu).collect(Collectors.toList());
        Assert.assertEquals(1, sysItems.size());
        Assert.assertEquals(3, sysItems.get(0).getChildren().size());
        Assert.assertTrue(sysItems.get(0).getChildren().stream()
                .filter(w -> w.getMenuCode() == UserMenuCode.Upload).findFirst().isPresent());
        Assert.assertTrue(sysItems.get(0).getChildren().stream()
                .filter(w -> w.getMenuCode() == UserMenuCode.Operday).findFirst().isPresent());
        Assert.assertTrue(sysItems.get(0).getChildren().stream()
                .filter(w -> w.getMenuCode() == UserMenuCode.Access).findFirst().isPresent());

        baseEntityRepository.executeUpdate("delete from UserRoleRln r where r.id = ?1", userRoleRln.getId());
        objects.remove(userRoleRln);

        LoginResult result2 = remoteAccess.invoke(AuthorizationServiceGwtSupport.class, "login", user.getUserName(), "123");
        Assert.assertEquals(result2.getLoginResultStatus(), LoginResult.LoginResultStatus.SUCCEEDED);
        Assert.assertTrue(result2.getUserMenu().getRootElements().isEmpty());
    }

    /**
     * Проверка авторизации незарегистрированного пользователя (ошибка)
     */
    @Test
    public void testAuth() {
        LoginResult res = remoteAccess.invoke(AuthorizationServiceGwtSupport.class.getName(), "login", new Object[]{"FakeUser", "123"});
        Assert.assertEquals(FAILED, res.getLoginResultStatus());
    }

    @Test @Ignore
    public void testLdapConnect() throws NamingException {
        Hashtable<String,String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://172.17.145.105:389");
        final String dname = "CN=SrvBarsGL,OU=Service_Accounts,OU=IMB _Special_,DC=IMB,DC=RU";
        env.put(Context.REFERRAL, "throw");
        env.put(Context.SECURITY_PRINCIPAL, dname);
        env.put(Context.SECURITY_CREDENTIALS, "7Vh2vJ75");

        DirContext ctx = new InitialDirContext(env);

//        final String userName = "ER21006_NOTEXIST";
        final String userName = "ER21006";

        Attributes attrs = new BasicAttributes(true);
        attrs.put(new BasicAttribute("cn", userName));

        SearchControls controls  = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(new String[]{"distinguishedName"});
        NamingEnumeration<?> namingEnumeration = ctx.search("DC=IMB,DC=RU"
                , format("(cn=%s)", userName), new String[]{"distinguishedName"}, controls);
        while (namingEnumeration.hasMore()) {
            SearchResult result = (SearchResult) namingEnumeration.next();
            log.info(result.getAttributes().get("distinguishedName").get().toString());
        }

    }

    private static class GLUserBuilder implements Builder<AppUser> {

        private final AppUser user;

        public GLUserBuilder() {
            user = new AppUser();
            user.setUserSecurityType(UserExternalType.L);
            user.setLocked(UserLocked.N);
            user.setFirstName("Name1");
            user.setSurname("Surname1");
            user.setUserPassword(ServerUtils.md5("123"));
        }

        public static GLUserBuilder create() {
            return new GLUserBuilder();
        }

        @Override
        public AppUser build() {
            return user;
        }

        public GLUserBuilder withName(String name) {
            user.setUserName(name);
            return this;
        }
    }


    private static Integer nextId() throws SQLException {
        return baseEntityRepository.nextIntegerId("SEQ_GL_AUT");
    }

    private static <T extends BaseEntity> T saveObject(T object) {
        return (T) baseEntityRepository.save(object);
    }

    private SecurityAction createActionIfAbsent(SecurityAction action) {
        return Optional.ofNullable((SecurityAction) baseEntityRepository
                .selectFirst(SecurityAction.class, "from SecurityAction a where a.securityActionCode = ?1"
                , new Object[]{action.getSecurityActionCode()})).orElseGet(() ->
        {
            SecurityAction action1 = saveObject(action);
            objects.add(action1);
            return action1;
        });
    }

    private UserMenuItem createMenuIfAbsent(UserMenuItem item) {
        return Optional.ofNullable((UserMenuItem) baseEntityRepository
                .selectFirst(UserMenuItem.class, "from UserMenuItem a where a.menuCode = ?1"
                , new Object[]{item.getMenuCode()})).orElseGet(() ->
        {
            UserMenuItem item1 = saveObject(item);
            objects.add(item1);
            return item1;
        });
    }

    private UserMenuActionRln createMenuActIfAbsent(UserMenuActionRln item) {
        return Optional.ofNullable((UserMenuActionRln) baseEntityRepository
                .selectFirst(UserMenuActionRln.class, "from UserMenuActionRln a where a.id = ?1"
                , new Object[]{item.getId()})).orElseGet(() ->
        {
            UserMenuActionRln item1 = saveObject(item);
            objects.add(item1);
            return item1;
        });
    }
}
