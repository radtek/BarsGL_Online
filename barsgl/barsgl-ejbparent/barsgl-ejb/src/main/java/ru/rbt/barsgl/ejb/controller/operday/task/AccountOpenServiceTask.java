package ru.rbt.barsgl.ejb.controller.operday.task;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.entity.acc.GLAccountRequest;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountService;
import ru.rbt.barsgl.ejb.repository.GLAccountRequestRepository;
import ru.rbt.barsgl.ejb.security.AuditController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.datarec.DataRecord;
import ru.rbt.barsgl.ejbcore.job.ParamsAwareRunnable;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.List;
import java.util.Properties;

import static java.lang.String.format;
import static ru.rbt.barsgl.ejb.entity.sec.AuditRecord.LogCode.Account;


/**
 * Created by ER18837 on 16.10.15.
 */
public class AccountOpenServiceTask implements ParamsAwareRunnable {

    private static final Logger log = Logger.getLogger(AccountOpenServiceTask.class);

    private static int maxRequestCount = 10;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    GLAccountRequestRepository accountRequestRepository;

    @Inject
    GLAccountService glAccountService;

    @EJB
    private AuditController auditController;

    @Override
    public void run(String jobName, Properties properties) throws Exception {
        executeWork();
    }

    public void executeWork() throws Exception {
//        if (checkOperdayState()) {
            beanManagedProcessor.executeInNewTxWithTimeout(((persistence, connection) -> {
                // T0: читаем запросы с STATUS = 'NEW' с UR чтоб исключить блокировку сортируем по дате берем максимально по умолчанию 10 (параметр)
                List<DataRecord> loadedRequests = accountRequestRepository.getRequestForProcessing(maxRequestCount);
                String msg = format("Предварительное кол-во запросов на открытие счетов '%s'", loadedRequests.size());
                log.info(msg);
                // T0: теперь читаем пакет по ID и проверяем статус в режиме CS
                for (DataRecord reqRecord : loadedRequests) {
                    String id = reqRecord.getString("REQUEST_ID");
                    GLAccountRequest request = accountRequestRepository.findById(GLAccountRequest.class, id);
                    if (GLAccountRequest.RequestStatus.NEW == request.getStatus()) {
                        processRequest(request);
                    } else {
                        log.info(format("Запрос '%s' в недопустимом статусе: '%s'", request.getId(), request.getStatus()));
                    }
                }
                return null;
            }), 60 * 10);
//        }
    }

    private void processRequest(GLAccountRequest request) throws Exception {
        accountRequestRepository.executeInNewTransaction(persistence -> {
            auditController.info(Account, format("Начало обработки запроса открытия счета c ИД '%s'", request.getId()));
            try {
                glAccountService.createRequestAccount(request);
                auditController.info(Account, format("Обработка открытия счета с ИД '%s' завершена", request.getId()));
                return null;
            } catch (Exception e) {
                auditController.error(Account, "Ошибка при обработке запроса открытия счета", request, e);
                return null;
            }
        });
    }
}
