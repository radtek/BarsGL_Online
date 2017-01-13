package ru.rbt.barsgl.ejbcore.remote.http;

/**
 * Сигнализирует об ошибке, связанной с осуществлением удаленного вызова службы (сетевые проблемы,
 * сериализация и т.п.)
 */
public class ServiceInvocationException extends ServiceException {

    /**
     * Конструктор по умолчанию
     */
    public ServiceInvocationException() {
    }

    /**
     * Создает новый экземпляр {@link ServiceInvocationException} с заданным сообщением
     * @param message Сообщение об ошибке
     */
    public ServiceInvocationException(String message) {
        super(message);
    }

    /**
     * Создает новый экземпляр {@link ServiceInvocationException} с заданным сообщением
     * и внутренним исключением
     * @param message Сообщение об ошибке
     * @param cause Внутреннее исключение
     */
    public ServiceInvocationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Создает новый экземпляр {@link ServiceInvocationException} с заданным
     * внутренним исключением
     * @param cause Внутреннее исключение
     */
    public ServiceInvocationException(Throwable cause) {
        super(cause);
    }

}
