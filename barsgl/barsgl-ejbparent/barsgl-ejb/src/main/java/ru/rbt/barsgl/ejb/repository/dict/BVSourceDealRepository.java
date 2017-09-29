package ru.rbt.barsgl.ejb.repository.dict;

import ru.rbt.barsgl.ejb.entity.dict.BVSourceDeal;
import ru.rbt.barsgl.ejb.entity.dict.BVSourceDealId;
import ru.rbt.barsgl.shared.dict.BVSourceDealWrapper;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import java.sql.SQLException;
import java.util.Date;

/**
 * Created by er18837 on 14.08.2017.
 */
public class BVSourceDealRepository extends AbstractBaseEntityRepository<BVSourceDeal, BVSourceDealId> {

    public void closePreviousRecord(BVSourceDeal newRecord, Date dateClose) {
        executeUpdate("update BVSourceDeal s set s.endDate = ?1 where s.id.sourceDeal = ?2 and s.id.startDate <= ?1 and s.endDate is null"
                        , dateClose, newRecord.getId().getSourceDeal());
    }

    public Date getNextStartDate(BVSourceDealWrapper wrapper) throws SQLException {
        String sql = "select min(DTB) from GL_BVPARM s where s.ID_SRC = ? and s.DTB > ?";
        DataRecord data = selectFirst(sql, wrapper.getSourceDeal(), wrapper.getStartDate());
        return (null == data) ? null : data.getDate(0);
    }

    public BVSourceDeal findIntersectedRecord(BVSourceDealWrapper wrapper) {
        BVSourceDealId id = new BVSourceDealId(wrapper.getSourceDeal(), wrapper.getStartDate());
        String sql = "from BVSourceDeal s where s.id <> ?1 and s.id.sourceDeal = ?2 ";
        if (null == wrapper.getEndDate()) {
            sql += " and (s.id.startDate >= ?3)";
            return selectFirst(BVSourceDeal.class, sql
                    , id, wrapper.getSourceDeal(), wrapper.getStartDate());

        } else {
            sql += " and ((s.endDate is null and s.id.startDate between ?3 and ?4)" +
                    " or (?3 between s.id.startDate and s.endDate)" +
                    " or (?4 between s.id.startDate and s.endDate))";
            return selectFirst(BVSourceDeal.class, sql
                    , id, wrapper.getSourceDeal(), wrapper.getStartDate(), wrapper.getEndDate());
        }
    }

/*
    public BVSourceDeal findIntersectedRecord(BVSourceDealWrapper wrapper) {
        BVSourceDealId id = new BVSourceDealId(wrapper.getSourceDeal(), wrapper.getStartDate());
        String sql = "from BVSourceDeal s where s.id <> ?1" +
                " and s.id.sourceDeal = ?2 " +
                " and (s.id.startDate = ?3 or not s.endDate is null and s.endDate >= ?3 and (s.id.startDate <= ?3";
        if (null == wrapper.getEndDate()) {
            return selectFirst(BVSourceDeal.class, sql + "))"
                    , id, wrapper.getSourceDeal(), wrapper.getStartDate());

        } else {
            return selectFirst(BVSourceDeal.class, sql + " or s.id.startDate <= ?4))"
                    , id, wrapper.getSourceDeal(), wrapper.getStartDate(), wrapper.getEndDate());
        }
    }
*/
}
