package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.cob.CobStepResult;
import ru.rbt.barsgl.ejb.entity.etl.EtlPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLBackValueOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLPosting;
import ru.rbt.barsgl.ejb.entity.gl.GlPdTh;
import ru.rbt.barsgl.ejb.integr.oper.IncomingPostingProcessor;
import ru.rbt.barsgl.ejb.integr.oper.OrdinaryPostingProcessor;
import ru.rbt.barsgl.ejb.integr.pst.GLOperationProcessor;
import ru.rbt.barsgl.ejb.integr.pst.SimpleOperationProcessor;
import ru.rbt.barsgl.ejb.integr.pst.TechOperationProcessor;
import ru.rbt.barsgl.ejb.repository.*;
import ru.rbt.barsgl.ejb.security.GLErrorController;
import ru.rbt.barsgl.ejbcore.BeanManagedProcessor;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.barsgl.shared.enums.CobStepStatus;
import ru.rbt.barsgl.shared.enums.OperState;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.ExceptionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import java.sql.DataTruncation;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.Operation;
import static ru.rbt.barsgl.ejb.integr.ValidationAwareHandler.validationErrorsToString;
import static ru.rbt.barsgl.shared.enums.OperState.*;
import static ru.rbt.ejbcore.util.StringUtils.isEmpty;
import static ru.rbt.ejbcore.validation.ValidationError.initSource;

/**
 * Created by er18837 on 11.07.2017.
 */
abstract public class AbstractEtlPostingController implements EtlMessageController<EtlPosting, GLOperation> {

    private static final Logger log = Logger.getLogger(EtlPostingController.class);

    @Inject
    protected Instance<IncomingPostingProcessor> postingProcessors;

    @Inject
    protected Instance<GLOperationProcessor> operationProcessors;

    @Inject
    protected OrdinaryPostingProcessor ordinaryPostingProcessor;

    @Inject
    protected SimpleOperationProcessor simpleOperationProcessor;

    @Inject
    protected OperdayController operdayController;

    @EJB
    protected GLOperationRepository operationRepository;

    @Inject
    protected BackValueOperationRepository bvOperationRepository;

    @Resource
    protected EJBContext context;

    @EJB
    protected EtlPostingRepository etlPostingRepository;

    @Inject
    protected PdRepository pdRepository;

    @EJB
    protected AuditController auditController;

    @EJB
    protected GLErrorController errorController;

    @EJB
    protected BeanManagedProcessor beanManagedProcessor;

    protected List<IncomingPostingProcessor> cachedPostingProcessors;

    protected List<GLOperationProcessor> cachedOperationProcessors;

    @EJB
    protected GLAccountRepository glAccountRepository;

    @EJB
    protected GlPdThRepository glPdThRepository;

