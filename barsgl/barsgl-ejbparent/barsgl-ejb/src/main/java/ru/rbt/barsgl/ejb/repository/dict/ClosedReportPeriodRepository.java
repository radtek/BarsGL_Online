package ru.rbt.barsgl.ejb.repository.dict;

import ru.rbt.barsgl.ejb.entity.dict.BVSourceDeal;
import ru.rbt.barsgl.ejb.entity.dict.BVSourceDealId;
import ru.rbt.barsgl.ejb.entity.dict.ClosedReportPeriod;
import ru.rbt.barsgl.shared.dict.ClosedReportPeriodWrapper;
import ru.rbt.ejbcore.repository.AbstractBaseEntityRepository;

import java.util.Date;

/**
 * Created by er18837 on 14.08.2017.
 */
public class ClosedReportPeriodRepository extends AbstractBaseEntityRepository<ClosedReportPeriod, Date> {

    public ClosedReportPeriod findIntersectedRecord(ClosedReportPeriodWrapper wrapper) {
        return selectFirst(ClosedReportPeriod.class, "from ClosedReportPeriod p where" +
                " (p.cutDate > ?1 and p.lastDate <= ?2) or" +
                " (p.cutDate < ?1 and p.lastDate >= ?2)", wrapper.getCutDate(), wrapper.getLastDate());
    }
}
