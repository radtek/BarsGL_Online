package ru.rbt.barsgl.ejb.controller.excel;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import ru.rbt.barsgl.ejb.entity.dict.BankCurrency;
import ru.rbt.barsgl.ejb.repository.BankCurrencyRepository;
import ru.rbt.ejbcore.mapping.YesNo;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static ru.rb.ucb.util.StringUtils.isEmpty;

/**
 * Created by er18837 on 18.10.2018.
 */
public abstract class UploadProcessorBase {
    public static final Logger log = Logger.getLogger(UploadProcessorBase.class);

    private final SimpleDateFormat onlyDate = new SimpleDateFormat("dd.MM.yyyy");
    public static final String LIST_DELIMITER = "#";

    private long startLine = 0;
    private long columnCount = 0;

    protected List<Object> rowHeader = null;

    @Inject
    protected BankCurrencyRepository bankCurrencyRepository;

    protected abstract long getColumnCount();
    protected abstract long getStartLine();
    protected abstract long getMaxLines();

    protected boolean checkRowHeader(List<Object> headerRow, long maxLineNum) throws ParamsParserException {
        startLine = getStartLine();
        columnCount = getColumnCount();
        rowHeader = headerRow;
        if (rowHeader.isEmpty())
            return false;

        if (rowHeader.size() < columnCount) {
            throw new ParamsParserException(format("Неверное количество столбцов: %d, должно быть не менее %d", rowHeader.size(), columnCount));
        }
        long maxRows = startLine + getMaxLines();
        if (maxLineNum > maxRows) {
            throw new ParamsParserException(format("Нельзя загрузить файл размером больше %d строк", maxRows));
        }

        return true;
    }

    protected Object getParam(List<Object> rowParams, int row, int index, boolean notNull, List<String> errorList) {
        if (rowParams.size() <= index) {
            return null;
        }
        Object param = rowParams.get(index);
        if (null == param) {
            if (notNull)
                errorList.add(format("%s Не задано значение", getLocation(row, index)));
            return null;
        }
        else
            return param;
    }

    protected <T> T getValue(List<Object> rowParams, int row, int index, boolean notNull, Class<T> clazz, List<String> errorList) throws ParamsParserException {
        return getValue(rowParams, row, index, notNull, clazz, 0, false, errorList);
    }

    protected <T> T getValue(List<Object> rowParams, int row, int index, boolean notNull, Class<T> clazz, int maxLength, boolean exact, List<String> errorList) throws ParamsParserException {
/*
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
*/
        Object param = getParam(rowParams, row, index, notNull, errorList);
        return getValue(param, row, index, notNull, clazz, maxLength, exact, errorList);
    }

