package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.excel.BatchProcessResult;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.entity.sec.AuditRecord;
import ru.rbt.barsgl.ejb.integr.bg.ManualOperationController;
import ru.rbt.barsgl.ejb.repository.BatchPackageRepository;
import ru.rbt.barsgl.ejb.repository.BatchPostingRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.shared.enums.BatchPostAction;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.InvisibleType;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static java.lang.String.format;
import static ru.rbt.barsgl.shared.enums.BatchPostAction.CONFIRM;
import static ru.rbt.barsgl.shared.enums.BatchPostAction.CONFIRM_NOW;
import static ru.rbt.barsgl.shared.enums.BatchPostStatus.*;

/**
 * Created by ER18837 on 01.07.16.
 */
public class PreCobBatchPostingTask  implements ParamsAwareRunnable {
    private static final Logger logger = Logger.getLogger(PreCobBatchPostingTask.class.getName());

    @Inject
    private OperdayController operdayController;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private AuditController auditController;

    @EJB
    private BatchPostingRepository postingRepository;

    @EJB
    private ManualOperationController operationController;

    @Inject
    private BatchPackageRepository packageRepository;

    @Inject
    private MovementReceiveTask movementReceiveTask;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        // TODO проверить запуск задачи ? история?
        executeWork();
    }

    public void executeWork() {
        final Operday operday = operdayController.getOperday();
        Date curdate = operday.getCurrentDate();
        try {
            auditController.info(AuditRecord.LogCode.PreCob, format("Начало обработки запросов на операцию с неподтвержденной датой за текущий ОД '%s'",
                    dateUtils.onlyDateString(curdate)));

            // проверить статус опердня, статус синхронизации
//            checkOperdayStatus(operday);

            // установить таймауты и статусы пакетов, ожидающих ответы
            processPostingsTimeout(operday);

            // обработать ручные запросы со статусом WAITDATE
            processManualWaitdate(operday);
            // обработать ручные запросы со статусом SIGNED, SIGNEDDATE
            processManualSigned(operday);
            // обработать пекетные запросы со статусом WAITDATE
            processPackagesWaitdate(operday);
            // обработать пекетные запросы со статусом SIGNED, SIGNEDDATE
            processPackagesSigned(operday);
            // обработать запросы со статусами WORKING
            processPostingsUnknown(operday);
            // сделать все остальные запросы невидимыми
            setPostingsInvisible(operday);

            auditController.info(AuditRecord.LogCode.PreCob, format("Успешное завершение обработки запросов на операцию с неподтвержденной датой за текущий ОД '%s'",
                    dateUtils.onlyDateString(curdate)));
        } catch (Throwable e) {
            auditController.error(AuditRecord.LogCode.PreCob
                    , format("Ошибка обработки запросов на операцию с неподтвержденной датой за текущий ОД '%s'",
                    dateUtils.onlyDateString(curdate)), null, e);
        }
    }

    /**
     * Устанавливает на запросы без ответов таймаут, отправляет пакеты с такими запросами на обработку
     * @param operday
     * @return
     * @throws Exception
     */
    private int processPostingsTimeout(Operday operday) throws Exception {
        return postingRepository.executeInNewTransaction(persistence -> {
            // найти запросы с таймаутом, изменить статус
            int cnt = movementReceiveTask.updatePostingsTimeout(operday.getCurrentDate(), 0);
            auditController.info(AuditRecord.LogCode.PreCob,
                    format("Установлен таймаут по запросам на операцию : %d", cnt));

            // найти пакеты, получившие все ответы, изменить статус
            cnt = movementReceiveTask.updatePackagesReceiveSrv(operday.getCurrentDate());
            auditController.info(AuditRecord.LogCode.PreCob,
                    format("Отправлены на обработку пакеты с запросами без ответов: %d", cnt));

            return cnt;
        });
    }


    /**
     * обработать ручные запросы со статусом WAITDATE
     * @param operday
     * @return
     * @throws Exception
     */
    private int processManualWaitdate(Operday operday) throws Exception {
        postingRepository.executeInNewTransaction(persistence ->
                    postingRepository.setPostingsWaitDate(operday.getCurrentDate())
        );
        return postingRepository.executeInNewTransaction(persistence -> {
            List<BatchPosting> postings = postingRepository.getPostingsWaitDate(operday.getCurrentDate());
            int errorCount = operationController.processPostings(postings);
            auditController.info(AuditRecord.LogCode.PreCob,
                    format("Обработано запросов на операцию (ручных) с неподтвержденной датой в текущий ОД: %d, из них с ошибкой: %d",
                    postings.size(), errorCount));

            return errorCount;
        });
    }

    /**
     * обработать ручные запросы со статусом SIGNED, SIGNEDDAGE
     * @param operday
     * @return
     * @throws Exception
     */
    private int processManualSigned(Operday operday) throws Exception {
        return postingRepository.executeInNewTransaction(persistence -> {
            List<BatchPosting> postings = postingRepository.getPostingsSigned(operday.getCurrentDate());
            int errorCount = operationController.processPostings(postings);
            auditController.info(AuditRecord.LogCode.PreCob,
                    format("Дообработано подтвержденных запросов на операцию (ручных): %d, из них с ошибкой: %d",
                            postings.size(), errorCount));

            return errorCount;
        });
    }

    /**
     * обработать пакетные запросы со статусом WAITDATE
     * @param operday
     * @return
     * @throws Exception
     */
    private int processPackagesWaitdate(Operday operday) throws Exception {
        Date curdate = operday.getCurrentDate();
        List<Long> packages = packageRepository.getPackagesWaitdate(curdate);
        int totalCount = 0;
        int errorCount = 0;

        for (Long packageId: packages) {
            postingRepository.executeInNewTransaction(persistence -> {
                packageRepository.setPackagePostingDate(packageId, curdate);
                return null;
            });
            BatchProcessResult result = operationController.processPackage(packageId, CONFIRM_NOW, WAITDATE, false);
            totalCount += result.getTotalCount();
            errorCount += result.getErrorCount();
        }
        auditController.info(AuditRecord.LogCode.PreCob,
                format("Обработано запросов на операцию с неподтвержденной датой из %d пакетов в текущий ОД: %d, из них с ошибкой: %d",
                        packages.size(), totalCount, errorCount));
        return errorCount;
    }

    /**
     * обработать пакетные запросы со статусом SIGNED, SIGNEDDAGE
     * @param operday
     * @return
     * @throws Exception
     */
    private int processPackagesSigned(Operday operday) throws Exception {
        Date curdate = operday.getCurrentDate();
        List<Long> packages = packageRepository.getPackagesSigned(curdate);
        int totalCount = 0;
        int errorCount = 0;

        for (Long packageId: packages) {
            BatchPosting posting = packageRepository.getOnePostingSigned(packageId);
            if (null == posting)
                continue;
            BatchPostStatus status = posting.getStatus();
            BatchPostAction action = operdayController.getOperday().getCurrentDate().equals(posting.getPostDate()) ? CONFIRM_NOW : CONFIRM;
            BatchProcessResult result = operationController.processPackage(packageId, action, status, false);
            totalCount += result.getTotalCount();
            errorCount += result.getErrorCount();
        }
        auditController.info(AuditRecord.LogCode.PreCob,
                format("Обработано запросов на операцию с подтвержденной датой из %d пакетов в текущий ОД: %d, из них с ошибкой: %d",
                        packages.size(), totalCount, errorCount));
        return errorCount;
    }

    /**
     * Обработка запросов со статусом WORKING по которым есть проводки
     * @param operday
     * @return
     */
    private int processPostingsUnknown(Operday operday) throws Exception {
        final BatchPostStatus statusOld = WORKING;
        List<DataRecord> postingsList = postingRepository.select("select ID, STATE from GL_BATPST b " +
                " where b.PROCDATE = ? and b.STATE = ? and b.INVISIBLE = ? and exists" +
                " (select 1 from GL_OPER o join GL_POSTING p  on p.GLO_REF = o.GLOID where" +
                " o.INP_METHOD in ('F', 'M') and o.PST_REF = b.ID and o.PROCDATE = ? and o.STATE = 'POST')",
                operday.getCurrentDate(), statusOld.name(), InvisibleType.N.name(), operday.getCurrentDate());
        if (null == postingsList)
            return 0;

        int count = 0;
        for (DataRecord data : postingsList) {
            final Long id = data.getLong(0);
            count += postingRepository.executeInNewTransaction(persistence -> {
                postingRepository.createPostingHistory(id, operdayController.getSystemDateTime(), null);
                return postingRepository.updatePostingStatusDeny(id, operdayController.getSystemDateTime(),
                        BatchPostStatus.COMPLETED, statusOld,
                        "Изменен статус процедурой закрытия дня – найдены проводки по данному сообщению");
                });
        }
        auditController.info(AuditRecord.LogCode.PreCob,
                format("Изменен статус запросов на операцию '%s' на '%s': %d", statusOld.name(), COMPLETED.name(), count));
        return postingsList.size();
    }

    /**
     * Сделать невидимыми все необработанные запросы
     * @param operday
     * @return
     */
    private int setPostingsInvisible(Operday operday) throws Exception {
        int count = postingRepository.executeInNewTransaction(persistence ->
            postingRepository.setUnprocessedPostingsInvisible(operdayController.getSystemDateTime(), operday.getCurrentDate()));
        auditController.info(AuditRecord.LogCode.PreCob,
                format("Для необработанных запросов на операцию установлен признак 'INVISIBLE' в 'S': %d", count));
        return count;
    }
}
