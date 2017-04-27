package ru.rbt.security.ejb.repository;

import javax.ejb.Stateless;
import ru.rbt.security.entity.AppUser;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by ER18837 on 30.09.15.
 */
@Stateless
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
