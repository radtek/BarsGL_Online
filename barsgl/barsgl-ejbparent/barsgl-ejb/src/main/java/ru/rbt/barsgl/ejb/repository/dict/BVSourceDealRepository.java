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
        executeUpdate("update BVSourceDeal s set s.endDate = ?1 where s.id.sourceDeal = ?2 and s.endDate is null"
                        , dateClose, newRecord.getId().getSourceDeal());
    }

    public BVSourceDeal findIntersectedRecord(BVSourceDealWrapper wrapper) {
        String sql = "from BVSourceDeal s where s.id.sourceDeal = ?1 " +
                " and (not s.endDate is null and s.endDate >= ?2 or s.id.startDate > ?2";
        if (null == wrapper.getEndDate()) {
            return selectFirst(BVSourceDeal.class, sql + ")"
                    , wrapper.getSourceDeal(), wrapper.getStartDate());

        } else {
            return selectFirst(BVSourceDeal.class, sql + " and s.id.startDate <= ?3)"
                    , wrapper.getSourceDeal(), wrapper.getStartDate(), wrapper.getEndDate());
        }
    }
}
