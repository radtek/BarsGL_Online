package ru.rbt.barsgl.ejb.controller.excel;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.common.controller.od.OperdayController;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.entity.etl.BatchPackage;
import ru.rbt.barsgl.ejb.entity.etl.BatchPosting;
import ru.rbt.barsgl.ejb.integr.bg.BatchPackageController;
import ru.rbt.barsgl.ejb.integr.oper.BatchPostingProcessor;
import ru.rbt.barsgl.ejb.repository.BankCurrencyRepository;
import ru.rbt.barsgl.ejb.repository.BatchPackageRepository;
import ru.rbt.barsgl.ejb.repository.BatchPostingRepository;
import ru.rbt.audit.controller.AuditController;
import ru.rbt.ejbcore.mapping.YesNo;
import ru.rbt.shared.security.RequestContext;
import ru.rbt.barsgl.ejbcore.util.ExcelParser;
import ru.rbt.ejbcore.util.StringUtils;
import ru.rbt.ejbcore.validation.ErrorCode;
import ru.rbt.barsgl.ejbcore.validation.ValidationContext;
import ru.rbt.ejbcore.validation.ValidationError;
import ru.rbt.shared.ctx.UserRequestHolder;
import ru.rbt.barsgl.shared.enums.BatchPackageState;
import ru.rbt.barsgl.shared.enums.BatchPostStatus;
import ru.rbt.barsgl.shared.enums.InputMethod;
import ru.rbt.barsgl.shared.enums.InvisibleType;
import ru.rbt.barsgl.shared.operation.ManualOperationWrapper;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.String.format;
import static ru.rb.ucb.util.StringUtils.isEmpty;
import static ru.rbt.audit.entity.AuditRecord.LogCode.BatchOperation;

/**
 * Created by ER18837 on 29.02.16.
 */
@Stateless
@LocalBean
public class BatchMessageProcessorBean implements BatchMessageProcessor {

    public static final Logger log = Logger.getLogger(BatchMessageProcessorBean.class);

    private static int START_ROW = 1;
    private static int COLUMN_COUNT = 16;   // TODO 17
    private static String LIST_DELIMITER = "#";

    private List<Object> rowHeader = null;

    @Inject
    OperdayController operdayController;

    @Inject
    private BatchPackageRepository packageRepository;

    @EJB
    private BatchPostingRepository postingRepository;

    @Inject
    private BankCurrencyRepository bankCurrencyRepository;

    @Inject
    private BatchPostingProcessor postingProcessor;

    @Inject
    private RequestContext contextBean;

    @EJB
    private AuditController auditController;

    @EJB
    private BatchPackageController packageController;

    @Override
    public String processMessage(File file, Map<String, String> params) throws Exception {
        String fileName = new String(params.get("filename").getBytes("Cp1252"), "Cp1251");
        String userIdStr = params.get("userid");
        Long userId = userIdStr == null ? null : Long.valueOf(userIdStr);
        boolean movementOff = "true".equals(params.get("movement_off"));
        String sourcePosting = params.get("source");
        String department = params.get("department");

        BatchPackage batchPackage = null;
        try (
                InputStream is = new FileInputStream(file);
                ExcelParser parser = new ExcelParser(is);
        ) {
            Iterator<List<Object>> it = parser.parseSafe(0);
            batchPackage = postingRepository.executeInNewTransaction(persistence ->
                    buildPackage(it, fileName, parser.getRowCount(), userId, sourcePosting, department, movementOff));
        }
        if (null == batchPackage )
            return "Нет строк для загрузки!";

        String result = new StringBuffer().append(LIST_DELIMITER)
                .append("ID пакета: ").append(batchPackage.getId()).append(LIST_DELIMITER)
                .append("Загружено строк всего: ").append(batchPackage.getPostingCount()).append(LIST_DELIMITER)
                .append("Загружено с ошибкой: ").append(batchPackage.getErrorCount()).append(LIST_DELIMITER)
                .toString();
        auditController.info(BatchOperation, "Загружен пакет из файла.\n" + result, BatchPackage.class.getName(), batchPackage.getId().toString());
        return result;
    }

