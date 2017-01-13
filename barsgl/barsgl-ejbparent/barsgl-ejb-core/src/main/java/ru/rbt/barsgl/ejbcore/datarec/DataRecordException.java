package ru.rbt.barsgl.ejbcore.datarec;

/**
 * Created by Ivan Sevastyanov
 */
/**
 * ошибка при работе с DataRecord
 */
public class DataRecordException extends RuntimeException {

    public DataRecordException() {
        super();
    }

    public DataRecordException(String message) {
        super(message);
    }

    public DataRecordException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataRecordException(Throwable cause) {
        super(cause);
    }

    public DataRecordException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause);
    }
}
