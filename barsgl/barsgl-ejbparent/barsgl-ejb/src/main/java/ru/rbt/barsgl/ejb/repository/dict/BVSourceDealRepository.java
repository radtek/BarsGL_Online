package ru.rbt.barsgl.ejb.repository.dict;

import ru.rbt.barsgl.ejb.entity.dict.BVSourceDeal;
import ru.rbt.barsgl.ejb.entity.dict.BVSourceDealId;
import ru.rbt.barsgl.shared.dict.BVSourceDealWrapper;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import java.util.Date;

/**
 * Created by er18837 on 14.08.2017.
 */
public class BVSourceDealRepository extends AbstractBaseEntityRepository<BVSourceDeal, BVSourceDealId> {

    public void closePreviousRecord(BVSourceDeal newRecord, Date dateClose) {
        executeUpdate("update BVSourceDeal s set s.dateEnd = ?1 where s.id.sourceDeal = ?2 and s.dateEnd is null"
                        , dateClose, newRecord.getId().getSourceDeal());
    }

    public BVSourceDeal findIntersectedRecord(BVSourceDealWrapper wrapper) {
        String startAfter = null == wrapper.getEndDate() ? "" : " and s.id.dateStart <= ?3";
        return selectFirst(BVSourceDeal.class, "from BVSourceDeal s where s.id.sourceDeal = ?1 and " +
                        " (not s.dateEnd is null and s.dateEnd >= ?2" +
                        " or s.id.dateStart >= ?2" + startAfter + ")"
                , wrapper.getSourceDeal(), wrapper.getStartDate(), wrapper.getEndDate());
    }
}
