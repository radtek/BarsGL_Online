package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.common.mapping.od.Operday;
import ru.rbt.barsgl.ejb.controller.BackvalueJournalController;
import ru.rbt.barsgl.ejb.entity.etl.EtlPackage;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.bg.EtlPostingController;
import ru.rbt.barsgl.ejb.integr.bg.EtlTechnicalPostingController;
import ru.rbt.barsgl.ejb.repository.EtlPackageRepository;
import ru.rbt.barsgl.ejb.repository.EtlPostingRepository;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.barsgl.ejb.repository.WorkdayRepository;
import ru.rbt.barsgl.ejb.repository.props.ConfigProperty;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.DefaultApplicationException;
import ru.rbt.barsgl.ejbcore.JpaAccessCallback;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.ejbcore.mapping.YesNo;
import ru.rbt.barsgl.ejbcore.repository.PropertiesRepository;
import ru.rbt.barsgl.shared.ExceptionUtils;
import ru.rbt.barsgl.shared.enums.EnumUtils;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang3.time.DateUtils.addDays;
import static org.apache.commons.lang3.time.DateUtils.truncate;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.barsgl.ejb.entity.etl.EtlPackage.PackageState.*;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.Package;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.Task;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.TechnicalPosting;
import static ru.rbt.barsgl.ejb.props.PropertyName.ETLPKG_PROCESS_COUNT;
import static ru.rbt.barsgl.ejb.props.PropertyName.PD_CONCURENCY;
import static ru.rbt.barsgl.shared.enums.ProcessingStatus.*;

/**
 * Created by Ivan Sevastyanov
 */
public class EtlStructureMonitorTask implements ParamsAwareRunnable {

    private static final Logger logger = Logger.getLogger(EtlStructureMonitorTask.class.getName());

    @Inject
    private EtlPackageRepository packageRepository;

    @EJB
    private EtlPostingRepository postingRepository;

    @EJB
    private EtlPostingController etlPostingController;

    @Inject
    private OperdayController operdayController;

    @Inject
    private ru.rbt.barsgl.ejbcore.util.DateUtils dateUtils;

    @Inject
    private GLOperationRepository operationRepository;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private AuditController auditController;

    @Inject
    private WorkdayRepository workdayRepository;

    @Inject
    private BackvalueJournalController journalController;

    @EJB
    private AsyncProcessor asyncProcessor;

    @EJB
    private PropertiesRepository propertiesRepository;

