package ru.rbt.barsgl.ejb.integr.bg;

import org.apache.log4j.Logger;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.audit.entity.AuditRecord;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.controller.excel.BatchProcessResult;
import ru.rbt.barsgl.ejb.entity.acc.GLAccount;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.entity.gl.GLManualOperation;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.entity.gl.GlPdTh;
import ru.rbt.barsgl.ejb.integr.ValidationAwareHandler;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountController;
import ru.rbt.barsgl.ejb.integr.oper.BatchTechPostingProcessor;
import ru.rbt.barsgl.ejb.integr.oper.ManualOperationProcessor;
import ru.rbt.barsgl.ejb.integr.oper.MovementCreateProcessor;
import ru.rbt.barsgl.ejb.integr.pst.TechOperationProcessor;
import ru.rbt.barsgl.ejb.integr.struct.MovementCreateData;
import ru.rbt.barsgl.ejb.repository.*;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.barsgl.shared.ErrorList;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.barsgl.shared.enums.*;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.barsgl.shared.operation.ManualTechOperationWrapper;
import ru.rbt.ejbcore.DefaultApplicationException;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.gwt.security.ejb.repository.access.AccessServiceSupport;
import ru.rbt.security.ejb.repository.AppUserRepository;
import ru.rbt.security.ejb.repository.access.SecurityActionRepository;
import ru.rbt.shared.Assert;
import ru.rbt.shared.ExceptionUtils;
import ru.rbt.shared.enums.SecurityActionCode;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.math.BigDecimal;
import java.sql.DataTruncation;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.String.format;
import static ru.rbt.audit.entity.AuditRecord.LogCode.BatchOperation;
import static ru.rbt.audit.entity.AuditRecord.LogCode.ManualOperation;
import static ru.rbt.barsgl.ejb.controller.excel.BatchProcessResult.BatchProcessDate.*;
import static ru.rbt.barsgl.ejb.entity.dict.BankCurrency.RUB;
import static ru.rbt.barsgl.shared.enums.BatchPostAction.CONFIRM_NOW;
import static ru.rbt.barsgl.shared.enums.BatchPostStatus.*;
import static ru.rbt.ejbcore.util.StringUtils.*;
import static ru.rbt.ejbcore.validation.ErrorCode.*;
import static ru.rbt.ejbcore.validation.ValidationError.initSource;

/**
 * Created by er23851 on 04.04.2017.
 */
public class ManualTechOperationController extends ValidationAwareHandler<ManualTechOperationWrapper> {

    private static final Logger log = Logger.getLogger(ManualTechOperationController.class);
    private static final String postingName = "GL_BATPST";
    public static final SimpleDateFormat timeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    @Inject
    private BankCurrencyRepository bankCurrencyRepository;

    @EJB
    private AuditController auditController;

    @Inject
    private BatchTechPostingProcessor postingProcessor;

    @EJB
    private BatchPostingRepository postingRepository;

    @EJB
    private ManualOperationRepository manualOperationRepository;

    @Inject
    private AppUserRepository userRepository;

    @Inject
    private OperdayController operdayController;

    @EJB
    private PdRepository pdRepository;

    @Inject
    private MovementCreateProcessor movementProcessor;

    @Inject
    private UserContext userContext;

    @Inject
    private DateUtils dateUtils;

    @EJB
    private GLAccountController glAccountController;

    private SecurityActionRepository actionRepository;

    @EJB
    private GlPdThRepository glPdThRepository;

    @Inject
    private GLOperationRepository glOperationRepository;

    @Inject
    private ManualOperationProcessor manualOperationProcessor;

    @Inject
    private TechOperationProcessor techOperationProcessor;

    @Inject
    private AccessServiceSupport accessServiceSupport;


    public RpcRes_Base<ManualTechOperationWrapper> updateTechOperation(ManualTechOperationWrapper operationWrapper) {

        try {
            String msg = "Ошибка при редактировании проводки";
            try {
                Date newDate = dateUtils.onlyDateParse(operationWrapper.getPostDateStr());
                Date editDay = org.apache.commons.lang3.time.DateUtils.addDays(operdayController.getOperday().getCurrentDate(), -30); // TODO -30
                // нельзя установить дату ранее 30 дней назад
                if (editDay.after(newDate))
                    throw new ValidationError(ErrorCode.POSTING_BACK_GT_30, dateUtils.onlyDateString(editDay));
                // для пользователей с OperPstChngDate не надо проверять колич-во дней назад
                if (!actionRepository.getAvailableActions(operationWrapper.getUserId()).contains(SecurityActionCode.OperPstChngDate) ) {
                    Date oldDate = manualOperationRepository.findById(GLManualOperation.class, operationWrapper.getId()).getPostDate();
                    Date minDate = newDate.before(oldDate) ? newDate : oldDate;
                    accessServiceSupport.checkUserAccessToBackValueDate(minDate, operationWrapper.getUserId());
                }

            } catch (ValidationError e) {
                auditController.warning(ManualOperation, msg, "PD", operationWrapper.getId().toString(), e);
                return new RpcRes_Base<>( operationWrapper, true, msg);
            } catch (Exception e){
                auditController.error(ManualOperation, msg, "PD", operationWrapper.getId().toString(), e);
                return new RpcRes_Base<>(operationWrapper, true, e.getMessage());
            }

            updateOperation(operationWrapper);
            msg = "Успешно!";
            auditController.info(ManualOperation, msg, "GL_OPER", operationWrapper.getId().toString());
            return new RpcRes_Base<>( operationWrapper, false, msg);
        } catch (Exception e) {
            String errMessage = operationWrapper.getErrorMessage();
            return new RpcRes_Base<>(operationWrapper, true, errMessage);
        }
    }

    public RpcRes_Base<ManualTechOperationWrapper> saveTechOperation(ManualTechOperationWrapper operationWrapper){

        String msg = "Успешно!";
        return new RpcRes_Base<>( operationWrapper, false, msg);
    }

