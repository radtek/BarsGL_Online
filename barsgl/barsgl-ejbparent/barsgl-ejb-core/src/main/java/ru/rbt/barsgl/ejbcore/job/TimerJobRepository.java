package ru.rbt.barsgl.ejbcore.job;

import ru.rbt.barsgl.ejbcore.mapping.job.TimerJob;
import ru.rbt.barsgl.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.Stateless;

/**
 * Created by Ivan Sevastyanov
 */
@Stateless
public class TimerJobRepository extends AbstractBaseEntityRepository<TimerJob, Long> {}