    public BatchPackage buildPackage(Iterator<List<Object>> it, String fileName, int maxRowNum, Long userId, String source, String department, boolean movementOff) throws Exception {
        if(!it.hasNext() || 0 == maxRowNum) {
            return null;
        }

        BatchPackage pkg = new BatchPackage();

        final UserRequestHolder requestHolder = contextBean.getRequest().orElse(UserRequestHolder.empty());
        String userName = requestHolder.getUser();
        String filial = requestHolder.getUserWrapper().getFilial();
        if (null == userId)
            userId = requestHolder.getUserWrapper().getId();
        pkg.setUserName(userName);

        Date curdate = operdayController.getOperday().getCurrentDate();
        Date timestamp = operdayController.getSystemDateTime();

        List<BatchPosting> postings = new ArrayList<>();
        List<String> errorList = new ArrayList<String>();

        rowHeader = it.next();
        if (rowHeader.isEmpty())
            return null;
        if (rowHeader.size() < COLUMN_COUNT) {
            String msg = "Неверное количество столбцов: " + rowHeader.size() + ", должно быть не менее " + COLUMN_COUNT;
            auditController.error(BatchOperation, "Ошибка при загрузке файла", null, msg);
            throw new ParamsParserException(msg);
        }

        int row = START_ROW;
        int maxRows = START_ROW + packageController.getMaxRowsExcel();
        if (maxRowNum > maxRows) {
            errorList.add(format("Нельзя загрузить файл размером больше %d строк", maxRows));
            throw new ParamsParserException(StringUtils.listToString(errorList, LIST_DELIMITER));
        }

        Date postDate0 = null;
        if(it.hasNext()) {
            BatchPosting posting0 = createPosting(it.next(), row, source, department, errorList);
            if (null == posting0)
                return null;
            postDate0 = posting0.getPostDate();
            if (null != postDate0) {
                checkBackvaluePermission(postDate0, userId);
                checkFilialPermission(posting0.getFilialDebit(), posting0.getFilialCredit(), userId);
                postings.add(posting0);
                row++;
                while(it.hasNext()) {
                    BatchPosting posting = createPosting(it.next(), row, source, department, errorList);
                    if (null != posting) {
                        checkDateEquals(postDate0, posting.getPostDate());
                        checkFilialPermission(posting.getFilialDebit(), posting.getFilialCredit(), userId);
                        postings.add(posting);
                    }
                    row++;
                    if (errorList.size() > 10)
                        break;
                }
            }
        }

        if (errorList.size() > 0) {
            throw new ParamsParserException(StringUtils.listToString(errorList, LIST_DELIMITER));
        };

        int errorCount = 0;
        for (BatchPosting posting : postings) {
            enrichmentPosting(posting, curdate, userName, filial);
            if (!validatePosting(posting))
                errorCount++;
        }

        pkg.setPostingCount(postings.size());
        pkg.setErrorCount(errorCount);
        pkg.setFileName(fileName);
        pkg.setDateLoad(new Date());
        pkg.setMovementOff(movementOff ? YesNo.Y : YesNo.N);
        pkg.setPostDate(postDate0);
        pkg.setProcDate(curdate);
        pkg.setPackageState(errorCount > 0 ? BatchPackageState.ERROR : BatchPackageState.LOADED);
        pkg = packageRepository.save(pkg);

        for (BatchPosting posting : postings) {
            posting.setPackageId(pkg.getId());
            postingRepository.save(posting);
        }
        return pkg;
    }

    public void checkFilialPermission(String filialDebit, String filialCredit, Long userId) throws Exception {
        try {
            postingProcessor.checkFilialPermission(filialDebit, filialCredit, userId);
        } catch (ValidationError e) {
            String msg = ErrorCode.PACKAGE_FILIAL_NOT_ALLOWED.getRawMessage();
            auditController.warning(BatchOperation, msg, null, e);
            throw new ParamsParserException(msg);
        }
    }

    public void checkBackvaluePermission(Date postDate, Long userId) throws Exception {
        try {
            postingProcessor.checkBackvaluePermission(postDate, userId);
        } catch (ValidationError e) {
            String msg = format("Недопустимая дата проводки: '%s'\n%s",
                    new SimpleDateFormat("dd.MM.yyyy").format(postDate), ValidationError.getErrorText(e.getMessage()));
            auditController.warning(BatchOperation, msg, null, e);
            throw new ParamsParserException(msg);
        }
    }

    public void checkDateEquals(Date postDate0, Date postDate) throws ParamsParserException {
        if (!postDate0.equals(postDate)) {
            throw new ParamsParserException("Пакет содержит проводки с разной датой");
        }
    }

