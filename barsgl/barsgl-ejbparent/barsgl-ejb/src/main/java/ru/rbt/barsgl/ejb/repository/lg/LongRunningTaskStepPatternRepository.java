package ru.rbt.barsgl.ejb.repository.lg;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import ru.rbt.barsgl.ejb.entity.lg.LongRunningTaskStepPattern;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by Ivan Sevastyanov on 19.10.2016.
 */
@Stateless
@LocalBean
public class LongRunningTaskStepPatternRepository extends AbstractBaseEntityRepository<LongRunningTaskStepPattern, Long> {
}
