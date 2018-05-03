package ru.rbt.barsgl.ejb.controller.operday.task;

import ru.rbt.audit.controller.AuditController;
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
import ru.rbt.barsgl.ejb.repository.WorkdayRepository;
import ru.rbt.barsgl.ejb.repository.props.ConfigProperty;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.barsgl.shared.enums.EnumUtils;
import ru.rbt.barsgl.shared.enums.ProcessingStatus;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.JpaAccessCallback;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.shared.ExceptionUtils;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang3.time.DateUtils.addDays;
import static org.apache.commons.lang3.time.DateUtils.truncate;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Package;
import static ru.rbt.audit.entity.AuditRecord.LogCode.*;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.BalanceMode.GIBRID;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.OperdayPhase.ONLINE;
import static ru.rbt.barsgl.ejb.common.mapping.od.Operday.PdMode.DIRECT;
import static ru.rbt.barsgl.ejb.entity.etl.EtlPackage.PackageState.*;
import static ru.rbt.barsgl.ejb.props.PropertyName.*;
import static ru.rbt.barsgl.shared.enums.ProcessingStatus.*;


/**
 * Created by Ivan Sevastyanov
 */
public class EtlStructureMonitorTask implements ParamsAwareRunnable {

    private static final Logger logger = Logger.getLogger(EtlStructureMonitorTask.class.getName());
    private static final Integer MANUAL_COUNT = 200;

    @Inject
    private EtlPackageRepository etlPackageRepository;

    @EJB
    private EtlPostingRepository etlPostingRepository;

    @EJB
    private EtlPostingController etlPostingController;

    @Inject
    private OperdayController operdayController;

    @Inject
    private ru.rbt.ejbcore.util.DateUtils dateUtils;

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

    @Inject
    private ProcessBatchOperationsTask processBatchOperationsTask;