    /**
     * Вадидирует операцию. Если только ошибка "Нет счета" = статус WTAC, иначе ERCHK
     * @param operationProcessor    - процессор для обработки операции
     * @param operation             - операция
     * @param isWtacPreStage        - true - online обработка, false - обработка WTAC
     * @return                      - true - можно продолжить обработку
     */
    protected boolean validateOperation(GLOperationProcessor operationProcessor, GLOperation operation, boolean isWtacPreStage) throws Exception {
        List<ValidationError> errors = operationProcessor.validate(operation, new ValidationContext());
        boolean toContinue = errors.isEmpty();
        if (!toContinue) {
            String msgCommon = format(" операции: '%s' ID_PST: '%s'", operation.getId(), operation.getAePostingId());
            if (isWtacPreStage && isOpenLater(errors, operation)){
                Date operDay = operdayController.getOperday().getCurrentDate();
                Date lastWorkingDay = operdayController.getOperday().getLastWorkingDay();
                Calendar vdAdd30 = Calendar.getInstance();
                vdAdd30.setTime(operation.getValueDate());
                vdAdd30.add(Calendar.DATE, 30);
//                if (operdayController.getOperday().getCurrentDate().after(vdAdd30.getTime())) {
//                    getGlAccOpenDate(
////дата открытия счета >  LWDATE, то открытие=LWDATE
////иначе POST
//
//                }else dateOpen = ;

                for (ValidationError it : errors) {
                    if (it.getCode() == ErrorCode.ACCOUNT_DB_IS_OPEN_LATER) {
                        if (operDay.after(vdAdd30.getTime())) {
                            if (glAccountRepository.getGlAccOpenDate(operation.getAccountDebit()).after(lastWorkingDay)){
                                updateOpenDate(operation.getAccountDebit(), lastWorkingDay);
                            }
                        }else
                            updateOpenDate(operation.getAccountDebit(), operation.getValueDate());

                    } else if (it.getCode() == ErrorCode.ACCOUNT_CR_IS_OPEN_LATER) {
                        if (operDay.after(vdAdd30.getTime())) {
                            if (glAccountRepository.getGlAccOpenDate(operation.getAccountCredit()).after(lastWorkingDay)){
                                updateOpenDate(operation.getAccountCredit(), lastWorkingDay);
                            }
                        }else
                            updateOpenDate(operation.getAccountCredit(), operation.getValueDate());
                    }
                }
                toContinue = true;
            }else if (isWtacPreStage && isPureWtac(errors)) {
                if (operation.isBackValue()) {
                    // TODO: статус BWTAC
                    createOperationExt(operationProcessor, operation, BWTAC);
                } else
                if (operation.isStorno()                    // сторно в тот же день - продолжить
                        && operation.stornoOneday(operdayController.getOperday().getCurrentDate())) {
                    toContinue = true;
                } else {
                    String msg = "Отложена обработка" + msgCommon;
                    operationErrorMessage(errors, msg, operation, WTAC, false);

                }
            }
            else {
                String msg = "Ошибка валидации" + msgCommon;
                OperState errState = BWTAC.equals(operation.getState()) ? BERWTAC
                            : BLOAD.equals(operation.getState()) ? BERCHK
                            : ERCHK;
                operationErrorMessage(errors, msg, operation, errState, true);
            }
        }
        return toContinue;
    }

    /**
     * поменять дату открытия в gl_acc, accrln, bsaacc
     */
    protected void updateOpenDate(String bsaacid, Date vdate) throws Exception{
        glAccountRepository.updGlAccOpenDate(bsaacid, vdate);
        glAccountRepository.updBsaaccOpenDate(bsaacid, vdate);
        glAccountRepository.updAccrlnOpenDate(bsaacid, vdate);
    }

    /**
     * заполняет филиалы, главу баланса, сторно операцию, исправляет счета 9999
     * @param operation
     */
    protected void preProcessOperation(GLOperation operation) throws Exception {
        operationRepository.setFilials(operation);              // Филиалы
        operationRepository.setBsChapter(operation);            // Глава баланса
        if (!GLOperation.flagTechOper.equals(operation.getBsChapter())) {
            correctAccounts9999(operation);
        }
        simpleOperationProcessor.setStornoOperation(operation); // надо найти сторнируемую ДО определения типа процессора
    }

    private void correctAccounts9999(GLOperation operation) throws SQLException {
        String accountDebit = operation.getAccountDebit();
        String accountCredit = operation.getAccountCredit();
        String accountCorr = "";
        if (!isEmpty(operation.getAccountKeyDebit())) {
            ValidationError error = glAccountRepository.checkAccount9999(accountDebit, accountCredit, GLOperation.OperSide.D);
            if (null != error && error.getCode().equals(ErrorCode.ACCOUNT_NOT_CORRESP)) {
                accountCorr = glAccountRepository.getAccount9999Corr(accountDebit);
                if (!isEmpty(accountCorr)) {
                    operation.setAccountDebit(accountCorr);
                }
                return;
            }
        }
        if (!isEmpty(operation.getAccountKeyCredit())) {
            ValidationError error = glAccountRepository.checkAccount9999(accountCredit, accountDebit, GLOperation.OperSide.D);
            if (null != error && error.getCode().equals(ErrorCode.ACCOUNT_NOT_CORRESP)) {
                accountCorr = glAccountRepository.getAccount9999Corr(accountCredit);
                if (!isEmpty(accountCorr)) {
                    operation.setAccountCredit(accountCorr);
                }
            }
        }
    }