    private void updateOperation(ManualTechOperationWrapper operation) throws ParseException {

        List<ValidationError> errors = validate(operation, new ValidationContext());

        if (errors.isEmpty()) {
            try {
                Date newPostDate = dateUtils.onlyDateParse(operation.getPostDateStr());
                Date newValDate = dateUtils.onlyDateParse(operation.getValueDateStr());
                GLManualOperation glOperation = manualOperationRepository.findById(GLManualOperation.class, operation.getId());


                glOperation.setPostDate(newPostDate);
                glOperation.setValueDate(newValDate);
                glOperation.setNarrative(operation.getNarrative());
                glOperation.setRusNarrativeShort(operation.getRusNarrativeShort());
                glOperation.setRusNarrativeLong(operation.getRusNarrativeLong());

                manualOperationRepository.save(glOperation);

                List<GlPdTh> glPdThList = glPdThRepository.select(GlPdTh.class, "from GlPdTh p where p.glOperationId = " + glOperation.getId());

                if (!glPdThList.isEmpty()) {
                    for (GlPdTh pd : glPdThList) {
                        pd.setNarrative(operation.getNarrative());
                        pd.setRusNarrShort(operation.getRusNarrativeShort()!=null?operation.getRusNarrativeShort():operation.getRusNarrativeLong());
                        pd.setRusNarrLong(operation.getRusNarrativeLong());
                        pd.setVald(newValDate);
                        pd.setPod(newPostDate);
                        glPdThRepository.save(pd);
                    }
                }
            } catch (Exception ex) {
                operation.getErrorList().addNewErrorDescription(ex.getMessage(),"");
                throw  ex;
            }
        }
            else {
                for (ValidationError e:errors) {
                    operation.getErrorList().addNewErrorDescription(e.getMessage(),e.getCode().getStrErrorCode());
                }
                auditController.error(ManualOperation, "Не найдены проводки по операции", "GLOperation", operation.getId().toString(), "Не найдены проводки по операции");
            }
        }

