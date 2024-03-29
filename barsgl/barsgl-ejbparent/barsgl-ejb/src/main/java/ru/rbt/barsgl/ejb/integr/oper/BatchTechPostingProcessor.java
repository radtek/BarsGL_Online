package ru.rbt.barsgl.ejb.integr.oper;

import ru.rbt.barsgl.ejb.repository.*;
import ru.rbt.gwt.security.ejb.repository.access.AccessServiceSupport;
import ru.rbt.barsgl.ejb.common.repository.od.BankCalendarDayRepository;
import ru.rbt.security.entity.access.PrmValue;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.entity.gl.BalanceChapter;
import ru.rbt.barsgl.ejb.entity.gl.GLOperation;
import ru.rbt.barsgl.ejb.integr.ValidationAwareHandler;
import ru.rbt.barsgl.ejb.integr.acc.GLAccountProcessor;
import ru.rbt.security.ejb.repository.access.PrmValueRepository;
import ru.rbt.barsgl.ejb.security.UserContext;
import ru.rbt.ejbcore.datarec.DataRecord;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.ejbcore.util.DateUtils;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.barsgl.shared.ErrorList;
import ru.rbt.shared.enums.*;
import ru.rbt.barsgl.shared.enums.*;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;
import ru.rbt.barsgl.shared.operation.ManualTechOperationWrapper;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rbt.barsgl.shared.enums.BatchPostStep.HAND1;
import static ru.rbt.ejbcore.util.StringUtils.*;
import static ru.rbt.ejbcore.validation.ErrorCode.*;

/**
 * Created by ER18837 on 13.08.15.
 */
public class BatchTechPostingProcessor extends ValidationAwareHandler<ManualTechOperationWrapper> {

    @Inject
    private BankCurrencyRepository bankCurrencyRepository;

    @Inject
    private OrdinaryPostingProcessor ordinaryPostingProcessor;

    @Inject
    private GLOperationRepository glOperationRepository;

    @Inject
    private GLTechOperationRepository glTechOperationRepository;

    @Inject
    private GLAccountRepository glAccountRepository;

    @EJB
    private BatchPostingRepository postingRepository;

    @Inject
    private PrmValueRepository prmValueRepository;

    @Inject
    private AccessServiceSupport accessServiceSupport;

    @Inject
    private GLAccountProcessor glAccountProcessor;

    @Inject
    private DateUtils dateUtils;

    @Inject
    private UserContext userContext;

    @Inject
    private BankCalendarDayRepository calendarDayRepository;