    protected <T> T getValue(Object param, int row, int index, boolean notNull, Class<T> clazz, int maxLength, boolean exact, List<String> errorList) throws ParamsParserException {
        if (null == param)
            return null;
        try {
            String valueStr = param.toString().trim();
            if (isEmpty(valueStr)) {
                if (notNull)
                    errorList.add(format("%s Не задано значение", getLocation(row, index)));
                return null;
            }
            if (clazz.equals(String.class)) {
                if (exact && valueStr.length() != maxLength) {
                    errorList.add(format("%s Неверная длина строка (<>%d): '%s'",
                            getLocation(row, index), maxLength, param));
                    return (T) valueStr;
                }
                else if (valueStr.length() > maxLength) {
                    errorList.add(format("%s Слишком длинная строка (>%d): '%s'",
                            getLocation(row, index), maxLength, param));
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

    protected String getString(List<Object> rowParams, int row, int index, boolean notNull, int maxLength, boolean exact, List<String> errorList) throws ParamsParserException {
        return getValue(rowParams, row, index, notNull, String.class, maxLength, exact, errorList);
    }

    protected String getString(List<Object> rowParams, int row, int index, boolean notNull, int maxLength, List<String> errorList) throws ParamsParserException {
//        return StringUtils.removeCtrlChars(getValue(rowParams, row, index, notNull, String.class, maxLength, false, errorList));
        return getValue(rowParams, row, index, notNull, String.class, maxLength, false, errorList);
    }

/*
    protected String get_NumberString(List<Object> rowParams, int row, int index, boolean notNull, int maxLength, boolean exact, List<String> errorList) throws ParamsParserException {
        String valueStr = getString(rowParams, row, index, notNull, maxLength, exact, errorList);
        try {
            Long.parseLong(valueStr);
            return valueStr;
        } catch (NumberFormatException e) {
            errorList.add(format("%s Неверный формат данных (надо число длиной %s%d символов): '%s'",
                    getLocation(row, index), exact ? "" : " <= ", maxLength, valueStr));
            return null;
        }
    }
*/

    protected String getLongAsString(List<Object> rowParams, int row, int index, boolean notNull, int maxLength, boolean exact, List<String> errorList) throws ParamsParserException {
        Object param_ex = getParam(rowParams, row, index, notNull, errorList);
        if (null == param_ex)
            return null;
        Class clazz = param_ex.getClass();
        if (clazz.equals(String.class)) {
            String param = (String) param_ex;
            String valueStr = getValue(param, row, index, notNull, String.class, maxLength, exact, errorList);
            if (null == valueStr)
                return null;
            try {
                Long.parseLong(valueStr);
                return valueStr;
            } catch (NumberFormatException e) {
                errorList.add(format("%s Неверный формат данных (надо число длиной %s%d символов): '%s'",
                        getLocation(row, index), exact ? "" : " <= ", maxLength, valueStr));
                return null;
            }
        } else {
            Long param = null;
            Double delta = 0.0;
            if (Long.class.isAssignableFrom(clazz)) {
                param = (Long) param_ex;
//            } else if (clazz.equals(Long.class) || clazz.equals(Integer.class) || clazz.equals(Short.class)) {
//                param = (Long) param_ex;
            } else if (clazz.equals(Double.class)) {
                param = ((Double) param_ex).longValue();
                delta = (Double) param_ex - param;
            } else if (clazz.equals(Float.class)) {
                param = ((Float) param_ex).longValue();
                delta = ((Float) param_ex).doubleValue() - param;
            } else {
                errorList.add(format("%s Неверный формат данных (надо '%s'): '%s'",
                        getLocation(row, index), getClassDescr(Long.class), param_ex));
                return null;
            }
            if (delta != 0) {
                errorList.add(format("%s Неверный формат данных (надо '%s'): '%s'",
                        getLocation(row, index), getClassDescr(Long.class), param_ex));
                return null;
            }
            return getValue(param, row, index, notNull, String.class, maxLength, exact, errorList);
        }
    }

    protected YesNo getYesNo(List<Object> rowParams, int row, int index, boolean notNull, List<String> errorList) throws ParamsParserException {
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

    protected BigDecimal getAmount(List<Object> rowParams, int row, int index, boolean notNull, List<String> errorList) throws ParamsParserException {
        Double amount = getValue(rowParams, row, index, notNull,Double.class, errorList);
        return null == amount ? null : new BigDecimal(amount).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    protected BankCurrency getCurrency(List<Object> rowParams, int row, int index, boolean notNull, List<String> errorList) throws ParamsParserException {
        String ccy = getValue(rowParams, row, index, notNull,String.class, 3, true, errorList);
        if (!isEmpty(ccy)) {
            BankCurrency currency = bankCurrencyRepository.findById(BankCurrency.class, ccy);
            if (null == currency)
                errorList.add(format("%s Неверная валюта: '%s'", getLocation(row, index), ccy));
            return currency;
        } else
            return null;
    }

    protected Date getDate(List<Object> rowParams, int row, int index, boolean notNull, Date fromDate, Date toDate, List<String> errorList) throws ParamsParserException {
        Date date = getValue(rowParams, row, index, notNull, java.util.Date.class, errorList);
        if (null != fromDate && null != date && date.before(fromDate)) {
            errorList.add(format("%s Неверная дата: '%s', должна быть >= '%s'", getLocation(row, index), onlyDate.format(date), onlyDate.format(fromDate)));
        }
        else if (null != toDate && null != date && date.after(toDate)) {
            errorList.add(format("%s Неверная дата: '%s', должна быть <= '%s'", getLocation(row, index), onlyDate.format(date), onlyDate.format(toDate)));
        }
        return date;
    }

    protected String getLocation(int row, int index) {
        return format("Cтрока '%d' столбец '%c' (%s): ",
                getRowNumber(row), getColumnChar(index), rowHeader.get(index));
    }

    protected char getColumnChar(int index) {
        return (char)((int)'A' + index);
    }

    protected long getRowNumber(int row) {
        return row + startLine;
    }

    protected String getClassDescr(Class clazz) {
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
