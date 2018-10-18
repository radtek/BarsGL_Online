package ru.rbt.barsgl.ejb.controller.operday.task;

import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.entity.acc.GLAccountRequest;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountService;
import ru.rbt.barsgl.ejb.props.PropertyName;
import ru.rbt.barsgl.ejb.repository.GLAccountRequestRepository;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.datarec.DataRecord;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.AccountRetrieve;


/**
 * Created by ER18837 on 16.10.15.
 */
public class AccountOpenServiceTask implements ParamsAwareRunnable {

    private static final Logger log = Logger.getLogger(AccountOpenServiceTask.class);

//    private static int maxRequestCount = 30;

//    @EJB
//    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private GLAccountRequestRepository accountRequestRepository;

    @Inject
    private GLAccountService glAccountService;

    @EJB
    private AuditController auditController;

    @EJB
    private PropertiesRepository propertiesRepository;

    @EJB
    private AsyncProcessor asyncProcessor;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        executeWork();
    }

    public void executeWork() throws Exception {
        try {
            accountRequestRepository.executeInNewTransaction(persistence -> {
                int hoursOld = propertiesRepository.getNumber(PropertyName.SRV_ACCRETRIEVE_HOURS.getName()).intValue();
                int maxRequestCount = propertiesRepository.getNumber(PropertyName.SRV_ACCRETRIEVE_REQUESTS.getName()).intValue();
                int maxThreads = propertiesRepository.getNumber(PropertyName.SRV_ACCRETRIEVE_THREADS.getName()).intValue();

                List<DataRecord> loadedRequests = accountRequestRepository.getRequestForProcessing(maxRequestCount, hoursOld);
                log.info(format("Предварительное кол-во запросов на открытие счетов '%s'", loadedRequests.size()));
                // читаем пакет по ID и проверяем статус

                ExecutorService executor = asyncProcessor.getBlockingQueueThreadPoolExecutor(maxThreads, maxThreads, maxRequestCount);

                loadedRequests.forEach(r -> {
                    executor.submit(() -> {
                        try {
                            accountRequestRepository.executeInNewTransaction(persistence1 -> {
                                GLAccountRequest request = accountRequestRepository.findById(GLAccountRequest.class, r.getString("REQUEST_ID"));
                                if (GLAccountRequest.RequestStatus.NEW == request.getStatus()) {
                                    processRequest(request);
                                } else {
                                    auditController.warning(AccountRetrieve, format("Запрос '%s' в недопустимом статусе: '%s'", request.getId(), request.getStatus()));
                                }
                                return null;
                            });
                        } catch (Throwable e) {
                            auditController.error(AccountRetrieve, format("Ошибка при обработке запроса '%s' на открытие счета", r.getString("REQUEST_ID")), null, e);
                        }
                    });
                });
                asyncProcessor.awaitTermination(executor, 1, TimeUnit.HOURS, System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
                return null;
            });
        } catch (Throwable e) {
            auditController.error(AccountRetrieve, "Ошибка при выполнении задачи AccountRetrieve: " + e.getMessage(), null, e);
        }
    }

    private void processRequest(GLAccountRequest request) throws Exception {
        accountRequestRepository.executeInNewTransaction(persistence -> {
            auditController.info(AccountRetrieve, format("Начало обработки запроса открытия счета c ИД '%s'", request.getId()));
            try {
                glAccountService.createRequestAccount(request);
                auditController.info(AccountRetrieve, format("Обработка открытия счета с ИД '%s' завершена", request.getId()));
                return null;
            } catch (Exception e) {
                auditController.error(AccountRetrieve, "Ошибка при обработке запроса открытия счета", request, e);
                return null;
            }
        });
    }
}
