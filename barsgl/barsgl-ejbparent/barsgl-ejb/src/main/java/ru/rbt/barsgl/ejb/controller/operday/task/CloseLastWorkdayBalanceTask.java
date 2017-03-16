package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.integr.bg.EtlPostingController;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.LastWorkdayStatus.OPEN;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.Operday;

/**
 * Created by Ivan Sevastyanov
 * Закрытие баланса предыдущего операционного дня
 */
public class CloseLastWorkdayBalanceTask implements ParamsAwareRunnable {

    @Inject
    private OperdayController operdayController;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private EtlPostingController etlPostingController;

    @EJB
    private AuditController auditController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        executeWork(true);
    }

    public void executeWork(boolean withStorno) {
        final Operday operday = operdayController.getOperday();
        try {
            auditController.info(Operday, format("Закрытие баланса предыдущего ОД '%s'. Текущий ОД '%s'."
                    , dateUtils.onlyDateString(operday.getLastWorkingDay()), dateUtils.onlyDateString(operday.getCurrentDate())));

            checkOperdayStatus(operdayController.getOperday());

            if (withStorno) {
                etlPostingController.reprocessErckStorno(operday.getLastWorkingDay(), operday.getCurrentDate());
            }

            operdayController.closeLastWorkdayBalance();

            auditController.info(Operday, format("Успешное окончание закрытия баланса предыдущего ОД '%s'. Текущий ОД '%s'."
                    , dateUtils.onlyDateString(operday.getLastWorkingDay()), dateUtils.onlyDateString(operday.getCurrentDate())));
        } catch (Exception e) {
            auditController.error(Operday, format("Ошибка окончание закрытия баланса предыдущего ОД '%s'. Текущий ОД '%s'."
                    , dateUtils.onlyDateString(operday.getLastWorkingDay()), dateUtils.onlyDateString(operday.getCurrentDate())), null, e);
        }
    }

    private void checkOperdayStatus(Operday operday) {
        Assert.isTrue(OPEN == operday.getLastWorkdayStatus()
                , format("Баланс предыдущего операционного дня '%s' находится в статусе '%s'"
                , dateUtils.onlyDateString(operday.getLastWorkingDay()), operday.getLastWorkdayStatus()));
    }
}
