package ru.rbt.barsgl.ejb.controller.operday;

import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.cob.CobStepResult;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.bg.FanForwardOperationController;
import ru.rbt.barsgl.ejb.integr.bg.FanOperationController;
import ru.rbt.barsgl.ejb.integr.bg.FanStornoBackvalueOperationController;
import ru.rbt.barsgl.ejb.integr.bg.FanStornoOnedayOperationController;
import ru.rbt.barsgl.ejb.repository.BackvalueJournalRepository;
import ru.rbt.barsgl.ejb.repository.GLOperationRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejbcore.AsyncProcessor;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.ejbcore.JpaAccessCallback;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejb.repository.properties.PropertiesRepository;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.Assert;
import ru.rbt.barsgl.shared.enums.CobStepStatus;
import ru.rbt.barsgl.shared.enums.OperState;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.all;
import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.PreCob;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.ejbcore.validation.ValidationError.initSource;

/**
 * Created by Ivan Sevastyanov
 * Шаги выполнения операций перед закрытием операционного дня
 */
@Stateless
@LocalBean
public class PreCobStepController {

    private static final Logger log = Logger.getLogger(PreCobStepController.class);

    @EJB
    private GLOperationRepository operationRepository;

    @EJB
    private OperdayController operdayController;

    @EJB
    private FanForwardOperationController fanForwardOperationController;

    @EJB
    private FanStornoBackvalueOperationController fanStornoBackvalueOperationController;

    @EJB
    private FanStornoOnedayOperationController fanStornoOnedayOperationController;

    @Inject
    private DateUtils dateUtils;

    @Inject
    private BackvalueJournalRepository backvalueJournalRepository;

    @EJB
    private BeanManagedProcessor beanManagedProcessor;

    @EJB
    private AuditController auditController;

    @EJB
    private AsyncProcessor asyncProcessor;

    @EJB
    private PropertiesRepository propertiesRepository;

    /**
     * Обработка вееров в preCob (основная)
     */
    public CobStepResult processFan() {
        Date operday = new Date(operdayController.getOperday().getCurrentDate().getTime());
        return processFan(operday);
    }

