package ru.rbt.security.ejb.repository.access;

import javax.ejb.Stateless;
import ru.rbt.security.ejb.entity.access.PrmValue;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by akichigi on 07.04.16.
 */
@Stateless
public class PrmValueRepository extends AbstractBaseEntityRepository<PrmValue, Long> {
}
