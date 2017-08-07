package ru.rbt.barsgl.ejb.repository.dict;

import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.util.Date;

/**
 * Created by er18837 on 07.08.2017.
 */
@Stateless
@LocalBean
public class LwdBalanceCutRepository extends AbstractBaseEntityRepository {

    public void updateCloseTimestamp(Date runDate, Date closeTimestamp) {
        executeUpdate("update LwdBalanceCut b set b.closeDateTime = ?1 where b.runDate = ?2", closeTimestamp, runDate);
    }
}
