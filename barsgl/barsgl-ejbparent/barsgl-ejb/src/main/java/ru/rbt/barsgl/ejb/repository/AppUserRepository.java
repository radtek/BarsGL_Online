package ru.rbt.barsgl.ejb.repository;

import ru.rbt.barsgl.ejb.entity.AppUser;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;

import java.util.List;

import static ru.rbt.barsgl.ejbcore.util.ServerUtils.md5;

/**
 * Created by ER18837 on 30.09.15.
 */
public class AppUserRepository extends AbstractBaseEntityRepository<AppUser, Integer> {

    /**
     * Поиск пользователя по имени
     * @param userName
     * @return
     */
    public AppUser findUserByName(String userName) {
        return selectFirst(AppUser.class, "from AppUser u where u.userName = ?1", userName);
    }
}
