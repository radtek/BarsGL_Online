package ru.rbt.barsgl.ejb.repository;

import ru.rbt.ejbcore.mapping.BaseEntity;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import java.util.List;

/**
 * Created by er22317 on 08.02.2018.
 */
public class BranchDictRepository<E extends BaseEntity<String>> extends AbstractBaseEntityRepository<E, String> {

    public <E> List<E> getAll(Class<E> clazz) {
        return select(clazz, "select t from " + clazz.getName() + " t", new Object[]{});
    }
}
