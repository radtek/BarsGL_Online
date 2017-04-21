package ru.rbt.ejbcore.validation;

import java.util.Arrays;
import java.util.stream.Collectors;
import ru.rbt.ejbcore.validation.ErrorCode;

import static java.lang.String.format;
import static ru.rbt.ejbcore.util.StringUtils.substr;

/**
 * Created by Ivan Sevastyanov
 */
public class ValidationError extends Error {

    private final ErrorCode code;
    private String message;
    private final String source;
    private String enityName;       // TODO добавиь в конструктор
    private String fieldName;

    public ValidationError(ErrorCode code, String ... params) {
        this.code = code;
        this.source = initSource();
        try {
            if (null != params) {
                this.message = format("Код ошибки '%s', сообщение '%s', источник %s\n"
                        , getCode().getErrorCode(), String.format(code.getRawMessage(), params), getSource());
            }
        } catch (Throwable e) {
            message = format("!ERROR! Код ошибки '%s', шаблон '%s', параметры: '%s', источник %s\n"
                    , getCode().getErrorCode(), code.getRawMessage()
                    , Arrays.asList(params).stream().collect(Collectors.joining(","))
                    , getSource());
        }
    }

    public ValidationError(String message, ErrorCode code, String ... params) {
        super(message);
        this.code = code;
        this.source = initSource();
        try {
            if (null != params) {
                this.message = format("Код ошибки '%s', сообщение '%s', источник %s\n"
                        , getCode().getErrorCode(), String.format(code.getRawMessage(), params), getSource());
            }
        } catch (Throwable e) {
            message = "!ERROR! Params: " + Arrays.asList(params).stream().collect(Collectors.joining(","));
        }
    }

    public ErrorCode getCode() {
        return code;
    }

    public String getSource() {
        return source;
    }

    public String getMessage() {
        return message;
    }

    public String getEnityName() {
        return enityName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public static String initSource() {
        return initSource(new Throwable());
    }

    public static String initSource(Throwable throwable) {
        for (final StackTraceElement el : throwable.getStackTrace()) {
            if (!el.getClassName().contains(ValidationError.class.getName())
                    && el.getClassName().contains("ru.rbt")
                    && 0 < el.getLineNumber()) {
                return el.getClassName() + ":" + el.getLineNumber();
            }
        }
        return "#UNDEFINED#" + ":" + 0;
    }

    public static String getErrorText(String errMessage) {
        return substr(errMessage, ", сообщение '", "', источник");
    }

    public static String getErrorCode(String errMessage) {
        return substr(errMessage, "Код ошибки '", "', сообщение ");
    }

}