    @Override
    public void fillValidationContext(ManualTechOperationWrapper target, ValidationContext context) {

       /* // Value Date
        context.addValidator(() -> {
            postingProcessor.checkDate(target, target.getValueDateStr(), "валютирования", false);
        });
        // Posting Date
        context.addValidator(() -> {
            postingProcessor.checkDate(target, target.getPostDateStr(), "проводки", true);
            Date pdate = postingProcessor.checkDateFormat(target.getPostDateStr(), "Дата проводки");
            Date vdate = postingProcessor.checkDateFormat(target.getValueDateStr(), "Дата валютирования");
            if ( (null != pdate )&& (null != vdate) && vdate.after(pdate)) {
                throw new ValidationError(POSTDATE_NOT_VALID,
                        target.getPostDateStr(),
                        target.getValueDateStr());
            }
        });*/

        // описания
        context.addValidator(() -> {
            String fieldName = "Основание ENG";
            String fieldValue = target.getNarrative();
            int maxLen = 300;
            if (null != fieldValue && maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        context.addValidator(() -> {
            String fieldName = "Основание RUS";
            String fieldValue = target.getRusNarrativeLong();
            int maxLen = 300;
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            } else if (maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });

    }

    /***
            * Обработка запроса от интерфейса на обработку запроса на операцию
     * @param wrapper
     * @return
             * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> processOperationRq(ManualTechOperationWrapper wrapper) throws Exception {

        try {
            if ((wrapper.getAction() == BatchPostAction.SAVE) || (wrapper.getAction() == BatchPostAction.SAVE_CONTROL))
            {
                checkTechAccount(wrapper);
            }
//            checkOperdayOnline(wrapper.getErrorList());
            switch (wrapper.getAction()) {
                case SAVE:              // сохранить - шаг 1 (INPUT)
                    return saveOperationRq(wrapper, BatchPostStatus.INPUT);
                case SAVE_CONTROL:      // сохранить и на подпись - шаг 1 (CONTROL)
                    return saveOperationRq(wrapper, BatchPostStatus.CONTROL);
                case CONTROL:           // на подпись - шаг 1 (CONTROL)
                    return forSignOperationRq(wrapper);
                case DELETE:            // удалить - шаг 1 (INVISIBLE)
                    return deleteOperationRq(wrapper);
                case UPDATE:            // изменить - шаг 1 (INPUT), шаг 2 (CONTROL)
                    return updateOperationRq(wrapper, wrapper.getStatus().getStep().isInputStep() ? BatchPostStatus.INPUT : BatchPostStatus.CONTROL);
                case UPDATE_CONTROL:    // изменить и на подпись - шаг 1 (CONTROL)
                    return updateOperationRq(wrapper, BatchPostStatus.CONTROL);
                case UPDATE_SIGN:       // изменить и подписать - шаг 2 (SIGNED, WAITDATE)
                {
                    updateOperationRq(wrapper, BatchPostStatus.CONTROL);
                    return authorizeOperationRq(wrapper);
                }
                case SIGN:              // подписать - шаг 2 (SIGNED, WAITDATE)
                    return authorizeOperationRq(wrapper);
                case CONFIRM:           // подтвердить прошлой датой - шаг 3 (SIGNEDDATE)
                    return confirmOperationRq(wrapper);
                case CONFIRM_NOW:       // подтвердить текущей датой - шаг 3 (SIGNEDDATE)
                    return confirmOperationRq(wrapper);
                case REFUSE:            // отказать - шаг 2 (REFUSE), 3 (REFUSEDATE)
                    return refuseOperationRq(wrapper, wrapper.getStatus().getStep().isControlStep() ? BatchPostStatus.REFUSE : BatchPostStatus.REFUSEDATE);    // TODO
            }
            return new RpcRes_Base<ManualOperationWrapper>(
                    wrapper, true, "Неверное действие");
        } catch (Throwable e) {
            String errorMsg = wrapper.getErrorMessage();
            String msg = "Ошибка обработки запроса на операцию";
            if (null != wrapper.getId())
                msg += " ID = " + wrapper.getId();
            if (!isEmpty(errorMsg)) {
                auditController.error(ManualOperation, msg, postingName, getWrapperId(wrapper), errorMsg);
                return new RpcRes_Base<>(wrapper, true, errorMsg);
            } else { //           if (null == validationEx && ) { // null == defaultEx &&
                addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
                auditController.error(ManualOperation, msg, postingName, getWrapperId(wrapper), e);
                return new RpcRes_Base<>(wrapper, true, e.getMessage());
            }
        }
    }

    private void manualOperationProcessed(BatchPosting posting) throws Exception {
        GLManualOperation operation = createOperation(posting);
        operation.setBsChapter("T");
        operation.setAmountPosting(operation.getAmountDebit());
        manualOperationRepository.save(operation,true);
        posting.setOperation(operation);
        posting.setProcDate(operdayController.getOperday().getCurrentDate());
        postingRepository.save(posting);
        List<GlPdTh> pdList = techOperationProcessor.createPdTh(operation);

        Long pcID = 0L;
        for (GlPdTh pd:pdList)
        {
            if (pd.getOperSide().equals(GLOperation.OperSide.D))
            {
                pcID = pd.getId();
            }
        }

        for (GlPdTh pd:pdList)
        {
            pd.setPcId(pcID);
            glPdThRepository.save(pd,true);
        }
    }

    public void setExchengeParameters (GLManualOperation operation) {
        // Основная валюта, сумма проводки в рублях
        BankCurrency ccyDebit = operation.getCurrencyDebit();
        BankCurrency ccyCredit = operation.getCurrencyCredit();

        // курсовая разница
        BigDecimal amountDebitRu = ( null != operation.getAmountDebitRu()) ?
                operation.getAmountDebitRu() : operation.getEquivalentDebit();
        BigDecimal amountCreditRu = ( null != operation.getAmountCreditRu()) ?
                operation.getAmountCreditRu() : operation.getEquivalentCredit();
        operation.setExchangeDifference(amountDebitRu.subtract(amountCreditRu));

        // основная валюта и сумма проводки
        if (RUB.equals(ccyDebit)) {         // Если ДЕБЕТ в рублях
            operation.setCurrencyMain(ccyDebit);        // основная валюта и сумма проводки по Дебету
            operation.setAmountPosting(operation.getAmountDebit());
        } else {                                        // Иначе
            operation.setCurrencyMain(ccyCredit);       // основная валюта по Кредиту
            if (RUB.equals(ccyCredit)) {    // Если КРЕДИТ в рублях
                operation.setAmountPosting(operation.getAmountCredit());
            } else {                        // Иначе
                operation.setAmountPosting(amountCreditRu);
            }
        }
    }

    /**
     * Интерфейс: Создает запрос на операцию с проверкой прав
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> saveOperationRq(ManualTechOperationWrapper wrapper, BatchPostStatus newStatus) throws Exception {
        try {
            checkUserPermission(wrapper);
        } catch (ValidationError e) {
            String msg = "Ошибка при сохранении запроса на операцию";
            if (null != wrapper.getId())
                msg += " ID = " + wrapper.getId();
            String errMessage = addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.warning(ManualOperation, msg, postingName, getWrapperId(wrapper), e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
        return saveOperationRqInternal(wrapper, newStatus);
    }

    /**
     * Проверяет право пользователя на работу с филиалом и проводками BackValue
     * @param wrapper
     * @throws Exception
     */
    public void checkUserPermission(ManualOperationWrapper wrapper) throws Exception {
        postingProcessor.checkFilialPermission(wrapper.getFilialDebit(), wrapper.getFilialCredit(), wrapper.getUserId());
        postingProcessor.checkBackvaluePermission(dateUtils.onlyDateParse(wrapper.getPostDateStr()), wrapper.getUserId());
    }

    public String addOperationErrorMessage(Throwable e, String msg, ErrorList errorList, String source) {
        String errCode = "";
        String errMessage = getErrorMessage(e);
        log.error(format("%s: %s. Обнаружена: %s\n'", msg, errMessage, source), e);
        if (null != errorList) {
            errCode = ValidationError.getErrorCode(errMessage);
            errMessage = ValidationError.getErrorText(errMessage);
            if (!errMessage.isEmpty()) {
                errorList.addNewErrorDescription( errMessage, errCode);
            }
        }
        return errMessage;
    }

    private String getWrapperId(ManualOperationWrapper wrapper) {
        return null == wrapper.getId() ? "" : wrapper.getId().toString();
    }

    private AuditRecord.LogCode getLogCode(ManualOperationWrapper wrapper) {
        return InputMethod.M.equals(wrapper.getInputMethod()) ? ManualOperation : BatchOperation;
    }

    /**
     * Интерфейс: Создает запрос на операцию без проверки прав (для тестов)
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> saveOperationRqInternal(ManualTechOperationWrapper wrapper, BatchPostStatus newStatus) throws Exception {
        BatchPosting posting = glOperationRepository.executeInNewTransaction(persistence -> createPosting(wrapper, newStatus));
        wrapper.setId(posting.getId());
        wrapper.setStatus (posting.getStatus());
        String msg = "Запрос на операцию ID = " + wrapper.getId() +
                (BatchPostAction.SAVE_CONTROL.equals(wrapper.getAction()) ? " передан на подпись" : " сохранён");
        auditController.info(ManualOperation, msg, posting);
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    /**
     * Создает запрос на операцию с предварительной валидацией
     * @param wrapper
     * @return
     * @throws Exception
     */
    private BatchPosting createPosting(ManualTechOperationWrapper wrapper, BatchPostStatus status) throws Exception {
        validateOperationRq(wrapper);

        try {
            BatchPosting posting = postingProcessor.createPosting(wrapper);    // создать операцию
            posting.setStatus(status);
            posting.setIsTech(YesNo.Y);
            posting.setNarrative(wrapper.getNarrative()!=null?wrapper.getNarrative():wrapper.getRusNarrativeShort());
            return postingRepository.save(posting);     // сохранить входящую операцию

        } catch (Throwable e) {
            String msg = "Ошибка при создании запроса на операцию";
            addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.error(ManualOperation, msg, null, e);
            throw new DefaultApplicationException(msg, e);
        }
    }


    private void validateOperationRq(ManualTechOperationWrapper wrapper) {
        List<ValidationError> errors = postingProcessor.validate(wrapper, new ValidationContext());

        if (!errors.isEmpty()) {
            throw new DefaultApplicationException(postingProcessor.validationErrorMessage(errors, wrapper.getErrorList()));
        }
    }

    public String getErrorMessage(Throwable throwable) {
        return ExceptionUtils.getErrorMessage(throwable,
                ValidationError.class, DataTruncation.class, SQLException.class, NullPointerException.class, DefaultApplicationException.class,
                PersistenceException.class);
    }

    /**
     * Создает входное сообщение по ручному вводу
     * @param wrapper
     * @return
     */
    public final GLManualOperation createOperation(ManualTechOperationWrapper wrapper, BatchPostStatus status) {
        GLManualOperation operation = new GLManualOperation();
        fillOperation(wrapper, operation);

        // опердень создания
        operation.setProcDate(userContext.getCurrentDate());
        // текущее системное время
        operation.setOperationTimestamp(userContext.getTimestamp());
        // создатель
        operation.setUserName(userContext.getUserName());
        operation.setUserName(userContext.getUserWrapper().getUserName());
        return operation;
    }

    /**
     * Заполняет поля запроса на операцию из входных параметров интерфейса
     * @param wrapper  - входные параметры
     * @param operation           - запрос на операцию
     * @return
     */
    public final GLManualOperation fillOperation(ManualTechOperationWrapper wrapper, GLManualOperation operation)  {

        operation.setInputMethod(wrapper.getInputMethod());
        operation.setSourcePosting(wrapper.getDealSrc());
        operation.setDealId(wrapper.getDealId());
        operation.setSubdealId(wrapper.getSubdealId());
        operation.setPaymentRefernce(wrapper.getPaymentRefernce());
        operation.setDeptId(StringUtils.trim(wrapper.getDeptId()));
        operation.setProfitCenter(wrapper.getProfitCenter());
        operation.setIsCorrection(wrapper.isCorrection() ? YesNo.Y : YesNo.N);
        operation.setStorno(YesNo.N);
        operation.setBsChapter("T");
        operation.setFan(YesNo.N);

        // Описание
        // TODO StringUtils.removeCtrlChars() пока убрала
        operation.setNarrative(wrapper.getNarrative());
        operation.setRusNarrativeLong(wrapper.getRusNarrativeLong());
        String narrativeShort = !isEmpty(wrapper.getRusNarrativeShort()) ?
                wrapper.getRusNarrativeShort() :
                substr(wrapper.getRusNarrativeLong(), 100);
        operation.setRusNarrativeShort(narrativeShort);

        // Даты и время
        operation.setValueDate(checkDateFormat(wrapper.getValueDateStr(), "Дата валютирования"));
        operation.setPostDate(checkDateFormat(wrapper.getPostDateStr(), "Дата проводки"));

        // Дебет
        operation.setAccountDebit(wrapper.getAccountDebit());
        operation.setFilialDebit(getFilial(wrapper.getFilialDebit(), wrapper.getAccountDebit(), GLOperation.OperSide.D));
        BankCurrency ccyDebit = bankCurrencyRepository.getCurrency(wrapper.getCurrencyDebit());
        operation.setCurrencyDebit(ccyDebit);
        operation.setAmountDebit(wrapper.getAmountDebit());

        // Кредит
        operation.setAccountCredit(wrapper.getAccountCredit());
        operation.setFilialCredit(getFilial(wrapper.getFilialCredit(), wrapper.getAccountCredit(), GLOperation.OperSide.C));
        BankCurrency ccyCredit = bankCurrencyRepository.getCurrency(wrapper.getCurrencyCredit());
        operation.setCurrencyCredit(ccyCredit);
        operation.setAmountCredit(wrapper.getAmountCredit());

        return operation;
    }

    public Date checkDateFormat(String dateStr, String name)
    {
        try {
            return dateUtils.onlyDateParse(dateStr);
        }catch (ParseException e){
            throw new ValidationError(BAD_DATE_FORMAT, name, dateStr);
        }
    }

    public void checkTechAccount(ManualTechOperationWrapper wrapper) throws ParseException {

        if (null != wrapper.getFilialDebit() && null!=wrapper.getFilialCredit())
        {
            if (!wrapper.getFilialDebit().equals(wrapper.getFilialCredit()))
            {
                throw  new ValidationError(ErrorCode.FILIAL_NOT_VALID,"Операции по техническим счетам должны проводиться только в рамках одного филиала");
            }
        }
        else{
            throw  new ValidationError(ErrorCode.FILIAL_NOT_FOUND,String.format("Не найден филиал по %s",wrapper.getFilialDebit()==null?"дебету":"кредиту"));
        }

        if (null!=wrapper.getCurrencyDebit() && null!=wrapper.getCurrencyCredit())
        {
            if (!wrapper.getCurrencyDebit().equalsIgnoreCase("RUR") && !wrapper.getCurrencyCredit().equalsIgnoreCase("RUR"))
            {
                throw new ValidationError(ErrorCode.ANY_CURRNCY_IS_RUR);
            }
        }

        if (StringUtils.isEmpty(wrapper.getAccountDebit()))
        {
            throw new ValidationError(ACCOUNT_TECH_NOT_CORRECT,"Пустой или неверный псевдосчёт", "", "");
        }
        else{
            GLAccount accountDr = glAccountController.findGLAccount(wrapper.getAccountDebit());
            if (accountDr==null)
            {
                throw new ValidationError(ACCOUNT_NOT_FOUND,wrapper.getAccountDebit(), "", "");
            }
            else {
                SimpleDateFormat df = new SimpleDateFormat("DD.MM.YYYY");
                Date valueDate = df.parse(wrapper.getValueDateStr());
                if ((accountDr.getDateClose()!=null) && (accountDr.getDateCloseNotNull().before(valueDate)))
                {
                    throw new ValidationError(ACCOUNT_IS_CLOSED,wrapper.getAccountCredit(), accountDr.getDateCloseNotNull().toString(), wrapper.getValueDateStr());
                }
            }
        }

        if (StringUtils.isEmpty(wrapper.getAccountCredit()))
        {
            throw new ValidationError(ACCOUNT_TECH_NOT_CORRECT,"Пустой или неверный псевдосчёт", "", "");
        }
        else{
            GLAccount accountCr = glAccountController.findGLAccount(wrapper.getAccountCredit());
            if (accountCr==null)
            {
                throw new ValidationError(ACCOUNT_NOT_FOUND,wrapper.getAccountCredit(), "", "");
            }
            else {
                SimpleDateFormat df = new SimpleDateFormat("DD.MM.YYYY");
                Date valueDate = df.parse(wrapper.getValueDateStr());
                if ((accountCr.getDateClose()!=null) && (accountCr.getDateCloseNotNull().before(valueDate)))
                {
                    throw new ValidationError(ACCOUNT_IS_CLOSED,wrapper.getAccountCredit(), accountCr.getDateCloseNotNull().toString(), wrapper.getValueDateStr());
                }
            }
        }
    }

    public String getFilial(String bsaAsid) {
        return glOperationRepository.getFilialByAccount(bsaAsid);
    }

    private  String getFilial(String filial, String bsaAsid, GLOperation.OperSide operSide) {
        String accountFilial = filial;
        /*String accountFilial = glOperationRepository.getFilialByAccount(bsaAsid);
        if (!isEmpty(filial) && !filial.equals(accountFilial)){
            throw new ValidationError(FILIAL_NOT_VALID, operSide.getMsgName(), filial, accountFilial);
        }*/
        return accountFilial;
    }

    private String getFilialByAlphaCode(String code)
    {
        String codeFilial = glOperationRepository.getFilialCBCCNbyCBCC(code);
        if (isEmpty(codeFilial) ){
            throw new ValidationError(FILIAL_NOT_VALID, "Неверный код филиала", code, "");
        }
        return codeFilial;
    }

    /**
     * Интерфейс: Передает запрос на операцию на подпись
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> forSignOperationRq(ManualTechOperationWrapper wrapper) throws Exception {
        try {
            checkUserPermission(wrapper);
        } catch (ValidationError e) {
            String msg = "Ошибка при передаче запроса на операцию ID = " + wrapper.getId() + " на подпись";
            String errMessage = addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.warning(ManualOperation, msg, postingName, getWrapperId(wrapper), e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
        return forSignOperationRqInternal(wrapper);
    }

    public RpcRes_Base<ManualOperationWrapper> forSignOperationRqInternal(ManualTechOperationWrapper wrapper) throws Exception {
        BatchPosting posting0 = getPostingWithCheck(wrapper, INPUT);
        BatchPosting posting = createPostingHistory(posting0, BatchPostStep.HAND1, wrapper.getAction());
        return setOperationRqStatusControl(wrapper, posting != posting0);
    }

    // TODO сверять значения Wrapper и Posting ?
    private BatchPosting getPostingWithCheck(ManualTechOperationWrapper wrapper, BatchPostStatus ... enabledStatus) {
        final BatchPosting posting = Optional.ofNullable(postingRepository.findById(wrapper.getId()))
                .orElseThrow(() -> new DefaultApplicationException("Не найден запрос на операцию с Id = " + wrapper.getId()));
        checkPostingStatus(posting, wrapper, enabledStatus);
        return posting;
    }

    private BatchPosting createPostingHistory(BatchPosting posting, BatchPostStep step, BatchPostAction action) throws Exception {
        return postingProcessor.needHistory(posting, step, action) ?
                glOperationRepository.executeInNewTransaction(persistence ->
                        postingRepository.createPostingHistory(posting.getId(), userContext.getTimestamp(), userContext.getUserName()))
                : posting;
    }

    public RpcRes_Base<ManualOperationWrapper> setOperationRqStatusControl(ManualTechOperationWrapper wrapper, boolean withChange) throws Exception {
        final BatchPostStatus status = CONTROL;
        BatchPostStatus oldStatus = wrapper.getStatus();
        int count = glOperationRepository.executeInNewTransaction(persistence -> {
            if (withChange) {
                return postingRepository.updatePostingStatusChanged(wrapper.getId(), userContext.getTimestamp(), userContext.getUserName(), status, oldStatus);
            } else {
                return postingRepository.updatePostingStatus(wrapper.getId(), status, oldStatus);
            }
        });
        if (0 == count)
            throw new ValidationError(POSTING_STATUS_WRONG, oldStatus.name(), oldStatus.getLabel());
        wrapper.setStatus(status);
        String msg = "Запрос на операцию ID = " + wrapper.getId() + " передан на подпись";
        auditController.info(ManualOperation, msg, postingName, getWrapperId(wrapper));
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    private boolean checkPostingStatus(BatchPosting posting, ManualTechOperationWrapper wrapper, BatchPostStatus ... enabledStatus) {
        if (!posting.getStatus().equals(wrapper.getStatus())) {
            String msg = String.format("Запрос на операцию ID = %d изменен, статус: '%s' ('%s')." +
                            "\n Обновите информацию и выполните операцию повторно"
                    , posting.getId(), posting.getStatus().name(), posting.getStatus().getLabel());
            wrapper.getErrorList().addErrorDescription(msg);
            throw new DefaultApplicationException(wrapper.getErrorMessage());
        }
        if (!InvisibleType.N.equals(posting.getInvisible())) {
            String msg = String.format("Запрос на операцию ID = %d изменен, признак 'Удален': '%s' ('%s')\n Обновите информацию",
                    posting.getId(), posting.getInvisible().name(), posting.getInvisible().getLabel() );
            wrapper.getErrorList().addErrorDescription(msg);
            throw new DefaultApplicationException(msg);
        }
        if (enabledStatus.length == 0)
            return true;
        for (BatchPostStatus status : enabledStatus) {
            if (status.equals(posting.getStatus())) {
                return true;
            }
        }
        String msg = String.format("Запрос на операцию ID = '%d': нельзя '%s' запрос в статусе: '%s' ('%s')", posting.getId(),
                wrapper.getAction().getLabel(), posting.getStatus().name(), posting.getStatus().getLabel());
        wrapper.getErrorList().addErrorDescription(msg);
        throw new DefaultApplicationException(msg);
    }

    /**
     * Интерфейс: Создает запрос на операцию без проверки прав (для тестов)
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> deleteOperationRq(ManualTechOperationWrapper wrapper) throws Exception {
        glOperationRepository.executeInNewTransaction(persistence -> deletePosting(wrapper));
        String msg = "Запрос на операцию ID = " + wrapper.getId() + " удалён";
        auditController.info(ManualOperation, msg, postingName, getWrapperId(wrapper));
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    private BatchPosting deletePosting(ManualTechOperationWrapper wrapper) {
        try {
            BatchPosting posting = getPostingWithCheck(wrapper);
            if (postingProcessor.needHistory(posting, BatchPostStep.HAND1, BatchPostAction.DELETE)) {
                postingRepository.setPostingInvisible(posting.getId(), userContext.getTimestamp(), userContext.getUserName());
                return postingRepository.findById(posting.getId());
            } else {
                postingRepository.deletePosting(posting.getId());   // удалить запрос на операцию
                return null;
            }

        } catch (Throwable e) {
            String msg = "Ошибка при удалении запроса на операцию ID = " + wrapper.getId();
            addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.error(ManualOperation, msg, postingName, getWrapperId(wrapper), e);
            throw new DefaultApplicationException(msg, e);
        }
    }

    /**
     * Интерфейс: Создает запрос на операцию с проверкой прав
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> updateOperationRq(ManualTechOperationWrapper wrapper, BatchPostStatus newStatus) throws Exception {
        try {
            checkUserPermission(wrapper);
        } catch (ValidationError e) {
            String msg = "Ошибка при изменении запроса на операцию ID = " + wrapper.getId();
            String errMessage = addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.warning(ManualOperation, msg, postingName, getWrapperId(wrapper), e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
        return updateOperationRqInternal(wrapper, newStatus);
    }

    /**
     * Интерфейс: Создает запрос на операцию без проверки прав (для тестов)
     * @param wrapper
     * @return
     * @throws Exception
     */
    public RpcRes_Base<ManualOperationWrapper> updateOperationRqInternal(ManualTechOperationWrapper wrapper, BatchPostStatus newStatus) throws Exception {
        BatchPostStatus status = wrapper.getStatus().getStep().isControlStep() ? CONTROL : newStatus;
        BatchPosting posting = glOperationRepository.executeInNewTransaction(persistence -> updatePosting(wrapper, status));
        wrapper.setId(posting.getId());
        wrapper.setStatus(posting.getStatus());
        String msg = "Запрос на операцию ID = " + wrapper.getId() +
                (BatchPostAction.UPDATE_CONTROL.equals(wrapper.getAction()) ? " изменён и передан на подпись" : " изменён");
        auditController.info(ManualOperation, msg, posting);
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    /**
     * Изменяет запрос на операцию с предварительной валидацией
     * @param wrapper
     * @return
     * @throws Exception
     */
    private BatchPosting updatePosting(ManualTechOperationWrapper wrapper, BatchPostStatus status) throws Exception {
        validateOperationRq(wrapper);

        try {
            BatchPosting posting0 = getPostingWithCheck(wrapper);
            BatchPosting posting = createPostingHistory(posting0, BatchPostStep.HAND1, wrapper.getAction());
            // редактировать операцию
            posting = postingProcessor.updatePosting(wrapper, posting);
            posting.setStatus(status);
            return postingRepository.update(posting);     // сохранить входящую операцию

        } catch (Throwable e) {
            String msg = "Ошибка при изменении запроса на операцию ID = " + wrapper.getId();
            addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.error(ManualOperation, msg, postingName, getWrapperId(wrapper), e);
            throw new DefaultApplicationException(msg, e);
        }
    }

    public RpcRes_Base<ManualOperationWrapper> authorizeOperationRq(ManualTechOperationWrapper wrapper) throws Exception {
        try {
//            checkProcessingAllowed(wrapper.getErrorList());
            BatchPostStep step = wrapper.getStatus().getStep();
            if (!step.isControlStep()) {
                return new RpcRes_Base<>(wrapper, false, "Неверный шаг оерации: " + step.name());
            }
//            if (SIGNED.equals(wrapper.getStatus())) {
//                return reprocessOperationRq(wrapper);
//            }
            postingProcessor.checkFilialPermission(wrapper.getFilialDebit(), wrapper.getFilialCredit(), wrapper.getUserId());
            BatchPosting posting0 = getPostingWithCheck(wrapper, CONTROL);
            checkHand12Diff(posting0);
            BatchPosting posting = createPostingHistory(posting0, wrapper.getStatus().getStep(), wrapper.getAction());
            // тестируем статус - что никто еще не менял
            updatePostingStatusNew(posting0, SIGNEDVIEW, wrapper);
            BatchPostStatus newStatus = getOperationRqStatusSigned(wrapper.getUserId(), posting.getPostDate());
            return setOperationRqStatusSigned(wrapper, userContext.getUserName(), SIGNEDVIEW, newStatus);

        } catch (ValidationError e) {
            String msg = "Ошибка при авторизации запроса на операцию ID = " + wrapper.getId();
            String errMessage = addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.error(ManualOperation, msg, postingName, getWrapperId(wrapper), e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
    }

    public void checkHand12Diff(BatchPosting posting) {
        BatchPostStep step = posting.getStatus().getStep();
        if (step.isControlStep()) {
            if (userContext.getUserName().equals(posting.getUserName())) {
                throw new ValidationError(POSTING_SAME_NOT_ALLOWED, posting.getId().toString());
            }
        }
    }

    public void updatePostingStatusNew(BatchPosting posting, BatchPostStatus newStatus, ManualOperationWrapper wrapper) throws Exception {
        int cnt = postingRepository.executeInNewTransaction(persistence -> postingRepository.updatePostingStatusNew(posting.getId(), newStatus));
        if (cnt != 1) {
            throw new ValidationError(ErrorCode.POSTING_IS_WORKING, posting.getId().toString(), newStatus.name());
        }
        if (null != wrapper)
            wrapper.setStatus(newStatus);
    }

    public BatchPostStatus getOperationRqStatusSigned(Long signerId, Date postDate) throws Exception {
        Date operday = operdayController.getOperday().getCurrentDate();
        BatchPostStatus newStatus;
        if (operday.equals(postDate)) {    // текущий день
            newStatus = SIGNED;
        } else if (postingProcessor.checkActionEnable(signerId, SecurityActionCode.OperHand3)) {
            newStatus = SIGNEDDATE;      // архивный день и 3-я рука
        } else {
            newStatus = WAITDATE;         // ждать 3-ю руку
        }
        return newStatus;
    }

    public RpcRes_Base<ManualOperationWrapper> setOperationRqStatusSigned(ManualTechOperationWrapper wrapper, String userName,
                                                                          BatchPostStatus newStatus, BatchPostStatus logStatus) throws Exception {
        BatchProcessResult result = new BatchProcessResult(wrapper.getId(), newStatus);
        BatchPostStatus oldStatus = wrapper.getStatus();
        BatchPostAction action = wrapper.getAction();
        int count = glOperationRepository.executeInNewTransaction(persistence -> {
            Date timestamp = userContext.getTimestamp();
            int cnt = 0;
            switch (newStatus) {
                case SIGNEDVIEW:
                    if (SIGNEDDATE.equals(logStatus)) {
                        cnt = postingRepository.signedConfirmPostingStatus(wrapper.getId(), timestamp, userName, newStatus, oldStatus);
                    } else {
                        cnt = postingRepository.signedPostingStatus(wrapper.getId(), timestamp, userName, newStatus, oldStatus);
                    }
                    break;
                case SIGNED:
                case WAITDATE:
                    cnt = postingRepository.signedPostingStatus(wrapper.getId(), timestamp, userName, newStatus, oldStatus);
                    break;
                case SIGNEDDATE:
                    if (wrapper.getStatus().getStep().isControlStep()) {
                        cnt = postingRepository.signedConfirmPostingStatus(wrapper.getId(), timestamp, userName, newStatus, oldStatus);
                        result.setProcessDate(BT_PAST);
                    } else {
                        cnt = postingRepository.confirmPostingStatus(wrapper.getId(), timestamp, userName, newStatus, oldStatus);
                        result.setStatus(CONFIRM);
                        result.setProcessDate(CONFIRM_NOW.equals(action) ? BT_NOW : BT_PAST);
                    }
                    break;
                default:
                    Assert.isTrue(false, "Неверный статус");
            }
            return cnt; //msg;
        });
        if (0 == count)
            throw new ValidationError(POSTING_STATUS_WRONG, oldStatus.name(), oldStatus.getLabel());

        if (newStatus==SIGNEDVIEW) {
            int cnt = 0;
            try {
                BatchPosting posting = postingRepository.findById(wrapper.getId());
                manualOperationProcessed(posting);
                Date timestamp = userContext.getTimestamp();
                cnt = postingRepository.signedConfirmPostingStatus(wrapper.getId(), timestamp, userName, COMPLETED, SIGNEDVIEW);
                result.setStatus(COMPLETED);
                wrapper.setStatus(COMPLETED);
            } catch (EJBException ex) {
                auditController.info(ManualOperation, ex.getMessage(), postingName, getWrapperId(wrapper));
                Date timestamp = userContext.getTimestamp();
                cnt = postingRepository.signedConfirmPostingStatus(wrapper.getId(), timestamp, userName, REFUSESRV, newStatus);
                wrapper.setStatus(REFUSESRV);
                result.setStatus(REFUSESRV);
            }
        }

        String msg = result.getPostSignedMessage();
        wrapper.getErrorList().addErrorDescription(msg);
        auditController.info(ManualOperation, msg, postingName, getWrapperId(wrapper));
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    /***
            * Проверяет, есть ли в операции контролируемые счета
     * Если есть, посылает движение и возвращает movementId, иначе пустую строку
     * В случае ошибки возникает исключение
     * @param wrapper
     * @return
             */
    public RpcRes_Base<ManualOperationWrapper> sendMovement(BatchPosting posting, ManualTechOperationWrapper wrapper, BatchPostStatus nextStatus) {
        if (null != posting.getReceiveTimestamp()) {
            String msg = String.format("По запоросу ID = '%s' уже было выполнено движение в MovementCreate: '%s', время: '%s'",
                    posting.getId().toString(), posting.getMovementId(), timeFormat.format(posting.getReceiveTimestamp()));
            auditController.info(getLogCode(wrapper), msg, posting);
            return new RpcRes_Base<>(wrapper, true, msg);              // TODO норма ?? ValidationError ??
        }
        try {
            MovementCreateData data = createMovementData(posting, wrapper);
            String movementId = data.getMessageUUID();
            RpcRes_Base<ManualOperationWrapper> res = setOperationRqStatusSend(wrapper, movementId, WAITSRV, nextStatus);
            List<MovementCreateData> movementData = new ArrayList<>();
            movementData.add(data);
            try {
                movementProcessor.sendRequests(movementData);
                return res;
            } catch (Throwable e) {     // ошибка отправки
                MovementErrorTypes error = data.getErrType();
                String msg = error.getMessage() + "\n" + data.getErrDescr();
                return setOperationRqStatusReceive(wrapper, movementId, ERRSRV, error.getCode(), msg);
            }
        }
        catch (Throwable e) {
            throw new DefaultApplicationException(logPostingError(e,
                    String.format("Ошибка при обращении к сервису движений по запросу ID = %s", getWrapperId(wrapper)),
                    wrapper, ERRSRV, MovementErrorTypes.ERR_REQUEST.getCode()));
        }
    }

    public MovementCreateData createMovementData(BatchPosting posting, ManualOperationWrapper wrapper) {
        String movementDr = null, movementCr = null;
        String oper = posting.getId().toString();
        String rand = getRundomUUID(6);
        if (posting.isControllableDebit()) {
            movementDr = oper + "D." + rand;
        }
        if (posting.isControllableCredit()) {
            movementCr = oper + "C." + rand;
        }
        if (isEmpty(movementDr) && isEmpty(movementCr)) {
            return null;
        }
        Date operday = operdayController.getOperday().getCurrentDate();
        MovementCreateData data = fillMovementData(posting, movementDr, movementCr, operday);
        String msg1 = String.format("Отправка в MovementCreate: ID = '%s', AC_DR = '%s', AMT_DR = %s, AC_CR = '%s', AMT_CR = %s",
                data.getMessageUUID(), data.getAccountCBD(), null == data.getOperAmountD() ? "" : data.getOperAmountD().toString(),
                data.getAccountCBC(), null == data.getOperAmountC() ? "" : data.getOperAmountC().toString());
        log.info(msg1);

        auditController.info(getLogCode(wrapper), msg1, postingName, getWrapperId(wrapper));

        return data;
    }

    public RpcRes_Base<ManualOperationWrapper> setOperationRqStatusSend(ManualTechOperationWrapper wrapper, String movementId, BatchPostStatus srvStatus, BatchPostStatus nextStatus) throws Exception {
//        BatchPostStatus srvStatus = WAITSRV;
        // устанавливаем статус = WAITSRV, movementId , SEND_SRV
        BatchPostStatus oldStatus = wrapper.getStatus();
        int count = glOperationRepository.executeInNewTransaction(persistence -> {
            return postingRepository.sendPostingStatus(wrapper.getId(), movementId, userContext.getTimestamp(), srvStatus, oldStatus);
        });
        if (0 == count) {
            throw new ValidationError(POSTING_STATUS_WRONG, oldStatus.name(), oldStatus.getLabel());
        }
        wrapper.setStatus(srvStatus);
//        String msg = "Запрос на операцию ID = " + wrapper.getId() + message;
        BatchProcessResult result = new BatchProcessResult(wrapper.getId(), nextStatus);
        result.setProcessDate(SIGNEDDATE.equals(nextStatus) ? BT_PAST : BT_EMPTY);
        String msg = result.getPostSendMessage();
        wrapper.getErrorList().addErrorDescription(msg);
        auditController.info(ManualOperation, msg, postingName, getWrapperId(wrapper));
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    private MovementCreateData fillMovementData(BatchPosting posting,
                                                String movementDr, String movementCr, Date operday) {
        MovementCreateData data = new MovementCreateData();
        if (!isEmpty(movementDr)) {
            data.setOperIdD(movementDr);
            data.setAccountCBD(posting.getAccountDebit());
            data.setOperAmountD(posting.getAmountDebit());
        }
        if (!isEmpty(movementCr)) {
            data.setOperIdC(movementCr);
            data.setAccountCBC(posting.getAccountCredit());
            data.setOperAmountC(posting.getAmountCredit());
        }

        data.setMessageUUID(ifEmpty(data.getOperIdD(), "") + "." + ifEmpty(data.getOperIdC(), "")); //+"."+UUID.randomUUID().toString().substring(0,6));

        data.setDestinationR(StringUtils.removeCtrlChars(posting.getRusNarrativeLong()));
        data.setPnar(pdRepository.getPnarManual(posting.getDealId(), posting.getSubDealId(), posting.getPaymentRefernce()));

        data.setPstDate(posting.getValueDate());
        data.setDealId(substr(!isEmpty(posting.getDealId()) ? posting.getDealId() : posting.getPaymentRefernce(), 15));
        data.setPstSource(posting.getSourcePosting());
        data.setDeptId(posting.getDeptId());
        data.setProfitCenter(posting.getProfitCenter());
        data.setCorrectionPst(YesNo.Y.equals(posting.getIsCorrection()));
        // TODO DEBUG
//        data.setOperCreate(userContext.getTimestamp());
//        data.setOperCreate(data.getPstDate());
        data.setOperCreate(operday);

        return data;
    }

    private String getRundomUUID(int num) {
        return UUID.randomUUID().toString().substring(0, num);
    }

    public RpcRes_Base<ManualOperationWrapper> setOperationRqStatusReceive(ManualOperationWrapper wrapper, String movementId,
                                                                           BatchPostStatus newStatus, int errorCode, String errorMessage) throws Exception {
        BatchPostStatus oldStatus = wrapper.getStatus();
        BatchPostStatus stepStatus = getStepStatus(wrapper.getStatus().getStep(), newStatus);
        Date timstamp = (TIMEOUTSRV == newStatus) ? null : userContext.getTimestamp();
        int count = glOperationRepository.executeInNewTransaction(persistence -> {
            return postingRepository.receivePostingStatus(wrapper.getId(), movementId, timstamp, stepStatus, oldStatus, errorCode, errorMessage);
        });
        if (0 == count) {
            throw new ValidationError(POSTING_STATUS_WRONG, oldStatus.name(), oldStatus.getLabel());
        }
        wrapper.setStatus(newStatus);
        BatchProcessResult result = new BatchProcessResult(wrapper.getId(), newStatus);
        String msg = result.getPostSignedMessage();

        wrapper.getErrorList().addErrorDescription(msg + errorMessage);
        if (errorCode == 0) {
            auditController.info(ManualOperation, msg, postingName, getWrapperId(wrapper));
        } else {
            auditController.error(ManualOperation, msg, postingName, getWrapperId(wrapper), errorMessage);
        }
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    public BatchPostStatus getStepStatus(BatchPostStep step, BatchPostStatus status) {
        if (step.equals(status.getStep()))
            return status;
        if (step.isConfirmStep()) {
            switch (status) {
                case SIGNED:        return SIGNEDDATE;
                case REFUSE:        return REFUSEDATE;
                case ERRPROC:       return ERRPROCDATE;
                case ERRSRV:        return ERRSRV;
                case REFUSESRV:     return REFUSESRV;
            }
        } else if (step.isControlStep()){
            switch (status) {
                case SIGNEDDATE:    return SIGNED;
                case REFUSEDATE:    return REFUSE;
                case ERRPROCDATE:   return ERRPROC;
                case ERRSRV:        return ERRSRV;
                case REFUSESRV:     return REFUSESRV;
            }
        }
        return status;
    }


    public String logPostingError(Throwable e, String msg, ManualTechOperationWrapper wrapper,
                                  BatchPostStatus status, int errorCode) {
        log.error("-->" + msg, e);
        addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
        try {
            // TODO здесь падает при обработке пакета!!
            glOperationRepository.executeInNewTransaction(persistence -> postingRepository.updatePostingStatusError(wrapper.getId(), wrapper.getErrorMessage(),
                    getStepStatus(wrapper.getStatus().getStep(), status), errorCode));
        } catch (Throwable e1) {
            return msg + "\n" + e.getMessage();
        }
        log.error("<--" + msg, e);
        auditController.error(getLogCode(wrapper), msg, postingName, getWrapperId(wrapper), e);
        return msg;
    }

    public RpcRes_Base<ManualOperationWrapper> confirmOperationRq(ManualTechOperationWrapper wrapper) throws Exception {
        try {
//            checkProcessingAllowed(wrapper.getErrorList());
            BatchPostStep step = wrapper.getStatus().getStep();
            if (!step.isConfirmStep()) {
                return new RpcRes_Base<>(wrapper, false, "Неверный шаг оерации: " + step.name());
            }
//            if (SIGNEDDATE.equals(wrapper.getStatus())) {
//                return reprocessOperationRq(wrapper);
//            }
            postingProcessor.checkFilialPermission(wrapper.getFilialDebit(), wrapper.getFilialCredit(), wrapper.getUserId());
            BatchPosting posting0 = getPostingWithCheck(wrapper, WAITDATE);
            BatchPosting posting = createPostingHistory(posting0, wrapper.getStatus().getStep(), wrapper.getAction());
            BatchPostStatus newStatus = SIGNEDDATE;
            // тестируем статус - что никто еще не менял
            updatePostingStatusNew(posting0, newStatus, wrapper);
            // устанавливаем статус
            Date operday = operdayController.getOperday().getCurrentDate();
            if (BatchPostAction.CONFIRM_NOW.equals(wrapper.getAction())) {
                wrapper.setPostDateStr(operday == null ? null : dateUtils.onlyDateString(operday));
                postingRepository.executeInNewTransaction(persistence -> {
                    postingRepository.setPostingDate(posting.getId(), userContext.getCurrentDate());
                    return null;
                });
            }
            return setOperationRqStatusSigned(wrapper, userContext.getUserName(), newStatus, newStatus);
//            return authorizeOperationRqInternal(posting, wrapper, newStatus);
        } catch (ValidationError e) {
            String msg = "Ошибка при подтверждении даты запроса на операцию ID = " + wrapper.getId();
            String errMessage = addOperationErrorMessage(e, msg, wrapper.getErrorList(), initSource());
            auditController.error(ManualOperation, msg, postingName, getWrapperId(wrapper), e);
            return new RpcRes_Base<>( wrapper, true, errMessage);
        }
    }

    public RpcRes_Base<ManualOperationWrapper> refuseOperationRq(ManualTechOperationWrapper wrapper, BatchPostStatus status) throws Exception {
        BatchPosting posting0 = getPostingWithCheck(wrapper, CONTROL, REFUSEDATE, ERRSRV, REFUSESRV, WAITDATE, ERRPROC, ERRPROCDATE);
        String msg;
        if (hasMovement(posting0)) {
            msg = "Нельзя вернуть запрос на операцию ID = " + wrapper.getId() + " на доработку,\nпо нему выполнен успешный запрос в сервис движений";
        } else {
            BatchPosting posting = createPostingHistory(posting0, wrapper.getStatus().getStep(), wrapper.getAction());
            BatchPostStatus oldStatus = posting.getStatus();
            int count = glOperationRepository.executeInNewTransaction(persistence -> {
                return postingRepository.refusePostingStatus(posting.getId(), wrapper.getReasonOfDeny(),
                        userContext.getTimestamp(), userContext.getUserName(), status, oldStatus);
            });
            if (0 == count)
                throw new ValidationError(POSTING_STATUS_WRONG, oldStatus.name(), oldStatus.getLabel());
            wrapper.setStatus(status);
            msg = "Запрос на операцию ID = " + wrapper.getId() + " возвращён на доработку";
        }
        auditController.info(ManualOperation, msg, postingName, getWrapperId(wrapper));
        return new RpcRes_Base<>(wrapper, false, msg);
    }

    private boolean hasMovement(BatchPosting posting) {
        //TODO не вполне корректная проверка. Нет однозначного признака прохождения movement
        return ((null != posting.getMovementId()) && !(ERRSRV.equals(posting.getStatus()) || REFUSESRV.equals(posting.getStatus())))
                || TIMEOUTSRV.equals(posting.getStatus()) ;
    }

    /**
     * Метод создания технической операции по ручной
     * @param posting
     * @return
     */
    private GLManualOperation createOperation(BatchPosting posting)
    {
        return manualOperationProcessor.createOperation(posting);
    }


}
