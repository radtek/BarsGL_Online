package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

import ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode;
import ru.rbt.barsgl.shared.criteria.Criteria;
import ru.rbt.barsgl.shared.criteria.CriteriaBuilder;
import ru.rbt.barsgl.shared.criteria.CriteriaLogic;

import java.util.Date;

import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.StamtUnloadFull;

/**
 * Created by ER18837 on 19.03.15.
 */
public class StamtUnloadFullTask extends AbstractStamtUnloadTask {

    protected Criteria buildOperationCriteria(Date operday) {
        CriteriaBuilder builder = CriteriaBuilder.create(CriteriaLogic.AND);
        builder.appendEQ("procdate", operday).appendEQ("postdate", operday);
        return builder.build();
    }

    protected String getTargetTableName () {
        return "GL_ETLSTM";
    }

    protected LogCode getLogCode() {
        return StamtUnloadFull;
    }

    @Override
    protected UnloadStamtParams getUnloadType() {
        return UnloadStamtParams.FULL_POSTING;
    }
}
