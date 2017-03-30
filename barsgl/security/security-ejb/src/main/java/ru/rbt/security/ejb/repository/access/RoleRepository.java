package ru.rbt.security.ejb.repository.access;

import javax.ejb.Stateless;
import ru.rbt.security.ejb.entity.access.Role;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by akichigi on 12.04.16.
 */
@Stateless
public class RoleRepository extends AbstractBaseEntityRepository<Role, Integer> {
}
