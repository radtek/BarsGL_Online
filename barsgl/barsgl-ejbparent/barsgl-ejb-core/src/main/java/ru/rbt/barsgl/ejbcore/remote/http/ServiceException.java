package ru.rbt.barsgl.ejbcore.remote.http;

/**
 * Сигнализирует об ошибке, связанной с работой инфраструктуры сервисов.<br/>
 * Обычно исключение типа {@link ServiceException} связано с ошибками вызова сервисов через прокси
 * и, с точки зрения кода конечного приложения, не является чем-то ожидаемым. Поэтому
 * {@link ServiceException} является runtime-исключением, хотя и имеет по сути то же значение,
 * что и {@link java.rmi.RemoteException} в распределенных приложениях.
 */
public class ServiceException extends RuntimeException {

    /**
     * Конструктор по умолчанию
     */
    public ServiceException() {
    }

    /**
     * Создает новый экземпляр {@link ServiceException} с заданным сообщением
     * @param message Сообщение об ошибке
     */
    public ServiceException(String message) {
        super(message);
    }

    /**
     * Создает новый экземпляр {@link ServiceException} с заданным сообщением
     * и внутренним исключением
     * @param message Сообщение об ошибке
     * @param cause Внутреннее исключение
     */
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Создает новый экземпляр {@link ServiceException} с заданным
     * внутренним исключением
     * @param cause Внутреннее исключение
     */
    public ServiceException(Throwable cause) {
        super(cause);
    }
}