    @Inject
    private ProcessBackValueOperationsTask processBackValueOperationsTask;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        executeWork(
                propertiesRepository.getNumber(ETLPKG_PROCESS_COUNT.getName()).intValue(),
                propertiesRepository.getNumber(BATPKG_PROCESS_COUNT.getName()).intValue(),
                propertiesRepository.getNumberDef(MANUAL_PROCESS_COUNT.getName(), MANUAL_COUNT.longValue() ).intValue(),
                isBatchAllowed());
    }

    public void executeWork() throws Exception {
        executeWork(5, 5, MANUAL_COUNT, true);
    }

    private boolean isBatchAllowed() {
        return "Y".equals(propertiesRepository.getStringDef(BATCH_PROCESS_ALLOWED.getName(), "Y").toUpperCase());
    }

    /**
     * обработка
     * @param etlPackageCount кол-во пакетов обработываемых за один раз
     * @throws Exception
     */
    public void executeWork(int etlPackageCount, int batPackageCount, int mnlPostingCount, boolean batchAllowed) throws Exception {
        if (checkOperdayState() && checkSetProcessingState()) {
            if (batchAllowed) {
                // обработка авторизованных ручных и пакетных операций
                processBatchOperationsTask.executeWork(batPackageCount, mnlPostingCount);
            }
            // обработка пакетов из АЕ
            processEtlPackages(etlPackageCount);
            // обработка авторизованных операций BackValue
            processBackValueOperationsTask.executeWork(mnlPostingCount);
        }
    }

    public void processEtlPackages(int packageCount) throws Exception {
        try {
            beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
                // T0: читаем пакеты в LOADED с UR чтоб исключить блокировку сортируем по дате берем максимально по умолчанию 10 (параметр)
                Date from = addDays(truncate(operdayController.getSystemDateTime(), Calendar.DATE), -7);
                Date to = addDays(truncate(operdayController.getSystemDateTime(), Calendar.DATE), 1);

            List<DataRecord> loadedPackages = etlPackageRepository.getPackagesForProcessing(packageCount, from, to
                        ,operdayController.getOperday().getCurrentDate());
                int cnt = loadedPackages.size();
            String msg = format("Предварительное кол-во пакетов АЕ для загрузки '%s' в режиме 'UR' с даты '%s' по '%s'"
                        , cnt, dateUtils.onlyDateString(from), dateUtils.onlyDateString(to));
                logger.info(msg);
                if (cnt > 0) {
                    auditController.info(Package, msg);
                String statistics = etlPackageRepository.getPackageStatistics(from, to);
                    logger.info("Необработанные пакеты:\n" + statistics);
                }
                // T0: теперь читаем пакет по ID и проверяем статус в режиме CS
                for (DataRecord pkgRecord : loadedPackages) {
                EtlPackage loadedPackage = etlPackageRepository.findById(EtlPackage.class, pkgRecord.getLong("ID_PKG"));
                if (LOADED == loadedPackage.getPackageState()) {
                    processEtlPackage(loadedPackage);
                    } else {
                        logger.info(format("Пакет '%s' в недопустимом статусе: '%s'", loadedPackage.getId(), loadedPackage.getPackageState()));
                    }
                }
                return null;
            }), 60 * 60);
        } catch (Throwable e) {
            auditController.error(Package, "Ошибка при выполнении задачи обработки входящих сообщений АЕ", null, e);
            throw e;
        }
    }

    public void processEtlPackage(EtlPackage loadedPackage) {
        logger.info(format("Обрабатывается пакет с ИД '%s' в транзакции: '%s'", loadedPackage.getId(), etlPostingRepository.getTransactionKey()));
        final long loadedPackageId = loadedPackage.getId();
        try {

            // T0: устанавливаем статус пакету WORKING
            etlPostingRepository.executeInNewTransaction(p -> {etlPackageRepository.updatePackageInprogress(loadedPackageId); return null;});

            // T0: читаем все провожки в пакете сортируем по ID
            List<EtlPosting> postings = etlPostingRepository.getPostingByPackage(loadedPackage, YesNo.N);
            logger.info(format("Проводок (не сторно) '%s' в пакете '%s'", postings.size(), loadedPackage.getId()));
            asyncProcessPostings(postings);

            postings = etlPostingRepository.getPostingByPackage(loadedPackage, YesNo.Y);
            logger.info(format("Проводок (сторно) '%s' в пакете '%s'", postings.size(), loadedPackage.getId()));
            asyncProcessPostings(postings);

            // устанавливаем статус на пакете по результатам обработки
            etlPostingRepository.executeInNewTransaction(p -> {setPackageState(loadedPackageId); return null;});
            logger.info(format("Обработка пакета с ИД '%s' завершена", loadedPackage.getId()));

            // переобработка ошибок по клиентским счетам
            reprocessClientFailedPostings(loadedPackage);
        } catch (Exception e) {
            auditController.error(Package, "Ошибка при обработке входного пакета", loadedPackage, e);
            try {
                etlPostingRepository.executeInNewTransaction(p -> {updatePackageStateError(loadedPackageId); return null;});
            } catch (Exception e1) {
                auditController.error(Package, "Ошибка при установке флага ошибки при обработке входного пакета", loadedPackage, e);
            }
        } finally {
            // pseudo online localization in DIRECT mode only
            try {
                if (DIRECT == operdayController.getOperday().getPdMode() && GIBRID != operdayController.getBalanceCalculationMode()) {
                    recalculateBackvalue(loadedPackage);
                }
            } catch (Throwable e) {
                auditController.error(Package, "Ошибка на принятии решения о проведении локализации по проводкам бэквалуе после окончания обработки пакета АЕ", null, e);
            }
        }
    }

    private void asyncProcessPostings(List<EtlPosting> postings) throws Exception {
        if(!postings.isEmpty()){
            long timeout = 1L;
            TimeUnit unit = TimeUnit.HOURS;
            final long tillTo = System.currentTimeMillis() + unit.toMillis(timeout);

            int maxConcurency = propertiesRepository
                    .getNumber(PD_CONCURENCY.getName()).intValue();
            
            ExecutorService executorService = asyncProcessor.getBlockingQueueThreadPoolExecutor(maxConcurency, maxConcurency, postings.size());

            postings.stream().forEach(posting -> {
                executorService.submit(() -> beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence, connection) -> {
                    try {
                        return etlPostingController.processMessage(posting);
                    } catch (Throwable e) {
                        logger.log(Level.SEVERE, format("Error on async processing of posting '%s'", posting.getId()), e);
                        etlPostingRepository.executeInNewTransaction(persistence0 -> {
                            etlPostingRepository.updatePostingStateError(posting, getErrorMessage(e));
                            return null;
                        });
                        return null;
                    }
                }));
            });

            asyncProcessor.awaitTermination(executorService, timeout, unit, tillTo);
        }
    }
    
    private void asyncProcessPostingsOld (List<EtlPosting> postings) throws Exception {
        List<JpaAccessCallback<GLOperation>> callbacks = postings.stream().map(
                posting -> (JpaAccessCallback<GLOperation>) persistence ->
                        beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence1, connection) -> {
                    try {
                        return etlPostingController.processMessage(posting);
                    } catch (Throwable e) {
                        logger.log(Level.SEVERE, format("Error on async processing of posting '%s'", posting.getId()), e);
                        etlPostingRepository.executeInNewTransaction(persistence0 -> {
                            etlPostingRepository.updatePostingStateError(posting, getErrorMessage(e));
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
        Long cntErrorPosting = etlPackageRepository.selectOne(Long.class
                , "select count(p) from EtlPosting p where p.etlPackage.id = ?1 and p.errorCode <> '0'"
                , etlPackageId);
        if (0 < cntErrorPosting) {
            etlPackageRepository.updatePackageStateProcessed(etlPackageId, ERROR, operdayController.getSystemDateTime());
        } else {
            etlPackageRepository.updatePackageStateProcessed(etlPackageId, PROCESSED, operdayController.getSystemDateTime());
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
        etlPackageRepository.updatePackageStateProcessed(etlPackageId, ERROR, operdayController.getSystemDateTime());
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
                etlPostingRepository.executeInNewTransaction(p -> {operdayController.setProcessingStatus(STOPPED); return null;});
                auditController.warning(Task, "Запрошена остановка обработки проводок", null, "Обработка остановлена");
            }
            return false;
        } else {
            if (ALLOWED == status) {
                etlPostingRepository.executeInNewTransaction(p -> {operdayController.setProcessingStatus(STARTED); return null;});
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
