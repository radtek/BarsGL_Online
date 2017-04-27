package ru.rbt.barsgl.ejbcore.job;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless
@LocalBean
public class TimerJobRepository extends AbstractBaseEntityRepository<TimerJob, Long> {}