    /**
     * Проверяет что ошибки есть и среди них только ACCOUNT_NOT_FOUND
     */
    protected boolean isPureWtac(List<ValidationError> errors) {
        boolean wtac = errors.size() > 0;
        for (ValidationError error : errors)
            if (error.getCode().compareTo(ErrorCode.ACCOUNT_NOT_FOUND)!=0
//                error.getCode().compareTo(ErrorCode.ACCOUNT_IS_CLOSED_BEFOR)!=0 &&
//                error.getCode().compareTo(ErrorCode.ACCOUNT_DB_IS_OPEN_LATER)!=0 &&
//                error.getCode().compareTo(ErrorCode.ACCOUNT_CR_IS_OPEN_LATER)!=0
                    )
                wtac = false;
        return wtac;
    }

    private boolean isOpenLater(List<ValidationError> errors, GLOperation operation) {
        for(ValidationError it: errors){
            if (it.getCode() != ErrorCode.ACCOUNT_DB_IS_OPEN_LATER &&
                    it.getCode() != ErrorCode.ACCOUNT_CR_IS_OPEN_LATER){
                return false;
            }else if (!glAccountRepository.isExistsGLAccountByOpenType(
                    it.getCode().equals(ErrorCode.ACCOUNT_DB_IS_OPEN_LATER)? operation.getAccountDebit(): operation.getAccountCredit()))
                return false;
//                {
//               return glAccountRepository.isExistsGLAccountByOpenType(
//                       it.getCode().equals(ErrorCode.ACCOUNT_DB_IS_OPEN_LATER)? operation.getAccountDebit(): operation.getAccountCredit());
//               if (!glAccountRepository.isExistsGLAccountByOpenType(
//                 it.getCode().equals(ErrorCode.ACCOUNT_DB_IS_OPEN_LATER)? operation.getAccountDebit(): operation.getAccountCredit())
//                 )return false;
//                Calendar vdAdd30 = Calendar.getInstance();
//                vdAdd30.setTime(operation.getValueDate());
//                vdAdd30.add(Calendar.DATE, 30);
//                if (operdayController.getOperday().getCurrentDate().after(vdAdd30.getTime())) {
////дата открытия счета >  LWDATE, то открытие=LWDATE
////иначе POST
//                    return false;
//                }
        }
        return true;
    }

