package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.audit.entity.AuditRecord;
import ru.rbt.barsgl.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

import javax.ejb.EJB;
import java.util.Properties;

/**
 * ER22228
 * Для теста MovementCreateProcessor
 */
public class KeyValueStorageSetter implements ParamsAwareRunnable {
    @EJB
    private AuditController auditController;

    @EJB
    private KeyValueStorage keyValueStorage;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        auditController.info(AuditRecord.LogCode.KeyValueStorageSetter, properties.get("operation") + "/" + properties.get("key") + "/" + properties.get("value"));
        switch ((String) properties.get("operation")) {
            case "setTaskContinue":
                keyValueStorage.setTaskContinue(
                    KeyValueStorage.TASKS.valueOf((String) properties.get("key")),
                    Boolean.valueOf((String) properties.get("value"))
                );
                break;
        }
    }
}

