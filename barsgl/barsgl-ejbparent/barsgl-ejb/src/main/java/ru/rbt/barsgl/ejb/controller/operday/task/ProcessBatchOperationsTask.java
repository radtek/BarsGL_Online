package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.excel.BatchProcessResult;
import ru.rbt.barsgl.ejb.entity.etl.BatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.integr.bg.ManualOperationController;
import ru.rbt.barsgl.ejb.repository.BatchPackageRepository;
import ru.rbt.barsgl.ejb.repository.BatchPostingRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.shared.enums.BatchPackageState;
import ru.rbt.barsgl.shared.enums.BatchPostAction;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.BatchOperation;
import static ru.rbt.barsgl.shared.enums.BatchPostAction.CONFIRM;
import static ru.rbt.barsgl.shared.enums.BatchPostAction.CONFIRM_NOW;
import static ru.rbt.barsgl.shared.enums.BatchPostStatus.SIGNED;
import static ru.rbt.barsgl.shared.enums.BatchPostStatus.SIGNEDDATE;

/**
 * Created by ER18837 on 26.01.17.
 */
public class ProcessBatchOperationsTask implements ParamsAwareRunnable {
    private static final Logger logger = Logger.getLogger(ProcessBatchOperationsTask.class.getName());
    private static final Integer MANUAL_COUNT = 1000;
    private static final Integer BATPKG_COUNT = 100;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private AuditController auditController;

    @Inject
    private OperdayController operdayController;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

    @Inject
    private BatchPostingRepository postingRepository;

    @Inject
    private BatchPackageRepository batchPackageRepository;

    @EJB
    private ManualOperationController manualOperationController;


    @Override
    public void run(String jobName, Properties properties) throws Exception {
        executeWork();
    }

    public void executeWork() throws Exception {
        executeWork(BATPKG_COUNT, MANUAL_COUNT);
    }

    /**
     * обработка
     * @throws Exception
     */
    public void executeWork(int batPackageCount, int mnlPostingCount) throws Exception {
        if (checkOperdayStatus()) {
            // обработка ручных операций
            processManualOperations(mnlPostingCount);
            // обработка пакетов Excel
            processBatchPackages(batPackageCount);
        }
    }

    public void processBatchPackages(int packageCount) throws Exception {
        beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
            // T0: читаем пакеты в IS_SIGNED с UR чтоб исключить блокировку сортируем по дате берем максимально по умолчанию 10 (параметр)
            Date curdate = operdayController.getOperday().getCurrentDate();
            List<Long> loadedPackages = batchPackageRepository.getPackagesForProcessing(packageCount, curdate);
            int cnt = loadedPackages.size();
            String msg = format("Кол-во пакетов Excel для обработки '%s' в режиме 'UR' за дату '%s'"
                    , cnt, dateUtils.onlyDateString(curdate));
            logger.info(msg);
            if (cnt > 0) {
                auditController.info(BatchOperation, msg);
                String statistics = batchPackageRepository.getPackagesStatistics(curdate);
                logger.info("Необработанные пакеты:\n" + statistics);
                // T0: теперь читаем пакет по ID и проверяем статус в режиме CS
                for (Long packageId : loadedPackages) {
                    BatchPackage pkg = batchPackageRepository.findById(packageId);
                    BatchPostStatus status = (BatchPackageState.IS_SIGNED == pkg.getPackageState()) ? SIGNED : SIGNEDDATE;
                    BatchPostAction action = operdayController.getOperday().getCurrentDate().equals(pkg.getPostDate()) ? CONFIRM_NOW : CONFIRM;
                    BatchProcessResult result = manualOperationController.processPackage(packageId, action, status, false);
                }
            }
            return null;
        }), 60 * 60);
    }

    public void processManualOperations(int operCount) throws Exception {
        beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
            // T0: читаем операции в SIGNED, SIGNEDDATE с UR чтоб исключить блокировку сортируем по дате берем максимально по умолчанию 10 (параметр)
            Date curdate = operdayController.getOperday().getCurrentDate();
            List<Long> postingsId = postingRepository.getPostingsForProcessing(operCount, curdate);
            int cnt = postingsId.size();
            String msg = format("Кол-во ручных операций для обработки '%s' в режиме 'UR' за дату '%s'"
                    , postingsId.size(), dateUtils.onlyDateString(curdate));
            logger.info(msg);
            if (cnt > 0) {
                auditController.info(BatchOperation, msg);
                String statistics = batchPackageRepository.getPackagesStatistics(curdate);
                logger.info("Необработанные пакеты:\n" + statistics);
                List<BatchPosting> postings = postingRepository.getPostingsById(postingsId);
                manualOperationController.processPostings(postings);
//            auditController.info(AuditRecord.LogCode.PreCob,
//                    format("Обработано запросов на операцию (ручных): %d, из них с ошибкой: %d", cnt, errorCount));
            }
            return null;
        }), 60 * 60);
    }

    public boolean checkOperdayStatus() {
        Operday.OperdayPhase phase = operdayController.getOperday().getPhase();
        boolean allowed = operdayController.isProcessingAllowed();
        if (Operday.OperdayPhase.ONLINE != phase || !allowed) {
            auditController.error(BatchOperation, "Нельзя запустить задачу обработки пакетных/ручных операций", null,
                    String.format("Опердень в статусе %s, обработка %s", phase.name(), allowed ? "разрешена" : "запрещена"));
            return false;
        }
        return true;
    }

}