    public CobStepResult processFan(Date operday) {
        try {
            Long cnt = operationRepository.getFanOperationLoad(operdayController.getOperday().getCurrentDate());
            if (cnt > 0) {
                return beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
                    String msg = format(" обработки вееров актуальных на дату '%s'"
                            , dateUtils.onlyDateString(operdayController.getOperday().getCurrentDate()));
                    auditController.info(PreCob, "Начало" + msg);

                    auditController.info(PreCob, "Обработка вееров СТОРНО с текущей датой");
                    int cnt1 = processStornoOnedayOperations(operday, true);

                    int cnt2 = processForwardOperations(operday, true);
                    auditController.info(PreCob, format("Обработано повторно (без учета ошибок) вееров НЕ СТОРНО %d", cnt2));

                    auditController.info(PreCob, "Обработка вееров СТОРНО с прошедшей датой");
                    int cnt3 = processStornoBackvalueOperations(operday, true);

                    auditController.info(PreCob, "Успешное завершение " + msg);

                    Long cntOk = operationRepository.getFanOperationProcessed(operdayController.getOperday().getCurrentDate());
                    return new CobStepResult(CobStepStatus.Success, format("Найдено вееров %d. Обработано вееров %d", cnt, cntOk));
                }, 60 * 60);
            } else {
                return new CobStepResult(CobStepStatus.Skipped, format("Нет вееров для обработки"));
            }
        } catch (Exception e) {
            auditController.error(PreCob, "Ошибка на шаге обработки вееров: ", null, e);
            return new CobStepResult(CobStepStatus.Error, "Ошибка на шаге обработки вееров", e.getMessage());
        }
    }

    /**
     * Обработка вееров с WTAC после открытия нового опердня (повторная)
     */
    public int reprocessWtacFan(Date operday) {
        try {
            return beanManagedProcessor.executeInNewTxWithTimeout((persistence, connection) -> {
                String msg = format(" повторной обработки вееров актуальных на дату '%s'"
                        , dateUtils.onlyDateString(operdayController.getOperday().getCurrentDate()));
                List<GLOperation> res = new ArrayList<GLOperation>();
                int cntError = 0;
                auditController.info(PreCob, "Начало" + msg);

                auditController.info(PreCob, format("Обработано повторно (без учета ошибок) вееров НЕ СТОРНО %s"
                        , processForwardOperations(operday, false)));

                auditController.info(PreCob, "Повторная обработка вееров СТОРНО с прошедшей датой");
                cntError += processStornoBackvalueOperations(operday, false);

                auditController.info(PreCob, "Успешное завершение" + msg);
                return cntError;
            }, 60 * 60);
        } catch (Throwable e) {
            auditController.error(PreCob, "Ошибка на шаге повторной обработки вееров: ", null, e);
            return 0;
        }
    }

    /**
     * Обрабатывает операции текущего дня
     * @param operday           - орпердень
     * @param isWtacPreStage    - true =первая обработка (пропустить WTAC)
     * @return кол-во обработанных склеек
     * @throws SQLException
     */
    private int processForwardOperations(Date operday, boolean isWtacPreStage) throws Exception {
        return operationRepository.executeTransactionally(connection -> {
            final int[] currentCount = {0};
            final int fanConcurrency = propertiesRepository.getNumber("fan.concurency").intValue();
            final int fanBatchsize = propertiesRepository.getNumber("fan.batchsize").intValue();
            try (PreparedStatement query =
                         connection.prepareStatement(
                                 "select DISTINCT PAR_RF from GL_OPER where FAN = 'Y' and STRN = 'N' " +
                                         "and PROCDATE = ? and STATE = ?")) {
                query.setDate(1, operday);
                query.setString(2, isWtacPreStage ? OperState.LOAD.name() : OperState.WTAC.name());
                List<String> refs = new ArrayList<>();
                try (ResultSet rs = query.executeQuery()) {
                    while (rs.next()) {
                        refs.add(rs.getString(1));
                        if (fanBatchsize == refs.size()) {
                            processFansAsync(refs, isWtacPreStage, fanConcurrency);
                            refs.clear();
                        }
                    }
                }
                if (!refs.isEmpty()) {
                    processFansAsync(refs, isWtacPreStage, fanConcurrency);
                }
            }
            return currentCount[0];
        });
    }

    private void processFansAsync(List<String> refs, boolean isWtacPreStage, int fanConcurrency) throws Exception {
        List<JpaAccessCallback<Boolean>> callbacks = refs.stream()
                .map(s -> (JpaAccessCallback<Boolean>) persistence
                        -> processFanOperation(s, fanForwardOperationController, isWtacPreStage)).collect(Collectors.toList());
        asyncProcessor.asyncProcessPooled(callbacks, fanConcurrency, 5L, TimeUnit.MINUTES);
    }

    /**
     * Обрабатывает сторно текущего дня
     * @param operday           - орпердень
     * @param isWtacPreStage    - true =первая обработка (пропустить WTAC)
     * @throws SQLException
     */
    private int processStornoOnedayOperations(Date operday, boolean isWtacPreStage) throws Exception {
        return operationRepository.executeTransactionally(connection -> {
            boolean res;
            int cnt = 0;
            try (PreparedStatement query =
                         connection.prepareStatement(
//                                 "select DISTINCT PAR_RF from GL_OPER where FAN = 'Y' and STRN = 'Y' " +
//                                         " and POSTDATE = PROCDATE and PROCDATE = ? and STATE = ?")) {
                                "select DISTINCT o1.PAR_RF from GL_OPER o1 left join GL_OPER o2 on o1.STRN_GLO = o2.GLOID" +
                                    " where o1.FAN = 'Y' and o1.STRN = 'Y' and o1.PROCDATE = ? and o1.STATE = ?" +
                                    " and o1.POSTDATE = o1.PROCDATE and o2.POSTDATE = o1.POSTDATE")) {
                query.setDate(1, operday);
                query.setString(2, isWtacPreStage ? OperState.LOAD.name() : OperState.WTAC.name());

                try (ResultSet rs = query.executeQuery()) {
                    while (rs.next()) {
                        String parentRef = rs.getString(1);
                        res = processFanOperation(parentRef, fanStornoOnedayOperationController, isWtacPreStage);
                        if (res)
                            cnt++;
                    }
                }
            }
            return cnt;
        });
    }

    /**
     * Обрабатывает сторно текущего дня
     * @param operday           - орпердень
     * @param isWtacPreStage    - true =первая обработка (пропустить WTAC)
     * @throws SQLException
     */
    private int processStornoBackvalueOperations(Date operday, boolean isWtacPreStage) throws Exception {
        return operationRepository.executeTransactionally(connection -> {
            boolean res;
            int cnt = 0;
            try (PreparedStatement query =
                         connection.prepareStatement(
//                                 "select DISTINCT PAR_RF from GL_OPER where FAN = 'Y' and STRN = 'Y' " +
//                                         "and POSTDATE < PROCDATE and PROCDATE = ? and STATE = ?")) {
                                 "select DISTINCT o1.PAR_RF from GL_OPER o1 left join GL_OPER o2 on o1.STRN_GLO = o2.GLOID" +
                                         " where o1.FAN = 'Y' and o1.STRN = 'Y' and o1.PROCDATE = ? and o1.STATE = ?" +
                                         " and (o1.POSTDATE < o1.PROCDATE or o2.POSTDATE != o1.POSTDATE)")) {
                query.setDate(1, operday);
                query.setString(2, isWtacPreStage ? OperState.LOAD.name() : OperState.WTAC.name());

                try (ResultSet rs = query.executeQuery()) {
                    while (rs.next()) {
                        String parentRef = rs.getString(1);
                        res = processFanOperation(parentRef, fanStornoBackvalueOperationController, isWtacPreStage);
                        if (res)
                            cnt++;
                    }
                }
            }
            return cnt;
        });
    }

    /**
     * Обрабатывает веерную операцию
     * @param parentRef                 - ключ веера
     * @param fanOperationController    - контроллер для обработки операции
     * @param isWtacPreStage            - true =первая обработка (пропустить WTAC)
     * @throws SQLException
     * @return                          - true - операция обработана успешно
     */
    private boolean processFanOperation(String parentRef, FanOperationController fanOperationController, boolean isWtacPreStage) throws SQLException {
        boolean res = false;
        String msg = format(" обработки частичных операций веера по референсу '%s'", parentRef);
        auditController.info(PreCob, "Начало" + msg);
        try {
            boolean toContinue = validateFanOperation(parentRef, fanOperationController, isWtacPreStage);
            if (toContinue) {
                // обработка в своей транзакции
                res = operationRepository.executeInNewTransaction(persistence -> {
                    List<GLOperation> operList = fanOperationController.processOperations(parentRef);
                    return !operList.isEmpty();
                });
            }
            auditController.info(PreCob, "Успешное завершение" + msg);
        } catch (Throwable e) {
            auditController.info(PreCob, "Ошибка " + msg);
        }
        return res;
    }

    /**
     * Проверяет корректность статусов всех операций веера
     * @param parentRef                 - ключ веера
     * @param fanOperationController    - контроллер для обработки операции
     * @param isWtacPreStage            - true =первая обработка (пропустить WTAC)
     * @return
     */
    private boolean validateFanOperation(String parentRef, FanOperationController fanOperationController, boolean isWtacPreStage) {
        boolean toContinue = false;
        YesNo storno = fanOperationController.getStorno();
        // получить все операции по вееру
        List<GLOperation> fans;
        try {
            // получить все операции по вееру
            fans = operationRepository.getFanOperationByRef(parentRef, storno);
        } catch (Throwable e) {
            fanOperationController.operationFanErrorMessage(e, format("Ошибка обработки веерных операций по референсу '%s'", parentRef),
                    null, parentRef, storno, OperState.ERPROC, initSource());
            return false;
        }
        try {
            // получить все входные сообщения по вееру
            List<EtlPosting> etl = operationRepository.getFanPostingByRef(parentRef, fanOperationController.getStorno());
            // убедиться, что все загружены нормально
            Assert.isTrue(all(etl, input -> 0 == input.getErrorCode())
                    , format("Для референса '%s' найдены веерные проводки, загруженные с ошибкой", parentRef));

            // получить все операции по вееру
//            List<GLOperation> fans = operationRepository.getFanOperationByRef(parentRef, fanOperationController.getStorno());
            auditController.info(PreCob, format("Для референса '%s' найдено '%s' частичных операций", parentRef, fans.size()));
            // Убедиться, что одинаковое число операций
            Assert.isTrue(etl.size() == fans.size()
                    , format("Для референса '%s' число частичных операций: %d не равно числу операций во входном сообщении: '%d'", parentRef, fans.size(), etl.size()));

            // убедиться, что все загружены нормально
            boolean isWtac = isPureWtac(fans);

            //  убедиться, что есть все счета
            if (isWtacPreStage && isWtac) { // первая обработка, есть WTAC
                toContinue = fanOperationController.isWtacEnabled();    // разрашение зависит от контроллера (только для Сторно Oneday
            }
            else {
                toContinue = true;          // нет WTAC или вторая обработка
            }

            // заполнить и проверить наличие филиалов (то есть счетов)
            if (toContinue) {
                toContinue = checkFilials(fans);
            }

        } catch (Throwable e) {
//            auditController.error(PreCob, format("Ошибка обработки веерных операций по референсу '%s'", parentRef), null, e);
            fanOperationController.operationFanErrorMessage(e, format("Ошибка обработки веерных операций по референсу '%s'", parentRef),
                    fans, parentRef, storno, OperState.ERPROC, initSource());
            return false;
        }

        return toContinue;
    }

    /**
     * Определяет, что среди операций есть WTAC
     * @param fans      - список операций
     * @return          - true, если есть WTAC. В случае ошибки генерит исключение
     */
    private boolean isPureWtac(List<GLOperation> fans) throws Exception {
        boolean isWtac = fans.stream().anyMatch(o -> OperState.WTAC == o.getState());
        List<GLOperation> failedOpers = fans.stream().filter(o
                -> OperState.WTAC != o.getState() && OperState.LOAD != o.getState()).collect(Collectors.toList());
        if (failedOpers.isEmpty()) {
            return isWtac;
        } else {
            throw new ValidationError(ErrorCode.FAN_INVALID_STATE
                    , failedOpers.get(0).getParentReference()
                    , OperState.LOAD.name(), failedOpers.stream()
                    .map(o -> format("id = '%s', state = '%s'", o.getId(), o.getState())).collect(Collectors.joining("; ", "<", ">")));
        }
    }

    /**
     * Заполняет и проверяет филиалы
     * @param fans      - список операций
     * @return          - true, если заполнены все филиалы (то есть все счета)
     * @throws Exception
     */
    private boolean checkFilials(List<GLOperation> fans) throws Exception {
        boolean isFilials = true;
        for (GLOperation operation : fans) {
            operationRepository.setFilials(operation);
            if ( isEmpty(operation.getFilialDebit()) ) {
                auditController.error(PreCob, format("Ошибка обработки веерной операции по референсу '%s'", operation.getParentReference()),
                        operation, format("Для частичной веерной операции '%d' нет филиала по дебету", operation.getId()));
                isFilials = false;
            }
            if ( isEmpty(operation.getFilialCredit()) ) {
                auditController.error(PreCob, format("Ошибка обработки веерной операции по референсу '%s'", operation.getParentReference()),
                        operation, format("Для частичной веерной операции '%d' нет филиала по кредиту", operation.getId()));
                isFilials = false;
            }
        }
        return isFilials;
    }

}
