package ru.rbt.barsgl.ejbcore.remote.http;

/**
 * Интерфейс, описывающий функциональность по осуществлению удаленных вызовов служб.<br/>
 * Реализация должна обеспечивать обработку ошибок, характерных для поддерживаемого протокола
 * передачи данных (HTTP, RMI/IIOP, Hessian и т.п.), а также необходимую функциональность
 * по сериализации запросов и ответов
 */
public interface ServiceInvoker {

    /**
     * Осуществляет удаленный вызов сервиса
     * @param request Объект запроса
     * @return Объект ответа
     * @throws ServiceInvocationException Если возникли ошибки при передаче данных или сериализации запроса/ответа
     */
    public ServiceResponse invokeService(ServiceRequest request) throws ServiceInvocationException;

}