    @EJB
    private EtlTechnicalPostingController technicalPostingController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        executeWork(propertiesRepository.getNumber(ETLPKG_PROCESS_COUNT.getName()).intValue());
    }

    public void executeWork() throws Exception {
        executeWork(5);
    }

    /**
     * обработка
     * @param packageCount кол-во пакетов обработываемых за один раз
     * @throws Exception
     */
    public void executeWork(int packageCount) throws Exception {
        if (checkOperdayState() && checkSetProcessingState()) {
            beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
                // T0: читаем пакеты в LOADED с UR чтоб исключить блокировку сортируем по дате берем максимально по умолчанию 10 (параметр)
                Date from = addDays(truncate(operdayController.getSystemDateTime(), Calendar.DATE), -7);
                Date to = addDays(truncate(operdayController.getSystemDateTime(), Calendar.DATE), 1);

                List<DataRecord> loadedPackages = packageRepository.getPackagesForProcessing(packageCount, from, to
                        ,operdayController.getOperday().getCurrentDate());
                int cnt = loadedPackages.size();
                String msg = format("Предварительное кол-во пакетов для загрузки '%s' в режиме 'UR' с даты '%s' по '%s'"
                        , cnt, dateUtils.onlyDateString(from), dateUtils.onlyDateString(to));
                logger.info(msg);
                if (cnt > 0) {
                    auditController.info(Package, msg);
                    String statistics = packageRepository.getPackageStatistics(from, to);
                    logger.info("Необработанные пакеты:\n" + statistics);
                }
                // T0: теперь читаем пакет по ID и проверяем статус в режиме CS
                for (DataRecord pkgRecord : loadedPackages) {
                    EtlPackage loadedPackage = packageRepository.findById(EtlPackage.class, pkgRecord.getLong("ID_PKG"));
                    if (LOADED == loadedPackage.getPackageState()) {
                        processPackage(loadedPackage);
                    } else {
                        logger.info(format("Пакет '%s' в недопустимом статусе: '%s'", loadedPackage.getId(), loadedPackage.getPackageState()));
                    }
                }
                return null;
            }), 60 * 60);
        }
    }

    public void processPackage(EtlPackage loadedPackage) {
        logger.info(format("Обрабатывается пакет с ИД '%s' в транзакции: '%s'", loadedPackage.getId(), postingRepository.getTransactionKey()));
        final long loadedPackageId = loadedPackage.getId();
        try {

            // T0: устанавливаем статус пакету WORKING
            postingRepository.executeInNewTransaction(p -> {packageRepository.updatePackageInprogress(loadedPackageId); return null;});

            // T0: читаем все провожки в пакете сортируем по ID
            List<EtlPosting> postings = postingRepository.getPostingByPackage(loadedPackage, YesNo.N);
            logger.info(format("Проводок (не сторно) '%s' в пакете '%s'", postings.size(), loadedPackage.getId()));
            asyncProcessPostings(postings);

            postings = postingRepository.getPostingByPackage(loadedPackage, YesNo.Y);
            logger.info(format("Проводок (сторно) '%s' в пакете '%s'", postings.size(), loadedPackage.getId()));
            asyncProcessPostings(postings);

            // устанавливаем статус на пакете по результатам обработки
            postingRepository.executeInNewTransaction(p -> {setPackageState(loadedPackageId); return null;});
            logger.info(format("Обработка пакета с ИД '%s' завершена", loadedPackage.getId()));

            // переобработка ошибок по клиентским счетам
            reprocessClientFailedPostings(loadedPackage);
        } catch (Exception e) {
            auditController.error(Package, "Ошибка при обработке входного пакета", loadedPackage, e);
            try {
                postingRepository.executeInNewTransaction(p -> {updatePackageStateError(loadedPackageId); return null;});
            } catch (Exception e1) {
                auditController.error(Package, "Ошибка при установке флага ошибки при обработке входного пакета", loadedPackage, e);
            }
        } finally {
            // pseudo online localization in DIRECT mode only
            if (DIRECT == operdayController.getOperday().getPdMode()) {
                recalculateBackvalue(loadedPackage);
            }
        }
    }

    private void asyncProcessPostings (List<EtlPosting> postings) throws Exception {
        List<JpaAccessCallback<GLOperation>> callbacks = postings.stream().map(
                posting -> (JpaAccessCallback<GLOperation>) persistence ->
                        beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence1, connection) -> {
                    try {
                        return etlPostingController.processMessage(posting);
                    } catch (Throwable e) {
                        logger.log(Level.SEVERE, format("Error on async processing of posting '%s'", posting.getId()), e);
                        postingRepository.executeInNewTransaction(persistence0 -> {
                            postingRepository.updatePostingStateError(posting, getErrorMessage(e));
                            return null;
                        });
                        return null;
                    }
                })
        ).collect(Collectors.toList());
        asyncProcessor.asyncProcessPooled(callbacks, propertiesRepository
                .getNumber(PD_CONCURENCY.getName()).intValue(), 1, TimeUnit.HOURS);
    }

    private String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable, DataTruncation.class, SQLException.class, DefaultApplicationException.class);
    }

    private void setPackageState(long etlPackageId) {
        Long cntErrorPosting = packageRepository.selectOne(Long.class
                , "select count(p) cnt from EtlPosting p where p.etlPackage.id = ?1 and p.errorCode <> '0'"
                , etlPackageId);
        if (0 < cntErrorPosting) {
            packageRepository.updatePackageStateProcessed(etlPackageId, ERROR, operdayController.getSystemDateTime());
        } else {
            packageRepository.updatePackageStateProcessed(etlPackageId, PROCESSED, operdayController.getSystemDateTime());
        }
    }

    public boolean checkOperdayState () {
        Operday operday = operdayController.getOperday();
        if (ONLINE != operday.getPhase()) {
            auditController.warning(Task, "Обработка входящих сообщений невозможна", null
                    , format("Текущий опердень '%s' в статусе '%s'", dateUtils.onlyDateString(operday.getCurrentDate()), operday.getPhase()));
            return false;
        }
        // нельзя работать даже в онлайн если опердень GL равен опердню Майдас
        // проверка только для режима DIRECT
        if (DIRECT == operday.getPdMode()
                && Objects.equals(operday.getCurrentDate(), workdayRepository.getWorkday())) {
            auditController.warning(Task, "Обработка входящих сообщений невозможна", null
                    , format("Текущий опердень '%s' в статусе '%s'. Операционный день Майдас равен ОД GL."
                        , dateUtils.onlyDateString(operday.getCurrentDate()), operday.getPhase()));
            return false;
        }
        return true;
    }

    private void updatePackageStateError(long etlPackageId) {
        packageRepository.updatePackageStateProcessed(etlPackageId, ERROR, operdayController.getSystemDateTime());
    }

    /**
     * локализация и пересчет по журналу сформированному пакетом
     * @throws Exception
     */
    private void recalculateBackvalue(EtlPackage etlPackage) {
        try {
            logger.info(format("Начало пересчета/локализации по пакету: '%s'", etlPackage.getId()));
            beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) ->
                {journalController.recalculateBackvalueJournal(); return null;});
            logger.info(format("Успешное окончание пересчета/локализации по пакету: '%s'", etlPackage.getId()));
        } catch (Exception e) {
            auditController.error(Task, format("Ошибка при пересчете остатков БС2/ локализации ПО ПАКЕТУ '%s'. " +
                    "Записи не прошедшие пересчет/локализацию в таблице GL_BVJRNL.STATE = 'ERROR'", etlPackage.getId()), null, e);
        }
    }

    /**
     * проверяем возможность обработки
     * если ru.rbt.barsgl.shared.enums.ProcessingStatus#REQUIRED или ru.rbt.barsgl.shared.enums.ProcessingStatus#STOPPED
     * обработку не начинаем
     * если REQUIRED выставляем STOPPED
     * если ALLOWED выставляем STARTED
     * @return true, если обработка разрешена
     */
    private boolean checkSetProcessingState() throws Exception {
        ProcessingStatus status = operdayController.getProcessingStatus();
        if (EnumUtils.contains(new ProcessingStatus[]{REQUIRED, STOPPED}, status)) {
            auditController.warning(Task, "Обработка проводок запрещена", null, "");
            if (REQUIRED == status) {
                postingRepository.executeInNewTransaction(p -> {operdayController.setProcessingStatus(STOPPED); return null;});
                auditController.warning(Task, "Запрошена остановка обработки проводок", null, "Обработка остановлена");
            }
            return false;
        } else {
            if (ALLOWED == status) {
                postingRepository.executeInNewTransaction(p -> {operdayController.setProcessingStatus(STARTED); return null;});
                auditController.warning(Task, "Запрошен запуск обработки проводок", null, "Обработка запущена");
            }
            return true;
        }
    }

    private void reprocessClientFailedPostings(EtlPackage etlPackage) {
        try {
            if (propertiesRepository.getString(ConfigProperty.ClientTransitReprocess.getValue()).equals("1")) {
                auditController.info(TechnicalPosting, format("Переобработка ошибок по клиентским счетам"));
                int count = technicalPostingController.reprocessPostingByPackage(etlPackage);
                auditController.info(TechnicalPosting, format("Отправлено на переобработку '%s' ошибок по клиентским счетам", count));
            } else {
                auditController.warning(TechnicalPosting, "Переобработка ошибок по клиентским счетам запрещена", null, "смотри конфигурацию GL_PRPRP");
            }
        } catch (Throwable e) {
            auditController.error(TechnicalPosting, "Ошибка переобработки ошибок по клиентским счетам", etlPackage, e);
        }
    }


}
