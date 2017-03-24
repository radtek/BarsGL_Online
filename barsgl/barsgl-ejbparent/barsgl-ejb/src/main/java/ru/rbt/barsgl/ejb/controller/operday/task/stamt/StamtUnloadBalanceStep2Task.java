package ru.rbt.barsgl.ejb.controller.operday.task.stamt;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.controller.operday.task.DwhUnloadStatus;
import ru.rbt.barsgl.ejb.controller.operday.task.TaskUtils;
import ru.rbt.barsgl.ejbcore.controller.etc.TextResourceController;
import ru.rbt.barsgl.ejb.repository.WorkprocRepository;
import ru.rbt.barsgl.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.CoreRepository;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.ejbcore.util.DateUtils;
import ru.rbt.barsgl.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.Assert;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static java.lang.String.format;
import static org.apache.commons.lang3.time.DateUtils.parseDate;
import static ru.rbt.barsgl.ejb.controller.operday.task.stamt.StamtUnloadController.STAMT_UNLOAD_FULL_DATE_KEY;
import static ru.rbt.barsgl.audit.entity.AuditRecord.LogCode.StamtUnload;
import static ru.rbt.barsgl.audit.entity.AuditRecord.LogCode.StamtUnloadBalDelta;

/**
 * Created by Ivan Sevastyanov on 27.01.2016.
 */
public class StamtUnloadBalanceStep2Task implements ParamsAwareRunnable {

    @EJB
    private CoreRepository repository;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private AuditController auditController;

    @Inject
    private StamtUnloadController unloadController;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private OperdayController operdayController;

    @Inject
    private TextResourceController textResourceController;

    @Inject
    private WorkprocRepository workprocRepository;

    @Inject
    private StamtUnloadBalanceTask unloadBalanceTask;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        Date executeDate = TaskUtils.getDateFromGLOD(properties, repository, operdayController.getOperday());
        auditController.info(StamtUnload
            , format("Начало выгрузки остатков для STAMT (шаг 2) за дату: '%s'", dateUtils.onlyDateString(executeDate)));

        long headerIdDelta = -1;
        try {
            if (checkRun(properties, executeDate, UnloadStamtParams.BALANCE_DELTA)) {
                headerIdDelta = unloadController.createHeader(executeDate, UnloadStamtParams.BALANCE_DELTA);
                auditController.info(StamtUnloadBalDelta
                    , format("Начало выгрузки остатков BACKDATE для STAMT за дату: '%s'", dateUtils.onlyDateString(executeDate)));
                auditController.info(StamtUnloadBalDelta
                    , format("Выгружено счетов с проводками BACKDATE для STAMT за дату: '%s', '%s'"
                                , dateUtils.onlyDateString(executeDate), unloadBalanceTask
                                        .fillDataDelta(executeDate, StamtUnloadBalanceTask.BalanceDeltaMode.Replace)));
                auditController.info(StamtUnloadBalDelta
                        , format("Выгружено лицевых счетов за дату: '%s', '%s'", dateUtils.onlyDateString(executeDate), fillLedgerAccountBalances(executeDate)));
                unloadController.setHeaderStatus(headerIdDelta, DwhUnloadStatus.SUCCEDED);
            }
        } catch (Exception e) {
            unloadController.setHeaderStatus(headerIdDelta, DwhUnloadStatus.ERROR);
            auditController.error(StamtUnload
                , format("Ошибка при выгрузке остатков BACKDATE в STAMT за '%s'", dateUtils.onlyDateString(executeDate)), null, e);
        }
        auditController.info(StamtUnload
            , format("Окончание выгрузки остатков для STAMT (шаг 2) за дату: '%s'", dateUtils.onlyDateString(executeDate)));
    }

    /**
     * остатки по лицевым счетам
     * @return
     * @throws Exception
     */
    private int fillLedgerAccountBalances(Date executeDate) throws Exception {
        try {
            return (int)repository.executeInNewTransaction(persistence -> {
                unloadController.createTemporaryTableWithDate("gl_tmp_curdate", "curdate", executeDate);
                return repository.executeNativeUpdate(textResourceController.getContent("ru/rbt/barsgl/ejb/etc/resource/stm/stmbal_delta_ledger.sql"));
            });
        } catch (Exception e) {
            auditController.error(StamtUnload, "Ошибка при выгрузке остатков по лицевым счетам", null, e);
            return 0;
        }
    }


    public boolean checkRun(Properties properties, Date executeDate, UnloadStamtParams params) throws Exception {
        try {
            if (TaskUtils.getCheckRun(properties, true)) {
                boolean isAlready = unloadController.getAlreadyHeaderCount(executeDate, params) > 0;
                Assert.isTrue(!isAlready, () -> new ValidationError(ErrorCode.STAMT_DELTA_ERR
                        , format("Выгрузка остатков по счетам (%s) для STAMT (шаг 2) в ОД '%s' невозможна"
                            , params.getParamName(), dateUtils.onlyDateString(executeDate))
                            , null, format("Выгрузка счетов уже произведена <%s>", isAlready)));

                final String stepName = Optional.ofNullable(
                        properties.getProperty("stepName")).orElse("P14").trim();
                final boolean isStepOk = workprocRepository.isStepOK(stepName, TaskUtils.getExecuteDate("operday", properties, executeDate));
                Assert.isTrue(isStepOk, () -> new ValidationError(ErrorCode.STAMT_DELTA_ERR
                        , format("Выгрузка остатков по счетам (%s) для STAMT (шаг 2) в ОД '%s' невозможна. Не завершен шаг '%s'"
                            , params.getParamName(), dateUtils.onlyDateString(executeDate), stepName)));
                unloadController.checkConsumed();
                return true;
            } else {
                return true;
            }
        } catch (ValidationError validationError) {
            auditController.warning(StamtUnload, "Ошибка выгрузки в STAMT (step 2)", null, validationError);
            return false;
        }
    }

}
