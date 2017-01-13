package ru.rbt.barsgl.ejbcore.mapping.job;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Created by Ivan Sevastyanov
 */
@Entity
@DiscriminatorValue("SINGLE")
public class SingleActionJob extends TimerJob{
}