    @Override
    public void fillValidationContext(ManualTechOperationWrapper target, ValidationContext context) {
        // ============== Дата ===============
        // TODO добавить возможность вводить только одну дату, или брать обе как опердень
        // Value Date, Posting Date
        context.addValidator(() -> {
            Date valueDate = checkDate(target, target.getValueDateStr(), "валютирования", false);
            Date postDate = checkDate(target, target.getPostDateStr(), "проводки", true);
            if ( (null != postDate )&& (null != valueDate) && valueDate.after(postDate)) {
                throw new ValidationError(POSTDATE_NOT_VALID,
                        target.getPostDateStr(),
                        target.getValueDateStr());
            }
            // Счет 707 - дата проводки
            if ("707".equals(StringUtils.substr(target.getAccountDebit(), 0, 3)) ||
                    "707".equals(StringUtils.substr(target.getAccountCredit(), 0, 3)))
            {
                Date currentDate = userContext.getCurrentDate();     // текущий опердень
                Date lastSpod = glAccountRepository.getLastSpodDate(currentDate);   // последний день SPOD
                Calendar calPost = Calendar.getInstance();
                calPost.setTime(postDate);
                Calendar calOper = Calendar.getInstance();
                calOper.setTime(currentDate);
                if ((calPost.get(Calendar.YEAR) != calOper.get(Calendar.YEAR)) || postDate.after(lastSpod)) {
                    calOper.set(calOper.get(Calendar.YEAR), 0, 1);
                    throw new ValidationError(OPERATION_707_AFTER_SPOD,
                        dateUtils.onlyDateString(calOper.getTime()), dateUtils.onlyDateString(lastSpod));
                }
            }
        });

        // ============ Валюта ==============
        // Валюта дебета
        context.addValidator(() -> {
            checkCurrency(target, GLOperation.OperSide.D, target.getCurrencyDebit(), "Дебет / Валюта",
                    target.getAccountDebit());
        });
        // Валюта кредита
        context.addValidator(() -> {
            checkCurrency(target, GLOperation.OperSide.C, target.getCurrencyCredit(), "Кредит / Валюта",
                    target.getAccountCredit());
        });

        // ============ Сумма ===============
        // сумма по дебету
        context.addValidator(() -> {
            // сумма в валюте
            checkAmount(target, GLOperation.OperSide.D, "RUR".equals(target.getCurrencyDebit()),
                    target.getAmountDebit(), target.getAmountRu(), "Дебет / Сумма");
        });
        // сумма по кредиту
        context.addValidator(() -> {
            checkAmount(target, GLOperation.OperSide.C, "RUR".equals(target.getCurrencyCredit()),
                    target.getAmountCredit(), target.getAmountRu(), "Кредит / Сумма");
        });
        // сумма в рублях
        context.addValidator(() -> {
            if (null == target.getAmountRu())
                return;
            BigDecimal amountRu = target.getAmountRu();
            if ("RUR".equals(target.getCurrencyDebit()) && !amountRu.equals(target.getAmountDebit()) ||
                "RUR".equals(target.getCurrencyCredit()) && !amountRu.equals(target.getAmountCredit())) {
                throw new ValidationError(AMOUNT_RU_INVALID, GLOperation.OperSide.N.getMsgName(), amountRu.toString(), "Сумма в рублях");
            }
        });
        // суммы дебета и кредита
        context.addValidator(() -> {
            if (null == target.getCurrencyDebit() || null == target.getCurrencyCredit())
                return;
            if (target.getCurrencyDebit().equals(target.getCurrencyCredit())) {
                // если валюты равны - суммы должны быть равны
                if (!target.getAmountDebit().equals(target.getAmountCredit())) {
                    throw new ValidationError(AMOUNT_NOT_EQUAL,
                            target.getAmountDebit().toString(), target.getAmountCredit().toString(),
                            "Дебет / Сумма", "Кредит / Сумма");
                }
            } else if ("RUR".equals(target.getCurrencyDebit()) || "RUR".equals(target.getCurrencyCredit())) {
                // если одна из валют рубль - суммы не должны быть равны
                if (target.getAmountDebit().equals(target.getAmountCredit())
                    && target.getAmountDebit().compareTo(new BigDecimal("0.1")) > 0) {
                    throw new ValidationError(AMOUNT_EQUAL,
                            target.getCurrencyDebit(), target.getCurrencyCredit(),
                            target.getAmountDebit().toString(),
                            "Дебет / Сумма", "Кредит / Сумма");
                }
            }
        });

        // ============== Общее ===============
        // Источник сделки
        context.addValidator(() -> {
            String fieldName = "Источник сделки";
            String fieldValue = target.getDealSrc();
            int maxLen = 7;
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            } else if (maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        // ИД платежа
        context.addValidator(() -> {
            String fieldName = "Номер платежного документа";
            String fieldValue = target.getPaymentRefernce();
            int maxLen = 20;
            if ( null != fieldValue && maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        // Номер сделки
        context.addValidator(() -> {
            String fieldName = "Номер сделки";
            String fieldValue = target.getDealId();
            int maxLen = 20;
            if (null != fieldValue && maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        // Номер субсделки
        context.addValidator(() -> {
            String fieldName = "Номер субсделки";
            String fieldValue = target.getSubdealId();
            int maxLen = 20;
            if (null != fieldValue) {
                if (maxLen < fieldValue.length())
                    throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
            checkDealId(target);
        });

        // Код департамента, создавшего проводку
        context.addValidator(() -> {
            String fieldName = "Подразделение";
            String fieldValue = target.getDeptId();
            int maxLen = 4;
            if (null != fieldValue &&  maxLen < fieldValue.length()) {
                throw new ValidationError(STRING_FIELD_IS_TOO_LONG, fieldValue, fieldName, Integer.toString(maxLen));
            }
        });
        // описания
        context.addValidator(() -> {
            String fieldName = "Основание ENG";
            String fieldValue = target.getNarrative();
            int maxLen = 300;
            if (isEmpty(fieldValue)) {
                throw new ValidationError(FIELD_IS_EMPTY, fieldName);
            } else if (null != fieldValue && maxLen < fieldValue.length()) {
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

    public Date checkDateFormat(String dateStr, String name)
    {
        try {
            return dateUtils.onlyDateParse(dateStr);
        }catch (ParseException e){
            throw new ValidationError(BAD_DATE_FORMAT, name, dateStr);
        }
    }

    public String getFilial(String bsaAsid) {
        return glOperationRepository.getFilialByAccount(bsaAsid);
    }

    private void checkCurrency(ManualOperationWrapper target, GLOperation.OperSide operSide,
                               String ccy, String currencyField, String account) {
        String sideRus = operSide.getMsgName();
        if (isEmpty(ccy)) {
            throw new ValidationError(FIELD_IS_EMPTY, currencyField);
        }
        BankCurrency currency = bankCurrencyRepository.getCurrency(ccy);    // есди нет - ValidationError
        currency = bankCurrencyRepository.refreshCurrency(currency);
        String ccyAccount = glAccountRepository.getAccountCurrency(account);
        if (!ccyAccount.equals(currency.getDigitalCode())) {
            throw new ValidationError(CURRENCY_CODE_NOT_MATCH_ACCOUNT, sideRus,
                    currency.getCurrencyCode(), ccyAccount,
                    currencyField);
        }
    }

    private void checkAmount(ManualOperationWrapper target, GLOperation.OperSide operSide, boolean isCurrencyRUR,
                             BigDecimal amount, BigDecimal amountRu, String fieldName) {
        boolean trueAmount;
        if (isCurrencyRUR) {                        // валюта - рубли
            trueAmount = (amount.signum() > 0);                             // в валюте > 0
        } else {                                    // в валюте > 0 или (в валюте == 0 и в рублях не пусто)
            trueAmount = (amount.signum() > 0) || ((amount.signum() == 0) && (amountRu != null));
        }
        // сумма в валюте
        if (!trueAmount) {
            throw new ValidationError(AMOUNT_INVALID, operSide.getMsgName(), amount.toString(), fieldName);
        }
    }

    public Date checkDate(ManualTechOperationWrapper target, String checkDateStr, String fieldName, boolean checkHoliday) {
        Date currentDate = userContext.getCurrentDate();     // текущий опердень
        Date checkDate = null;
        try {
            if(null != checkDateStr)
                checkDate = new SimpleDateFormat(target.dateFormat).parse(checkDateStr);
        } catch (ParseException e) {

        }
        if ( null == checkDate ) {
            throw new ValidationError(DATE_AFTER_OPERDAY, fieldName,
                    "не задана",
                    dateUtils.onlyDateString(currentDate));
        } else if (checkDate.after(currentDate)){
            throw new ValidationError(DATE_AFTER_OPERDAY, fieldName,
                    dateUtils.onlyDateString(checkDate),
                    dateUtils.onlyDateString(currentDate));
        } else if (checkHoliday && !calendarDayRepository.isWorkday(checkDate)) {
            // TODO проверка на выходные
            throw new ValidationError(DATE_IS_HOLIDAY, fieldName,
                    dateUtils.onlyDateString(checkDate));
        }
        return checkDate;
    }

    public void checkDealId(ManualOperationWrapper wrapper ) {
        glAccountProcessor.checkDealId(checkDateFormat(wrapper.getPostDateStr(), "Дата проводки"), wrapper.getDealSrc(),
                wrapper.getDealId(), wrapper.getSubdealId(), wrapper.getPaymentRefernce());
    }

    private  String getFilial(String filial, String bsaAsid, GLOperation.OperSide operSide) {
        String accountFilial = glTechOperationRepository.getFilialByAccount(bsaAsid);
        if (!isEmpty(filial) && !filial.equals(accountFilial)){
            throw new ValidationError(FILIAL_NOT_VALID, operSide.getMsgName(), filial, accountFilial);
        }
        return accountFilial;
    }

    /**
     * Создает входное сообщение по ручному вводу
     * @param wrapper
     * @return
     */
    public final BatchPosting createPosting(ManualOperationWrapper wrapper) {
        BatchPosting posting = new BatchPosting();

        fillPosting(wrapper, posting);

        // опердень создания
        posting.setProcDate(userContext.getCurrentDate());
        posting.setInvisible(InvisibleType.N);
        posting.setInputMethod(wrapper.getInputMethod());
        // текущее системное время
        posting.setCreateTimestamp(userContext.getTimestamp());
        // создатель
        posting.setUserName(userContext.getUserName());
        posting.setUserFilial(userContext.getUserWrapper().getFilial());
        return posting;
    }

    /**
     * Создает входное сообщение по ручному вводу
     * @param wrapper
     * @return
     */
    public final BatchPosting updatePosting(ManualOperationWrapper wrapper, BatchPosting posting) {
        fillPosting(wrapper, posting);

        // очистить ошибки
        posting.setErrorCode(null);
        posting.setErrorMessage(null);
        posting.setReasonOfDeny(null);

        // очистить движение
        posting.setMovementId(null);
        posting.setSendTimestamp(null);
        posting.setReceiveTimestamp(null);

        // текущее системное время
        posting.setChangeTimestamp(userContext.getTimestamp());
        posting.setChangeName(userContext.getUserName());   // изменятель
        return posting;
    }

    /**
     * Заполняет поля запроса на операцию из входных параметров интерфейса
     * @param wrapper  - входные параметры
     * @param posting           - запрос на операцию
     * @return
     */
    public final BatchPosting fillPosting(ManualOperationWrapper wrapper, BatchPosting posting)  {

        posting.setInputMethod(wrapper.getInputMethod());
        posting.setSourcePosting(wrapper.getDealSrc());
        posting.setDealId(wrapper.getDealId());
        posting.setSubDealId(wrapper.getSubdealId());
        posting.setPaymentRefernce(wrapper.getPaymentRefernce());
        posting.setDeptId(StringUtils.trim(wrapper.getDeptId()));
        posting.setProfitCenter(wrapper.getProfitCenter());
        posting.setIsCorrection(wrapper.isCorrection() ? YesNo.Y : YesNo.N);

        // Описание
        // TODO StringUtils.removeCtrlChars() пока убрала
        posting.setNarrative(wrapper.getNarrative());
        posting.setRusNarrativeLong(wrapper.getRusNarrativeLong());
        String narrativeShort = !isEmpty(wrapper.getRusNarrativeShort()) ?
                wrapper.getRusNarrativeShort() :
                substr(wrapper.getRusNarrativeLong(), 100);
        posting.setRusNarrativeShort(narrativeShort);

        // Даты и время
        posting.setValueDate(checkDateFormat(wrapper.getValueDateStr(), "Дата валютирования"));
        posting.setPostDate(checkDateFormat(wrapper.getPostDateStr(), "Дата проводки"));

            // Дебет
        posting.setAccountDebit(wrapper.getAccountDebit());
        posting.setFilialDebit(getFilial(wrapper.getFilialDebit(), wrapper.getAccountDebit(), GLOperation.OperSide.D));
        BankCurrency ccyDebit = bankCurrencyRepository.getCurrency(wrapper.getCurrencyDebit());
        posting.setCurrencyDebit(ccyDebit);
        posting.setAmountDebit(wrapper.getAmountDebit());

        // Кредит
        posting.setAccountCredit(wrapper.getAccountCredit());
        posting.setFilialCredit(getFilial(wrapper.getFilialCredit(), wrapper.getAccountCredit(), GLOperation.OperSide.C));
        BankCurrency ccyCredit = bankCurrencyRepository.getCurrency(wrapper.getCurrencyCredit());
        posting.setCurrencyCredit(ccyCredit);
        posting.setAmountCredit(wrapper.getAmountCredit());

        return posting;
    }

    public String validationErrorMessage(List<ValidationError> errors, ErrorList descriptors) {
        StringBuilder result = new StringBuilder(format("Обнаружены ошибки валидации входных данных\n"));
        result.append(validationErrorsToString(errors, descriptors));
        return result.toString();
    }

    public void checkFilialPermission(String filialDebit, String filialCredit, Long userId) throws Exception {
        List<PrmValue> prm = prmValueRepository.select(PrmValue.class, "from PrmValue p where p.userId = ?1 and p.prmCode = ?2",
                userId, PrmValueEnum.HeadBranch);

        if (prm == null || prm.isEmpty() || prm.stream().filter(x ->
                x.getPrmValue().equals(filialCredit) ||
                x.getPrmValue().equals(filialDebit) ||
                x.getPrmValue().equals("*")).count() == 0){
            throw new ValidationError(POSTING_FILIAL_NOT_ALLOWED, filialDebit, filialCredit);
        }
    }

    public void checkBackvaluePermission(Date postDate, Long userId) throws Exception {
        accessServiceSupport.checkUserAccessToBackValueDate(postDate, userId);
    }

    public boolean checkActionEnable(Long userId, SecurityActionCode actionCode) throws SQLException {
        DataRecord rec = prmValueRepository.selectFirst("select 1 from GL_AU_USRRL ur\n" +
                "join GL_AU_ROLE r on ur.ID_ROLE = r.ID_ROLE\n" +
                "join GL_AU_ACTRL ar on r.ID_ROLE = ar.ID_ROLE\n" +
                "join GL_AU_ACT a on a.ID_ACT = ar.ID_ACT\n" +
                "where ID_USER = ? and ACT_CODE = ?", userId, actionCode.name());
        return null != rec;
    }

    /**
     * Проверяет, нужно ли создавать историческую запись запроса на операцию
     * @return
     */
    public boolean needHistory(BatchPosting posting, BatchPostStep step, BatchPostAction action){
        if (null == posting)
            return false;
        String postName = "";

        switch (step) {
            case HAND1:
                postName = posting.getUserName();
                break;
            case HAND2:
                postName = posting.getSignerName();
                break;
            case HAND3:
                postName = posting.getConfirmName();
                break;
        }

        boolean otherUser = !isEmpty(postName) && !postName.equals(userContext.getUserName());
        String actionName = action.name();
        boolean otherAction = (actionName.startsWith("UPDATE") || actionName.startsWith("REFUSE"))
                && !isEmpty(posting.getChangeName());
        String statusName = posting.getStatus().name();
        boolean oterStatus = statusName.startsWith("REFUSE") || statusName.startsWith("SIGNED");    // || statusName.startsWith("ERR")
        return otherUser || oterStatus || otherAction;
    }
}