    private boolean validatePosting(BatchPosting posting) {
        ManualOperationWrapper wrapper = postingProcessor.createOperationWrapper(posting);
        List<ValidationError> errors = postingProcessor.validate(wrapper, new ValidationContext());

        if (!errors.isEmpty()) {
            posting.setErrorCode(1);
            posting.setErrorMessage(postingProcessor.validationErrorMessage(errors, wrapper.getErrorList()));
            return false;
        }
        return true;
    }

    public BatchPosting createPosting(List<Object> rowParams, int row, String source, String department, List<String> errorList) throws ParamsParserException {
        if (null == rowParams || rowParams.isEmpty())
            return null;

        BatchPosting posting = new BatchPosting();
        posting.setPostDate(getValue(rowParams, row, 0, true, java.util.Date.class, errorList));
        posting.setDealId(getString(rowParams, row, 1, false, 20, errorList));
        posting.setSubDealId(getString(rowParams, row, 2, false, 20, errorList));
        posting.setPaymentRefernce(getString(rowParams, row, 3, false, 20, errorList));

        posting.setAccountDebit(getString(rowParams, row, 4, true, 20, true, errorList));
        posting.setCurrencyDebit(getCurrency(rowParams, row, 5, true, errorList));
        posting.setAmountDebit(getAmount(rowParams, row, 6, true, errorList));
        posting.setAccountCredit(getString(rowParams, row, 7, true, 20, true, errorList));
        posting.setCurrencyCredit(getCurrency(rowParams, row, 8, true, errorList));
        posting.setAmountCredit(getAmount(rowParams, row, 9, true, errorList));
        posting.setAmountRu(getAmount(rowParams, row, 10, false, errorList));

        posting.setNarrative(getString(rowParams, row, 11, false,  300, errorList));
        posting.setRusNarrativeLong(getString(rowParams, row, 12, true, 300, errorList));
        posting.setRusNarrativeShort(getString(rowParams, row, 13, false, 100, errorList));
        posting.setProfitCenter(getString(rowParams, row, 14, false, 4, errorList));
        posting.setIsCorrection(getYesNo(rowParams, row, 15, false, errorList));

        Date valueDate = getValue(rowParams, row, 16, false, java.util.Date.class, errorList);
        posting.setValueDate((null != valueDate) ? valueDate : posting.getPostDate());

        posting.setCreateTimestamp(new Date());

        // TODO передавать как параметры
        posting.setSourcePosting(source);
        posting.setDeptId(department);

        posting.setRowNumber(getRowNumber(row));  // TODO передавать?
        posting.setFilialDebit(postingProcessor.getFilial(posting.getAccountDebit()));
        posting.setFilialCredit(postingProcessor.getFilial(posting.getAccountCredit()));
        return posting;
    }

    private BatchPosting enrichmentPosting(BatchPosting posting, Date curdate, String userName, String filial) {
        posting.setStatus(BatchPostStatus.INPUT);
        posting.setInputMethod(InputMethod.F);
        posting.setInvisible(InvisibleType.N);
//        posting.setFilialDebit(postingProcessor.getFilial(posting.getAccountDebit()));
//        posting.setFilialCredit(postingProcessor.getFilial(posting.getAccountCredit()));

        // опердень создания
        posting.setProcDate(curdate);
        // текущее системное время
        posting.setCreateTimestamp(new Date());
        // создатель
        posting.setUserName(userName);
        posting.setUserFilial(filial);
        return posting;
    }

    private String getString(List<Object> rowParams, int row, int index, boolean notNull, int maxLength, boolean exact, List<String> errorList) throws ParamsParserException {
        return getValue(rowParams, row, index, notNull, String.class, maxLength, exact, errorList);
    }

    private String getString(List<Object> rowParams, int row, int index, boolean notNull, int maxLength, List<String> errorList) throws ParamsParserException {
//        return StringUtils.removeCtrlChars(getValue(rowParams, row, index, notNull, String.class, maxLength, false, errorList));
        return getValue(rowParams, row, index, notNull, String.class, maxLength, false, errorList);
    }

    private <T> T getValue(List<Object> rowParams, int row, int index, boolean notNull, Class<T> clazz, List<String> errorList) throws ParamsParserException {
        return getValue(rowParams, row, index, notNull, clazz, 0, false, errorList);
    }

