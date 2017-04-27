package ru.rbt.security.ejb.repository.access;

import javax.ejb.Stateless;
import ru.rbt.security.entity.access.Role;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by akichigi on 12.04.16.
 */
@Stateless
public class RoleRepository extends AbstractBaseEntityRepository<Role, Integer> {
}
