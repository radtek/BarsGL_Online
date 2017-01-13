package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

import ru.rbt.barsgl.ejb.entity.sec.AuditRecord;
import ru.rbt.barsgl.shared.criteria.Criteria;
import ru.rbt.barsgl.shared.criteria.CriteriaBuilder;

import javax.inject.Inject;
import java.util.Date;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.controller.operday.task.stamt.UnloadStamtParams.DELTA_POSTING;
import static ru.rbt.barsgl.shared.criteria.CriteriaLogic.AND;

/**
 * Created by ER18837 on 19.03.15.
 */
public class StamtUnloadDeltaTask extends AbstractStamtUnloadTask {

    @Inject
    private StamtUnloadController unloadController;

    @Override
    protected Criteria buildOperationCriteria(Date operday) {
        return CriteriaBuilder.create(AND).appendEQ("procdate", operday).appendLT("postdate", operday).build();
    }

    @Override
    protected String getTargetTableName() {
        return "GL_ETLSTMD";
    }

    @Override
    protected AuditRecord.LogCode getLogCode() {
        return AuditRecord.LogCode.StamtUnloadDelta;
    }

    @Override
    protected UnloadStamtParams getUnloadType() {
        return DELTA_POSTING;
    }

    @Override
    protected void postUnload() {
        try {
            auditController.info(getLogCode(), String.format("Удалено инкрем.данных '%s'", cleanIncData()));
        } catch (Exception e) {
            auditController.error(getLogCode(), "Ошибка при удалении инкрементальных данных", null, e);
        }
    }

    @Override
    protected void preUnload() {
        try {
            auditController.info(getLogCode(), format("Исключаем в текущей выгрузке ранее выгруженные PCID '%s'", unloadController.moveIntrodayHistory()));
        } catch (Exception e) {
            auditController.error(getLogCode(), "Ошибка регистрации ранее выгруженных данных", null, e);
        }
    }

    /**
     * Очистка данных об инкрементальных выгрузках
     * @return кол-во ранее выгруженных PCID
     * @throws Exception
     */
    private int cleanIncData() throws Exception {
        return (int) repository.executeInNewTransaction(persistence -> repository.executeNativeUpdate("delete from gl_etlstma"));
    }
}