    private <T> T getValue(List<Object> rowParams, int row, int index, boolean notNull, Class<T> clazz, int maxLength, boolean exact, List<String> errorList) throws ParamsParserException {
        if (rowParams.size() <= index) {
            return null;
        }
        Object param = rowParams.get(index);
        try {
            if (null == param) {
                if (notNull)
                    errorList.add(format("%s Не задано значение", getLocation(row, index)));
                return null;
            }
            String valueStr = param.toString().trim();
            if (isEmpty(valueStr)) {
                if (notNull)
                    errorList.add(format("%s Не задано значение", getLocation(row, index)));
                return null;
            }
            if (clazz.equals(String.class)) {
                if (exact && valueStr.length() != maxLength) {
                    errorList.add(format("%s Неверная длина строка (<>%d): '%s'",
                            getLocation(row, index), maxLength, rowParams.get(index)));
                    return (T) valueStr;
                }
                else if (valueStr.length() > maxLength) {
                    errorList.add(format("%s Слишком длинная строка (>%d): '%s'",
                            getLocation(row, index), maxLength, rowParams.get(index)));
                    return (T) valueStr.substring(0, maxLength);
                }
                else {  // (Double)param - (int)(double)(Double) param == 0
                    return (T) ((param instanceof Double && ((Double)param - (int)(double)(Double) param == 0)) ?
                            String.format("%.0f", (Double) param) : valueStr);
                }
            } else if (clazz.isAssignableFrom(param.getClass())) {
                return (T) param;
            } else if (clazz.equals(Date.class)) {
                Date value = DateUtils.parseDate(valueStr, "dd.MM.yyyy", "dd.MM.yy");
                return (T) value;
            } else if (clazz.equals(Double.class)) {
                Double value = Double.parseDouble(valueStr);
                return (T) value;
            } else if (clazz.equals(Long.class)) {
                Long value = Long.parseLong(valueStr);
                return (T) value;
            } else if (clazz.equals(Integer.class)) {
                Integer value = Integer.parseInt(valueStr);
                return (T) value;
            } else if (clazz.equals(Short.class)) {
                Short value = Short.parseShort(valueStr);
                return (T) value;
            } else {
                errorList.add(format("%s Неверный формат данных (надо '%s'): '%s'",
                        getLocation(row, index), getClassDescr(clazz), param));
                return null;
            }
        } catch (NumberFormatException e){
            errorList.add(format("%s Неверный формат данных (надо '%s'): '%s'",
                    getLocation(row, index), getClassDescr(clazz), param));
            return null;
        } catch (Throwable e){
            final String message = format("%s Ошибка при получении значения",
                    getLocation(row, index), e.getMessage());
            log.error(message, e);
            throw new ParamsParserException(message);
        }
    }

    private YesNo getYesNo(List<Object> rowParams, int row, int index, boolean notNull, List<String> errorList) throws ParamsParserException {
        String value = getString(rowParams, row, index, notNull, 3, errorList);
        if (isEmpty(value))
            return null;

        switch(value.toUpperCase().charAt(0)) {
            case '0':
            case 'N':
                return YesNo.N;
            case '1':
            case 'Y':
                return YesNo.Y;
            default:
                errorList.add(format("%s Неверное значение (допустимо Y/1 или N/0): '%s'",
                        getLocation(row, index), value));
                return null;
        }
    }

    private BigDecimal getAmount(List<Object> rowParams, int row, int index, boolean notNull, List<String> errorList) throws ParamsParserException {
        Double amount = getValue(rowParams, row, index, notNull,Double.class, errorList);
        return null == amount ? null : new BigDecimal(amount).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private BankCurrency getCurrency(List<Object> rowParams, int row, int index, boolean notNull, List<String> errorList) throws ParamsParserException {
        String ccy = getValue(rowParams, row, index, notNull,String.class, 3, true, errorList);
        if (!isEmpty(ccy)) {
            BankCurrency currency = bankCurrencyRepository.findById(BankCurrency.class, ccy);
            if (null == currency)
                errorList.add(format("%s Неверная валюта: '%s'", getLocation(row, index), ccy));
            return currency;
        } else
            return null;
    }

    private String getLocation(int row, int index) {
        return format("Cтрока '%d' столбец '%c' (%s): ",
                getRowNumber(row), getColumnChar(index), rowHeader.get(index));
    }

    private char getColumnChar(int index) {
        return (char)((int)'A' + index);
    }

    private int getRowNumber(int row) {
        return row + START_ROW;
    }

    private String getClassDescr(Class clazz) {
        String classDescr = clazz.getSimpleName();
        switch (classDescr) {
            case "String":
                classDescr = "строка";
                break;
            case "Double":
                classDescr = "числовое значение";
                break;
            case "Integer":
            case "Long":
                classDescr = "целое число";
                break;
        }
        return classDescr;
    }


}
