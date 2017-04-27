package ru.rbt.barsgl.ejb.repository.dict.AccType;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import ru.rbt.barsgl.ejb.entity.dict.AccType.ActLog;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by akichigi on 21.09.16.
 */
@Stateless
@LocalBean
public class ActLogRepository extends AbstractBaseEntityRepository<ActLog, Long> {
}
