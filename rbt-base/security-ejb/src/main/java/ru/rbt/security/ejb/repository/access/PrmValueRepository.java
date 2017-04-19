package ru.rbt.security.ejb.repository.access;

import javax.ejb.Stateless;
import ru.rbt.security.entity.access.PrmValue;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by akichigi on 07.04.16.
 */
@Stateless
public class PrmValueRepository extends AbstractBaseEntityRepository<PrmValue, Long> {
}
