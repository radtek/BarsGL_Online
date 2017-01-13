package ru.rbt.barsgl.ejb.controller.operday;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.security.AuditController;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.JobControl;

/**
 * Created by ER21006 on 07.04.2016.
 */
public class PdModeController {

    @Inject
    private OperdayController operdayController;

    @Inject
    private AuditController auditController;

    public Operday.PdMode swithPdMode(Operday.PdMode fromMode) throws Exception {
        auditController.info(JobControl, format("Переключение режима обработки проводок. Исходный режим: '%s'", fromMode));
        return operdayController.swithPdMode(fromMode);
    }

}
