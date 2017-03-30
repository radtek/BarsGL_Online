package ru.rbt.security.ejb.repository.access;

import javax.ejb.Stateless;
import ru.rbt.security.ejb.entity.access.PrmValueHistory;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;



/**
 * Created by akichigi on 11.04.16.
 */
@Stateless
public class PrmValueHistoryRepository extends AbstractBaseEntityRepository<PrmValueHistory, Long> {
}
