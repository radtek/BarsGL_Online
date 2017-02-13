package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.od.OperdaySynchronizationController;
import ru.rbt.barsgl.audit.entity.AuditRecord;
import ru.rbt.barsgl.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

import javax.ejb.EJB;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.barsgl.audit.entity.AuditRecord.LogCode.BufferModeSyncTask;

/**
 * Created by Ivan Sevastyanov on 03.11.2016.
 * @deprecated As emergency workaround only! Do not delete it!
 *
 */
@Deprecated
public class PdSyncTaskOld implements ParamsAwareRunnable {
    @EJB
    private OperdaySynchronizationController synchronizationController;

    @EJB
    private CoreRepository coreRepository;

    @EJB
    private AuditController auditController;

    @EJB
    private OperdayController operdayController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        auditController.info(BufferModeSyncTask, "Запуск задачи синхронизации проводок/оборотов");
        try {
            coreRepository.executeInNewTransaction(persistence -> {synchronizationController.syncPostings(); return null;});
            auditController.info(BufferModeSyncTask, "Cинхронизация проводок/оборотов выполнена");
            auditController.info(BufferModeSyncTask, format("Перенесено проводок из буфера в архив: %s"
                    , synchronizationController.moveGLPdsToHistory(operdayController.getOperday().getCurrentDate())));
        } catch (Exception e) {
            auditController.error(BufferModeSyncTask, "Ошибка синхронизации проводок/оборотов", null, e);
        }
    }
}
