package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.entity.dict.LwdBalanceCut;
import ru.rbt.barsgl.ejb.entity.dict.LwdBalanceCutView;
import ru.rbt.barsgl.ejb.repository.dict.LwdBalanceCutRepository;
import ru.rbt.barsgl.ejb.repository.dict.LwdCutCachedRepository;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Date;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;

/**
 * Created by er18837 on 07.08.2017.
 */
public class CloseLwdBalanceCutTask implements ParamsAwareRunnable {

    @Inject
    private OperdayController operdayController;

    @Inject
    private CloseLastWorkdayBalanceTask closeLastWorkdayBalanceTask;

    @Inject
    private LwdCutCachedRepository lwdCutCachedRepository;

    @EJB
    private LwdBalanceCutRepository lwdBalanceCutRepository;

    @EJB
    private AuditController auditController;

    @Inject
    DateUtils dateUtils;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        executeWork();
    }

    public void executeWork() {
        final Operday operday = operdayController.getOperday();
        final Date currentDateTime = operdayController.getSystemDateTime();
        final Date currentDate = DateUtils.onlyDate(currentDateTime);

        LwdBalanceCutView cutRecord = lwdCutCachedRepository.getRecord();
        if (ONLINE == operday.getPhase()                                    // TODO нужна проверка на ONLINE ??
                && null != cutRecord                                        // есть запись
                && null == cutRecord.getCloseDateTime()                     // еще не закрывали из задачи
                && currentDate.equals(cutRecord.getRunDate())               // надо закрыть сегодня
                && !currentDateTime.before(cutRecord.getCutDateTime())) {   // пришло время!
            auditController.info(AuditRecord.LogCode.Operday, String.format("Требуется принудительное закрытие баланса предыдущего дня: дата '%s', время '%s'",
                    dateUtils.onlyDateString(cutRecord.getRunDate()), cutRecord.getCutTime()));
            if (OPEN == operday.getLastWorkdayStatus()) {
                try {
                    lwdBalanceCutRepository.executeInNewTransaction(persistence -> {
                        closeLastWorkdayBalanceTask.executeWork(true);
                        lwdBalanceCutRepository.updateCloseTimestamp(currentDate, currentDateTime);
                        lwdCutCachedRepository.flushCache();
                        return null;
                    });
                } catch (Throwable e) {
                    // пишем ошибку и выходим
                    auditController.error(AuditRecord.LogCode.Operday, format("Ошибка закрытия БАЛАНСА предыдущего ОД из задачи принудительного закрытия баланса. Текущий ОД '%s'"
                            , dateUtils.onlyDateString(operday.getCurrentDate())), null, e);
                }
            } else {
                auditController.info(AuditRecord.LogCode.Operday, "Баланс предыдущего дня уже был закрыт вне задачи принудительного закрытия");
                try {
                    lwdBalanceCutRepository.executeInNewTransaction(persistence -> {
                        lwdBalanceCutRepository.updateCloseTimestamp(currentDate, currentDateTime);
                        lwdCutCachedRepository.flushCache();
                        return null;
                    });
                } catch (Throwable e) {
                    // пишем ошибку и выходим
                    auditController.error(AuditRecord.LogCode.Operday, format("Ошибка закрытия БАЛАНСА предыдущего ОД из задачи принудительного закрытия баланса. Текущий ОД '%s'"
                            , dateUtils.onlyDateString(operday.getCurrentDate())), null, e);
                }
            }
        }


    }
}
