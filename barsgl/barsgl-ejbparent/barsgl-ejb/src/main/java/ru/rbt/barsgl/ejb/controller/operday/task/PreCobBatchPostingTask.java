package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.excel.BatchProcessResult;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.integr.bg.ManualOperationController;
import ru.rbt.barsgl.ejb.repository.BatchPackageRepository;
import ru.rbt.barsgl.ejb.repository.BatchPostingRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejbcore.util.DateUtils;
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
    private static final String DELIM = "; \n";

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

    public String executeWork() {
        final Operday operday = operdayController.getOperday();
        Date curdate = operday.getCurrentDate();
        try {
            auditController.info(AuditRecord.LogCode.PreCob, format("Начало обработки запросов на операцию с неподтвержденной датой за текущий ОД '%s'",
                    dateUtils.onlyDateString(curdate)));

            // проверить статус опердня, статус синхронизации
//            checkOperdayStatus(operday);

            // установить таймауты и статусы пакетов, ожидающих ответы
            String msg = processPostingsTimeout(operday);

            // обработать ручные запросы со статусом WAITDATE
            msg += processManualWaitdate(operday);
            // обработать ручные запросы со статусом SIGNED, SIGNEDDATE
            msg += processManualSigned(operday);
            // обработать пекетные запросы со статусом WAITDATE
            msg += processPackagesWaitdate(operday);
            // обработать пекетные запросы со статусом SIGNED, SIGNEDDATE
            msg += processPackagesSigned(operday);
            // обработать запросы со статусами WORKING
            msg += processPostingsUnknown(operday);
            // сделать все остальные запросы невидимыми
            msg += setPostingsInvisible(operday);

            String msg1;
            auditController.info(AuditRecord.LogCode.PreCob, msg1 = format("Успешное завершение обработки запросов на операцию с неподтвержденной датой за текущий ОД '%s'",
                    dateUtils.onlyDateString(curdate)));
            return msg + msg1;
        } catch (Throwable e) {
            String msg;
            auditController.error(AuditRecord.LogCode.PreCob
                    , msg = format("Ошибка обработки запросов на операцию с неподтвержденной датой за текущий ОД '%s'",
                    dateUtils.onlyDateString(curdate)), null, e);
            return msg;
        }
    }

    /**
     * Устанавливает на запросы без ответов таймаут, отправляет пакеты с такими запросами на обработку
     * @param operday
     * @return
     * @throws Exception
     */
    private String processPostingsTimeout(Operday operday) throws Exception {
        return postingRepository.executeInNewTransaction(persistence -> {
            String msg1 = "";
            // найти запросы с таймаутом, изменить статус
            int cnt1 = movementReceiveTask.updatePostingsTimeout(operday.getCurrentDate(), 0);
            auditController.info(AuditRecord.LogCode.PreCob,
                    msg1 = format("Установлен таймаут по запросам на операцию : %d", cnt1));

            // найти пакеты, получившие все ответы, изменить статус
            String msg2 = "";
            int cnt2 = movementReceiveTask.updatePackagesReceiveSrv(operday.getCurrentDate());
            auditController.info(AuditRecord.LogCode.PreCob,
                    msg2 = format("Отправлены на обработку пакеты с запросами без ответов: %d", cnt2));

            return cnt1+cnt2 > 0 ? msg1 + DELIM + msg2 + DELIM : "";
        });
    }


    /**
     * обработать ручные запросы со статусом WAITDATE
     * @param operday
     * @return
     * @throws Exception
     */
    private String processManualWaitdate(Operday operday) throws Exception {
        postingRepository.executeInNewTransaction(persistence ->
                    postingRepository.setPostingsWaitDate(operday.getCurrentDate())
        );
        return postingRepository.executeInNewTransaction(persistence -> {
            List<BatchPosting> postings = postingRepository.getPostingsWaitDate(operday.getCurrentDate());
            int errorCount = operationController.processPostings(postings);
            String msg;
            auditController.info(AuditRecord.LogCode.PreCob,
                    msg = format("Обработано запросов на операцию (ручных) с неподтвержденной датой в текущий ОД: %d, из них с ошибкой: %d",
                    postings.size(), errorCount));

            return postings.size() > 0 ? msg + DELIM : "";
        });
    }

    /**
     * обработать ручные запросы со статусом SIGNED, SIGNEDDAGE
     * @param operday
     * @return
     * @throws Exception
     */
    private String processManualSigned(Operday operday) throws Exception {
        return postingRepository.executeInNewTransaction(persistence -> {
            List<BatchPosting> postings = postingRepository.getPostingsSigned(operday.getCurrentDate());
            int errorCount = operationController.processPostings(postings);
            String msg;
            auditController.info(AuditRecord.LogCode.PreCob,
                    msg = format("Дообработано подтвержденных запросов на операцию (ручных): %d, из них с ошибкой: %d",
                            postings.size(), errorCount));

            return postings.size() > 0 ? msg + DELIM : "";
        });
    }

    /**
     * обработать пакетные запросы со статусом WAITDATE
     * @param operday
     * @return
     * @throws Exception
     */
    private String processPackagesWaitdate(Operday operday) throws Exception {
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
        String msg;
        auditController.info(AuditRecord.LogCode.PreCob,
                msg = format("Обработано запросов на операцию с неподтвержденной датой из %d пакетов в текущий ОД: %d, из них с ошибкой: %d",
                        packages.size(), totalCount, errorCount));
        return totalCount > 0 ? msg + DELIM : "";
    }

    /**
     * обработать пакетные запросы со статусом SIGNED, SIGNEDDAGE
     * @param operday
     * @return
     * @throws Exception
     */
    private String processPackagesSigned(Operday operday) throws Exception {
        Date curdate = operday.getCurrentDate();
        List<Long> packages = packageRepository.getPackagesSigned(curdate);
        int totalCount = 0;
        int errorCount = 0;

        for (Long packageId: packages) {
            BatchPosting posting = postingRepository.getOnePostingByPackageSigned(packageId);
            if (null == posting)
                continue;
            BatchPostStatus status = posting.getStatus();
            BatchPostAction action = operdayController.getOperday().getCurrentDate().equals(posting.getPostDate()) ? CONFIRM_NOW : CONFIRM;
            BatchProcessResult result = operationController.processPackage(packageId, action, status, false);
            totalCount += result.getTotalCount();
            errorCount += result.getErrorCount();
        }
        String msg;
        auditController.info(AuditRecord.LogCode.PreCob,
                msg = format("Обработано запросов на операцию с подтвержденной датой из %d пакетов в текущий ОД: %d, из них с ошибкой: %d",
                        packages.size(), totalCount, errorCount));
        return totalCount > 0 ? msg + DELIM : "";
    }

    /**
     * Обработка запросов со статусом WORKING по которым есть проводки
     * @param operday
     * @return
     */
    private String processPostingsUnknown(Operday operday) throws Exception {
        final BatchPostStatus statusOld = WORKING;
        List<DataRecord> postingsList = postingRepository.select("select ID, STATE from GL_BATPST b " +
                " where b.PROCDATE = ? and b.STATE = ? and b.INVISIBLE = ? and exists" +
                " (select 1 from GL_OPER o join GL_POSTING p  on p.GLO_REF = o.GLOID where" +
                " o.INP_METHOD in ('F', 'M') and o.PST_REF = b.ID and o.PROCDATE = ? and o.STATE = 'POST')",
                operday.getCurrentDate(), statusOld.name(), InvisibleType.N.name(), operday.getCurrentDate());
        if (null == postingsList)
            return "";

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
        String msg;
        auditController.info(AuditRecord.LogCode.PreCob,
                msg = format("Изменен статус запросов на операцию '%s' на '%s': %d", statusOld.name(), COMPLETED.name(), count));
        return count > 0 ? msg + DELIM : "";
    }

    /**
     * Сделать невидимыми все необработанные запросы
     * @param operday
     * @return
     */
    private String setPostingsInvisible(Operday operday) throws Exception {
        int count = postingRepository.executeInNewTransaction(persistence ->
            postingRepository.setUnprocessedPostingsInvisible(operdayController.getSystemDateTime(), operday.getCurrentDate()));
        String msg;
        auditController.info(AuditRecord.LogCode.PreCob,
                msg = format("Для необработанных запросов на операцию установлен признак 'INVISIBLE' в 'S': %d", count));
        return count > 0 ? msg + DELIM : "";
    }
}
