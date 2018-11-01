package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.controller.acc.AccountBatchStateController;
import ru.rbt.barsgl.ejb.entity.etl.AccountBatchPackage;
import ru.rbt.barsgl.ejb.repository.AccountBatchPackageRepository;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejbcore.datarec.DataRecord;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.List;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountBatch;
import static ru.rbt.barsgl.shared.enums.AccountBatchPackageState.IS_VALID;
import static ru.rbt.barsgl.shared.enums.AccountBatchPackageState.ON_VALID;

/**
 * Created by Ivan Sevastyanov on 01.11.2018.
 */
public class AccountBatchOpenTask implements ParamsAwareRunnable {

    @Inject
    private AccountBatchStateController batchStateController;

    @EJB
    private AuditController auditController;

    @EJB
    private AccountBatchPackageRepository batchPackageRepository;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        List<DataRecord> batPakages = batchPackageRepository.selectMaxRows("select id_pkg from gl_acbatpkg where state = ? order by id_pkg", 5
                , new Object[]{ON_VALID.name()});
        for (DataRecord id : batPakages) {
            if (validatePackage(id.getLong("id_pkg"))) {
                auditController.info(AccountBatch, format("Пакет '%s' прошел валидацию", id.getLong("id_pkg")));
                processPackage(id.getLong("id_pkg"));
                auditController.info(AccountBatch, format("Закончена обработка пакета '%s'", id.getLong("id_pkg")));
            }
        }
    }

    private boolean validatePackage(long batchPackageId) {
        try {
            return batchPackageRepository.executeInNewTransaction(persistence -> {
                AccountBatchPackage batchPackage = batchPackageRepository.findById(AccountBatchPackage.class, batchPackageId);
                if (ON_VALID == batchPackage.getState()) {
                    batchStateController.startValidation(batchPackage);
                }
                return batchPackageRepository.refresh(batchPackage).getState() == IS_VALID;
            });
        } catch (Exception e) {
            auditController.error(AccountBatch, format("Необработанная ошибка при валидации пакета '%s'", batchPackageId), null, e);
            return false;
        }
    }

    private void processPackage(long batchPackageId) {
        try {
            batchPackageRepository.executeInNewTransaction(persistence -> {
                AccountBatchPackage batchPackage = batchPackageRepository.findById(AccountBatchPackage.class, batchPackageId);
                batchStateController.startProcess(batchPackage);
                return null;
            });
        } catch (Exception e) {
            auditController.error(AccountBatch, format("Необработанная ошибка при обработке пакета '%s'", batchPackageId), null, e);
        }
    }
}
