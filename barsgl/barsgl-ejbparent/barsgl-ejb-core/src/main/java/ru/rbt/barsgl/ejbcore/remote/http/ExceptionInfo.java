package ru.rbt.barsgl.ejbcore.remote.http;

import java.io.*;
import java.lang.reflect.Constructor;

/**
 * Используется для передачи по сети информации об исключении
 */
public final class ExceptionInfo implements Serializable {

    public static final String THIS_EXCEPTION_WAS_THROWN_REMOTELY = "(THIS EXCEPTION WAS THROWN REMOTELY)";

    private String _class;
    private String _message;
    private byte[] _trace;
    private ExceptionInfo _cause;

    /**
     * Конструктор по умолчанию (нужен для сериализации)
     */
    public ExceptionInfo() {
    }

    /**
     * Создает новый экземпляр {@link ExceptionInfo}, который описывает
     * заданный объект типа {@link Throwable}
     * @param th Объект типа {@link Throwable}, который необходимо описать
     */
    public ExceptionInfo(Throwable th) {
        _class = th.getClass().getName();
        _message = th.getMessage();
        // В JDK 1.4 только сама VM умеет сериализовать объекты типа StackTraceElement,
        // поэтому для совместимости с другими сериализаиторами (например, Hessian)
        // храним стек в виде массива байт
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(th.getStackTrace());
            oos.flush();
            _trace = baos.toByteArray();
        } catch (IOException ignore) {
            _trace = null;
        } finally {
            if (oos != null) {
                try { oos.close(); } catch (Throwable ignore) {}
            }
        }
        if (th.getCause() != null) {
            _cause = new ExceptionInfo(th.getCause());
        }
    }

    /**
     * Возвращает имя класса исключения
     * @return Имя класса исключения
     */
    public String getClassName() {
        return _class;
    }

    /**
     * Возвращает сообщение исключения
     * @return Сообщенеи исключения
     */
    public String getMessage() {
        return _message;
    }

    /**
     * Возвращает стек вызова исключения
     * @return Стек вызова исключения
     */
    public StackTraceElement[] getStackTrace() {
        if (_trace == null) {
            return new StackTraceElement[0];
        }
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new ByteArrayInputStream(_trace));
            return (StackTraceElement[])ois.readObject();
        } catch (Exception ignore) {
            return new StackTraceElement[0];
        } finally {
            if (ois != null) {
                try { ois.close(); } catch (Throwable ignore) {}
            }
        }
    }

    /**
     * Возвращает объект внутреннего исключения в виде экземпляра {@link ExceptionInfo}
     * @return Экземпляр {@link ExceptionInfo}, описывающий внутреннее исключения
     */
    public ExceptionInfo getCause() {
        return _cause;
    }

    /**
     * Вытается воссоздать реальное исключение по его описанию.<br/>
     * Поскольку {@link ExceptionInfo} используется в основном для передачи информации об удаленных исключениях,
     * то в точности воссоздать удаленное исключение локально может и не получиться, поскольку в локальной
     * JVM может не оказаться подходящего класса. В этом случае создается объект типа {@link RemotelyThrownException},
     * с сообщением и стеком, соответствующим удаленно выброшенному исключению
     * @return Объект исключения, воссозданный по его описанию, хранящемуся в данном экземпляре {@link ExceptionInfo}
     */
    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    public Throwable createThrowable() {
        try {
            Class cls = Class.forName(_class);
            Constructor ctor;
            Throwable error;
            try {
                ctor = cls.getConstructor(new Class[] {String.class});
                error = (Throwable)ctor.newInstance(_message);
/*
                error = (Throwable)ctor.newInstance(
                        ((_message == null || "".equals(_message)) ? "" : _message + " ") +
                            THIS_EXCEPTION_WAS_THROWN_REMOTELY);
*/
            } catch (NoSuchMethodException ex) {
                ctor = cls.getConstructor();
                error = (Throwable)ctor.newInstance();
            }
            error.setStackTrace(getStackTrace());
            ExceptionInfo causeInfo = this.getCause();
            if (causeInfo != null) {
                error.initCause( causeInfo.createThrowable() );
            }
            return error;
        } catch (Throwable th) {
            return new RemotelyThrownException(this);
        }
    }

}
