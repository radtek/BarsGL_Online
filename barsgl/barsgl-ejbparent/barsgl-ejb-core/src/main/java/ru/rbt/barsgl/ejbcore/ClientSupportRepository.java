package ru.rbt.barsgl.ejbcore;

import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;
import ru.rbt.shared.enums.Repository;

import javax.persistence.EntityManager;

/**
 * Created by akichigi on 14.05.15.
 */
public class ClientSupportRepository extends AbstractBaseEntityRepository {

    public Object selectOne(Repository repository, Class clazz, String jpaQuery, Object... params) {
        try {
            EntityManager persistence = getPersistence(repository);
            return super.selectOne(persistence, clazz, jpaQuery, params);
        } catch (Exception ex) {
            //TODO надо сделать нормальную обработку
            throw new RuntimeException("Ошибка", ex);
        }
    }
    public Object selectFirst(Repository repository, Class clazz, String jpaQuery, Object... params) {
        try {
            EntityManager persistence = getPersistence(repository);
            return super.selectFirst(persistence, clazz, jpaQuery, params);
        } catch (Exception ex) {
            //TODO надо сделать нормальную обработку
            throw new RuntimeException("Ошибка", ex);
        }
    }
}