    /**
     * Создает операцию из входной проводки
     * @param etlPostingProcessor
     * @param posting
     * @return
     * @throws Exception
     */
    protected GLOperation createOperation(IncomingPostingProcessor etlPostingProcessor
            , EtlPosting posting) throws Exception {
        return beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence,connection) -> {
            String msgCommon = format(" АЕ: '%s', ID_PST: '%s'", posting.getId(), posting.getAePostingId());
            List<ValidationError> errors = etlPostingProcessor.validate(posting, new ValidationContext());

            if (errors.isEmpty()) {
                GLOperation operation = etlPostingProcessor.createOperation(posting);       // создать операцию

                operation.setState(OperState.LOAD);

                return operationRepository.save(operation);                            // сохранить операцию
            } else {
                String msg = "Ошибка при создании операции по проводке";
                postingErrorMessage (errors, msg + msgCommon, posting);
                return null;
//                throw new DefaultApplicationException(etlPostingProcessor.validationErrorMessage(posting, errors));
            }
        });
    }

    /**
     * создаем запись в таблице расширения GL_OPEREXT
     * @param operation
     * @return
     * @throws Exception
     */
    public GLBackValueOperation createOperationExt(GLOperationProcessor processor, GLOperation operation, OperState state) throws Exception {
        return beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence,connection) -> {
            GLBackValueOperation operationBackValue = (GLBackValueOperation)operation;  // backValueRepository.findById(GLBackValueOperation.class, operation.getId());
            processor.createOperationExt(operationBackValue);
            operation.setState(state);
            return bvOperationRepository.update(operationBackValue);
        });
    }

    /**
     * Заполняет дополнительные параметры операции
     * @param etlPostingProcessor
     * @param operation
     * @throws Exception
     */
    protected GLOperation enrichmentOperation(IncomingPostingProcessor etlPostingProcessor
            , GLOperation operation) throws Exception {
        return beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence,connection) -> {
            etlPostingProcessor.enrichment(operation);                                  // обогащение операции
            return operationRepository.update(operation);
        });
    }

    /**
     * Заполняет дополнительные параметры операции
     * @param etlPostingProcessor
     * @param operation
     * @throws Exception
     */
    protected GLOperation setDateParameters(IncomingPostingProcessor etlPostingProcessor
            , GLOperation operation) throws Exception {
        return beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence,connection) -> {
            etlPostingProcessor.setDateParameters(operation);                                  // обогащение операции
            return operationRepository.update(operation);
        });
    }

    /**
     * Ищем или создаем и заполняем счет в операции по одной стороне
     * @param operation
     * @return
     * @throws Exception
     */
    protected GLOperation fillAccount(GLOperationProcessor processor, GLOperation operation, GLOperation.OperSide operSide) throws Exception {
        return beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence,connection) -> {
            processor.createAccount(operation, operSide);
            return operationRepository.update(operation);
        });
    }

    /**
     * Обновляет поля операции: дата проводки, схема проводок, находит ссылки на операции, устанавливает спец параметры
     * @param operationProcessor
     * @param operation
     * @throws Exception
     */
    protected void updateOperation(GLOperationProcessor operationProcessor, GLOperation operation) throws Exception {
        beanManagedProcessor.executeInNewTxWithDefaultTimeout((persistence,connection) -> {
            operation.setProcDate(operdayController.getOperday().getCurrentDate());
            operation.setPstScheme(operationProcessor.getOperationType());              // определить схему операции
            operation.setCreationDate(operdayController.getSystemDateTime());
            operationProcessor.resolveOperationReference(operation);
            operationProcessor.setSpecificParameters(operation);
            operationRepository.update(operation);
            return null;
        });
    }

    /**
     * создаем проводки, мемордера, пересчет (локализация)
     * @param operationProcessor
     * @param operation
     * @throws Exception
     */
    protected void finalOperation(GLOperationProcessor operationProcessor, GLOperation operation) throws Exception {
        beanManagedProcessor.executeInNewTxWithDefaultTimeout((connection,persistence) -> {

            if (operationProcessor instanceof TechOperationProcessor) {

                TechOperationProcessor techOperationProcessor = (TechOperationProcessor) operationProcessor;
                List<GlPdTh> pdthList = techOperationProcessor.createPdTh(operation);
                glPdThRepository.processGlPdTh(operation,pdthList,OperState.POST);
            }
            else {
                List<GLPosting> pstList = operationProcessor.createPosting(operation);      // обработать операцию
                if (!pstList.isEmpty()) {                                                   // создать проводки
                    operationProcessor.resolvePostingReference(operation, pstList);
                    pdRepository.processPosting(pstList, operationProcessor.getSuccessStatus());                             // обработать / записать проводки
                } else {
                    operationRepository.updateOperationStatusSuccess(operation, operationProcessor.getSuccessStatus());
                }

            }
            return null;
        });
    }


    protected void operationErrorMessage(Throwable e, String msg, GLOperation operation, OperState state, String source) throws Exception {
        auditController.error(Operation, msg, operation, e);
        final String errorMessage = format("%s: \n%s Обнаружена: %s", msg, getErrorMessage(e), source);
        log.error(errorMessage, e);
        operationRepository.executeInNewTransaction(persistence -> {
            operationRepository.updateOperationStatusError(operation, state, errorMessage);
            return null;
        });
        errorController.error(msg, operation, e);
    }

    protected void operationErrorMessage(List<ValidationError> errors, String msg, GLOperation operation, OperState state, boolean isError) throws Exception {
        final String errorAudit = validationErrorsToString(errors);
        if (isError) {
            auditController.error(Operation, msg, operation, errorAudit);
        } else {
            auditController.warning(Operation, msg, operation, errorAudit);
        }
        final String errorMessage = msg + " \n" + errorAudit;
        log.error(errorMessage);
        operationRepository.executeInNewTransaction(persistence -> {
            operationRepository.updateOperationStatusError(operation, state, errorMessage);
            return null;
        });
        errorController.error(msg, operation, errors);
    }

    protected void postingErrorMessage (Throwable e, String msg, EtlPosting posting, String source) {
        auditController.error(Operation, msg, posting, e);
        errorController.error(msg, posting, e);
        final String errorMessage = format("%s \n%s Обнаружена: %s", msg, getErrorMessage(e), source);
        log.error(errorMessage, e);
        etlPostingRepository.updatePostingStateError(posting, errorMessage);
    }

    protected void postingErrorMessage (List<ValidationError> errors, String msg, EtlPosting posting) {
        final String errorDescr = format("Обнаружены ошибки валидации входных данных по проводке АЕ '%s'", posting.getId());
        String errorAudit = errorDescr + " \n" + validationErrorsToString(errors);
        auditController.error(Operation, msg, posting, errorAudit);
        errorController.error(msg  + " \n" + errorDescr, posting, errors);
        String errorMessage = msg + " \n" + errorAudit;
        log.error(errorMessage);
        etlPostingRepository.updatePostingStateError(posting, errorMessage);
    }

    /**
     * поиск обработчика проводки АЕ
     * @param posting
     * @return
     */
    protected IncomingPostingProcessor findPostingProcessor(EtlPosting posting) throws Exception {
        return findSupported(cachedPostingProcessors, p -> p.isSupported(posting)
                , () -> new DefaultApplicationException(format("Не найдено обработчика для проводоки АЕ '%s'", posting))
                , () -> new DefaultApplicationException(format("Найдено больше одного обработчика для проводоки АЕ '%s'", posting)));
    }

    /**
     * поиск обработчика для операции
     * @param operation
     * @return
     */
    public GLOperationProcessor findOperationProcessor(GLOperation operation) throws Exception {
        return findSupported(cachedOperationProcessors, p -> p.isSupported(operation)
                , () -> new DefaultApplicationException(format("Не найдено обработчика для операции '%s'", operation))
                , () -> new DefaultApplicationException(format("Найдено больше одного обработчика для операции '%s'", operation)));

    }

    protected String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, DefaultApplicationException.class);
    }

    /**
     * Повторный поиск, создание, заполнение счетов операции. Актуально при обработке операций в статусе <code>OperState.WTAC</code>.
     * @return <code>true</code> если не было ошибок, иначе <code>false</code>
     */
    protected boolean refillAccounts(GLOperation operation) throws Exception {
        try {
            GLOperationProcessor processor = findOperationProcessor(operation);
            operation = fillAccount(processor, operation, GLOperation.OperSide.D);
            operation = fillAccount(processor, operation, GLOperation.OperSide.C);
            return true;
        } catch (Exception e) {
            String msg = format("Ошибка поиска (создания) счетов при обработке отложенной операции '%s'", operation);
//            auditController.error(Operation, msg, operation, e);
            operationErrorMessage(e, msg, operation, ERCHK, initSource());
            return false;
        }
    }

    private <T> T findSupported(List<T> beans, Predicate<T> predicate
            , Supplier<? extends Exception> throwsNotFound
            , Supplier<? extends Exception> throwsTooMany) throws Exception {
        List<T> filtered = beans.stream().filter(predicate).collect(Collectors.toList());
        return 1 == filtered.size() ? filtered.get(0) : (filtered.isEmpty() ? throwsSupplied(throwsNotFound)
                : throwsSupplied(throwsTooMany));
    }

    private <T> T throwsSupplied(Supplier<? extends Exception> supplier) throws Exception {
        throw supplier.get();
    }

    @PostConstruct
    public void postConstruct() {
        cachedOperationProcessors = new ArrayList<>();
        cachedPostingProcessors = new ArrayList<>();
        for (GLOperationProcessor processor : operationProcessors) {
            cachedOperationProcessors.add(processor);
        }
        for (IncomingPostingProcessor processor : postingProcessors) {
            cachedPostingProcessors.add(processor);
        }
    }
}
